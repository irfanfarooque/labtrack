package labtrack;
import labtrack.exception.DuplicateRecordException;
import labtrack.exception.ValidationException;
import labtrack.model.User;
import labtrack.service.AppContext;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
/** Authentication: seeding, login, wrong credentials, duplicates, validation. */
class AuthServiceTest {
    @Test
    void seedLoginAndValidation() throws Exception {
        Path shared = TestDb.DIR;
        AppContext ctx = new AppContext();
        ctx.auth().seedDefaultUser();
        ctx.auth().seedDefaultUser();
        User admin = ctx.auth().login("admin", "lab123");
        assertEquals("admin", admin.username());
        assertEquals("ADMIN", admin.role());
        assertNotNull(new AppContext().auth().login("ADMIN", "lab123"));
        assertThrows(ValidationException.class, () -> ctx.auth().login("admin", "wrong-password"));
        assertThrows(ValidationException.class, () -> ctx.auth().login("ghost", "lab123"));
        assertThrows(ValidationException.class, () -> ctx.auth().login("", ""));
        assertThrows(ValidationException.class, () -> ctx.auth().login(" ", " "));
        assertThrows(DuplicateRecordException.class, () -> ctx.auth().createUser("admin", "other123", "USER"));
        assertThrows(ValidationException.class, () -> ctx.auth().createUser("bob", "abc", "USER"));
        assertThrows(ValidationException.class, () -> ctx.auth().createUser("  ", "secret1", "USER"));
        User bob = ctx.auth().createUser("bob", "secret1", "USER");
        assertEquals("USER", bob.role());
        assertEquals("bob", ctx.auth().login("bob", "secret1").username());
    }
}
