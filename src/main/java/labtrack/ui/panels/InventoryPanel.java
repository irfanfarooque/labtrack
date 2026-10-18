package labtrack.ui.panels;
import labtrack.exception.LabTrackException;
import labtrack.model.Category;
import labtrack.model.ComputerEquipment;
import labtrack.model.Condition;
import labtrack.model.Equipment;
import labtrack.model.EquipmentStatus;
import labtrack.service.AppContext;
import labtrack.ui.MainFrame;
import labtrack.ui.dialogs.EquipmentDialog;
import labtrack.util.Tasks;
import labtrack.util.UiUtil;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
/** Full CRUD screen for the equipment inventory: search, filters, double-click to view. */
public class InventoryPanel extends JPanel implements MainFrame.Refreshable {
    private final AppContext ctx;
    private final JTextField searchField = UiUtil.field("Search name, asset tag, serial, manufacturer…");
    private final JComboBox<Category> categoryFilter = new JComboBox<>();
    private final JComboBox<String> conditionFilter = new JComboBox<>(new String[]{"Any condition", "Excellent", "Good", "Fair", "Poor", "Damaged"});
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"Any status", "Available", "Issued", "Damaged", "Maintenance", "Archived"});
    private final JCheckBox includeArchived = new JCheckBox("Include archived");
    private final DefaultTableModel model = UiUtil.tableModel("Asset Tag", "Name", "Category", "Type", "Serial", "Manufacturer", "Location", "Condition", "Status");
    private final JTable table = UiUtil.table(model);
    private List<Equipment> rows = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private boolean reloadingFilter;
    public InventoryPanel(AppContext ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        searchField.setColumns(24);
        searchField.addActionListener(e -> refresh());
        categoryFilter.addActionListener(e -> { if (!reloadingFilter) refresh(); });
        conditionFilter.addActionListener(e -> refresh());
        statusFilter.addActionListener(e -> refresh());
        includeArchived.addActionListener(e -> refresh());
        JPanel actions = UiUtil.bar(
                UiUtil.btn("+ Add Equipment", UiUtil.Kind.PRIMARY, this::addEquipment),
                UiUtil.btn("Edit Selected", UiUtil.Kind.SECONDARY, this::editSelected),
                UiUtil.btn("Archive / Restore", UiUtil.Kind.SECONDARY, this::archiveSelected),
                UiUtil.btn("Delete", UiUtil.Kind.DANGER, this::deleteSelected),
                UiUtil.btn("+ Category", UiUtil.Kind.SECONDARY, this::addCategory),
                UiUtil.btn("Refresh", UiUtil.Kind.SECONDARY, this::refresh));
        JPanel toolbar = new JPanel(new BorderLayout(0, 10));
        toolbar.setOpaque(false);
        toolbar.add(UiUtil.bar(searchField, categoryFilter, conditionFilter, statusFilter, includeArchived), BorderLayout.NORTH);
        toolbar.add(actions, BorderLayout.SOUTH);
        add(toolbar, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UiUtil.BORDER));
        add(scroll, BorderLayout.CENTER);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && selected() != null) new EquipmentDialog(UiUtil.owner(InventoryPanel.this), categories, selected(), false).open();
            }
        });
    }
    @Override
    public void refresh() {
        Tasks.run(this, () -> {
            List<Category> cats = ctx.equipment().categories();
            Category selected = (Category) categoryFilter.getSelectedItem();
            int categoryId = selected == null ? 0 : selected.getId();
            return new Object[]{cats, ctx.equipment().list(searchField.getText(), categoryId, selectedCondition(), selectedStatus(), includeArchived.isSelected())};
        }, data -> {
            @SuppressWarnings("unchecked")
            List<Category> cats = (List<Category>) data[0];
            @SuppressWarnings("unchecked")
            List<Equipment> items = (List<Equipment>) data[1];
            categories = cats;
            reloadCategoryFilter();
            rows = items;
            model.setRowCount(0);
            for (Equipment e : rows)
                model.addRow(new Object[]{e.getCode(), e.getName(), e.getCategory() == null ? "" : e.getCategory().getName(), e.equipmentType(),
                        e.getSerialNumber(), e.getManufacturer(), e.getLocation(), e.getCondition().label(), e.getStatus().label()});
        });
    }
    private void reloadCategoryFilter() {
        reloadingFilter = true;
        try {
            Category selected = (Category) categoryFilter.getSelectedItem();
            int selectedId = selected == null ? 0 : selected.getId();
            categoryFilter.removeAllItems();
            categoryFilter.addItem(new Category(0, "All categories"));
            for (Category c : categories) categoryFilter.addItem(c);
            for (int i = 0; i < categoryFilter.getItemCount(); i++)
                if (categoryFilter.getItemAt(i).getId() == selectedId) categoryFilter.setSelectedIndex(i);
        } finally {
            reloadingFilter = false;
        }
    }
    private Equipment selected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        return modelRow < rows.size() ? rows.get(modelRow) : null;
    }
    private void addEquipment() {
        if (categories.isEmpty()) { UiUtil.showError(this, "Add a category first."); return; }
        EquipmentDialog dialog = new EquipmentDialog(UiUtil.owner(this), categories, null, true);
        if (!dialog.open()) return;
        Equipment entered = dialog.getResult();
        Tasks.runVoid(this, () -> ctx.equipment().create(entered.getCode(), entered.getName(), entered instanceof ComputerEquipment,
                entered.getCategory().getId(), entered.getSerialNumber(), entered.getManufacturer(), entered.getLocation(),
                entered.getPurchaseDate(), entered.getCondition(), entered.getNotes()),
                () -> { refresh(); UiUtil.info(this, "Equipment added successfully."); });
    }
    private void editSelected() {
        Equipment item = selected();
        if (item == null) { UiUtil.info(this, "Select an item to edit."); return; }
        if (item.isArchived()) { UiUtil.showError(this, "Restore this item before editing it."); return; }
        EquipmentDialog dialog = new EquipmentDialog(UiUtil.owner(this), categories, item, true);
        if (!dialog.open()) return;
        Equipment entered = dialog.getResult();
        item.setName(entered.getName());
        item.setCategory(entered.getCategory());
        item.setSerialNumber(entered.getSerialNumber());
        item.setManufacturer(entered.getManufacturer());
        item.setLocation(entered.getLocation());
        item.setPurchaseDate(entered.getPurchaseDate());
        item.setCondition(entered.getCondition());
        item.setNotes(entered.getNotes());
        Tasks.runVoid(this, () -> ctx.equipment().update(item), () -> { refresh(); UiUtil.info(this, "Equipment updated."); });
    }
    private void archiveSelected() {
        Equipment item = selected();
        if (item == null) { UiUtil.info(this, "Select an item first."); return; }
        if (item.isArchived()) {
            Tasks.runVoid(this, () -> ctx.equipment().restore(item.getId()), () -> { UiUtil.info(this, "Item restored to active inventory."); refresh(); });
            return;
        }
        if (!UiUtil.confirm(this, "Archive '" + item.displayLabel() + "'? It stays in the database with all its history but cannot be lent.", "Archive equipment")) return;
        Tasks.runVoid(this, () -> ctx.equipment().archive(item.getId()), () -> { UiUtil.info(this, "Item archived."); refresh(); });
    }
    private void deleteSelected() {
        Equipment item = selected();
        if (item == null) { UiUtil.info(this, "Select an item first."); return; }
        if (!UiUtil.confirm(this, "PERMANENTLY delete '" + item.displayLabel() + "'?\nThis cannot be undone.", "Delete equipment")) return;
        Tasks.runVoid(this, () -> ctx.equipment().delete(item.getId()), () -> { UiUtil.info(this, "Item deleted."); refresh(); });
    }
    private void addCategory() {
        String name = JOptionPane.showInputDialog(this, "New category name:", "Add Category", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        try {
            ctx.equipment().addCategory(name);
            UiUtil.info(this, "Category '" + name.trim() + "' added.");
            refresh();
        } catch (LabTrackException ex) {
            UiUtil.showError(this, ex.getMessage());
        }
    }
    private Condition selectedCondition() {
        Object v = conditionFilter.getSelectedItem();
        return v == null || "Any condition".equals(v) ? null : Condition.from(v.toString());
    }
    private EquipmentStatus selectedStatus() {
        Object v = statusFilter.getSelectedItem();
        return v == null || "Any status".equals(v) ? null : EquipmentStatus.from(v.toString());
    }
}
