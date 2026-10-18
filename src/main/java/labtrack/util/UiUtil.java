package labtrack.util;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
/** Shared look-and-feel helpers: colors, fonts and small reusable components. */
public final class UiUtil {
    public static final Color PRIMARY = new Color(0x1D4ED8), DANGER = new Color(0xB91C1C), SUCCESS = new Color(0x15803D), WARNING = new Color(0xB45309);
    public static final Color SIDEBAR = new Color(0x1E293B), SIDEBAR_T = new Color(0xCBd5E1);
    public static final Color BG = new Color(0xF1F5F9), SURFACE = Color.WHITE, BORDER = new Color(0xCBD5E1), TEXT = new Color(0x1E293B), MUTED = new Color(0x64748B);
    public enum Kind { PRIMARY, SECONDARY, DANGER, SUCCESS }
    private UiUtil() {}
    /** FlatLaf when available, otherwise the system look and feel; sets the global font. */
    public static void installLookAndFeel() {
        try {
            Class<?> flat = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            UIManager.setLookAndFeel((LookAndFeel) flat.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        }
        Font base = new Font("Segoe UI", Font.PLAIN, 13);
        UIManager.put("defaultFont", base);
        UIManager.put("Table.font", base);
        UIManager.put("Button.font", base);
    }
    public static JLabel label(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(color);
        return l;
    }
    public static JLabel h1(String t) { return label(t, 20, Font.BOLD, TEXT); }
    public static JLabel h2(String t) { return label(t, 14, Font.BOLD, TEXT); }
    public static JLabel muted(String t) { return label(t, 12, Font.PLAIN, MUTED); }
    /** Button running an action; the kind only tints the label (plain look-and-feel button). */
    public static JButton btn(String text, Kind kind, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", kind == Kind.PRIMARY ? Font.BOLD : Font.PLAIN, 13));
        b.setForeground(kind == Kind.DANGER ? DANGER : kind == Kind.SUCCESS ? SUCCESS : kind == Kind.PRIMARY ? PRIMARY : TEXT);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }
    /** Left-aligned horizontal toolbar bar with gaps. */
    public static JPanel bar(Component... components) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        for (Component c : components) p.add(c);
        return p;
    }
    /** The top-level Frame owning a component (dialogs need it as owner). */
    public static Frame owner(Component c) { return (Frame) javax.swing.SwingUtilities.getWindowAncestor(c); }
    public static JTextField field(String placeholder) {
        JTextField f = new JTextField(14);
        f.setToolTipText(placeholder);
        return f;
    }
    /** GridBag form row: label on the left, field on the right. */
    public static JPanel formRow(String labelText, JComponent field) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0; gc.insets = new Insets(4, 6, 4, 6); gc.anchor = GridBagConstraints.WEST;
        JLabel l = label(labelText, 13, Font.PLAIN, TEXT);
        l.setPreferredSize(new Dimension(130, l.getPreferredSize().height));
        row.add(l, gc);
        gc.gridx = 1; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        row.add(field, gc);
        return row;
    }
    /** White bordered card wrapping panel content. */
    public static JPanel card(JComponent content) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(SURFACE);
        wrap.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        wrap.add(content, BorderLayout.CENTER);
        return wrap;
    }
    /** Non-editable table model. */
    public static DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }
    /** Basic styled table. */
    public static JTable table(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(26);
        t.setShowVerticalLines(true);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }
    public static void showError(Component parent, String message) { JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE); }
    public static void info(Component parent, String message) { JOptionPane.showMessageDialog(parent, message, "LabTrack", JOptionPane.INFORMATION_MESSAGE); }
    public static boolean confirm(Component parent, String message, String title) {
        return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }
    /** Dashboard statistic card: bold number with a caption. */
    public static JPanel statCard(String caption, String value, Color accent) {
        JPanel body = new JPanel(new BorderLayout(0, 2));
        body.setBackground(SURFACE);
        body.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        body.add(label(value, 24, Font.BOLD, accent), BorderLayout.NORTH);
        body.add(label(caption, 12, Font.PLAIN, MUTED), BorderLayout.SOUTH);
        return body;
    }
}
