package labtrack.dao;
import labtrack.database.DatabaseManager;
import labtrack.model.MaintenanceRecord;
import labtrack.model.MaintenanceStatus;
import labtrack.model.MaintenanceType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
/** JDBC access for damage reports and maintenance records. */
public class MaintenanceDao {
    private static final String BASE = """
        SELECT m.id, m.equipment_id, e.code || ' — ' || e.name AS equipment_label, m.type, m.description,
               m.resolution_notes, m.status, m.reported_date, m.resolved_date, m.reported_by
          FROM maintenance_records m JOIN equipment e ON e.id = m.equipment_id""";
    private final DatabaseManager db;
    public MaintenanceDao(DatabaseManager db) { this.db = db; }
    /** statusFilter: OPEN (pending + in progress), COMPLETED or null; typeFilter: DAMAGE, MAINTENANCE or null. */
    public List<MaintenanceRecord> find(String statusFilter, String typeFilter, String text) throws SQLException {
        String like = text == null || text.isBlank() ? null : "%" + text.trim() + "%";
        List<MaintenanceRecord> out = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(BASE +
                " WHERE (? IS NULL OR m.status = ? OR (? = 'OPEN' AND m.status <> 'COMPLETED')) AND (? IS NULL OR m.type = ?)" +
                "   AND (? IS NULL OR e.code LIKE ? OR e.name LIKE ? OR m.description LIKE ?) ORDER BY m.id DESC")) {
            ps.setString(1, statusFilter); ps.setString(2, statusFilter); ps.setString(3, statusFilter);
            ps.setString(4, typeFilter); ps.setString(5, typeFilter);
            for (int i = 0; i < 4; i++) ps.setString(6 + i, like);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }
    public MaintenanceRecord findById(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(BASE + " WHERE m.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }
    public int countOpenForEquipment(Connection c, int equipmentId) throws SQLException { return count(c, "SELECT COUNT(*) FROM maintenance_records WHERE equipment_id = ? AND status <> 'COMPLETED'", equipmentId); }
    public int countForEquipment(Connection c, int equipmentId) throws SQLException { return count(c, "SELECT COUNT(*) FROM maintenance_records WHERE equipment_id = ?", equipmentId); }
    private static int count(Connection c, String sql, int equipmentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }
    public void insert(Connection c, MaintenanceRecord r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO maintenance_records(equipment_id, type, description, status, reported_date, reported_by) VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getEquipmentId()); ps.setString(2, r.getType().name()); ps.setString(3, r.getDescription());
            ps.setString(4, MaintenanceStatus.PENDING.name()); ps.setString(5, r.getReportedDate().toString()); ps.setString(6, r.getReportedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); r.setId(keys.getInt(1)); }
        }
    }
    public void updateStatus(Connection c, int id, MaintenanceStatus status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE maintenance_records SET status=? WHERE id=?")) { ps.setString(1, status.name()); ps.setInt(2, id); ps.executeUpdate(); }
    }
    public void complete(Connection c, int id, String resolutionNotes, LocalDate resolvedDate) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE maintenance_records SET status='COMPLETED', resolution_notes=?, resolved_date=? WHERE id=?")) {
            ps.setString(1, resolutionNotes); ps.setString(2, resolvedDate.toString()); ps.setInt(3, id);
            ps.executeUpdate();
        }
    }
    private MaintenanceRecord map(ResultSet rs) throws SQLException {
        String reported = rs.getString("reported_date"), resolved = rs.getString("resolved_date");
        return new MaintenanceRecord(rs.getInt("id"), rs.getInt("equipment_id"), rs.getString("equipment_label"), MaintenanceType.from(rs.getString("type")),
                rs.getString("description"), rs.getString("resolution_notes"), MaintenanceStatus.from(rs.getString("status")),
                reported == null ? null : LocalDate.parse(reported), resolved == null ? null : LocalDate.parse(resolved), rs.getString("reported_by"));
    }
}
