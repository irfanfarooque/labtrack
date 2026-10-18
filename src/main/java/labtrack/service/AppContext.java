package labtrack.service;
import labtrack.dao.BorrowerDao;
import labtrack.dao.EquipmentDao;
import labtrack.dao.IssueDao;
import labtrack.dao.MaintenanceDao;
import labtrack.dao.UserDao;
import labtrack.database.DatabaseManager;
/** Wires the DAOs and services together (manual dependency injection). */
public class AppContext {
    private final EquipmentService equipmentService;
    private final IssueService issueService;
    private final MaintenanceService maintenanceService;
    private final ReportService reportService;
    private final AuthService authService;
    public AppContext() {
        DatabaseManager db = DatabaseManager.getInstance();
        EquipmentDao equipmentDao = new EquipmentDao(db);
        BorrowerDao borrowerDao = new BorrowerDao(db);
        IssueDao issueDao = new IssueDao(db);
        MaintenanceDao maintenanceDao = new MaintenanceDao(db);
        equipmentService = new EquipmentService(db, equipmentDao, issueDao, maintenanceDao);
        issueService = new IssueService(db, equipmentDao, borrowerDao, issueDao);
        maintenanceService = new MaintenanceService(db, equipmentDao, maintenanceDao);
        reportService = new ReportService(db, equipmentDao, issueDao, maintenanceDao);
        authService = new AuthService(db, new UserDao(db));
    }
    public EquipmentService equipment() { return equipmentService; }
    public IssueService issues() { return issueService; }
    public MaintenanceService maintenance() { return maintenanceService; }
    public ReportService reports() { return reportService; }
    public AuthService auth() { return authService; }
}
