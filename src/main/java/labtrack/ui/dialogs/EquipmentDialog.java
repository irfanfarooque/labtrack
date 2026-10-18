package labtrack.ui.dialogs;
import labtrack.model.Category;
import labtrack.model.ComputerEquipment;
import labtrack.model.Condition;
import labtrack.model.ElectronicEquipment;
import labtrack.model.Equipment;
import labtrack.model.EquipmentStatus;
import labtrack.util.Dates;
import labtrack.util.UiUtil;
import javax.swing.*;
import java.awt.Frame;
import java.time.LocalDate;
import java.util.List;
/** Add / edit / view one piece of equipment. Validates input before closing. */
public class EquipmentDialog extends FormDialog {
    private final JTextField codeField = UiUtil.field("Asset tag, e.g. AST-0042");
    private final JTextField nameField = UiUtil.field("Equipment name");
    private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Electronic Equipment", "Computer Equipment"});
    private final JComboBox<Category> categoryCombo = new JComboBox<>();
    private final JTextField serialField = UiUtil.field("Serial number (optional)");
    private final JTextField manufacturerField = UiUtil.field("Manufacturer");
    private final JTextField locationField = UiUtil.field("Room / cabinet / bench");
    private final JTextField dateField = UiUtil.field("yyyy-MM-dd");
    private final JComboBox<Condition> conditionCombo = new JComboBox<>(Condition.values());
    private final JTextArea notesArea = new JTextArea(3, 24);
    private final boolean editable;
    private Equipment result;
    public EquipmentDialog(Frame owner, List<Category> categories, Equipment editing, boolean editable) {
        super(owner, editing == null ? "Add Equipment" : (editable ? "Edit Equipment" : "Equipment Details"),
                editing == null ? "Register a new item in the inventory."
                        : (editable ? "Change the details of this item." : "Read-only view of this item."));
        this.editable = editable;
        categoryCombo.addItem(new Category(0, "— select category —"));
        categories.forEach(categoryCombo::addItem);
        addRow("Asset tag *", codeField);
        addRow("Name *", nameField);
        addRow("Type", typeCombo);
        addRow("Category *", categoryCombo);
        addRow("Serial number", serialField);
        addRow("Manufacturer", manufacturerField);
        addRow("Location", locationField);
        addRow("Purchase date", dateField);
        addRow("Condition", conditionCombo);
        addRow("Notes", new JScrollPane(notesArea));
        if (editing != null) fill(editing);
        if (!editable) setInputsEnabled(false);
    }
/** Shows the dialog; true when saved (or closed in view mode). */
    public boolean open() {
        return open(editable ? "Save" : "Close", editable ? UiUtil.Kind.PRIMARY : UiUtil.Kind.SECONDARY, () -> {
            if (editable) build();
            return true;
        });
    }
    public Equipment getResult() { return result; }
    private void fill(Equipment e) {
        codeField.setText(e.getCode());
        nameField.setText(e.getName());
        typeCombo.setSelectedItem(e.equipmentType());
        categoryCombo.setSelectedItem(e.getCategory());
        serialField.setText(e.getSerialNumber());
        manufacturerField.setText(e.getManufacturer());
        locationField.setText(e.getLocation());
        dateField.setText(Dates.format(e.getPurchaseDate()));
        conditionCombo.setSelectedItem(e.getCondition());
        notesArea.setText(e.getNotes());
    }
    private void build() throws Exception {
        LocalDate purchase = Dates.parse(dateField.getText());
        Category category = (Category) categoryCombo.getSelectedItem();
        String code = codeField.getText().trim(), name = nameField.getText().trim();
        String serial = serialField.getText().trim(), notes = notesArea.getText().trim();
        Condition condition = (Condition) conditionCombo.getSelectedItem();
        EquipmentStatus status = EquipmentStatus.AVAILABLE;
        result = "Computer Equipment".equals(typeCombo.getSelectedItem())
                ? new ComputerEquipment(0, code, name, category, serial, manufacturerField.getText().trim(),
                        locationField.getText().trim(), purchase, condition, status, notes)
                : new ElectronicEquipment(0, code, name, category, serial, manufacturerField.getText().trim(),
                        locationField.getText().trim(), purchase, condition, status, notes);
    }
}
