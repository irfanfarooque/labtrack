package labtrack.ui.dialogs;
import labtrack.util.UiUtil;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
/** Shared dialog scaffold: title/subtitle header, label-field rows, Save/Cancel bar with error handling. */
public class FormDialog extends JDialog {
    @FunctionalInterface
    public interface SaveAction { boolean save() throws Exception; }
    private final JPanel rows = new JPanel();
    private boolean saved;
    public FormDialog(Window owner, String title, String subtitle) {
        super(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(UiUtil.label(title, 16, Font.BOLD, UiUtil.TEXT));
        head.add(Box.createVerticalStrut(2));
        head.add(UiUtil.label(subtitle, 12, Font.PLAIN, UiUtil.MUTED));
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(UiUtil.SURFACE);
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        root.add(head, BorderLayout.NORTH);
        root.add(rows, BorderLayout.CENTER);
        setContentPane(root);
    }
    protected void addRow(String label, JComponent field) { rows.add(UiUtil.formRow(label, field)); }
/** Disables every input inside the dialog (used for read-only view mode). */
    protected void setInputsEnabled(boolean enabled) {
        disableRecursive(getContentPane(), enabled);
    }
    private static void disableRecursive(java.awt.Container parent, boolean enabled) {
        for (java.awt.Component child : parent.getComponents()) {
            if (child instanceof javax.swing.text.JTextComponent || child instanceof javax.swing.JComboBox
                    || child instanceof javax.swing.AbstractButton) child.setEnabled(enabled);
            if (child instanceof java.awt.Container c) disableRecursive(c, enabled);
        }
    }
/** Shows the dialog. Save runs the action; returning true closes it and makes open() return true. */
    protected boolean open(String saveLabel, UiUtil.Kind kind, SaveAction onSave) {
        JButton save = UiUtil.btn(saveLabel, kind, () -> {
            try {
                if (onSave.save()) { saved = true; dispose(); }
            } catch (Exception e) { UiUtil.showError(this, e.getMessage()); }
        });
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setOpaque(false);
        bar.add(UiUtil.btn("Cancel", UiUtil.Kind.SECONDARY, this::dispose));
        bar.add(save);
        ((JPanel) getContentPane()).add(bar, BorderLayout.SOUTH);
        pack();
        setSize(Math.max(560, getWidth()), getHeight());
        setLocationRelativeTo(getOwner());
        setVisible(true);
        return saved;
    }
}
