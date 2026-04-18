package at.jku.se.smarthome.service.mock;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.mindrot.jbcrypt.BCrypt;

import at.jku.se.smarthome.model.User;
import at.jku.se.smarthome.service.api.UserService;
import at.jku.se.smarthome.service.real.auth.JdbcUserRegistrationStore;
import at.jku.se.smarthome.service.real.auth.UserRegistrationStore;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock User Service providing user management functionality.
 */
public class MockUserService extends UserService {

    /** Session timeout in milliseconds (30 minutes). */
    private static final long SESSION_TIMEOUT_MILLIS = 30 * 60 * 1000L;
    /** Maximum failed login attempts before throttling. */
    private static final int THROTTLE_THRESHOLD = 3;
    
    /** Singleton instance of MockUserService. */
    private static MockUserService instance;
    /** Observable list of users in the system. */
    private final ObservableList<User> users;
    /** Store for persisted user registration data. */
    private final UserRegistrationStore registrationStore;
    /** Map tracking failed login attempts by email. */
    private final Map<String, Integer> failedLoginAttempts;
    /** Map tracking when users are blocked from login by email. */
    private final Map<String, Long> blockedUntilByEmail;
    /** Email of the current logged-in user. */
    private String currentUserEmail;
    /** Username of the current logged-in user. */
    private String currentUsername;
    /** Role of the current logged-in user. */
    private String currentUserRole;
    /** Status of the current logged-in user. */
    private String currentUserStatus;
    /** Timestamp when the current session expires. */
    private long currentSessionExpiresAt;
    
    /**
     * Private constructor for singleton pattern.
     */
    private MockUserService() {
        this(new JdbcUserRegistrationStore());
    }

    /**
     * Constructs a MockUserService with a provided registration store.
     *
     * @param registrationStore the user registration store to use
     */
    public MockUserService(UserRegistrationStore registrationStore) {
        super();
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

    /**
     * Resets the singleton for unit testing.
     * Must NOT be called from production code.
     */
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
     *
     * @param email email address for the new user
     * @param username display name or login name
     * @param password raw password value
     * @param confirmPassword repeated password value
     * @return true when registration succeeds, otherwise false
     */
    public boolean register(String email, String username, String password, String confirmPassword) {
        return registerUser(email, username, password, confirmPassword) == RegistrationStatus.SUCCESS;
    }

    /**
        * Registers a new user and returns a status for UI feedback.
        *
        * @param email email address for the new user
        * @param username display name or login name
        * @param password raw password value
        * @param confirmPassword repeated password value
        * @return registration outcome status
     */
    @Override
    public RegistrationStatus registerUser(String email, String username, String password, String confirmPassword) {
        RegistrationStatus status = RegistrationStatus.INVALID_INPUT;
        String normalizedEmail = normalizeEmail(email);
        String normalizedUsername = normalizeValue(username);

        if (normalizedEmail != null && normalizedUsername != null && !isBlank(password) && !isBlank(confirmPassword)) {
            if (!password.equals(confirmPassword)) {
                status = RegistrationStatus.PASSWORD_MISMATCH;
            } else if (users.stream().anyMatch(user -> user.getEmail().equalsIgnoreCase(normalizedEmail))) {
                status = RegistrationStatus.DUPLICATE_EMAIL;
            } else {
                try {
                    if (registrationStore.emailExists(normalizedEmail)) {
                        status = RegistrationStatus.DUPLICATE_EMAIL;
                    } else {
                        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
                        registrationStore.save(new UserRegistrationStore.PersistedUser(
                                normalizedEmail,
                                normalizedUsername,
                                passwordHash,
                                "Member",
                                "Active"
                        ));

                        users.add(new User(normalizedEmail, normalizedUsername, passwordHash, "Member", "Active"));
                        status = RegistrationStatus.SUCCESS;
                    }
                } catch (UserRegistrationStore.StoreConfigurationException exception) {
                    status = RegistrationStatus.DATABASE_NOT_CONFIGURED;
                } catch (UserRegistrationStore.DuplicateEmailException exception) {
                    status = RegistrationStatus.DUPLICATE_EMAIL;
                } catch (UserRegistrationStore.StoreException exception) {
                    status = RegistrationStatus.DATABASE_ERROR;
                }
            }
        }
        return status;
    }
    
    /**
     * Authenticates a user.
     *
     * @param email email address used for login
     * @param password raw password value
     * @return true when authentication succeeds, otherwise false
     */
    public synchronized boolean login(String email, String password) {
        return authenticate(email, password) == LoginStatus.SUCCESS;
    }

    /**
        * Authenticates a user and returns a detailed status for UI handling.
        *
        * @param email email address used for login
        * @param password raw password value
        * @return authentication outcome status
     */
    @Override
    public synchronized LoginStatus authenticate(String email, String password) {
        LoginStatus status = LoginStatus.INVALID_INPUT;
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail != null && !isBlank(password)) {
            long now = System.currentTimeMillis();
            if (isBlocked(normalizedEmail, now)) {
                status = LoginStatus.THROTTLED;
            } else {
                try {
                    Optional<UserRegistrationStore.PersistedUser> persistedUser = registrationStore.findByEmail(normalizedEmail);
                    if (persistedUser.isEmpty()) {
                        recordFailedLogin(normalizedEmail, now);
                        status = LoginStatus.AUTHENTICATION_FAILED;
                    } else {
                        UserRegistrationStore.PersistedUser user = persistedUser.get();
                        if (!passwordsMatch(user.passwordHash(), password)) {
                            recordFailedLogin(normalizedEmail, now);
                            status = LoginStatus.AUTHENTICATION_FAILED;
                        } else if (!isActive(user.status())) {
                            recordFailedLogin(normalizedEmail, now);
                            status = LoginStatus.ACCOUNT_INACTIVE;
                        } else {
                            clearFailedLogins(normalizedEmail);
                            establishSession(user.email(), user.username(), user.role(), user.status(), now);
                            updateLastLoginTimestamp(normalizedEmail);
                            status = LoginStatus.SUCCESS;
                        }
                    }
                } catch (UserRegistrationStore.StoreConfigurationException exception) {
                    status = LoginStatus.DATABASE_NOT_CONFIGURED;
                } catch (UserRegistrationStore.StoreException exception) {
                    status = LoginStatus.DATABASE_ERROR;
                }
            }
        }
        return status;
    }
    
