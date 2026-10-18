package labtrack.ui.dialogs;
import labtrack.exception.ValidationException;
import labtrack.model.Condition;
import labtrack.model.MaintenanceRecord;
import labtrack.util.UiUtil;
import javax.swing.*;
import java.awt.Frame;
/** Closes a damage/maintenance record: resolution notes plus the repaired condition. */
public class CompleteMaintenanceDialog extends FormDialog {
    public record Request(String resolutionNotes, Condition newCondition) {}
    private final JTextArea notesArea = new JTextArea(4, 24);
    private final JComboBox<Condition> conditionCombo = new JComboBox<>(Condition.values());
    private Request result;
    public CompleteMaintenanceDialog(Frame owner, MaintenanceRecord record) {
        super(owner, "Complete Record", record.getEquipmentLabel() + " · " + record.getType().label()
                + " reported " + record.getReportedDate() + ": " + record.getDescription());
        conditionCombo.setSelectedItem(Condition.GOOD);
        addRow("Resolution notes *", new JScrollPane(notesArea));
        addRow("Condition after repair *", conditionCombo);
        addRow("Note", UiUtil.muted("The item only becomes available again if no other record is open and its condition allows it."));
    }
    public boolean open() {
        return open("Complete", UiUtil.Kind.SUCCESS, () -> {
            if (notesArea.getText().isBlank())
                throw new ValidationException("Resolution notes are required.");
            result = new Request(notesArea.getText().trim(), (Condition) conditionCombo.getSelectedItem());
            return true;
        });
    }
    public Request getResult() { return result; }
}
