package labtrack.ui.dialogs;
import labtrack.exception.ValidationException;
import labtrack.model.Equipment;
import labtrack.util.UiUtil;
import javax.swing.*;
import java.awt.Frame;
import java.util.List;
/** Opens a damage report or a maintenance request: pick the equipment, describe the problem. */
public class MaintenanceDialog extends FormDialog {
    public record Request(Equipment item, String description, String reportedBy) {}
    private final JComboBox<Equipment> equipmentCombo = new JComboBox<>();
    private final JTextArea descriptionArea = new JTextArea(4, 24);
    private final JTextField reportedByField = UiUtil.field("Who reported it");
    private final JLabel noteLabel = UiUtil.muted("");
    private Request result;
    public MaintenanceDialog(Frame owner, String title, List<Equipment> items) {
        super(owner, title, "The selected item stays locked until the record is completed.");
        items.forEach(equipmentCombo::addItem);
        equipmentCombo.addActionListener(e -> updateNote());
        reportedByField.setText("Lab staff");
        updateNote();
        addRow("Equipment *", equipmentCombo);
        addRow("Details", noteLabel);
        addRow("Description *", new JScrollPane(descriptionArea));
        addRow("Reported by", reportedByField);
    }
    private void updateNote() {
        Equipment item = (Equipment) equipmentCombo.getSelectedItem();
        noteLabel.setText(item == null ? "" : item.equipmentType() + " · " + item.getCategory()
                + " · Condition: " + item.getCondition().label() + " · Checklist: " + item.inspectionChecklist());
    }
    public boolean open() {
        return open("Save Record", UiUtil.Kind.PRIMARY, () -> {
            if (descriptionArea.getText().isBlank()) throw new ValidationException("A description of the problem is required.");
            result = new Request((Equipment) equipmentCombo.getSelectedItem(), descriptionArea.getText().trim(), reportedByField.getText().trim());
            return true;
        });
    }
    public Request getResult() { return result; }
}
