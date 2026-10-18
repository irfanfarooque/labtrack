package labtrack;
import labtrack.service.AppContext;
import labtrack.ui.panels.InventoryPanel;
import labtrack.util.UiUtil;
import org.junit.jupiter.api.Test;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
/** Regression: the inventory must not rebuild itself while idle, and row selection must survive. */
class InventorySelectionTest {
    @Test
    void tableStaysCalmAndSelectionSurvives() throws Exception {
        Path shared = TestDb.DIR;
        AppContext ctx = new AppContext();
        labtrack.adapter.LegacyDataSeeder.seedIfEmpty(ctx);
        InventoryPanel[] holder = new InventoryPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            UiUtil.installLookAndFeel();
            holder[0] = new InventoryPanel(ctx);
            holder[0].refresh();
        });
        JTable table = (JTable) field(holder[0], "table");
        long deadline = System.currentTimeMillis() + 5000;
        int rows = 0;
        while (System.currentTimeMillis() < deadline && (rows = rowCount(table)) == 0) Thread.sleep(50);
        assertTrue(rows > 0, "inventory table must load rows");
        AtomicInteger churn = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> table.getModel().addTableModelListener(e -> churn.incrementAndGet()));
        Thread.sleep(1000);
        int idleEvents = churn.get();
        SwingUtilities.invokeAndWait(() -> table.setRowSelectionInterval(0, 0));
        Thread.sleep(700);
        int[] selected = new int[]{-1};
        SwingUtilities.invokeAndWait(() -> selected[0] = table.getSelectedRow());
        System.out.println("PROBE: idle model events in 1s = " + idleEvents + ", selected row after 700ms = " + selected[0]);
        assertEquals(0, idleEvents, "table must not rebuild itself while idle");
        assertTrue(selected[0] >= 0, "row selection must survive while idle");
    }
    private static int rowCount(JTable t) throws Exception {
        int[] n = new int[1];
        SwingUtilities.invokeAndWait(() -> n[0] = t.getModel().getRowCount());
        return n[0];
    }
    private static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }
}
