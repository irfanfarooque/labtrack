package labtrack.dao;
import labtrack.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/** Read-only queries behind the dashboard and the report screens. */
public class ReportDao {
    private final DatabaseManager db;
    public ReportDao(DatabaseManager db) { this.db = db; }
    /** Keys: TOTAL, AVAILABLE, ISSUED, DAMAGED, MAINTENANCE, OVERDUE. */
    public Map<String, Integer> dashboardStats() throws SQLException {
        Map<String, Integer> stats = new LinkedHashMap<>(), byStatus = new LinkedHashMap<>();
        int total = 0;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT status, COUNT(*) FROM equipment GROUP BY status"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { byStatus.put(rs.getString(1), rs.getInt(2)); total += rs.getInt(2); }
        }
        stats.put("TOTAL", total);
        stats.put("AVAILABLE", byStatus.getOrDefault("AVAILABLE", 0));
        stats.put("ISSUED", byStatus.getOrDefault("ISSUED", 0));
        stats.put("DAMAGED", byStatus.getOrDefault("DAMAGED", 0));
        stats.put("MAINTENANCE", byStatus.getOrDefault("MAINTENANCE", 0));
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM issue_records WHERE status='ACTIVE' AND date(due_date) < date('now','localtime')"); ResultSet rs = ps.executeQuery()) {
            rs.next();
            stats.put("OVERDUE", rs.getInt(1));
        }
        return stats;
    }
    /** Rows: [when, activity, details] — merged feed of issues, returns and maintenance events. */
    public List<String[]> recentActivity(int limit) throws SQLException {
        List<String[]> out = new ArrayList<>();
        String sql = "SELECT ts, type, details FROM (" +
                " SELECT i.issued_date ts, 'Issue' type, e.code || ' → ' || b.name details FROM issue_records i JOIN equipment e ON e.id=i.equipment_id JOIN borrowers b ON b.id=i.borrower_id" +
                " UNION ALL SELECT i.returned_date, 'Return', e.code || ' ← ' || b.name FROM issue_records i JOIN equipment e ON e.id=i.equipment_id JOIN borrowers b ON b.id=i.borrower_id WHERE i.returned_date IS NOT NULL" +
                " UNION ALL SELECT m.reported_date, CASE m.type WHEN 'DAMAGE' THEN 'Damage' ELSE 'Maintenance' END, e.code || ' — ' || m.description FROM maintenance_records m JOIN equipment e ON e.id=m.equipment_id" +
                " UNION ALL SELECT m.resolved_date, 'Resolved', e.code || ' — ' || COALESCE(m.resolution_notes,'') FROM maintenance_records m JOIN equipment e ON e.id=m.equipment_id WHERE m.resolved_date IS NOT NULL" +
                ") WHERE ts IS NOT NULL ORDER BY ts DESC LIMIT ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3)}); }
        }
        return out;
    }
    public List<String[]> rows(String sql) throws SQLException {
        List<String[]> out = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String[] row = new String[rs.getMetaData().getColumnCount()];
                for (int i = 0; i < row.length; i++) row[i] = rs.getString(i + 1) == null ? "" : rs.getString(i + 1);
                out.add(row);
            }
        }
        return out;
    }
    public List<String[]> categoryConditionSummary() throws SQLException {
        return rows("SELECT c.name, COUNT(*), SUM(CASE WHEN e.condition='EXCELLENT' THEN 1 ELSE 0 END), SUM(CASE WHEN e.condition='GOOD' THEN 1 ELSE 0 END)," +
                " SUM(CASE WHEN e.condition='FAIR' THEN 1 ELSE 0 END), SUM(CASE WHEN e.condition='POOR' THEN 1 ELSE 0 END), SUM(CASE WHEN e.condition='DAMAGED' THEN 1 ELSE 0 END)" +
                " FROM equipment e JOIN categories c ON c.id = e.category_id GROUP BY c.id ORDER BY c.name");
    }
    public List<String[]> statusSummary() throws SQLException { return rows("SELECT status, COUNT(*) FROM equipment GROUP BY status ORDER BY status"); }
    public List<String[]> categorySummary() throws SQLException {
        return rows("SELECT c.name, COUNT(e.id) FROM categories c LEFT JOIN equipment e ON e.category_id = c.id GROUP BY c.id ORDER BY c.name");
    }
}
