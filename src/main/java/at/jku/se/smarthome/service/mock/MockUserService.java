package at.jku.se.smarthome.service.mock;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.mindrot.jbcrypt.BCrypt;

import at.jku.se.smarthome.model.User;
import at.jku.se.smarthome.service.real.auth.JdbcUserRegistrationStore;
import at.jku.se.smarthome.service.real.auth.UserRegistrationStore;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock User Service providing user management functionality.
 */
public class MockUserService {

    private static final long SESSION_TIMEOUT_MILLIS = 30 * 60 * 1000L;
    private static final int THROTTLE_THRESHOLD = 3;

    public enum RegistrationStatus {
        SUCCESS,
        INVALID_INPUT,
        PASSWORD_MISMATCH,
        DUPLICATE_EMAIL,
        DATABASE_NOT_CONFIGURED,
        DATABASE_ERROR
    }

    public enum LoginStatus {
        SUCCESS,
        INVALID_INPUT,
        AUTHENTICATION_FAILED,
        ACCOUNT_INACTIVE,
        THROTTLED,
        DATABASE_NOT_CONFIGURED,
        DATABASE_ERROR
    }
    
    private static MockUserService instance;
    private final ObservableList<User> users;
    private final UserRegistrationStore registrationStore;
    private final Map<String, Integer> failedLoginAttempts;
    private final Map<String, Long> blockedUntilByEmail;
    private String currentUserEmail;
    private String currentUsername;
    private String currentUserRole;
    private String currentUserStatus;
    private long currentSessionExpiresAt;
    
    private MockUserService() {
        this(new JdbcUserRegistrationStore());
    }

    public MockUserService(UserRegistrationStore registrationStore) {
        this.users = FXCollections.observableArrayList();
        this.registrationStore = registrationStore;
        this.failedLoginAttempts = new HashMap<>();
        this.blockedUntilByEmail = new HashMap<>();
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
    public synchronized boolean login(String email, String password) {
        return authenticate(email, password) == LoginStatus.SUCCESS;
    }

    /**
     * Authenticates a user and returns a detailed status for UI handling.
     */
    public synchronized LoginStatus authenticate(String email, String password) {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail == null || isBlank(password)) {
            return LoginStatus.INVALID_INPUT;
        }

        long now = System.currentTimeMillis();
        if (isBlocked(normalizedEmail, now)) {
            return LoginStatus.THROTTLED;
        }

        try {
            Optional<UserRegistrationStore.PersistedUser> persistedUser = registrationStore.findByEmail(normalizedEmail);
            if (persistedUser.isEmpty()) {
                recordFailedLogin(normalizedEmail, now);
                return LoginStatus.AUTHENTICATION_FAILED;
            }

            UserRegistrationStore.PersistedUser user = persistedUser.get();
            if (!passwordsMatch(user.passwordHash(), password)) {
                recordFailedLogin(normalizedEmail, now);
                return LoginStatus.AUTHENTICATION_FAILED;
            }

            if (!isActive(user.status())) {
                recordFailedLogin(normalizedEmail, now);
                return LoginStatus.ACCOUNT_INACTIVE;
            }

            clearFailedLogins(normalizedEmail);
            establishSession(user.email(), user.username(), user.role(), user.status(), now);
            updateLastLoginTimestamp(normalizedEmail);
            return LoginStatus.SUCCESS;
        } catch (UserRegistrationStore.StoreConfigurationException exception) {
            return LoginStatus.DATABASE_NOT_CONFIGURED;
        } catch (UserRegistrationStore.StoreException exception) {
            return LoginStatus.DATABASE_ERROR;
        }
    }
    
    /**
     * Gets the current user's email.
     */
    public synchronized String getCurrentUserEmail() {
        invalidateExpiredSessionIfNeeded();
        return currentUserEmail;
    }
    
    /**
     * Gets the current user's role.
     */
    public synchronized String getCurrentUserRole() {
        invalidateExpiredSessionIfNeeded();
        return currentUserRole != null ? currentUserRole : "Guest";
    }

