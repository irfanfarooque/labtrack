package labtrack.model;
import java.time.LocalDate;
/** Base class for every tracked item; subclasses decide borrowing rules (runtime polymorphism). */
public abstract class Equipment implements Borrowable, Maintainable {
    private int id;
    private String code;
    private String name;
    private Category category;
    private String serialNumber, manufacturer, location, notes;
    private LocalDate purchaseDate;
    private Condition condition;
    private EquipmentStatus status;
    public Equipment(int id, String code, String name, Category category, String serialNumber,
                     String manufacturer, String location, LocalDate purchaseDate,
                     Condition condition, EquipmentStatus status, String notes) {
        this.id = id; this.code = code; this.name = name; this.category = category;
        this.serialNumber = serialNumber; this.manufacturer = manufacturer; this.location = location;
        this.purchaseDate = purchaseDate;
        this.condition = condition == null ? Condition.GOOD : condition;
        this.status = status == null ? EquipmentStatus.AVAILABLE : status;
        this.notes = notes;
    }
/** Equipment family: the DAO uses it to rebuild the correct subclass. */
    public abstract String equipmentType();
    @Override
    public boolean isBorrowable() { return status == EquipmentStatus.AVAILABLE && condition != Condition.DAMAGED; }
    @Override
    public String borrowNote() { return "Standard loan terms apply."; }
    @Override
    public boolean isMaintenanceLocked() { return status == EquipmentStatus.DAMAGED || status == EquipmentStatus.MAINTENANCE; }
    @Override
    public String inspectionChecklist() { return "Visual inspection and functional check."; }
    public boolean isArchived() { return status == EquipmentStatus.ARCHIVED; }
    public String displayLabel() { return code + " — " + name; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String s) { this.serialNumber = s; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String m) { this.manufacturer = m; }
    public String getLocation() { return location; }
    public void setLocation(String l) { this.location = l; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate d) { this.purchaseDate = d; }
    public Condition getCondition() { return condition; }
    public void setCondition(Condition c) { this.condition = c; }
    public EquipmentStatus getStatus() { return status; }
    public void setStatus(EquipmentStatus s) { this.status = s; }
    public String getNotes() { return notes; }
    public void setNotes(String n) { this.notes = n; }
}
