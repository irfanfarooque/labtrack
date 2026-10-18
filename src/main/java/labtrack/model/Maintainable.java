package labtrack.model;
/** Anything that can be repaired; implementations report their lock state and inspection checklist. */
public interface Maintainable {
    boolean isMaintenanceLocked();
    String inspectionChecklist();
}
