package labtrack.util;
import javax.swing.JComponent;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.util.function.Consumer;
/** Runs database work off the Swing Event Dispatch Thread and delivers results back on it. */
public final class Tasks {
    @FunctionalInterface
    public interface Work<T> { T run() throws Exception; }
    @FunctionalInterface
    public interface Action { void run() throws Exception; }
    private Tasks() {}
    public static <T> void run(Component parent, Work<T> backgroundWork, Consumer<T> onSuccess) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return backgroundWork.run(); }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    UiUtil.showError(parent instanceof JComponent jc ? jc : null, cause.getMessage());
                }
            }
        }.execute();
    }
    public static void runVoid(Component parent, Action backgroundWork, Runnable onSuccess) {
        run(parent, () -> { backgroundWork.run(); return null; }, ignored -> onSuccess.run());
    }
}
