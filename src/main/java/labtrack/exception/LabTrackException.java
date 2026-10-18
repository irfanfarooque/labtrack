package labtrack.exception;
/** Base checked exception for all LabTrack business errors. */
public class LabTrackException extends Exception {
    public LabTrackException(String message) { super(message); }
    public LabTrackException(String message, Throwable cause) { super(message, cause); }
}
