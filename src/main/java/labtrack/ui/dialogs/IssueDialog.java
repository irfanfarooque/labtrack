package labtrack.ui.dialogs;
import labtrack.exception.ValidationException;
import labtrack.model.Equipment;
import labtrack.util.Dates;
import labtrack.util.UiUtil;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.time.LocalDate;
import java.util.List;
/** Issue equipment to a borrower: borrower details, dates and remarks. */
public class IssueDialog extends FormDialog {
/** Everything the panel needs to call the issue service. */
    public record Request(int equipmentId, String equipmentLabel, String name, String code, String role,
                          String email, String phone, LocalDate issued, LocalDate due, String remarks) {}
    private final JComboBox<Equipment> equipmentCombo = new JComboBox<>();
    private final JLabel borrowNoteLabel = UiUtil.muted("");
    private final JTextField nameField = UiUtil.field("Full name");
    private final JTextField codeField = UiUtil.field("Student / staff ID");
    private final JComboBox<String> roleCombo = new JComboBox<>(new String[]{"STUDENT", "STAFF"});
    private final JTextField emailField = UiUtil.field("Email (optional)");
    private final JTextField phoneField = UiUtil.field("Phone (optional)");
    private final JTextField issuedField = UiUtil.field("yyyy-MM-dd");
    private final JTextField dueField = UiUtil.field("yyyy-MM-dd");
    private final JTextArea remarksArea = new JTextArea(2, 22);
    private final boolean fixed;
    private Request result;
    public IssueDialog(Frame owner, List<Equipment> availableItems, Equipment fixedItem) {
        super(owner, "Issue Equipment", "An item can only be issued while it is available and in usable condition.");
        fixed = fixedItem != null;
        if (fixedItem != null) equipmentCombo.addItem(fixedItem);
        else availableItems.forEach(equipmentCombo::addItem);
        equipmentCombo.setEnabled(!fixed);
        equipmentCombo.addActionListener(e -> updateBorrowNote());
        issuedField.setText(LocalDate.now().toString());
        dueField.setText(LocalDate.now().plusDays(7).toString());
        updateBorrowNote();
        if (fixed) {
            JPanel info = new JPanel(new BorderLayout());
            info.setOpaque(false);
            info.add(UiUtil.label(fixedItem.displayLabel(), 13, java.awt.Font.PLAIN, UiUtil.TEXT), BorderLayout.NORTH);
            info.add(borrowNoteLabel, BorderLayout.SOUTH);
            addRow("Equipment", info);
        } else {
            addRow("Equipment *", equipmentCombo);
        }
        addRow("Borrower name *", nameField);
        addRow("Borrower ID *", codeField);
        addRow("Role", roleCombo);
        addRow("Email", emailField);
        addRow("Phone", phoneField);
        addRow("Issue date *", issuedField);
        addRow("Due date *", dueField);
        addRow("Remarks", new JScrollPane(remarksArea));
        if (fixed) addRow("Loan note", borrowNoteLabel);
    }
    public boolean open() {
        return open("Issue Equipment", UiUtil.Kind.PRIMARY, this::build);
    }
    public Request getResult() { return result; }
    private void updateBorrowNote() {
        Equipment item = (Equipment) equipmentCombo.getSelectedItem();
        borrowNoteLabel.setText(item == null ? "" : "Note: " + item.borrowNote());
    }
    private boolean build() throws Exception {
        Equipment item = (Equipment) equipmentCombo.getSelectedItem();
        if (item == null) throw new ValidationException("Choose the equipment to issue.");
        if (nameField.getText().isBlank()) throw new ValidationException("Borrower name is required.");
        if (codeField.getText().isBlank()) throw new ValidationException("Borrower ID is required.");
        LocalDate issued = Dates.parse(issuedField.getText());
        LocalDate due = Dates.parse(dueField.getText());
        if (issued == null || due == null) throw new ValidationException("Issue date and due date are required.");
        if (due.isBefore(issued)) throw new ValidationException("Due date cannot be before the issue date.");
        result = new Request(item.getId(), item.displayLabel(), nameField.getText().trim(),
                codeField.getText().trim(), (String) roleCombo.getSelectedItem(),
                emailField.getText().trim(), phoneField.getText().trim(), issued, due, remarksArea.getText().trim());
        return true;
    }
}
