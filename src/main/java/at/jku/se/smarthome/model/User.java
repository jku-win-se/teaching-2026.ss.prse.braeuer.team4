package at.jku.se.smarthome.model;

import javafx.beans.property.SimpleStringProperty;

/**
 * Represents a User in the SmartHome system.
 */
public class User {
    
    /** Email address of the user. */
    private final SimpleStringProperty email;
    /** Display name or login name. */
    private final SimpleStringProperty username;
    /** Stored password or password hash. */
    private final SimpleStringProperty password;
    /** Assigned user role. */
    private final SimpleStringProperty role;
    /** Current account status. */
    private final SimpleStringProperty status;
    
    /**
     * Creates a user record for the smart-home system.
     *
     * @param email unique user email address
     * @param username display name or login name
     * @param password stored password or password hash value
     * @param role assigned role
     * @param status current account status
     */
    public User(String email, String username, String password, String role, String status) {
        this.email = new SimpleStringProperty(email);
        this.username = new SimpleStringProperty(username);
        this.password = new SimpleStringProperty(password);
        this.role = new SimpleStringProperty(role);
        this.status = new SimpleStringProperty(status);
    }

    /**
     * Returns the email address.
     *
     * @return email address
     */
    public String getEmail() { return email.get(); }
    /**
     * Updates the email address.
     *
     * @param email updated email address
     */
    public void setEmail(String email) { this.email.set(email); }
    /**
     * Exposes the JavaFX email property.
     *
     * @return email property
     */
    public SimpleStringProperty emailProperty() { return email; }

    /**
     * Returns the username.
     *
     * @return username
     */
    public String getUsername() { return username.get(); }
    /**
     * Updates the username.
     *
     * @param username updated username
     */
    public void setUsername(String username) { this.username.set(username); }
    /**
     * Exposes the JavaFX username property.
     *
     * @return username property
     */
    public SimpleStringProperty usernameProperty() { return username; }

    /**
     * Returns the stored password value.
     *
     * @return password or password hash
     */
    public String getPassword() { return password.get(); }
    /**
     * Updates the stored password value.
     *
     * @param password updated password or password hash
     */
    public void setPassword(String password) { this.password.set(password); }
    /**
     * Exposes the JavaFX password property.
     *
     * @return password property
     */
    public SimpleStringProperty passwordProperty() { return password; }

    /**
     * Returns the assigned role.
     *
     * @return user role
     */
    public String getRole() { return role.get(); }
    /**
     * Updates the assigned role.
     *
     * @param role updated role
     */
    public void setRole(String role) { this.role.set(role); }
    /**
     * Exposes the JavaFX role property.
     *
     * @return role property
     */
    public SimpleStringProperty roleProperty() { return role; }

    /**
     * Returns the account status.
     *
     * @return account status
     */
    public String getStatus() { return status.get(); }
    /**
     * Updates the account status.
     *
     * @param status updated account status
     */
    public void setStatus(String status) { this.status.set(status); }
    /**
     * Exposes the JavaFX status property.
     *
     * @return status property
     */
    public SimpleStringProperty statusProperty() { return status; }
}
