package at.jku.se.smarthome.service;

import at.jku.se.smarthome.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock User Service providing user management functionality.
 */
public class MockUserService {
    
    private static MockUserService instance;
    private final ObservableList<User> users;
    private String currentUserEmail;
    private String currentUserRole;
    
    private MockUserService() {
        this.users = FXCollections.observableArrayList();
        initializeMockUsers();
    }
    
    public static synchronized MockUserService getInstance() {
        if (instance == null) {
            instance = new MockUserService();
        }
        return instance;
    }
    
    private void initializeMockUsers() {
        users.add(new User("owner@smarthome.com", "owner", "password123", "Owner", "Active"));
        users.add(new User("member@smarthome.com", "member", "password123", "Member", "Active"));
        users.add(new User("guest@smarthome.com", "guest", "password123", "Member", "Inactive"));
        users.add(new User("test", "test", "test", "Member", "Active"));
    }
    
    /**
     * Registers a new user.
     */
    public boolean register(String email, String username, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            return false;
        }
        
        if (users.stream().anyMatch(u -> u.getEmail().equals(email))) {
            return false;
        }
        
        User newUser = new User(email, username, password, "Member", "Active");
        users.add(newUser);
        return true;
    }
    
    /**
     * Authenticates a user.
     */
    public boolean login(String email, String password) {
        User user = users.stream()
                .filter(u -> u.getEmail().equals(email) && u.getPassword().equals(password))
                .findFirst()
                .orElse(null);
        
        if (user != null && "Active".equalsIgnoreCase(user.getStatus())) {
            this.currentUserEmail = email;
            this.currentUserRole = user.getRole();
            return true;
        }
        return false;
    }
    
    /**
     * Gets the current user's email.
     */
    public String getCurrentUserEmail() {
        return currentUserEmail;
    }
    
    /**
     * Gets the current user's role.
     */
    public String getCurrentUserRole() {
        return currentUserRole != null ? currentUserRole : "Guest";
    }

    /**
     * Gets the current user object.
     */
    public User getCurrentUser() {
        if (currentUserEmail == null) {
            return null;
        }

        return users.stream()
                .filter(user -> user.getEmail().equals(currentUserEmail))
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks whether the current user is an owner.
     */
    public boolean isOwner() {
        return "Owner".equalsIgnoreCase(getCurrentUserRole());
    }

    /**
     * Checks whether the current user can manage devices and rules.
     */
    public boolean canManageSystem() {
        return isOwner();
    }
    
    /**
     * Gets all users.
     */
    public ObservableList<User> getUsers() {
        return users;
    }
    
    /**
     * Invites a member.
     */
    public boolean inviteUser(String email, String role) {
        if (users.stream().anyMatch(u -> u.getEmail().equals(email))) {
            return false;
        }
        
        User newUser = new User(email, email.split("@")[0], "temporary", role, "Pending");
        users.add(newUser);
        return true;
    }
    
    /**
     * Revokes access for a user.
     */
    public boolean revokeUser(String email) {
        User user = users.stream()
                .filter(candidate -> candidate.getEmail().equals(email))
                .findFirst()
                .orElse(null);

        if (user == null) {
            return false;
        }

        if ("Owner".equalsIgnoreCase(user.getRole())) {
            return false;
        }

        user.setStatus("Revoked");
        if (email.equals(currentUserEmail)) {
            logout();
        }
        return true;
    }

    /**
     * Restores access for a previously revoked or pending member.
     */
    public boolean restoreUser(String email) {
        User user = users.stream()
                .filter(candidate -> candidate.getEmail().equals(email))
                .findFirst()
                .orElse(null);

        if (user == null || "Owner".equalsIgnoreCase(user.getRole())) {
            return false;
        }

        user.setStatus("Active");
        return true;
    }
    
    /**
     * Logs out the current user.
     */
    public void logout() {
        this.currentUserEmail = null;
        this.currentUserRole = null;
    }
}
