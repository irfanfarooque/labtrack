package labtrack.model;
import java.time.LocalDate;
public class MaintenanceRecord {
    private int id;
    private final int equipmentId;
    private final String equipmentLabel, description, resolutionNotes, reportedBy;
    private final MaintenanceType type;
    private final MaintenanceStatus status;
    private final LocalDate reportedDate, resolvedDate;
    public MaintenanceRecord(int id, int equipmentId, String equipmentLabel, MaintenanceType type,
                             String description, String resolutionNotes, MaintenanceStatus status,
                             LocalDate reportedDate, LocalDate resolvedDate, String reportedBy) {
        this.id = id; this.equipmentId = equipmentId; this.equipmentLabel = equipmentLabel; this.type = type;
        this.description = description; this.resolutionNotes = resolutionNotes; this.status = status;
        this.reportedDate = reportedDate; this.resolvedDate = resolvedDate; this.reportedBy = reportedBy;
    }
    public boolean isPending() { return status != MaintenanceStatus.COMPLETED; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEquipmentId() { return equipmentId; }
    public String getEquipmentLabel() { return equipmentLabel; }
    public MaintenanceType getType() { return type; }
    public String getDescription() { return description; }
    public String getResolutionNotes() { return resolutionNotes; }
    public MaintenanceStatus getStatus() { return status; }
    public LocalDate getReportedDate() { return reportedDate; }
    public LocalDate getResolvedDate() { return resolvedDate; }
    public String getReportedBy() { return reportedBy; }
}
