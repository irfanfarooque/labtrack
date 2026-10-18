package labtrack.ui;
import labtrack.exception.LabTrackException;
import labtrack.model.User;
import labtrack.service.AuthService;
import labtrack.util.UiUtil;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
/** Modal sign-in screen shown before the main window; keeps the authenticated user. */
public class LoginDialog extends JDialog {
    private final AuthService auth;
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JLabel error = UiUtil.label(" ", 12, Font.PLAIN, UiUtil.DANGER);
    private User user;
    public LoginDialog(Window owner, AuthService auth) {
        super(owner, "LabTrack Login", Dialog.ModalityType.APPLICATION_MODAL);
        this.auth = auth;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        usernameField.setPreferredSize(passwordField.getPreferredSize());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> attemptLogin());
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new javax.swing.BoxLayout(head, javax.swing.BoxLayout.Y_AXIS));
        head.add(UiUtil.label("LabTrack", 22, Font.BOLD, UiUtil.TEXT));
        head.add(UiUtil.label("Sign in to manage laboratory equipment", 12, Font.PLAIN, UiUtil.MUTED));
        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new javax.swing.BoxLayout(rows, javax.swing.BoxLayout.Y_AXIS));
        rows.add(UiUtil.formRow("Username", usernameField));
        rows.add(UiUtil.formRow("Password", passwordField));
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(rows, BorderLayout.NORTH);
        center.add(error, BorderLayout.CENTER);
        center.add(UiUtil.muted("Sample account: admin / lab123"), BorderLayout.SOUTH);
        JButton loginButton = UiUtil.btn("Log In", UiUtil.Kind.PRIMARY, this::attemptLogin);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setOpaque(false);
        bar.add(UiUtil.btn("Exit", UiUtil.Kind.SECONDARY, this::dispose));
        bar.add(loginButton);
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(UiUtil.SURFACE);
        root.setBorder(BorderFactory.createEmptyBorder(20, 22, 16, 22));
        root.add(head, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(bar, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(loginButton);
    }
    /** Shows the dialog; true when the user authenticated (see getUser()). */
    public boolean open() {
        pack();
        setSize(Math.max(430, getWidth()), getHeight());
        setLocationRelativeTo(getOwner());
        setVisible(true);
        return user != null;
    }
    public User getUser() { return user; }
    private void attemptLogin() {
        try {
            user = auth.login(usernameField.getText(), new String(passwordField.getPassword()));
            dispose();
        } catch (LabTrackException e) {
            error.setText(e.getMessage());
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }
}
