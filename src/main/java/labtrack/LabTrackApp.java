package labtrack;
import labtrack.adapter.LegacyDataSeeder;
import labtrack.exception.LabTrackException;
import labtrack.service.AppContext;
import labtrack.ui.LoginDialog;
import labtrack.ui.MainFrame;
import labtrack.util.UiUtil;
import javax.swing.SwingUtilities;
/** LabTrack application entry point: login gate, then the main window. */
public final class LabTrackApp {
    private LabTrackApp() {}
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UiUtil.installLookAndFeel();
            AppContext ctx = new AppContext();
            LegacyDataSeeder.seedIfEmpty(ctx);
            try { ctx.auth().seedDefaultUser(); }
            catch (LabTrackException e) { UiUtil.showError(null, e.getMessage()); return; }
            startSession(ctx);
        });
    }
    /** Shows the login dialog; on success opens the main window, re-prompting after logout. */
    private static void startSession(AppContext ctx) {
        LoginDialog login = new LoginDialog(null, ctx.auth());
        if (!login.open()) return;
        MainFrame frame = new MainFrame(ctx, login.getUser().username());
        frame.onLogout(() -> { frame.dispose(); startSession(ctx); });
        frame.setVisible(true);
    }
}
