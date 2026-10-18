package labtrack.dao;
import labtrack.database.DatabaseManager;
import labtrack.model.Category;
import labtrack.model.ComputerEquipment;
import labtrack.model.Condition;
import labtrack.model.ElectronicEquipment;
import labtrack.model.Equipment;
import labtrack.model.EquipmentStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
/** JDBC access for equipment and categories; rebuilds the correct subclass from the type column. */
public class EquipmentDao {
    private static final String BASE = """
        SELECT e.id, e.code, e.name, e.type, e.category_id, c.name AS category_name, e.serial_number,
               e.manufacturer, e.location, e.purchase_date, e.condition, e.status, e.notes
          FROM equipment e JOIN categories c ON c.id = e.category_id""";
    private final DatabaseManager db;
    public EquipmentDao(DatabaseManager db) { this.db = db; }
    public List<Category> categories() throws SQLException {
        List<Category> out = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT id, name FROM categories ORDER BY name"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(new Category(rs.getInt(1), rs.getString(2)));
        }
        return out;
    }
    public Category addCategory(String name) throws SQLException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO categories(name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name.trim());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); return new Category(keys.getInt(1), name.trim()); }
        }
    }
    public Equipment findById(int id) throws SQLException {
        try (Connection c = db.getConnection()) { return findById(c, id); }
    }
    public Equipment findById(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(BASE + " WHERE e.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }
    public Equipment findByCode(String code) throws SQLException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(BASE + " WHERE e.code = ?")) {
            ps.setString(1, code.trim());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }
    public boolean serialExists(String serial, int excludeId) throws SQLException {
        if (serial == null || serial.isBlank()) return false;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM equipment WHERE serial_number = ? AND id <> ?")) {
            ps.setString(1, serial.trim()); ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.getInt(1) > 0; }
        }
    }
    /** Filtered listing; null/blank filters are ignored. */
    public List<Equipment> search(String text, int categoryId, String condition, String status) throws SQLException {
        List<Equipment> out = new ArrayList<>();
        String like = text == null || text.isBlank() ? "" : "%" + text.trim() + "%";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(BASE +
                " WHERE (? = '' OR e.code LIKE ? OR e.name LIKE ? OR e.serial_number LIKE ? OR e.manufacturer LIKE ?)" +
                "   AND (? = 0 OR e.category_id = ?) AND (? = '' OR e.condition = ?) AND (? = '' OR e.status = ?) ORDER BY e.name COLLATE NOCASE")) {
            ps.setString(1, text == null ? "" : text.trim());
            for (int i = 0; i < 4; i++) ps.setString(2 + i, like);
            ps.setInt(6, categoryId); ps.setInt(7, categoryId);
            ps.setString(8, condition == null ? "" : condition); ps.setString(9, condition == null ? "" : condition);
            ps.setString(10, status == null ? "" : status); ps.setString(11, status == null ? "" : status);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }
    public void insert(Equipment e) throws SQLException {
        try (Connection c = db.getConnection()) { insert(c, e); }
    }
    public void insert(Connection c, Equipment e) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO equipment(code, name, type, category_id, serial_number, manufacturer, location, purchase_date, condition, status, notes)" +
                " VALUES (?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getCode()); ps.setString(2, e.getName());
            ps.setString(3, e.equipmentType().startsWith("Computer") ? "COMPUTER" : "ELECTRONIC");
            ps.setInt(4, e.getCategory().getId()); ps.setString(5, blankToNull(e.getSerialNumber()));
            ps.setString(6, blankToNull(e.getManufacturer())); ps.setString(7, blankToNull(e.getLocation()));
            ps.setString(8, e.getPurchaseDate() == null ? null : e.getPurchaseDate().toString());
            ps.setString(9, e.getCondition().name()); ps.setString(10, e.getStatus().name()); ps.setString(11, blankToNull(e.getNotes()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); e.setId(keys.getInt(1)); }
        }
    }
    public void update(Equipment e) throws SQLException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE equipment SET name=?, category_id=?, serial_number=?, manufacturer=?, location=?, purchase_date=?, condition=?, notes=? WHERE id=?")) {
            ps.setString(1, e.getName()); ps.setInt(2, e.getCategory().getId()); ps.setString(3, blankToNull(e.getSerialNumber()));
            ps.setString(4, blankToNull(e.getManufacturer())); ps.setString(5, blankToNull(e.getLocation()));
            ps.setString(6, e.getPurchaseDate() == null ? null : e.getPurchaseDate().toString());
            ps.setString(7, e.getCondition().name()); ps.setString(8, blankToNull(e.getNotes())); ps.setInt(9, e.getId());
            ps.executeUpdate();
        }
    }
    public void updateStatus(int id, EquipmentStatus status) throws SQLException {
        try (Connection c = db.getConnection()) { updateStatus(c, id, status); }
    }
    public void updateStatus(Connection c, int id, EquipmentStatus status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE equipment SET status=? WHERE id=?")) { ps.setString(1, status.name()); ps.setInt(2, id); ps.executeUpdate(); }
    }
    public void updateCondition(Connection c, int id, Condition condition) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE equipment SET condition=? WHERE id=?")) { ps.setString(1, condition.name()); ps.setInt(2, id); ps.executeUpdate(); }
    }
    public void delete(int id) throws SQLException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM equipment WHERE id=?")) { ps.setInt(1, id); ps.executeUpdate(); }
    }
    private Equipment map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        Category category = new Category(rs.getInt("category_id"), rs.getString("category_name"));
        String purchase = rs.getString("purchase_date");
        LocalDate purchaseDate = purchase == null ? null : LocalDate.parse(purchase);
        Condition condition = Condition.from(rs.getString("condition"));
        EquipmentStatus status = EquipmentStatus.from(rs.getString("status"));
        if ("COMPUTER".equals(rs.getString("type")))
            return new ComputerEquipment(id, rs.getString("code"), rs.getString("name"), category, rs.getString("serial_number"), rs.getString("manufacturer"), rs.getString("location"), purchaseDate, condition, status, rs.getString("notes"));
        return new ElectronicEquipment(id, rs.getString("code"), rs.getString("name"), category, rs.getString("serial_number"), rs.getString("manufacturer"), rs.getString("location"), purchaseDate, condition, status, rs.getString("notes"));
    }
    static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
