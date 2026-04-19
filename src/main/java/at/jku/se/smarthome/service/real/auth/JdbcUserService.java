package at.jku.se.smarthome.service.real.auth;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.mindrot.jbcrypt.BCrypt;

import at.jku.se.smarthome.model.User;
import at.jku.se.smarthome.service.api.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * JDBC-backed user service implementation.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CyclomaticComplexity"})
public final class JdbcUserService extends UserService {

    /** Session timeout in milliseconds (30 minutes). */
    private static final long SESSION_TIMEOUT_MILLIS = 30 * 60 * 1000L;
    /** Maximum failed login attempts before throttling. */
    private static final int THROTTLE_THRESHOLD = 3;
    /** Lock for singleton synchronization. */
    private static final Object INSTANCE_LOCK = new Object();

    /** Singleton instance. */
    private static JdbcUserService instance;

    /** Observable list of users. */
    private final ObservableList<User> users;
    /** User registration store. */
    private final UserRegistrationStore registrationStore;
    /** Map tracking failed login attempts by email. */
    private final Map<String, Integer> failedLoginAttempts;
    /** Map tracking login throttle periods by email. */
    private final Map<String, Long> blockedUntilByEmail;
    /** Current user email. */
    private String currentUserEmail;
    /** Current user username. */
    private String currentUsername;
    /** Current user role. */
    private String currentUserRole;
    /** Current user status. */
    private String currentUserStatus;
    /** Timestamp when current session expires. */
    private long currentSessionExpiresAt;

    /**
     * Private constructor for singleton pattern.
     */
    private JdbcUserService() {
        this(new JdbcUserRegistrationStore());
    }

    /**
     * Constructs JdbcUserService with a registration store.
     *
     * @param registrationStore the user registration store
     */
    public JdbcUserService(UserRegistrationStore registrationStore) {
        super();
        this.users = FXCollections.observableArrayList();
        this.registrationStore = registrationStore;
        this.failedLoginAttempts = new HashMap<>();
        this.blockedUntilByEmail = new HashMap<>();
        loadPersistedUsers();
        initializeDefaultUsers();
    }

    public static JdbcUserService getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new JdbcUserService();
            }
            return instance;
        }
    }

    /**
     * Resets the singleton for unit testing.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public static void resetForTesting() {
        synchronized (INSTANCE_LOCK) {
            instance = null;
        }
    }

    @Override
    @SuppressWarnings({"PMD.OnlyOneReturn", "PMD.UseObjectForClearerAPI"})
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

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public LoginStatus authenticate(String email, String password) {
        synchronized (this) {
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
    }

    @Override
    public String getCurrentUserEmail() {
        synchronized (this) {
            invalidateExpiredSessionIfNeeded();
            return currentUserEmail;
        }
    }

    @Override
    protected String getCurrentUserRoleInternal() {
        invalidateExpiredSessionIfNeeded();
        return currentUserRole;
    }

    @Override
    public User getCurrentUser() {
        synchronized (this) {
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
    }

    @Override
    public ObservableList<User> getUsers() {
        return users;
    }

    @Override
    public boolean hasActiveSession() {
        synchronized (this) {
            invalidateExpiredSessionIfNeeded();
            return currentUserEmail != null;
        }
    }

    @Override
    public long getRemainingThrottleSeconds(String email) {
        synchronized (this) {
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
    }

    @Override
    public boolean inviteUser(String email, String role) {
        boolean invited = false;
        if (!users.stream().anyMatch(u -> u.getEmail().equals(email))) {
            User newUser = new User(email, email.contains("@") ? email.split("@")[0] : email, "temporary", role, "Pending");
            users.add(newUser);
            invited = true;
        }
        return invited;
    }

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

    @Override
    public void logout() {
        synchronized (this) {
            this.currentSessionExpiresAt = 0;
        }
    }

    private void initializeDefaultUsers() {
        if (users.stream().noneMatch(user -> "owner@smarthome.com".equalsIgnoreCase(user.getEmail()))) {
            users.add(new User("owner@smarthome.com", "owner", "password123", "Owner", "Active"));
        }
        if (users.stream().noneMatch(user -> "member@smarthome.com".equalsIgnoreCase(user.getEmail()))) {
            users.add(new User("member@smarthome.com", "member", "password123", "Member", "Active"));
        }
        if (users.stream().noneMatch(user -> "guest@smarthome.com".equalsIgnoreCase(user.getEmail()))) {
            users.add(new User("guest@smarthome.com", "guest", "password123", "Member", "Inactive"));
        }
        if (users.stream().noneMatch(user -> "test".equalsIgnoreCase(user.getEmail()))) {
            users.add(new User("test", "test", "test", "Member", "Active"));
        }
    }

    private void loadPersistedUsers() {
        try {
            registrationStore.findAllUsers().forEach(persisted -> users.add(new User(
                    persisted.email(),
                    persisted.username(),
                    persisted.passwordHash(),
                    persisted.role(),
                    persisted.status()
            )));
        } catch (UserRegistrationStore.StoreException exception) {
            System.getLogger(JdbcUserService.class.getName()).log(
                    System.Logger.Level.WARNING,
                    "Failed to load persisted users. Continuing with in-memory defaults.",
                    exception
            );
        }
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
        return value == null || value.isBlank();
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
            System.getLogger(JdbcUserService.class.getName()).log(
                    System.Logger.Level.WARNING,
                    "Authentication succeeded but failed to update last-login metadata.",
                    exception
            );
        }
    }
}
