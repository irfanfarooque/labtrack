package labtrack.util;
import labtrack.exception.ValidationException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
/** ISO date parsing/formatting and overdue calculations. */
public final class Dates {
    private Dates() {}
    public static LocalDate parse(String iso) throws ValidationException {
        if (iso == null || iso.isBlank()) return null;
        try { return LocalDate.parse(iso.trim()); }
        catch (DateTimeParseException e) { throw new ValidationException("Invalid date '" + iso + "'. Use format yyyy-MM-dd."); }
    }
    public static String format(LocalDate date) { return date == null ? "" : date.toString(); }
    public static boolean isOverdue(LocalDate dueDate) { return dueDate != null && dueDate.isBefore(LocalDate.now()); }
    public static long overdueDays(LocalDate dueDate) { return isOverdue(dueDate) ? ChronoUnit.DAYS.between(dueDate, LocalDate.now()) : 0; }
}
