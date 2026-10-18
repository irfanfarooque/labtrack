package labtrack.model;
import java.time.LocalDate;
public class ElectronicEquipment extends Equipment {
    public ElectronicEquipment(int id, String code, String name, Category category, String serialNumber,
                               String manufacturer, String location, LocalDate purchaseDate,
                               Condition condition, EquipmentStatus status, String notes) {
        super(id, code, name, category, serialNumber, manufacturer, location, purchaseDate, condition, status, notes);
    }
    @Override
    public String equipmentType() { return "Electronic Equipment"; }
/** Powered test gear in poor condition is unsafe to lend. */
    @Override
    public boolean isBorrowable() { return super.isBorrowable() && getCondition() != Condition.POOR; }
    @Override
    public String borrowNote() { return "Return with all probes, cables and accessories."; }
    @Override
    public String inspectionChecklist() { return "Visual inspection, power-on test, output verification."; }
}
