package labtrack;
import labtrack.adapter.LegacyDataSeeder;
import labtrack.service.AppContext;
import labtrack.ui.MainFrame;
import labtrack.util.UiUtil;
import javax.swing.SwingUtilities;
/** LabTrack application entry point. */
public final class LabTrackApp {
    private LabTrackApp() {}
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UiUtil.installLookAndFeel();
            AppContext ctx = new AppContext();
            LegacyDataSeeder.seedIfEmpty(ctx);
            new MainFrame(ctx).setVisible(true);
        });
    }
}
