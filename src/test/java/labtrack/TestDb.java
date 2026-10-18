package labtrack;
import java.nio.file.Files;
import java.nio.file.Path;
/** One temporary database directory per test run, shared by all test classes. */
final class TestDb {
    static final Path DIR = createFreshDirectory();
    private TestDb() {}
    private static Path createFreshDirectory() {
        try {
            Path dir = Files.createTempDirectory("labtrack-test");
            System.setProperty("labtrack.db.dir", dir.toString());
            return dir;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create test database folder", e);
        }
    }
}
