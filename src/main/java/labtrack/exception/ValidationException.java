package labtrack.exception;
/** Thrown when user input fails validation. */
public class ValidationException extends LabTrackException {
    public ValidationException(String message) { super(message); }
}
