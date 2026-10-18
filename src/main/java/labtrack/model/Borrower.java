package labtrack.model;
public class Borrower {
    private int id;
    private String name, code, role, email, phone;
    public Borrower(int id, String name, String code, String role, String email, String phone) {
        this.id = id; this.name = name; this.code = code; this.role = role; this.email = email; this.phone = phone;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getRole() { return role; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String displayLabel() { return code + " — " + name + " (" + role.toLowerCase() + ")"; }
    @Override
    public String toString() { return displayLabel(); }
}
