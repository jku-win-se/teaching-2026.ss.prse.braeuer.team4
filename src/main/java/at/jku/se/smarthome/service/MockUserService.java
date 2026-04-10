package at.jku.se.smarthome.service;

import java.util.Locale;

import org.mindrot.jbcrypt.BCrypt;

import at.jku.se.smarthome.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock User Service providing user management functionality.
 */
public class MockUserService {

    public enum RegistrationStatus {
        SUCCESS,
        INVALID_INPUT,
        PASSWORD_MISMATCH,
        DUPLICATE_EMAIL,
        DATABASE_NOT_CONFIGURED,
        DATABASE_ERROR
    }
    
    private static MockUserService instance;
    private final ObservableList<User> users;
    private final UserRegistrationStore registrationStore;
    private String currentUserEmail;
    private String currentUserRole;
    
    private MockUserService() {
        this(new JdbcUserRegistrationStore());
    }

    MockUserService(UserRegistrationStore registrationStore) {
        this.users = FXCollections.observableArrayList();
        this.registrationStore = registrationStore;
        initializeMockUsers();
    }
    
    public static synchronized MockUserService getInstance() {
        if (instance == null) {
            instance = new MockUserService();
        }
        return instance;
    }

    public static synchronized void resetForTesting() {
        instance = null;
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
        return registerUser(email, username, password, confirmPassword) == RegistrationStatus.SUCCESS;
    }

    /**
     * Registers a new user and returns a status for UI feedback.
     */
    public RegistrationStatus registerUser(String email, String username, String password, String confirmPassword) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedUsername = normalizeValue(username);

        if (normalizedEmail == null || normalizedUsername == null || isBlank(password) || isBlank(confirmPassword)) {
            return RegistrationStatus.INVALID_INPUT;
        }

        if (!password.equals(confirmPassword)) {
            return RegistrationStatus.PASSWORD_MISMATCH;
        }

        if (users.stream().anyMatch(user -> user.getEmail().equalsIgnoreCase(normalizedEmail))) {
            return RegistrationStatus.DUPLICATE_EMAIL;
        }

        try {
            if (registrationStore.emailExists(normalizedEmail)) {
                return RegistrationStatus.DUPLICATE_EMAIL;
            }

            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
            registrationStore.save(new UserRegistrationStore.PersistedUser(
                    normalizedEmail,
                    normalizedUsername,
                    passwordHash,
                    "Member",
                    "Active"
            ));

            users.add(new User(normalizedEmail, normalizedUsername, passwordHash, "Member", "Active"));
            return RegistrationStatus.SUCCESS;
        } catch (UserRegistrationStore.StoreConfigurationException exception) {
            return RegistrationStatus.DATABASE_NOT_CONFIGURED;
        } catch (UserRegistrationStore.DuplicateEmailException exception) {
            return RegistrationStatus.DUPLICATE_EMAIL;
        } catch (UserRegistrationStore.StoreException exception) {
            return RegistrationStatus.DATABASE_ERROR;
        }
    }
    
    /**
     * Authenticates a user.
     */
    public boolean login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        User user = users.stream()
            .filter(u -> normalizedEmail != null && u.getEmail().equalsIgnoreCase(normalizedEmail)
                && passwordsMatch(u.getPassword(), password))
                .findFirst()
                .orElse(null);
        
        if (user != null && "Active".equalsIgnoreCase(user.getStatus())) {
            this.currentUserEmail = user.getEmail();
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

    private String normalizeEmail(String email) {
        String normalized = normalizeValue(email);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean passwordsMatch(String storedPassword, String candidatePassword) {
        if (storedPassword == null || candidatePassword == null) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return BCrypt.checkpw(candidatePassword, storedPassword);
        }
        return storedPassword.equals(candidatePassword);
    }
}
