package labtrack;
import labtrack.model.Borrower;
import labtrack.model.Equipment;
import labtrack.model.IssueRecord;
import labtrack.model.IssueStatus;
import labtrack.model.MaintenanceRecord;
import labtrack.model.MaintenanceStatus;
import labtrack.model.MaintenanceType;
import labtrack.service.AppContext;
import labtrack.ui.MainFrame;
import labtrack.ui.dialogs.*;
import labtrack.util.UiUtil;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import java.awt.GraphicsEnvironment;
import java.time.LocalDate;
import java.util.List;
import javax.swing.SwingUtilities;
import static org.junit.jupiter.api.Assertions.assertTrue;
/** Launches the real UI (window + every dialog) on a display; skipped when headless. */
class UiSmokeTest {
    @Test
    void mainWindowAndDialogsComeUp() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "No display - skipped");
        java.nio.file.Path shared = TestDb.DIR;
        SwingUtilities.invokeAndWait(() -> {
            UiUtil.installLookAndFeel();
            AppContext ctx = new AppContext();
            labtrack.adapter.LegacyDataSeeder.seedIfEmpty(ctx);
            MainFrame frame = new MainFrame(ctx);
            frame.setVisible(true);
            assertTrue(frame.isShowing() && frame.getTitle().contains("LabTrack"));
            for (String nav : new String[]{"Dashboard", "Equipment Inventory", "Issue & Return", "Damage & Maintenance", "Reports"})
                assertTrue(findButton(frame, nav) != null, "sidebar nav button missing: " + nav);
            findButton(frame, "Reports").doClick();
            findButton(frame, "Dashboard").doClick();
            try {
                Equipment item = ctx.equipment().list(null, 0, null, null, true).get(0);
                new labtrack.ui.LoginDialog(frame, ctx.auth()).dispose();
                new EquipmentDialog(frame, ctx.equipment().categories(), item, true).dispose();
                new EquipmentDialog(frame, ctx.equipment().categories(), null, true).dispose();
                new EquipmentDialog(frame, ctx.equipment().categories(), item, false).dispose();
                new IssueDialog(frame, List.of(item), null).dispose();
                new IssueDialog(frame, null, item).dispose();
                new ReturnDialog(frame, sampleIssue(item)).dispose();
                new MaintenanceDialog(frame, "Report Damage", List.of(item)).dispose();
                new CompleteMaintenanceDialog(frame, sampleMaintenance(item)).dispose();
            } catch (Exception e) { throw new IllegalStateException(e); }
            frame.dispose();
        });
    }
    private static IssueRecord sampleIssue(Equipment item) {
        Borrower b = new Borrower(1, "Test Borrower", "STU-999", "STUDENT", "", "");
        return new IssueRecord(1, item.getId(), item.displayLabel(), b.getId(), b.displayLabel(), LocalDate.now().minusDays(2), LocalDate.now().plusDays(5), null, null, null, IssueStatus.ACTIVE);
    }
    private static javax.swing.JButton findButton(java.awt.Component root, String text) {
        if (root instanceof javax.swing.JButton b && text.equals(b.getText())) return b;
        if (root instanceof java.awt.Container c)
            for (java.awt.Component child : c.getComponents()) {
                javax.swing.JButton found = findButton(child, text);
                if (found != null) return found;
            }
        return null;
    }
    private static MaintenanceRecord sampleMaintenance(Equipment item) {
        return new MaintenanceRecord(1, item.getId(), item.displayLabel(), MaintenanceType.MAINTENANCE, "Smoke-test record", null, MaintenanceStatus.PENDING, LocalDate.now(), null, "tester");
    }
}
