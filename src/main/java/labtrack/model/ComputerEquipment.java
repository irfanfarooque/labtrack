package labtrack.model;
import java.time.LocalDate;
/** Computers, laptops and peripherals: a stricter kind of electronic equipment. */
public class ComputerEquipment extends ElectronicEquipment {
    public ComputerEquipment(int id, String code, String name, Category category, String serialNumber,
                             String manufacturer, String location, LocalDate purchaseDate,
                             Condition condition, EquipmentStatus status, String notes) {
        super(id, code, name, category, serialNumber, manufacturer, location, purchaseDate, condition, status, notes);
    }
    @Override
    public String equipmentType() { return "Computer Equipment"; }
    @Override
    public String borrowNote() { return "User data must be backed up before return."; }
    @Override
    public String inspectionChecklist() { return "Boot test, OS/antivirus check, peripheral test."; }
}
