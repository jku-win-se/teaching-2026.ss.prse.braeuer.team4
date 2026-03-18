package at.jku.se.smarthome.model;

import javafx.beans.property.SimpleStringProperty;

/**
 * Represents a User in the SmartHome system.
 */
public class User {
    
    private final SimpleStringProperty email;
    private final SimpleStringProperty username;
    private final SimpleStringProperty password;
    private final SimpleStringProperty role;
    private final SimpleStringProperty status;
    
    public User(String email, String username, String password, String role, String status) {
        this.email = new SimpleStringProperty(email);
        this.username = new SimpleStringProperty(username);
        this.password = new SimpleStringProperty(password);
        this.role = new SimpleStringProperty(role);
        this.status = new SimpleStringProperty(status);
    }
    
    public String getEmail() { return email.get(); }
    public void setEmail(String email) { this.email.set(email); }
    public SimpleStringProperty emailProperty() { return email; }
    
    public String getUsername() { return username.get(); }
    public void setUsername(String username) { this.username.set(username); }
    public SimpleStringProperty usernameProperty() { return username; }
    
    public String getPassword() { return password.get(); }
    public void setPassword(String password) { this.password.set(password); }
    public SimpleStringProperty passwordProperty() { return password; }
    
    public String getRole() { return role.get(); }
    public void setRole(String role) { this.role.set(role); }
    public SimpleStringProperty roleProperty() { return role; }
    
    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
    public SimpleStringProperty statusProperty() { return status; }
}
