package labtrack;
import labtrack.adapter.LegacyAssetRecord;
import labtrack.adapter.LegacyDataSeeder;
import labtrack.adapter.LegacyTsvAdapter;
import labtrack.exception.DuplicateRecordException;
import labtrack.exception.OperationException;
import labtrack.exception.ValidationException;
import labtrack.model.Category;
import labtrack.model.ComputerEquipment;
import labtrack.model.Condition;
import labtrack.model.Equipment;
import labtrack.model.EquipmentStatus;
import labtrack.model.IssueRecord;
import labtrack.model.IssueStatus;
import labtrack.model.MaintenanceStatus;
import labtrack.model.MaintenanceType;
import labtrack.service.AppContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
/** End-to-end tests of the service layer against a real temporary SQLite database. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LabTrackIntegrationTest {
    private static AppContext ctx;
    private static Path dbDir;
    @BeforeAll
    static void setUpDatabase() {
        dbDir = TestDb.DIR;
        ctx = new AppContext();
        LegacyDataSeeder.seedIfEmpty(ctx);
    }
    private static Category category(String name) throws Exception {
        try { return ctx.equipment().addCategory(name); }
        catch (DuplicateRecordException e) { return ctx.equipment().categoryByName(name); }
    }
    private static Equipment add(String code, String name, Category cat, Condition cond) throws Exception {
        return ctx.equipment().create(code, name, false, cat.getId(), null, "TestMake", "Lab A", LocalDate.now().minusDays(30), cond, "unit test item");
    }
    @Test
    @Order(1)
    void equipmentCrudValidationAndDuplicates() throws Exception {
        Category cat = category("Test Oscilloscope");
        Equipment item = add("TST-0001", "Scope Under Test", cat, Condition.GOOD);
        assertTrue(item.getId() > 0);
        assertEquals(EquipmentStatus.AVAILABLE, ctx.equipment().get(item.getId()).getStatus());
        assertThrows(DuplicateRecordException.class, () -> ctx.equipment().create("TST-0001", "Other", false, cat.getId(), null, null, null, null, Condition.GOOD, null));
        assertThrows(ValidationException.class, () -> ctx.equipment().create(" ", "X", false, cat.getId(), null, null, null, null, Condition.GOOD, null));
        assertThrows(ValidationException.class, () -> ctx.equipment().create("TST-0002", " ", false, cat.getId(), null, null, null, null, Condition.GOOD, null));
        assertThrows(ValidationException.class, () -> ctx.equipment().create("TST-0002", "X", false, 0, null, null, null, null, Condition.GOOD, null));
        Equipment withSerial = ctx.equipment().create("TST-0002", "Serial One", false, cat.getId(), "SER-777", "Rigol", "Lab B", LocalDate.now(), Condition.GOOD, null);
        assertThrows(DuplicateRecordException.class, () -> ctx.equipment().create("TST-0003", "Serial Two", false, cat.getId(), "SER-777", null, null, null, Condition.GOOD, null));
        withSerial.setName("Serial One Renamed");
        withSerial.setCondition(Condition.FAIR);
        ctx.equipment().update(withSerial);
        assertEquals("Serial One Renamed", ctx.equipment().get(withSerial.getId()).getName());
        assertEquals(Condition.FAIR, ctx.equipment().get(withSerial.getId()).getCondition());
        assertFalse(ctx.equipment().list("TST-000", 0, null, null, true).isEmpty());
        assertEquals(1, ctx.equipment().list("SER-777", 0, null, null, true).size());
        assertEquals(1, ctx.equipment().list("Renamed", 0, null, null, true).size());
        assertThrows(DuplicateRecordException.class, () -> ctx.equipment().addCategory("Test Oscilloscope"));
        assertThrows(ValidationException.class, () -> ctx.equipment().addCategory("  "));
    }
    @Test
    @Order(2)
    void archiveRestoreAndSafeDelete() throws Exception {
        Category cat = category("Test Multimeter");
        Equipment archived = add("TST-0010", "Archived Meter", cat, Condition.GOOD);
        ctx.equipment().archive(archived.getId());
        assertEquals(EquipmentStatus.ARCHIVED, ctx.equipment().get(archived.getId()).getStatus());
        assertTrue(ctx.equipment().list(null, 0, null, null, false).stream().noneMatch(e -> e.getId() == archived.getId()));
        ctx.equipment().restore(archived.getId());
        assertEquals(EquipmentStatus.AVAILABLE, ctx.equipment().get(archived.getId()).getStatus());
        Equipment fresh = add("TST-0011", "Delete Me", cat, Condition.GOOD);
        ctx.equipment().delete(fresh.getId());
        assertThrows(OperationException.class, () -> ctx.equipment().get(fresh.getId()));
        Equipment used = add("TST-0012", "Keep Me", cat, Condition.GOOD);
        ctx.issues().issue(used.getId(), "Alice", "STU-100", "STUDENT", "", "", LocalDate.now(), LocalDate.now().plusDays(3), null);
        IssueRecord active = ctx.issues().list("ACTIVE", "Keep Me").get(0);
        ctx.issues().returnItem(active.getId(), LocalDate.now(), Condition.GOOD, null);
        assertThrows(OperationException.class, () -> ctx.equipment().delete(used.getId()));
    }
    @Test
    @Order(3)
    void issueReturnOverdueAndBorrowRules() throws Exception {
        Category cat = category("Test Generator");
        Equipment item = add("TST-0020", "FuncGen", cat, Condition.GOOD);
        IssueRecord record = ctx.issues().issue(item.getId(), "Bob", "STU-200", "STUDENT", "bob@mail.edu", "555-0100", LocalDate.now(), LocalDate.now().plusDays(7), "lab work");
        assertEquals(IssueStatus.ACTIVE, record.getStatus());
        assertEquals(EquipmentStatus.ISSUED, ctx.equipment().get(item.getId()).getStatus());
        assertThrows(OperationException.class, () -> ctx.issues().issue(item.getId(), "Eve", "STU-201", "STUDENT", "", "", LocalDate.now(), LocalDate.now().plusDays(2), null));
        IssueRecord returned = ctx.issues().returnItem(record.getId(), LocalDate.now().plusDays(2), Condition.GOOD, "all probes returned");
        assertEquals(IssueStatus.RETURNED, returned.getStatus());
        assertEquals(EquipmentStatus.AVAILABLE, ctx.equipment().get(item.getId()).getStatus());
        assertThrows(OperationException.class, () -> ctx.issues().returnItem(record.getId(), LocalDate.now(), Condition.GOOD, null));
        assertThrows(ValidationException.class, () -> ctx.issues().issue(item.getId(), "Bob", "STU-200", "STUDENT", "", "", LocalDate.now().plusDays(5), LocalDate.now(), null));
        ctx.issues().issue(item.getId(), "Bob", "STU-200", "STUDENT", "", "", LocalDate.now(), LocalDate.now().plusDays(2), null);
        ctx.issues().returnItem(ctx.issues().list("ACTIVE", "Bob").get(0).getId(), LocalDate.now(), Condition.GOOD, null);
        List<IssueRecord> history = ctx.issues().list(null, "STU-200");
        assertEquals(2, history.size());
        assertEquals(history.get(0).getBorrowerId(), history.get(1).getBorrowerId());
        Equipment stale = add("TST-0030", "Power Supply", cat, Condition.GOOD);
        ctx.issues().issue(stale.getId(), "Cara", "STU-300", "STUDENT", "", "", LocalDate.now().minusDays(10), LocalDate.now().minusDays(2), null);
        IssueRecord overdue = ctx.issues().overdue().stream().filter(r -> r.getEquipmentLabel().contains("TST-0030")).findFirst().orElseThrow();
        assertTrue(overdue.isOverdue());
        assertEquals(2, overdue.overdueDays());
        Equipment broken = add("TST-0031", "Broken Scope", cat, Condition.DAMAGED);
        Equipment poor = add("TST-0032", "Poor Scope", cat, Condition.POOR);
        assertThrows(OperationException.class, () -> ctx.issues().issue(broken.getId(), "Cara", "STU-300", "STUDENT", "", "", LocalDate.now(), LocalDate.now().plusDays(1), null));
        assertThrows(OperationException.class, () -> ctx.issues().issue(poor.getId(), "Cara", "STU-300", "STUDENT", "", "", LocalDate.now(), LocalDate.now().plusDays(1), null));
    }
    @Test
    @Order(4)
    void damageAndMaintenanceLifecycle() throws Exception {
        Category cat = category("Test Meter");
        Equipment item = add("TST-0040", "DMM", cat, Condition.GOOD);
        IssueRecord record = ctx.issues().issue(item.getId(), "Dan", "STU-400", "STUDENT", "", "", LocalDate.now(), LocalDate.now().plusDays(2), null);
        ctx.issues().returnItem(record.getId(), LocalDate.now(), Condition.DAMAGED, "probe socket broken");
        assertEquals(EquipmentStatus.DAMAGED, ctx.equipment().get(item.getId()).getStatus());
        assertTrue(ctx.maintenance().list("OPEN", "DAMAGE", "TST-0040").size() >= 1);
        assertThrows(OperationException.class, () -> ctx.maintenance().reportDamage(item.getId(), "more damage", "tester"));
        assertThrows(OperationException.class, () -> ctx.maintenance().requestMaintenance(item.getId(), "service", "tester"));
        var damage = ctx.maintenance().list("OPEN", "DAMAGE", "TST-0040").get(0);
        ctx.maintenance().complete(damage.getId(), "Replaced probe socket", Condition.GOOD);
        assertEquals(EquipmentStatus.AVAILABLE, ctx.equipment().get(item.getId()).getStatus());
        assertThrows(OperationException.class, () -> ctx.maintenance().complete(damage.getId(), "again", Condition.GOOD));
        Equipment pc = ctx.equipment().create("TST-0050", "Lab PC", true, cat.getId(), "PC-SN-1", "Dell", "Lab C", LocalDate.now().minusYears(1), Condition.GOOD, null);
        assertTrue(pc instanceof ComputerEquipment);
        ctx.maintenance().requestMaintenance(pc.getId(), "Fan replacement", "tech");
        assertEquals(EquipmentStatus.MAINTENANCE, ctx.equipment().get(pc.getId()).getStatus());
        var job = ctx.maintenance().list("OPEN", "MAINTENANCE", "TST-0050").get(0);
        assertEquals(MaintenanceType.MAINTENANCE, job.getType());
        ctx.maintenance().updateStatus(job.getId(), MaintenanceStatus.IN_PROGRESS);
        assertEquals(MaintenanceStatus.IN_PROGRESS, ctx.maintenance().list("OPEN", "MAINTENANCE", "TST-0050").get(0).getStatus());
        assertThrows(ValidationException.class, () -> ctx.maintenance().complete(job.getId(), " ", Condition.GOOD));
        ctx.maintenance().complete(job.getId(), "Fan replaced, chassis scratched", Condition.POOR);
        assertEquals(EquipmentStatus.DAMAGED, ctx.equipment().get(pc.getId()).getStatus());
        var second = ctx.maintenance().requestMaintenance(pc.getId(), "Cosmetic rework", "tech");
        ctx.maintenance().complete(second.getId(), "Cleaned and tested", Condition.GOOD);
        assertEquals(EquipmentStatus.AVAILABLE, ctx.equipment().get(pc.getId()).getStatus());
        Equipment other = add("TST-0051", "Issued Scope", cat, Condition.GOOD);
        ctx.issues().issue(other.getId(), "Eve", "STU-500", "STUDENT", "", "", LocalDate.now(), LocalDate.now().plusDays(1), null);
        assertThrows(OperationException.class, () -> ctx.maintenance().reportDamage(other.getId(), "smoke", "tester"));
    }
    @Test
    @Order(5)
    void dashboardReportsAdapterAndPersistence() throws Exception {
        Map<String, Integer> stats = ctx.reports().dashboardStats();
        assertEquals(countRows("equipment"), stats.get("TOTAL"));
        assertFalse(ctx.reports().recentActivity().isEmpty());
        assertFalse(ctx.reports().overdueIssues().isEmpty());
        assertFalse(ctx.reports().inventory().isEmpty());
        assertFalse(ctx.reports().borrowingHistory().isEmpty());
        var specs = ctx.reports().reportSpecs();
        assertEquals(10, specs.size());
        LegacyTsvAdapter adapter = new LegacyTsvAdapter();
        Equipment legacy = adapter.adapt(new LegacyAssetRecord(Map.of("ASTID", "LEG-9", "DESC", "Legacy Scope", "CLASS", "ELECTRONIC", "CATEGORY", "Oscilloscope", "COND", "Good")));
        assertEquals("LEG-9", legacy.getCode());
        assertTrue(legacy instanceof labtrack.model.ElectronicEquipment);
        assertTrue(adapter.adapt(new LegacyAssetRecord(Map.of("ASTID", "LEG-10", "DESC", "Legacy PC", "CLASS", "COMPUTER", "CATEGORY", "Computer", "COND", "Fair"))) instanceof ComputerEquipment);
        List<Equipment> parsed = adapter.readAll(new java.io.StringReader("ASTID\tDESC\tCLASS\tCATEGORY\tMAKE\tSERIAL\tLOCTYPE\tPURDATE\tCOND\tNOTES\n# comment\nLEG-11\tBench PSU\tELECTRONIC\tPower Supply\tMastech\tM-1\tLab\t2021-05-05\tGood\t\n"));
        assertEquals(1, parsed.size());
        assertEquals("LEG-11", parsed.get(0).getCode());
        assertEquals(1, ctx.equipment().list("AST-0001", 0, null, null, true).size());
        int countAfterSeed = ctx.equipment().list(null, 0, null, null, true).size();
        LegacyDataSeeder.seedIfEmpty(ctx);
        assertEquals(countAfterSeed, ctx.equipment().list(null, 0, null, null, true).size());
        Equipment marker = add("TST-0090", "Persistence Marker", category("Test Meter"), Condition.GOOD);
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbDir.resolve("labtrack.db"));
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM equipment WHERE code = 'TST-0090'")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
        assertNotNull(new AppContext().equipment().get(marker.getId()));
    }
    private static int countRows(String table) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbDir.resolve("labtrack.db"));
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
