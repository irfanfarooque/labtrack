package labtrack.database;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
/** Thread-safe Singleton owning the SQLite database; short-lived connections, transactional writes. */
public final class DatabaseManager {
    private static volatile DatabaseManager instance;
    private final String jdbcUrl;
    private DatabaseManager(Path dbFile) throws SQLException {
        try { Files.createDirectories(dbFile.toAbsolutePath().getParent()); }
        catch (Exception e) { throw new SQLException("Cannot create data folder: " + dbFile.getParent(), e); }
        jdbcUrl = "jdbc:sqlite:" + dbFile.toAbsolutePath();
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            initSchema(c);
        }
    }
    public static DatabaseManager getInstance() {
        DatabaseManager local = instance;
        if (local == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    try { instance = new DatabaseManager(defaultDatabaseFile()); }
                    catch (SQLException e) { throw new IllegalStateException("Cannot open database: " + e.getMessage(), e); }
                }
                local = instance;
            }
        }
        return local;
    }
/** Data folder: ~/.labtrack by default, overridable with -Dlabtrack.db.dir=... */
    public static Path defaultDatabaseFile() {
        String override = System.getProperty("labtrack.db.dir");
        Path dir = (override == null || override.isBlank())
                ? Paths.get(System.getProperty("user.home"), ".labtrack") : Paths.get(override);
        return dir.resolve("labtrack.db");
    }
/** True when a SQLite UNIQUE constraint was violated (used to build friendly errors). */
    public static boolean isUniqueViolation(SQLException e) {
        return e.getMessage() != null && e.getMessage().toUpperCase().contains("UNIQUE");
    }
    public Connection getConnection() throws SQLException { return DriverManager.getConnection(jdbcUrl); }
/** Runs several DAO calls atomically: commit on success, rollback on any failure. */
    public <T> T inTransaction(SqlWork<T> work) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try { T result = work.run(c); c.commit(); return result; }
            catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException se) throw se;
                throw new SQLException(e.getMessage(), e);
            }
        }
    }
    @FunctionalInterface
    public interface SqlWork<T> { T run(Connection connection) throws SQLException; }
    private void initSchema(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " name TEXT NOT NULL COLLATE NOCASE UNIQUE)");
            st.execute("CREATE TABLE IF NOT EXISTS equipment (id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " code TEXT NOT NULL UNIQUE, name TEXT NOT NULL, type TEXT NOT NULL DEFAULT 'ELECTRONIC'," +
                    " category_id INTEGER NOT NULL REFERENCES categories(id), serial_number TEXT UNIQUE," +
                    " manufacturer TEXT, location TEXT, purchase_date TEXT," +
                    " condition TEXT NOT NULL DEFAULT 'GOOD', status TEXT NOT NULL DEFAULT 'AVAILABLE'," +
                    " notes TEXT, created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')))");
            st.execute("CREATE TABLE IF NOT EXISTS borrowers (id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " name TEXT NOT NULL, code TEXT NOT NULL UNIQUE, role TEXT NOT NULL DEFAULT 'STUDENT'," +
                    " email TEXT, phone TEXT)");
            st.execute("CREATE TABLE IF NOT EXISTS issue_records (id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " equipment_id INTEGER NOT NULL REFERENCES equipment(id) ON DELETE CASCADE," +
                    " borrower_id INTEGER NOT NULL REFERENCES borrowers(id)," +
                    " issued_date TEXT NOT NULL, due_date TEXT NOT NULL, returned_date TEXT," +
                    " return_condition TEXT, remarks TEXT, status TEXT NOT NULL DEFAULT 'ACTIVE')");
            st.execute("CREATE TABLE IF NOT EXISTS maintenance_records (id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " equipment_id INTEGER NOT NULL REFERENCES equipment(id) ON DELETE CASCADE," +
                    " type TEXT NOT NULL, description TEXT NOT NULL, resolution_notes TEXT," +
                    " status TEXT NOT NULL DEFAULT 'PENDING', reported_date TEXT NOT NULL," +
                    " resolved_date TEXT, reported_by TEXT)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_issue_equipment ON issue_records(equipment_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_maint_equipment ON maintenance_records(equipment_id)");
        }
    }
}
