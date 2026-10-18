package labtrack.model;
public enum IssueStatus {
    ACTIVE("Active"), RETURNED("Returned");
    private final String label;
    IssueStatus(String label) { this.label = label; }
    public String label() { return label; }
    public static IssueStatus from(String value) {
        return "RETURNED".equalsIgnoreCase(value == null ? "" : value.trim()) ? RETURNED : ACTIVE;
    }
}
