package labtrack.model;
/** Physical condition of a piece of equipment. */
public enum Condition {
    EXCELLENT("Excellent"), GOOD("Good"), FAIR("Fair"), POOR("Poor"), DAMAGED("Damaged");
    private final String label;
    Condition(String label) { this.label = label; }
    public String label() { return label; }
    public static Condition from(String value) {
        if (value == null || value.isBlank()) return GOOD;
        String v = value.trim().toUpperCase();
        for (Condition c : values()) if (c.name().equals(v) || c.label.equalsIgnoreCase(v)) return c;
        throw new IllegalArgumentException("Unknown condition: " + value);
    }
}
