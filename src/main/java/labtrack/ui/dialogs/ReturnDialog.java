package labtrack.ui.dialogs;
import labtrack.exception.ValidationException;
import labtrack.model.Condition;
import labtrack.model.IssueRecord;
import labtrack.util.Dates;
import labtrack.util.UiUtil;
import javax.swing.*;
import java.awt.Frame;
import java.time.LocalDate;
/** Records the return of one borrowed item, including its condition on return. */
public class ReturnDialog extends FormDialog {
    public record Request(LocalDate returnedDate, Condition condition, String remarks) {}
    private final JTextField dateField = UiUtil.field("yyyy-MM-dd");
    private final JComboBox<Condition> conditionCombo = new JComboBox<>(Condition.values());
    private final JTextArea remarksArea = new JTextArea(2, 22);
    private Request result;
    public ReturnDialog(Frame owner, IssueRecord record) {
        super(owner, "Record Return", record.getEquipmentLabel() + " · Borrower: " + record.getBorrowerLabel()
                + " · Issued " + record.getIssuedDate() + " · Due " + record.getDueDate()
                + (record.isOverdue() ? " · OVERDUE by " + record.overdueDays() + " day(s)" : ""));
        dateField.setText(LocalDate.now().toString());
        conditionCombo.setSelectedItem(Condition.GOOD);
        addRow("Return date *", dateField);
        addRow("Condition on return *", conditionCombo);
        addRow("Remarks", new JScrollPane(remarksArea));
        addRow("Note", UiUtil.muted("Choosing 'Damaged' also opens a damage record and keeps the item locked."));
    }
    public boolean open() {
        return open("Record Return", UiUtil.Kind.SUCCESS, () -> {
            LocalDate date = Dates.parse(dateField.getText());
            if (date == null) throw new ValidationException("Return date is required.");
            result = new Request(date, (Condition) conditionCombo.getSelectedItem(), remarksArea.getText().trim());
            return true;
        });
    }
    public Request getResult() { return result; }
}