    /**
        * Gets the current user's email.
        *
        * @return current user email, or null when no session exists
     */
    @Override
    public synchronized String getCurrentUserEmail() {
        invalidateExpiredSessionIfNeeded();
        return currentUserEmail;
    }
    
    @Override
    protected String getCurrentUserRoleInternal() {
        invalidateExpiredSessionIfNeeded();
        return currentUserRole;
    }

    @Override
    public synchronized String getCurrentUserRole() {
        return super.getCurrentUserRole();
    }

    /**
        * Gets the current user object.
        *
        * @return current user object, or null when no session exists
     */
    public synchronized User getCurrentUser() {
        invalidateExpiredSessionIfNeeded();
        User result = null;
        if (currentUserEmail != null) {
            result = users.stream()
                    .filter(user -> user.getEmail().equals(currentUserEmail))
                    .findFirst()
                    .orElse(null);

            if (result == null) {
                result = new User(
                        currentUserEmail,
                        currentUsername != null ? currentUsername : currentUserEmail,
                        "",
                        currentUserRole != null ? currentUserRole : "Guest",
                        currentUserStatus != null ? currentUserStatus : "Active"
                );
            }
        }
        return result;
    }

    /**
     * Checks whether the current user is an owner.
     *
     * @return true when the current user is an owner, otherwise false
     */
    public synchronized boolean isOwner() {
        return "Owner".equalsIgnoreCase(getCurrentUserRole());
    }

    /**
     * Checks whether the current user can manage devices and rules.
     *
     * @return true when the current user can manage the system, otherwise false
     */
    public synchronized boolean canManageSystem() {
        return isOwner();
    }
    
    /**
     * Gets all users.
     *
     * @return observable list of users
     */
    @Override
    public ObservableList<User> getUsers() {
        return users;
    }

    /**
     * Indicates whether a non-expired user session exists.
     *
     * @return true when a session is active, otherwise false
     */
    @Override
    public synchronized boolean hasActiveSession() {
        invalidateExpiredSessionIfNeeded();
        return currentUserEmail != null;
    }

