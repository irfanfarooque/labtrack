package labtrack.service;
import labtrack.dao.EquipmentDao;
import labtrack.dao.IssueDao;
import labtrack.dao.MaintenanceDao;
import labtrack.database.DatabaseManager;
import labtrack.exception.DuplicateRecordException;
import labtrack.exception.LabTrackException;
import labtrack.exception.OperationException;
import labtrack.exception.ValidationException;
import labtrack.model.Category;
import labtrack.model.ComputerEquipment;
import labtrack.model.Condition;
import labtrack.model.ElectronicEquipment;
import labtrack.model.Equipment;
import labtrack.model.EquipmentStatus;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
/** Inventory rules: validation, duplicate prevention, archive/restore, safe delete. */
public class EquipmentService {
    private final DatabaseManager db;
    private final EquipmentDao equipmentDao;
    private final IssueDao issueDao;
    private final MaintenanceDao maintenanceDao;
    public EquipmentService(DatabaseManager db, EquipmentDao equipmentDao, IssueDao issueDao, MaintenanceDao maintenanceDao) {
        this.db = db; this.equipmentDao = equipmentDao; this.issueDao = issueDao; this.maintenanceDao = maintenanceDao;
    }
    public List<Category> categories() throws SQLException { return equipmentDao.categories(); }
    public Category categoryByName(String name) throws SQLException {
        return categories().stream().filter(c -> c.getName().equalsIgnoreCase(name.trim())).findFirst().orElse(null);
    }
    public Category addCategory(String name) throws LabTrackException {
        if (name == null || name.isBlank()) throw new ValidationException("Category name is required.");
        try { return equipmentDao.addCategory(name); }
        catch (SQLException e) {
            if (DatabaseManager.isUniqueViolation(e)) throw new DuplicateRecordException("Category '" + name.trim() + "' already exists.");
            throw new LabTrackException("Cannot add category: " + e.getMessage(), e);
        }
    }
/** Inventory listing; archived items are hidden unless requested. */
    public List<Equipment> list(String text, int categoryId, Condition condition, EquipmentStatus status, boolean includeArchived) throws SQLException {
        List<Equipment> all = equipmentDao.search(text, categoryId, condition == null ? null : condition.name(), status == null ? null : status.name());
        return includeArchived ? all : all.stream().filter(e -> !e.isArchived()).toList();
    }
    public Equipment get(int id) throws LabTrackException {
        try {
            Equipment e = equipmentDao.findById(id);
            if (e == null) throw new OperationException("Equipment #" + id + " was not found.");
            return e;
        } catch (OperationException e) { throw e; }
        catch (SQLException e) { throw new LabTrackException("Cannot load equipment: " + e.getMessage(), e); }
    }
/** Creates a new item; the type flag picks the concrete class (polymorphism). */
    public Equipment create(String code, String name, boolean computerType, int categoryId, String serial,
                            String manufacturer, String location, LocalDate purchaseDate, Condition condition, String notes) throws LabTrackException {
        requireText(code, "Asset tag is required.");
        requireText(name, "Equipment name is required.");
        Category category = findCategory(categoryId);
        Equipment item = computerType
                ? new ComputerEquipment(0, code.trim(), name.trim(), category, serial, manufacturer, location, purchaseDate, condition, EquipmentStatus.AVAILABLE, notes)
                : new ElectronicEquipment(0, code.trim(), name.trim(), category, serial, manufacturer, location, purchaseDate, condition, EquipmentStatus.AVAILABLE, notes);
        return importEquipment(item);
    }
/** Inserts an Equipment object already built elsewhere (dialog or legacy adapter). */
    public Equipment importEquipment(Equipment template) throws LabTrackException {
        try {
            if (equipmentDao.serialExists(template.getSerialNumber(), template.getId()))
                throw new DuplicateRecordException("Serial number '" + template.getSerialNumber().trim() + "' is already used by another item.");
            equipmentDao.insert(template);
            return template;
        } catch (DuplicateRecordException e) { throw e; }
        catch (SQLException e) { throw translate(e, template.getCode(), template.getSerialNumber()); }
    }
    public void update(Equipment item) throws LabTrackException {
        requireText(item.getCode(), "Asset tag is required.");
        requireText(item.getName(), "Equipment name is required.");
        if (item.getCategory() == null || item.getCategory().getId() <= 0) throw new ValidationException("Please choose a category.");
        try {
            if (equipmentDao.serialExists(item.getSerialNumber(), item.getId()))
                throw new DuplicateRecordException("Serial number '" + item.getSerialNumber().trim() + "' is already used by another item.");
            equipmentDao.update(item);
        } catch (DuplicateRecordException e) { throw e; }
        catch (SQLException e) { throw translate(e, item.getCode(), item.getSerialNumber()); }
    }
/** Archive keeps the item and its history, but hides it from lending. */
    public void archive(int id) throws LabTrackException {
        get(id);
        try (var c = db.getConnection()) {
            if (issueDao.countActiveForEquipment(c, id) > 0)
                throw new OperationException("This item is currently issued. Record the return before archiving.");
        } catch (SQLException e) { throw new LabTrackException("Archive failed: " + e.getMessage(), e); }
        setStatusQuietly(id, EquipmentStatus.ARCHIVED);
    }
    public void restore(int id) throws LabTrackException {
        Condition condition = get(id).getCondition();
        setStatusQuietly(id, condition == Condition.DAMAGED ? EquipmentStatus.DAMAGED : EquipmentStatus.AVAILABLE);
    }
/** Hard delete only when the item has no history at all; otherwise archive instead. */
    public void delete(int id) throws LabTrackException {
        Equipment item = get(id);
        try (var c = db.getConnection()) {
            if (issueDao.countForEquipment(c, id) > 0 || maintenanceDao.countForEquipment(c, id) > 0)
                throw new OperationException("This item has borrowing or maintenance history and cannot be deleted. Use Archive instead to preserve its records.");
            equipmentDao.delete(id);
        } catch (OperationException e) { throw e; }
        catch (SQLException e) { throw new LabTrackException("Cannot delete '" + item.getName() + "': " + e.getMessage(), e); }
    }
    private Category findCategory(int id) throws LabTrackException {
        if (id <= 0) throw new ValidationException("Please choose a category.");
        try {
            return categories().stream().filter(c -> c.getId() == id).findFirst()
                    .orElseThrow(() -> new OperationException("Selected category no longer exists."));
        } catch (SQLException e) { throw new LabTrackException("Cannot load categories: " + e.getMessage(), e); }
    }
    private void setStatusQuietly(int id, EquipmentStatus status) throws LabTrackException {
        try { equipmentDao.updateStatus(id, status); }
        catch (SQLException e) { throw new LabTrackException("Cannot update status: " + e.getMessage(), e); }
    }
    private static void requireText(String value, String message) throws ValidationException {
        if (value == null || value.isBlank()) throw new ValidationException(message);
    }
    private static LabTrackException translate(SQLException e, String code, String serial) {
        if (!DatabaseManager.isUniqueViolation(e)) return new LabTrackException("Database error: " + e.getMessage(), e);
        String msg = e.getMessage();
        if (msg.contains("code")) return new DuplicateRecordException("Asset tag '" + code.trim() + "' is already used.");
        if (msg.contains("serial")) return new DuplicateRecordException("Serial number '" + serial + "' is already used.");
        return new DuplicateRecordException("A record with this identifier already exists.");
    }
}
