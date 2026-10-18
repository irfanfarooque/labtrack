package labtrack.exception;
/** Thrown when a unique identifier already exists (asset tag, serial number, borrower ID). */
public class DuplicateRecordException extends LabTrackException {
    public DuplicateRecordException(String message) { super(message); }
}
