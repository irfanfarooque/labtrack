package labtrack.ui;
import labtrack.service.AppContext;
import labtrack.ui.panels.BorrowingPanel;
import labtrack.ui.panels.DashboardPanel;
import labtrack.ui.panels.InventoryPanel;
import labtrack.ui.panels.MaintenancePanel;
import labtrack.ui.panels.ReportsPanel;
import labtrack.util.UiUtil;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
/** Main window: dark navigation sidebar, page header, card-based content area. */
public class MainFrame extends JFrame {
    /** Implemented by every page so the frame can refresh it when shown. */
    public interface Refreshable { void refresh(); }
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Map<String, Runnable> refreshers = new LinkedHashMap<>();
    private final Map<String, String> subtitles = new LinkedHashMap<>();
    private final JLabel titleLabel = UiUtil.h1(""), subtitleLabel = UiUtil.muted("");
    public MainFrame(AppContext ctx) {
        super("LabTrack — Laboratory Equipment Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 680));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        addPage("dashboard", new DashboardPanel(ctx), "Dashboard", "Overview of laboratory equipment and recent activity");
        addPage("inventory", new InventoryPanel(ctx), "Equipment Inventory", "Add, edit, search and archive laboratory equipment");
        addPage("borrowing", new BorrowingPanel(ctx), "Issue & Return", "Lending records, returns and overdue tracking");
        addPage("maintenance", new MaintenancePanel(ctx), "Damage & Maintenance", "Damage reports, repairs and inspection history");
        addPage("reports", new ReportsPanel(ctx), "Reports", "Database-driven views over inventory, loans and maintenance");
        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        add(buildHeader(), BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        navigate("dashboard");
    }
    private void addPage(String key, JPanel panel, String title, String subtitle) {
        panel.setBackground(UiUtil.BG);
        content.add(panel, key);
        subtitles.put(key, subtitle);
        refreshers.put(key, () -> ((Refreshable) panel).refresh());
        JButton b = new JButton(title);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        b.setForeground(UiUtil.SIDEBAR_T);
        b.setBackground(UiUtil.SIDEBAR);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(BorderFactory.createEmptyBorder(12, 22, 12, 22));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        b.addActionListener(e -> navigate(key));
        navButtons.put(key, b);
    }
    private void navigate(String key) {
        for (Map.Entry<String, JButton> e : navButtons.entrySet()) {
            boolean selected = e.getKey().equals(key);
            e.getValue().setBackground(selected ? new Color(0x334155) : UiUtil.SIDEBAR);
            e.getValue().setForeground(selected ? Color.WHITE : UiUtil.SIDEBAR_T);
        }
        cards.show(content, key);
        titleLabel.setText(navButtons.get(key).getText());
        subtitleLabel.setText(subtitles.get(key));
        refreshers.get(key).run();
    }
    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(UiUtil.SIDEBAR);
        side.setPreferredSize(new Dimension(240, 0));
        JPanel brand = new JPanel(new BorderLayout());
        brand.setBackground(UiUtil.SIDEBAR);
        brand.setBorder(new EmptyBorder(26, 24, 22, 24));
        brand.add(UiUtil.label("LabTrack", 24, Font.BOLD, Color.WHITE), BorderLayout.NORTH);
        brand.add(UiUtil.label("Laboratory Equipment Management", 11, Font.PLAIN, UiUtil.SIDEBAR_T), BorderLayout.SOUTH);
        side.add(brand, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 4));
        buttons.setBackground(UiUtil.SIDEBAR);
        buttons.setBorder(new EmptyBorder(10, 0, 10, 0));
        for (JButton b : navButtons.values()) buttons.add(b);
        side.add(buttons, BorderLayout.CENTER);
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.LEFT));
        foot.setBackground(UiUtil.SIDEBAR);
        foot.setBorder(new EmptyBorder(10, 24, 16, 24));
        foot.add(UiUtil.label("SQLite database · works offline", 11, Font.PLAIN, UiUtil.SIDEBAR_T));
        side.add(foot, BorderLayout.SOUTH);
        return side;
    }
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiUtil.BORDER), new EmptyBorder(18, 28, 16, 28)));
        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(titleLabel, BorderLayout.NORTH);
        titles.add(subtitleLabel, BorderLayout.SOUTH);
        header.add(titles, BorderLayout.WEST);
        header.add(UiUtil.label(LocalDate.now().toString(), 12, Font.PLAIN, UiUtil.MUTED), BorderLayout.EAST);
        return header;
    }
}
