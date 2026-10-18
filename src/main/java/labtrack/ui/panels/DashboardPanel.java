package labtrack.ui.panels;
import labtrack.service.AppContext;
import labtrack.ui.MainFrame;
import labtrack.util.Tasks;
import labtrack.util.UiUtil;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.List;
import java.util.Map;
/** Dashboard: live statistics from the database plus a recent-activity feed. */
public class DashboardPanel extends JPanel implements MainFrame.Refreshable {
    private record DashboardData(Map<String, Integer> stats, List<String[]> activity) {}
    private final AppContext ctx;
    private final JPanel statsGrid = new JPanel(new GridLayout(2, 3, 18, 18));
    private final DefaultTableModel activityModel = UiUtil.tableModel("When", "Activity", "Details");
    public DashboardPanel(AppContext ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(0, 18));
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        statsGrid.setOpaque(false);
        add(statsGrid, BorderLayout.NORTH);
        JPanel card = UiUtil.card(new JScrollPane(UiUtil.table(activityModel)));
        JPanel wrap = new JPanel(new BorderLayout(0, 10));
        wrap.setOpaque(false);
        wrap.add(UiUtil.h2("Recent Activity"), BorderLayout.NORTH);
        wrap.add(card, BorderLayout.CENTER);
        add(wrap, BorderLayout.CENTER);
    }
    @Override
    public void refresh() {
        Tasks.run(this, () -> new DashboardData(ctx.reports().dashboardStats(), ctx.reports().recentActivity()), this::showData);
    }
    private void showData(DashboardData data) {
        Map<String, Integer> s = data.stats();
        Object[][] cards = {{"Total Equipment", "TOTAL", UiUtil.PRIMARY}, {"Available", "AVAILABLE", UiUtil.SUCCESS},
                {"Currently Issued", "ISSUED", new Color(0x0284C7)}, {"Damaged", "DAMAGED", UiUtil.DANGER},
                {"Under Maintenance", "MAINTENANCE", UiUtil.WARNING}, {"Overdue Returns", "OVERDUE", new Color(0xB91C1C)}};
        statsGrid.removeAll();
        for (Object[] c : cards) statsGrid.add(UiUtil.statCard((String) c[0], String.valueOf(s.getOrDefault((String) c[1], 0)), (Color) c[2]));
        statsGrid.revalidate();
        statsGrid.repaint();
        activityModel.setRowCount(0);
        for (String[] a : data.activity()) activityModel.addRow(a);
        if (activityModel.getRowCount() == 0)
            activityModel.addRow(new Object[]{"—", "Empty", "No activity recorded yet. Issue equipment or report damage to see events here."});
    }
}
