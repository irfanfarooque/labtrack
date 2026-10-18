package labtrack.dao;
import labtrack.database.DatabaseManager;
import labtrack.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
/** JDBC access for login accounts (password hash + salt stay inside this layer). */
public class UserDao {
    /** One stored account including its credentials; only the service layer may inspect these. */
    public record Credentials(int id, String username, String passwordHash, String salt, String role) {}
    private final DatabaseManager db;
    public UserDao(DatabaseManager db) { this.db = db; }
    public int count() throws SQLException {
        try (Connection c = db.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) { rs.next(); return rs.getInt(1); }
    }
    public Credentials findByUsername(String username) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, username, password_hash, salt, role FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Credentials(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)) : null;
            }
        }
    }
    public User insert(String username, String passwordHash, String salt, String role) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO users(username, password_hash, salt, role) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username); ps.setString(2, passwordHash); ps.setString(3, salt); ps.setString(4, role);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); return new User(keys.getInt(1), username, role); }
        }
    }
}
