package labtrack.ui.panels;
import labtrack.model.Equipment;
import labtrack.model.EquipmentStatus;
import labtrack.model.MaintenanceRecord;
import labtrack.model.MaintenanceStatus;
import labtrack.model.MaintenanceType;
import labtrack.service.AppContext;
import labtrack.ui.MainFrame;
import labtrack.ui.dialogs.CompleteMaintenanceDialog;
import labtrack.ui.dialogs.MaintenanceDialog;
import labtrack.util.Tasks;
import labtrack.util.UiUtil;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
/** Damage reports and maintenance work: open records (damage or maintenance) and completed history. */
public class MaintenancePanel extends JPanel implements MainFrame.Refreshable {
    private final AppContext ctx;
    private final JTextField searchField = UiUtil.field("Search equipment or description…");
    private final DefaultTableModel openModel = UiUtil.tableModel("ID", "Equipment", "Type", "Description", "Reported", "Reported By", "Status");
    private final DefaultTableModel doneModel = UiUtil.tableModel("ID", "Equipment", "Type", "Description", "Reported", "Resolved", "Resolution", "Status");
    private final JTable openTable = UiUtil.table(openModel), doneTable = UiUtil.table(doneModel);
    private final JTabbedPane tabs = new JTabbedPane();
    private List<MaintenanceRecord> openRows = new ArrayList<>();
    public MaintenancePanel(AppContext ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        searchField.setColumns(22);
        searchField.addActionListener(e -> refresh());
        add(UiUtil.bar(searchField,
                UiUtil.btn("Report Damage…", UiUtil.Kind.DANGER, () -> openRecord(MaintenanceType.DAMAGE)),
                UiUtil.btn("Request Maintenance…", UiUtil.Kind.PRIMARY, () -> openRecord(MaintenanceType.MAINTENANCE)),
                UiUtil.btn("Start Work", UiUtil.Kind.SECONDARY, this::startWork),
                UiUtil.btn("Complete…", UiUtil.Kind.SUCCESS, this::completeRecord),
                UiUtil.btn("Refresh", UiUtil.Kind.SECONDARY, this::refresh)), BorderLayout.NORTH);
        tabs.addTab("Pending Work", wrap(openTable, "No pending damage or maintenance records."));
        tabs.addTab("Completed", wrap(doneTable, "Nothing has been completed yet."));
        add(tabs, BorderLayout.CENTER);
    }
    private JPanel wrap(JTable table, String emptyMessage) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UiUtil.BORDER));
        JPanel p = new JPanel(new BorderLayout());
        p.add(scroll, BorderLayout.CENTER);
        p.add(UiUtil.muted(emptyMessage), BorderLayout.SOUTH);
        return p;
    }
    @Override
    public void refresh() {
        Tasks.run(this, () -> new Object[]{ctx.maintenance().list("OPEN", null, searchField.getText()),
                ctx.maintenance().list("COMPLETED", null, searchField.getText())}, data -> {
            @SuppressWarnings("unchecked")
            List<MaintenanceRecord> open = (List<MaintenanceRecord>) data[0];
            @SuppressWarnings("unchecked")
            List<MaintenanceRecord> done = (List<MaintenanceRecord>) data[1];
            openRows = open;
            fill(openModel, open, false);
            fill(doneModel, done, true);
        });
    }
    private void fill(DefaultTableModel model, List<MaintenanceRecord> rows, boolean completed) {
        model.setRowCount(0);
        for (MaintenanceRecord r : rows) {
            if (completed)
                model.addRow(new Object[]{r.getId(), r.getEquipmentLabel(), r.getType().label(), r.getDescription(), r.getReportedDate(),
                        r.getResolvedDate() == null ? "" : r.getResolvedDate(), r.getResolutionNotes() == null ? "" : r.getResolutionNotes(), r.getStatus().label()});
            else
                model.addRow(new Object[]{r.getId(), r.getEquipmentLabel(), r.getType().label(), r.getDescription(),
                        r.getReportedDate(), r.getReportedBy(), r.getStatus().label()});
        }
    }
    private MaintenanceRecord selectedOpen() {
        int viewRow = openTable.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = openTable.convertRowIndexToModel(viewRow);
        return modelRow < openRows.size() ? openRows.get(modelRow) : null;
    }
    private void openRecord(MaintenanceType type) {
        boolean damage = type == MaintenanceType.DAMAGE;
        List<Equipment> movable;
        try {
            movable = ctx.equipment().list(null, 0, null, null, false).stream()
                    .filter(e -> e.getStatus() != EquipmentStatus.ISSUED && !e.isArchived()).toList();
        } catch (Exception e) {
            UiUtil.showError(this, "Cannot load equipment: " + e.getMessage());
            return;
        }
        if (movable.isEmpty()) { UiUtil.info(this, "No equipment is available to attach a record to."); return; }
        MaintenanceDialog dialog = new MaintenanceDialog(UiUtil.owner(this), damage ? "Report Damage" : "Request Maintenance", movable);
        if (!dialog.open()) return;
        MaintenanceDialog.Request req = dialog.getResult();
        Equipment item = req.item();
        Tasks.runVoid(this, () -> {
            if (damage) ctx.maintenance().reportDamage(item.getId(), req.description(), req.reportedBy());
            else ctx.maintenance().requestMaintenance(item.getId(), req.description(), req.reportedBy());
        }, () -> {
            UiUtil.info(this, damage ? "Damage reported. The item is now locked until it is repaired."
                    : "Maintenance requested. The item is now locked until work completes.");
            refresh();
        });
    }
    private void startWork() {
        MaintenanceRecord record = selectedOpen();
        if (record == null) { UiUtil.info(this, "Select a pending record first."); return; }
        if (record.getStatus() != MaintenanceStatus.PENDING) { UiUtil.info(this, "This record is already in progress."); return; }
        Tasks.runVoid(this, () -> ctx.maintenance().updateStatus(record.getId(), MaintenanceStatus.IN_PROGRESS),
                () -> { UiUtil.info(this, "Record marked as in progress."); refresh(); });
    }
    private void completeRecord() {
        MaintenanceRecord record = selectedOpen();
        if (record == null) { UiUtil.info(this, "Select a pending record first."); return; }
        CompleteMaintenanceDialog dialog = new CompleteMaintenanceDialog(UiUtil.owner(this), record);
        if (!dialog.open()) return;
        CompleteMaintenanceDialog.Request req = dialog.getResult();
        Tasks.runVoid(this, () -> ctx.maintenance().complete(record.getId(), req.resolutionNotes(), req.newCondition()), () -> {
            try {
                UiUtil.info(this, "Record completed. Equipment status is now: " + ctx.equipment().get(record.getEquipmentId()).getStatus().label() + ".");
            } catch (Exception e) {
                UiUtil.info(this, "Record completed.");
            }
            refresh();
        });
    }
}
