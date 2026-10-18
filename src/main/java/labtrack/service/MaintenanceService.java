package labtrack.service;
import labtrack.dao.EquipmentDao;
import labtrack.dao.MaintenanceDao;
import labtrack.database.DatabaseManager;
import labtrack.exception.LabTrackException;
import labtrack.exception.OperationException;
import labtrack.exception.ValidationException;
import labtrack.model.Condition;
import labtrack.model.Equipment;
import labtrack.model.EquipmentStatus;
import labtrack.model.MaintenanceRecord;
import labtrack.model.MaintenanceStatus;
import labtrack.model.MaintenanceType;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
/** Damage and maintenance workflows; damaged or maintained equipment stays locked until resolved. */
public class MaintenanceService {
    private final DatabaseManager db;
    private final EquipmentDao equipmentDao;
    private final MaintenanceDao maintenanceDao;
    public MaintenanceService(DatabaseManager db, EquipmentDao equipmentDao, MaintenanceDao maintenanceDao) {
        this.db = db; this.equipmentDao = equipmentDao; this.maintenanceDao = maintenanceDao;
    }
    public List<MaintenanceRecord> list(String statusFilter, String typeFilter, String text) throws SQLException {
        return maintenanceDao.find(statusFilter, typeFilter, text);
    }
    public MaintenanceRecord reportDamage(int equipmentId, String description, String reportedBy) throws LabTrackException {
        return openRecord(equipmentId, description, reportedBy, MaintenanceType.DAMAGE);
    }
    public MaintenanceRecord requestMaintenance(int equipmentId, String description, String reportedBy) throws LabTrackException {
        return openRecord(equipmentId, description, reportedBy, MaintenanceType.MAINTENANCE);
    }
    private MaintenanceRecord openRecord(int equipmentId, String description, String reportedBy, MaintenanceType type) throws LabTrackException {
        if (description == null || description.isBlank()) throw new ValidationException("A description of the problem is required.");
        try {
            return db.inTransaction(c -> {
                Equipment item = equipmentDao.findById(c, equipmentId);
                if (item == null) throw new SQLException("Equipment was not found.");
                if (item.getStatus() == EquipmentStatus.ISSUED)
                    throw new SQLException("'" + item.displayLabel() + "' is currently issued. Record the return first.");
                if (item.isArchived())
                    throw new SQLException("'" + item.displayLabel() + "' is archived and cannot be modified.");
                if (maintenanceDao.countOpenForEquipment(c, equipmentId) > 0)
                    throw new SQLException("'" + item.displayLabel() + "' already has an open damage or maintenance record.");
                MaintenanceRecord record = new MaintenanceRecord(0, equipmentId, item.displayLabel(), type,
                        description.trim(), null, MaintenanceStatus.PENDING, LocalDate.now(), null,
                        reportedBy == null || reportedBy.isBlank() ? "Lab staff" : reportedBy.trim());
                maintenanceDao.insert(c, record);
                equipmentDao.updateStatus(c, equipmentId, type == MaintenanceType.DAMAGE ? EquipmentStatus.DAMAGED : EquipmentStatus.MAINTENANCE);
                return record;
            });
        } catch (SQLException e) { throw translate(e); }
    }
    /** PENDING <-> IN_PROGRESS; only open records may change status. */
    public void updateStatus(int recordId, MaintenanceStatus status) throws LabTrackException {
        try {
            db.inTransaction(c -> {
                MaintenanceRecord record = maintenanceDao.findById(c, recordId);
                if (record == null) throw new SQLException("Maintenance record was not found.");
                if (record.getStatus() == MaintenanceStatus.COMPLETED) throw new SQLException("This record is already completed.");
                maintenanceDao.updateStatus(c, recordId, status);
                return null;
            });
        } catch (SQLException e) { throw translate(e); }
    }
    /** Completes a record; the item only becomes AVAILABLE when no other record is open and its condition allows it. */
    public MaintenanceRecord complete(int recordId, String resolutionNotes, Condition newCondition) throws LabTrackException {
        if (resolutionNotes == null || resolutionNotes.isBlank()) throw new ValidationException("Resolution notes are required to close a record.");
        if (newCondition == null) throw new ValidationException("Please select the condition after repair.");
        try {
            return db.inTransaction(c -> {
                MaintenanceRecord record = maintenanceDao.findById(c, recordId);
                if (record == null) throw new SQLException("Maintenance record was not found.");
                if (record.getStatus() == MaintenanceStatus.COMPLETED) throw new SQLException("This record is already completed.");
                maintenanceDao.complete(c, recordId, resolutionNotes.trim(), LocalDate.now());
                equipmentDao.updateCondition(c, record.getEquipmentId(), newCondition);
                EquipmentStatus target;
                if (maintenanceDao.countOpenForEquipment(c, record.getEquipmentId()) > 0)
                    target = equipmentDao.findById(c, record.getEquipmentId()).getStatus();
                else if (newCondition == Condition.DAMAGED || newCondition == Condition.POOR)
                    target = EquipmentStatus.DAMAGED;
                else
                    target = EquipmentStatus.AVAILABLE;
                equipmentDao.updateStatus(c, record.getEquipmentId(), target);
                return maintenanceDao.findById(c, recordId);
            });
        } catch (SQLException e) { throw translate(e); }
    }
    private static LabTrackException translate(SQLException e) {
        return e.getCause() instanceof LabTrackException le ? le : new OperationException(e.getMessage());
    }
}
