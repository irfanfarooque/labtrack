package labtrack.model;
/** Single source of truth for equipment availability: borrowable only while AVAILABLE (plus condition rules). */
public enum EquipmentStatus {
    AVAILABLE("Available"), ISSUED("Issued"), DAMAGED("Damaged"), MAINTENANCE("Maintenance"), ARCHIVED("Archived");
    private final String label;
    EquipmentStatus(String label) { this.label = label; }
    public String label() { return label; }
    public static EquipmentStatus from(String value) {
        if (value == null || value.isBlank()) return AVAILABLE;
        String v = value.trim().toUpperCase();
        for (EquipmentStatus s : values()) if (s.name().equals(v) || s.label.equalsIgnoreCase(v)) return s;
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
