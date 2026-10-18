package labtrack.model;
/** Extensible equipment category stored in the database. */
public class Category {
    private final int id;
    private final String name;
    public Category(int id, String name) { this.id = id; this.name = name; }
    public int getId() { return id; }
    public String getName() { return name; }
    /** Two categories are the same when they share the database id (needed for combo-box selection). */
    @Override
    public boolean equals(Object o) { return o instanceof Category c && c.id == id; }
    @Override
    public int hashCode() { return id; }
    @Override
    public String toString() { return name; }
}
