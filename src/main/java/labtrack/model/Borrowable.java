package labtrack.model;
/** Anything that can be lent out; implementations decide their own borrowing rules. */
public interface Borrowable {
    boolean isBorrowable();
    String borrowNote();
}
