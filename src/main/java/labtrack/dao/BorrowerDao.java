package labtrack.dao;
import labtrack.database.DatabaseManager;
import labtrack.model.Borrower;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
/** JDBC access for borrowers (students and staff). */
public class BorrowerDao {
    private static final String COLS = "id, name, code, role, email, phone";
    private final DatabaseManager db;
    public BorrowerDao(DatabaseManager db) { this.db = db; }
    public Borrower findByCode(String code) throws SQLException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM borrowers WHERE code = ?")) {
            ps.setString(1, code.trim());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }
    public Borrower findById(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM borrowers WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }
    /** Finds a borrower by ID or registers a new one; always returns the persisted row. */
    public Borrower findOrCreate(Connection c, Borrower b) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM borrowers WHERE code = ?")) {
            ps.setString(1, b.getCode().trim());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO borrowers(name, code, role, email, phone) VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getName()); ps.setString(2, b.getCode()); ps.setString(3, b.getRole()); ps.setString(4, b.getEmail()); ps.setString(5, b.getPhone());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); b.setId(keys.getInt(1)); }
            return b;
        }
    }
    private Borrower map(ResultSet rs) throws SQLException { return new Borrower(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)); }
}
