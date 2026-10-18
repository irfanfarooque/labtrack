package labtrack.dao;
import labtrack.database.DatabaseManager;
import labtrack.model.Condition;
import labtrack.model.IssueRecord;
import labtrack.model.IssueStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
/** JDBC access for borrowing records. */
public class IssueDao {
    private static final String BASE = """
        SELECT i.id, i.equipment_id, e.code || ' — ' || e.name AS equipment_label, i.borrower_id,
               b.code || ' — ' || b.name || ' (' || LOWER(b.role) || ')' AS borrower_label,
               i.issued_date, i.due_date, i.returned_date, i.return_condition, i.remarks, i.status
          FROM issue_records i JOIN equipment e ON e.id = i.equipment_id JOIN borrowers b ON b.id = i.borrower_id""";
    private final DatabaseManager db;
    public IssueDao(DatabaseManager db) { this.db = db; }
    /** statusFilter: ACTIVE, RETURNED or null for all; text matches equipment and borrower fields. */
    public List<IssueRecord> find(String statusFilter, String text) throws SQLException {
        String like = text == null || text.isBlank() ? null : "%" + text.trim() + "%";
        List<IssueRecord> out = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(BASE +
                " WHERE (? IS NULL OR i.status = ?) AND (? IS NULL OR e.code LIKE ? OR e.name LIKE ? OR b.name LIKE ? OR b.code LIKE ?) ORDER BY i.id DESC")) {
            ps.setString(1, statusFilter); ps.setString(2, statusFilter);
            for (int i = 0; i < 5; i++) ps.setString(3 + i, like);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }
    public List<IssueRecord> findOverdue() throws SQLException {
        List<IssueRecord> out = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(BASE + " WHERE i.status = 'ACTIVE' AND date(i.due_date) < date('now','localtime') ORDER BY i.due_date"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }
    public IssueRecord findById(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(BASE + " WHERE i.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }
    public int countActiveForEquipment(Connection c, int equipmentId) throws SQLException { return count(c, "SELECT COUNT(*) FROM issue_records WHERE equipment_id = ? AND status = 'ACTIVE'", equipmentId); }
    public int countForEquipment(Connection c, int equipmentId) throws SQLException { return count(c, "SELECT COUNT(*) FROM issue_records WHERE equipment_id = ?", equipmentId); }
    private static int count(Connection c, String sql, int equipmentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }
    public void insert(Connection c, IssueRecord r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO issue_records(equipment_id, borrower_id, issued_date, due_date, remarks, status) VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getEquipmentId()); ps.setInt(2, r.getBorrowerId());
            ps.setString(3, r.getIssuedDate().toString()); ps.setString(4, r.getDueDate().toString());
            ps.setString(5, r.getRemarks()); ps.setString(6, IssueStatus.ACTIVE.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); r.setId(keys.getInt(1)); }
        }
    }
    public void markReturned(Connection c, int id, LocalDate returnedDate, Condition condition, String remarks) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE issue_records SET returned_date=?, return_condition=?, remarks=?, status='RETURNED' WHERE id=?")) {
            ps.setString(1, returnedDate.toString()); ps.setString(2, condition.name()); ps.setString(3, remarks); ps.setInt(4, id);
            ps.executeUpdate();
        }
    }
    private IssueRecord map(ResultSet rs) throws SQLException {
        String returned = rs.getString("returned_date"), condition = rs.getString("return_condition");
        return new IssueRecord(rs.getInt("id"), rs.getInt("equipment_id"), rs.getString("equipment_label"), rs.getInt("borrower_id"), rs.getString("borrower_label"),
                LocalDate.parse(rs.getString("issued_date")), LocalDate.parse(rs.getString("due_date")),
                returned == null ? null : LocalDate.parse(returned), condition == null ? null : Condition.valueOf(condition),
                rs.getString("remarks"), IssueStatus.from(rs.getString("status")));
    }
}