    /**
     * Returns the remaining login throttle duration for the supplied email.
     *
     * @param email email address to inspect
     * @return remaining throttle time in seconds, or zero when not throttled
     */
    @Override
    public synchronized long getRemainingThrottleSeconds(String email) {
        String normalizedEmail = normalizeEmail(email);
        long remainingSeconds = 0;
        if (normalizedEmail != null) {
            long now = System.currentTimeMillis();
            if (isBlocked(normalizedEmail, now)) {
                long blockedUntil = blockedUntilByEmail.getOrDefault(normalizedEmail, now);
                long remainingMillis = Math.max(0, blockedUntil - now);
                remainingSeconds = Math.max(1, (remainingMillis + 999) / 1000);
            }
        }
        return remainingSeconds;
    }
    
    /**
        * Invites a member.
        *
        * @param email email address of the invited user
        * @param role role assigned to the invited user
        * @return true when the invite was created, otherwise false
     */
    @Override
    public boolean inviteUser(String email, String role) {
        boolean invited = false;
        if (!users.stream().anyMatch(u -> u.getEmail().equals(email))) {
            User newUser = new User(email, email.split("@")[0], "temporary", role, "Pending");
            users.add(newUser);
            invited = true;
        }
        return invited;
    }
    
    /**
        * Revokes access for a user.
        *
        * @param email email address of the user to revoke
        * @return true when access was revoked, otherwise false
     */
    @Override
    public boolean revokeUser(String email) {
        boolean revoked = false;
        User user = users.stream()
                .filter(candidate -> candidate.getEmail().equals(email))
                .findFirst()
                .orElse(null);

        if (user != null && !"Owner".equalsIgnoreCase(user.getRole())) {
            user.setStatus("Revoked");
            if (email.equals(currentUserEmail)) {
                logout();
            }
            revoked = true;
        }
        return revoked;
    }

    /**
        * Restores access for a previously revoked or pending member.
        *
        * @param email email address of the user to restore
        * @return true when access was restored, otherwise false
     */
    @Override
    public boolean restoreUser(String email) {
        boolean restored = false;
        User user = users.stream()
                .filter(candidate -> candidate.getEmail().equals(email))
                .findFirst()
                .orElse(null);

        if (user != null && !"Owner".equalsIgnoreCase(user.getRole())) {
            user.setStatus("Active");
            restored = true;
        }
        return restored;
    }
    
    /**
        * Logs out the current user.
     */
    @Override
    public synchronized void logout() {
        this.currentUserEmail = null;
        this.currentUsername = null;
        this.currentUserRole = null;
        this.currentUserStatus = null;
        this.currentSessionExpiresAt = 0;
    }

    /**
     * Forces the current session to expire for test scenarios.
     */
    public void expireSessionForTesting() {
        this.currentSessionExpiresAt = System.currentTimeMillis() - 1;
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeValue(email);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeValue(String value) {
        String normalized = null;
        if (value != null) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                normalized = trimmed;
            }
        }
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean passwordsMatch(String storedPassword, String candidatePassword) {
        boolean matches = false;
        if (storedPassword != null && candidatePassword != null) {
            if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
                matches = BCrypt.checkpw(candidatePassword, storedPassword);
            } else {
                matches = storedPassword.equals(candidatePassword);
            }
        }
        return matches;
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
        boolean blocked = false;
        Long blockedUntil = blockedUntilByEmail.get(normalizedEmail);
        if (blockedUntil != null && blockedUntil > now) {
            blocked = true;
        } else if (blockedUntil != null && blockedUntil <= now) {
            blockedUntilByEmail.remove(normalizedEmail);
        }
        return blocked;
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
        long delayMillis;
        if (failedAttempts < THROTTLE_THRESHOLD) {
            delayMillis = 0L;
        } else if (failedAttempts == THROTTLE_THRESHOLD) {
            delayMillis = 2_000L;
        } else if (failedAttempts == THROTTLE_THRESHOLD + 1) {
            delayMillis = 5_000L;
        } else {
            delayMillis = 15_000L;
        }
        return delayMillis;
    }

    private void updateLastLoginTimestamp(String normalizedEmail) {
        try {
            registrationStore.updateLastLogin(normalizedEmail);
        } catch (UserRegistrationStore.StoreException exception) {
            // Authentication already succeeded. Do not discard the session over metadata sync.
        }
    }
}
