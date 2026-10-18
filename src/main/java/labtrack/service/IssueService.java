package labtrack.service;
import labtrack.dao.BorrowerDao;
import labtrack.dao.EquipmentDao;
import labtrack.dao.IssueDao;
import labtrack.dao.MaintenanceDao;
import labtrack.database.DatabaseManager;
import labtrack.exception.LabTrackException;
import labtrack.exception.OperationException;
import labtrack.exception.ValidationException;
import labtrack.model.Borrower;
import labtrack.model.Condition;
import labtrack.model.Equipment;
import labtrack.model.EquipmentStatus;
import labtrack.model.IssueRecord;
import labtrack.model.IssueStatus;
import labtrack.model.MaintenanceRecord;
import labtrack.model.MaintenanceStatus;
import labtrack.model.MaintenanceType;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
/** Borrowing workflow; every multi-record update runs inside one database transaction. */
public class IssueService {
    private final DatabaseManager db;
    private final EquipmentDao equipmentDao;
    private final BorrowerDao borrowerDao;
    private final IssueDao issueDao;
    public IssueService(DatabaseManager db, EquipmentDao equipmentDao, BorrowerDao borrowerDao, IssueDao issueDao) {
        this.db = db; this.equipmentDao = equipmentDao; this.borrowerDao = borrowerDao; this.issueDao = issueDao;
    }
    public List<IssueRecord> list(String statusFilter, String text) throws SQLException { return issueDao.find(statusFilter, text); }
    public List<IssueRecord> overdue() throws SQLException { return issueDao.findOverdue(); }
/** Issues one item; finds the borrower by staff/student ID or registers a new one, then locks the item. */
    public IssueRecord issue(int equipmentId, String borrowerName, String borrowerCode, String role,
                             String email, String phone, LocalDate issuedDate, LocalDate dueDate, String remarks) throws LabTrackException {
        if (borrowerName == null || borrowerName.isBlank()) throw new ValidationException("Borrower name is required.");
        if (borrowerCode == null || borrowerCode.isBlank()) throw new ValidationException("Borrower ID (registration number) is required.");
        if (issuedDate == null || dueDate == null) throw new ValidationException("Issue date and due date are required.");
        if (dueDate.isBefore(issuedDate)) throw new ValidationException("Due date cannot be before the issue date.");
        try {
            return db.inTransaction(c -> {
                Equipment item = equipmentDao.findById(c, equipmentId);
                if (item == null) throw new SQLException("Equipment was not found.");
                if (!item.isBorrowable())
                    throw new SQLException(item.equipmentType() + " '" + item.displayLabel() + "' cannot be issued (status: "
                            + item.getStatus().label() + ", condition: " + item.getCondition().label() + ").");
                if (issueDao.countActiveForEquipment(c, equipmentId) > 0)
                    throw new SQLException("This item already has an active loan.");
                Borrower borrower = borrowerDao.findOrCreate(c, new Borrower(0, borrowerName.trim(), borrowerCode.trim(),
                        role == null || role.isBlank() ? "STUDENT" : role.trim(), blank(email), blank(phone)));
                IssueRecord record = new IssueRecord(0, equipmentId, item.displayLabel(), borrower.getId(),
                        borrower.displayLabel(), issuedDate, dueDate, null, null, blank(remarks), IssueStatus.ACTIVE);
                issueDao.insert(c, record);
                equipmentDao.updateStatus(c, equipmentId, EquipmentStatus.ISSUED);
                return record;
            });
        } catch (SQLException e) { throw translate(e); }
    }
/** Records a return; a return in damaged condition also opens a damage record. */
    public IssueRecord returnItem(int issueId, LocalDate returnedDate, Condition condition, String remarks) throws LabTrackException {
        if (returnedDate == null) throw new ValidationException("Return date is required.");
        if (condition == null) throw new ValidationException("Please select the return condition.");
        try {
            return db.inTransaction(c -> {
                IssueRecord record = issueDao.findById(c, issueId);
                if (record == null) throw new SQLException("Borrowing record was not found.");
                if (record.getStatus() == IssueStatus.RETURNED)
                    throw new SQLException("This item was already returned on " + record.getReturnedDate() + ".");
                Equipment item = equipmentDao.findById(c, record.getEquipmentId());
                if (item == null) throw new SQLException("Equipment for this record was not found.");
                issueDao.markReturned(c, issueId, returnedDate, condition, blank(remarks));
                equipmentDao.updateCondition(c, record.getEquipmentId(), condition);
                if (condition == Condition.DAMAGED) {
                    String note = remarks == null || remarks.isBlank() ? "Item returned in damaged condition." : remarks.trim();
                    new MaintenanceDao(db).insert(c, new MaintenanceRecord(0, record.getEquipmentId(), item.displayLabel(),
                            MaintenanceType.DAMAGE, note, null, MaintenanceStatus.PENDING, returnedDate, null, "Return desk"));
                    equipmentDao.updateStatus(c, record.getEquipmentId(), EquipmentStatus.DAMAGED);
                } else {
                    equipmentDao.updateStatus(c, record.getEquipmentId(), EquipmentStatus.AVAILABLE);
                }
                return issueDao.findById(c, issueId);
            });
        } catch (SQLException e) { throw translate(e); }
    }
    private static LabTrackException translate(SQLException e) {
        return e.getCause() instanceof LabTrackException le ? le : new OperationException(e.getMessage());
    }
    private static String blank(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
