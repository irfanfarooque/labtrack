package labtrack.exception;
/** Thrown when a record is missing or an operation is not allowed in the current state. */
public class OperationException extends LabTrackException {
    public OperationException(String message) { super(message); }
}
