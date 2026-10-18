package labtrack.model;
/** A login account; passwords are hashed in the service layer and never stored or returned in plain text. */
public record User(int id, String username, String role) {}