    /**
     * Gets the current user object.
     */
    public synchronized User getCurrentUser() {
        invalidateExpiredSessionIfNeeded();
        if (currentUserEmail == null) {
            return null;
        }

        User cachedUser = users.stream()
                .filter(user -> user.getEmail().equals(currentUserEmail))
                .findFirst()
                .orElse(null);

        if (cachedUser != null) {
            return cachedUser;
        }

        return new User(
                currentUserEmail,
                currentUsername != null ? currentUsername : currentUserEmail,
                "",
                currentUserRole != null ? currentUserRole : "Guest",
                currentUserStatus != null ? currentUserStatus : "Active"
        );
    }

    /**
     * Checks whether the current user is an owner.
     */
    public synchronized boolean isOwner() {
        return "Owner".equalsIgnoreCase(getCurrentUserRole());
    }

    /**
     * Checks whether the current user can manage devices and rules.
     */
    public synchronized boolean canManageSystem() {
        return isOwner();
    }
    
    /**
     * Gets all users.
     */
    public ObservableList<User> getUsers() {
        return users;
    }

    public synchronized boolean hasActiveSession() {
        invalidateExpiredSessionIfNeeded();
        return currentUserEmail != null;
    }

    public synchronized long getRemainingThrottleSeconds(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return 0;
        }

        long now = System.currentTimeMillis();
        if (!isBlocked(normalizedEmail, now)) {
            return 0;
        }

        long blockedUntil = blockedUntilByEmail.getOrDefault(normalizedEmail, now);
        long remainingMillis = Math.max(0, blockedUntil - now);
        return Math.max(1, (remainingMillis + 999) / 1000);
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
    public synchronized void logout() {
        this.currentUserEmail = null;
        this.currentUsername = null;
        this.currentUserRole = null;
        this.currentUserStatus = null;
        this.currentSessionExpiresAt = 0;
    }

    public void expireSessionForTesting() {
        this.currentSessionExpiresAt = System.currentTimeMillis() - 1;
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

    private boolean isActive(String status) {
        return "Active".equalsIgnoreCase(status);
    }

    private void establishSession(String email, String username, String role, String status, long now) {
        this.currentUserEmail = email;
        this.currentUsername = username;
        this.currentUserRole = role;
        this.currentUserStatus = status;
        this.currentSessionExpiresAt = now + SESSION_TIMEOUT_MILLIS;
    }

    private void invalidateExpiredSessionIfNeeded() {
        if (currentUserEmail == null || currentSessionExpiresAt <= 0) {
            return;
        }

        if (System.currentTimeMillis() >= currentSessionExpiresAt) {
            logout();
        }
    }

    private boolean isBlocked(String normalizedEmail, long now) {
        Long blockedUntil = blockedUntilByEmail.get(normalizedEmail);
        if (blockedUntil == null) {
            return false;
        }

        if (blockedUntil <= now) {
            blockedUntilByEmail.remove(normalizedEmail);
            return false;
        }

        return true;
    }

    private void recordFailedLogin(String normalizedEmail, long now) {
        int attempts = failedLoginAttempts.merge(normalizedEmail, 1, Integer::sum);
        long delayMillis = determineThrottleDelayMillis(attempts);
        if (delayMillis > 0) {
            blockedUntilByEmail.put(normalizedEmail, now + delayMillis);
        }
    }

    private void clearFailedLogins(String normalizedEmail) {
        failedLoginAttempts.remove(normalizedEmail);
        blockedUntilByEmail.remove(normalizedEmail);
    }

    private long determineThrottleDelayMillis(int failedAttempts) {
        if (failedAttempts < THROTTLE_THRESHOLD) {
            return 0;
        }

        if (failedAttempts == THROTTLE_THRESHOLD) {
            return 2_000L;
        }

        if (failedAttempts == THROTTLE_THRESHOLD + 1) {
            return 5_000L;
        }

        return 15_000L;
    }

    private void updateLastLoginTimestamp(String normalizedEmail) {
        try {
            registrationStore.updateLastLogin(normalizedEmail);
        } catch (UserRegistrationStore.StoreException exception) {
            // Authentication already succeeded. Do not discard the session over metadata sync.
        }
    }
}
