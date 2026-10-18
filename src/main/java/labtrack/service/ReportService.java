package labtrack.service;
import labtrack.dao.EquipmentDao;
import labtrack.dao.IssueDao;
import labtrack.dao.MaintenanceDao;
import labtrack.dao.ReportDao;
import labtrack.database.DatabaseManager;
import labtrack.model.Equipment;
import labtrack.model.IssueRecord;
import labtrack.model.MaintenanceRecord;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
/** Dashboard numbers and the catalogue of report queries shown on the reports screen. */
public class ReportService {
/** One selectable report: display name, column headers and the live query behind it. */
    public record ReportSpec(String name, String[] columns, Rows loader) {}
    @FunctionalInterface
    public interface Rows { List<String[]> load() throws SQLException; }
    private final ReportDao reportDao;
    private final EquipmentDao equipmentDao;
    private final IssueDao issueDao;
    private final MaintenanceDao maintenanceDao;
    public ReportService(DatabaseManager db, EquipmentDao equipmentDao, IssueDao issueDao, MaintenanceDao maintenanceDao) {
        this.equipmentDao = equipmentDao; this.issueDao = issueDao; this.maintenanceDao = maintenanceDao;
        this.reportDao = new ReportDao(db);
    }
    public Map<String, Integer> dashboardStats() throws SQLException { return reportDao.dashboardStats(); }
    public List<String[]> recentActivity() throws SQLException { return reportDao.recentActivity(9); }
    public List<ReportSpec> reportSpecs() {
        return List.of(
            new ReportSpec("Complete Inventory",
                new String[]{"Asset Tag", "Name", "Category", "Type", "Serial", "Manufacturer", "Location", "Purchase Date", "Condition", "Status"},
                () -> inventory().stream().map(this::equipmentRow).toList()),
            new ReportSpec("Equipment by Category & Condition",
                new String[]{"Category", "Total", "Excellent", "Good", "Fair", "Poor", "Damaged"},
                reportDao::categoryConditionSummary),
            new ReportSpec("Items per Category", new String[]{"Category", "Items"}, reportDao::categorySummary),
            new ReportSpec("Available vs Issued (by Status)", new String[]{"Status", "Count"}, reportDao::statusSummary),
            new ReportSpec("Active Borrowing Records",
                new String[]{"ID", "Equipment", "Borrower", "Issued", "Due", "Status"},
                () -> issueRows(activeIssues())),
            new ReportSpec("Overdue Returns",
                new String[]{"ID", "Equipment", "Borrower", "Issued", "Due", "Days Overdue"},
                () -> overdueIssues().stream().map(r -> new String[]{String.valueOf(r.getId()), r.getEquipmentLabel(),
                        r.getBorrowerLabel(), r.getIssuedDate().toString(), r.getDueDate().toString(),
                        String.valueOf(r.overdueDays())}).toList()),
            new ReportSpec("Damaged Equipment",
                new String[]{"Asset Tag", "Name", "Category", "Condition", "Location", "Notes"},
                () -> damagedEquipment().stream().map(e -> new String[]{e.getCode(), e.getName(),
                        e.getCategory() == null ? "" : e.getCategory().getName(), e.getCondition().label(),
                        nullSafe(e.getLocation()), nullSafe(e.getNotes())}).toList()),
            new ReportSpec("Pending Maintenance",
                new String[]{"ID", "Equipment", "Type", "Description", "Reported", "Reported By", "Status"},
                () -> maintenanceRows(pendingMaintenance())),
            new ReportSpec("Completed Maintenance",
                new String[]{"ID", "Equipment", "Type", "Description", "Reported", "Resolved", "Resolution"},
                () -> maintenanceDao.find("COMPLETED", "MAINTENANCE", null).stream()
                        .map(r -> new String[]{String.valueOf(r.getId()), r.getEquipmentLabel(), r.getType().label(),
                                r.getDescription(), str(r.getReportedDate()), str(r.getResolvedDate()),
                                nullSafe(r.getResolutionNotes())}).toList()),
            new ReportSpec("Equipment Borrowing History",
                new String[]{"ID", "Equipment", "Borrower", "Issued", "Due", "Returned", "Condition", "Status"},
                () -> borrowingHistory().stream().map(r -> new String[]{String.valueOf(r.getId()), r.getEquipmentLabel(),
                        r.getBorrowerLabel(), r.getIssuedDate().toString(), r.getDueDate().toString(),
                        str(r.getReturnedDate()), r.getReturnCondition() == null ? "" : r.getReturnCondition().label(),
                        r.getStatus().label()}).toList()));
    }
    public List<Equipment> inventory() throws SQLException { return equipmentDao.search(null, 0, null, null); }
    public List<IssueRecord> activeIssues() throws SQLException { return issueDao.find("ACTIVE", null); }
    public List<IssueRecord> overdueIssues() throws SQLException { return issueDao.findOverdue(); }
    public List<IssueRecord> borrowingHistory() throws SQLException { return issueDao.find(null, null); }
    public List<Equipment> damagedEquipment() throws SQLException { return equipmentDao.search(null, 0, "DAMAGED", null); }
    public List<MaintenanceRecord> pendingMaintenance() throws SQLException { return maintenanceDao.find("OPEN", "MAINTENANCE", null); }
    private List<String[]> issueRows(List<IssueRecord> records) {
        return records.stream().map(r -> new String[]{String.valueOf(r.getId()), r.getEquipmentLabel(),
                r.getBorrowerLabel(), r.getIssuedDate().toString(), r.getDueDate().toString(), r.getStatus().label()}).toList();
    }
    private List<String[]> maintenanceRows(List<MaintenanceRecord> records) {
        return records.stream().map(r -> new String[]{String.valueOf(r.getId()), r.getEquipmentLabel(),
                r.getType().label(), r.getDescription(), str(r.getReportedDate()), nullSafe(r.getReportedBy()),
                r.getStatus().label()}).toList();
    }
    private String[] equipmentRow(Equipment e) {
        return new String[]{e.getCode(), e.getName(), e.getCategory() == null ? "" : e.getCategory().getName(),
                e.equipmentType(), nullSafe(e.getSerialNumber()), nullSafe(e.getManufacturer()), nullSafe(e.getLocation()),
                str(e.getPurchaseDate()), e.getCondition().label(), e.getStatus().label()};
    }
    private static String str(java.time.LocalDate d) { return d == null ? "" : d.toString(); }
    private static String nullSafe(String s) { return s == null ? "" : s; }
}
