package labtrack.model;
public enum MaintenanceStatus {
    PENDING("Pending"), IN_PROGRESS("In progress"), COMPLETED("Completed");
    private final String label;
    MaintenanceStatus(String label) { this.label = label; }
    public String label() { return label; }
    public static MaintenanceStatus from(String value) {
        String v = value == null ? "" : value.trim().toUpperCase().replace(' ', '_');
        return switch (v) { case "IN_PROGRESS", "INPROGRESS" -> IN_PROGRESS;
            case "COMPLETED", "RESOLVED" -> COMPLETED; default -> PENDING; };
    }
}
