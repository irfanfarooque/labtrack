package labtrack.model;
public enum MaintenanceType {
    DAMAGE("Damage report"), MAINTENANCE("Maintenance");
    private final String label;
    MaintenanceType(String label) { this.label = label; }
    public String label() { return label; }
    public static MaintenanceType from(String value) { return "DAMAGE".equalsIgnoreCase(value) ? DAMAGE : MAINTENANCE; }
}
