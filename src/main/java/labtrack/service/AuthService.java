package labtrack.service;
import labtrack.dao.UserDao;
import labtrack.database.DatabaseManager;
import labtrack.exception.DuplicateRecordException;
import labtrack.exception.LabTrackException;
import labtrack.exception.ValidationException;
import labtrack.model.User;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.HexFormat;
/** Login accounts: PBKDF2 password hashing, verification, and first-run seeding of the sample admin. */
public class AuthService {
    private static final int ITERATIONS = 120_000, SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserDao userDao;
    public AuthService(DatabaseManager db, UserDao userDao) { this.userDao = userDao; }
    /** Creates the sample admin account on a fresh database; a no-op once any user exists. */
    public void seedDefaultUser() throws LabTrackException {
        try { if (userDao.count() == 0) createUser("admin", "lab123", "ADMIN"); }
        catch (SQLException e) { throw new LabTrackException("Cannot prepare user accounts: " + e.getMessage(), e); }
    }
    public User createUser(String username, String password, String role) throws LabTrackException {
        if (username == null || username.isBlank()) throw new ValidationException("Username is required.");
        if (password == null || password.length() < 4) throw new ValidationException("Password must be at least 4 characters.");
        String name = username.trim();
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        try {
            return userDao.insert(name, hash(password, salt), HexFormat.of().formatHex(salt),
                    role == null || role.isBlank() ? "USER" : role.trim());
        } catch (SQLException e) {
            if (DatabaseManager.isUniqueViolation(e)) throw new DuplicateRecordException("Username '" + name + "' already exists.");
            throw new LabTrackException("Cannot create user: " + e.getMessage(), e);
        }
    }
    /** Verifies credentials; unknown users still hash once so both failures take the same time. */
    public User login(String username, String password) throws LabTrackException {
        if (username == null || username.isBlank() || password == null || password.isBlank())
            throw new ValidationException("Username and password are required.");
        try {
            UserDao.Credentials c = userDao.findByUsername(username.trim());
            if (c == null) { hash(password, new byte[SALT_BYTES]); throw new ValidationException("Invalid username or password."); }
            boolean ok = MessageDigest.isEqual(HexFormat.of().parseHex(c.passwordHash()),
                    HexFormat.of().parseHex(hash(password, HexFormat.of().parseHex(c.salt()))));
            if (!ok) throw new ValidationException("Invalid username or password.");
            return new User(c.id(), c.username(), c.role());
        } catch (SQLException e) { throw new LabTrackException("Login failed: " + e.getMessage(), e); }
    }
    /** PBKDF2-WithHmacSHA256, 256-bit key, per-user random salt; compared in constant time by the caller. */
    static String hash(String password, byte[] salt) {
        try {
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] key = f.generateSecret(new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)).getEncoded();
            return HexFormat.of().formatHex(key);
        } catch (GeneralSecurityException e) { throw new IllegalStateException("Password hashing is unavailable", e); }
    }
}
