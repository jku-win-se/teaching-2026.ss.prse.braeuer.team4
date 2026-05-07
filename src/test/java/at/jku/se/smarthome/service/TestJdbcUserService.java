package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.User;
import at.jku.se.smarthome.service.api.UserService;
import at.jku.se.smarthome.service.real.auth.JdbcUserService;
import at.jku.se.smarthome.service.real.auth.UserRegistrationStore;

/**
 * Unit tests for JdbcUserService using an in-memory UserRegistrationStore.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods"})
public class TestJdbcUserService {

    /** In-memory store for testing. */
    private InMemoryUserStore store;
    /** Service under test. */
    private JdbcUserService service;

    /**
     * Sets up test fixtures before each test.
     */
    @Before
    public void setUp() {
        store = new InMemoryUserStore();
        service = new JdbcUserService(store);
    }

    /**
     * Tears down after each test.
     */
    @After
    public void tearDown() {
        JdbcUserService.resetForTesting();
    }

    // -----------------------------------------------------------------------
    // Registration
    // -----------------------------------------------------------------------

    /** Successful registration returns SUCCESS. */
    @Test
    public void registerUserSuccess() {
        assertEquals(UserService.RegistrationStatus.SUCCESS,
                service.registerUser("new@example.com", "newuser", "pass123", "pass123"));
    }

    /** Successful registration adds user to observable list. */
    @Test
    public void registerUserAddsToList() {
        service.registerUser("new@example.com", "newuser", "pass123", "pass123");
        assertTrue(service.getUsers().stream().anyMatch(u -> "new@example.com".equalsIgnoreCase(u.getEmail())));
    }

    /** Null email returns INVALID_INPUT. */
    @Test
    public void registerUserNullEmailReturnsInvalidInput() {
        assertEquals(UserService.RegistrationStatus.INVALID_INPUT,
                service.registerUser(null, "user", "pass", "pass"));
    }

    /** Blank email returns INVALID_INPUT. */
    @Test
    public void registerUserBlankEmailReturnsInvalidInput() {
        assertEquals(UserService.RegistrationStatus.INVALID_INPUT,
                service.registerUser("   ", "user", "pass", "pass"));
    }

    /** Null username returns INVALID_INPUT. */
    @Test
    public void registerUserNullUsernameReturnsInvalidInput() {
        assertEquals(UserService.RegistrationStatus.INVALID_INPUT,
                service.registerUser("a@b.com", null, "pass", "pass"));
    }

    /** Null password returns INVALID_INPUT. */
    @Test
    public void registerUserNullPasswordReturnsInvalidInput() {
        assertEquals(UserService.RegistrationStatus.INVALID_INPUT,
                service.registerUser("a@b.com", "user", null, "pass"));
    }

    /** Mismatched passwords return PASSWORD_MISMATCH. */
    @Test
    public void registerUserPasswordMismatch() {
        assertEquals(UserService.RegistrationStatus.PASSWORD_MISMATCH,
                service.registerUser("a@b.com", "user", "pass1", "pass2"));
    }

    /** Duplicate email in memory returns DUPLICATE_EMAIL. */
    @Test
    public void registerUserDuplicateEmailInMemory() {
        service.registerUser("dup@example.com", "user1", "pass", "pass");
        assertEquals(UserService.RegistrationStatus.DUPLICATE_EMAIL,
                service.registerUser("dup@example.com", "user2", "pass", "pass"));
    }

    /** Duplicate email in store returns DUPLICATE_EMAIL. */
    @Test
    public void registerUserDuplicateEmailInStore() throws Exception {
        store.save(new UserRegistrationStore.PersistedUser("store@example.com", "u", "h", "Member", "Active"));
        assertEquals(UserService.RegistrationStatus.DUPLICATE_EMAIL,
                service.registerUser("store@example.com", "user", "pass", "pass"));
    }

    /** StoreConfigurationException returns DATABASE_NOT_CONFIGURED. */
    @Test
    public void registerUserStoreConfigError() {
        InMemoryUserStore failingStore = new InMemoryUserStore() {
            @Override
            public boolean emailExists(String email) throws StoreException {
                throw new StoreConfigurationException("not configured", null);
            }
        };
        JdbcUserService svc = new JdbcUserService(failingStore);
        assertEquals(UserService.RegistrationStatus.DATABASE_NOT_CONFIGURED,
                svc.registerUser("a@b.com", "user", "pass", "pass"));
    }

    /** StoreException returns DATABASE_ERROR. */
    @Test
    public void registerUserStoreError() {
        InMemoryUserStore failingStore = new InMemoryUserStore() {
            @Override
            public boolean emailExists(String email) throws StoreException {
                throw new StoreException("db error");
            }
        };
        JdbcUserService svc = new JdbcUserService(failingStore);
        assertEquals(UserService.RegistrationStatus.DATABASE_ERROR,
                svc.registerUser("a@b.com", "user", "pass", "pass"));
    }

    // -----------------------------------------------------------------------
    // Authentication
    // -----------------------------------------------------------------------

    /** Successful login with registered user. */
    @Test
    public void authenticateSuccessWithRegisteredUser() {
        service.registerUser("auth@example.com", "authuser", "pass123", "pass123");
        assertEquals(UserService.LoginStatus.SUCCESS,
                service.authenticate("auth@example.com", "pass123"));
    }

    /** Null email returns INVALID_INPUT. */
    @Test
    public void authenticateNullEmailReturnsInvalidInput() {
        assertEquals(UserService.LoginStatus.INVALID_INPUT,
                service.authenticate(null, "pass"));
    }

    /** Blank password returns INVALID_INPUT. */
    @Test
    public void authenticateBlankPasswordReturnsInvalidInput() {
        assertEquals(UserService.LoginStatus.INVALID_INPUT,
                service.authenticate("auth@example.com", ""));
    }

    /** Unknown user returns AUTHENTICATION_FAILED. */
    @Test
    public void authenticateUnknownUserReturnsFailed() {
        assertEquals(UserService.LoginStatus.AUTHENTICATION_FAILED,
                service.authenticate("unknown@example.com", "pass"));
    }

    /** Wrong password returns AUTHENTICATION_FAILED. */
    @Test
    public void authenticateWrongPasswordReturnsFailed() {
        service.registerUser("wrong@example.com", "user", "pass123", "pass123");
        assertEquals(UserService.LoginStatus.AUTHENTICATION_FAILED,
                service.authenticate("wrong@example.com", "wrongpassword"));
    }

    /** Inactive user returns ACCOUNT_INACTIVE. */
    @Test
    public void authenticateInactiveUserReturnsAccountInactive() throws Exception {
        // Add an inactive user directly to the store
        store.save(new UserRegistrationStore.PersistedUser(
                "inactive@example.com", "inactive", "hash", "Member", "Inactive"));
        service.getUsers().add(new User("inactive@example.com", "inactive", "hash", "Member", "Inactive"));
        assertEquals(UserService.LoginStatus.ACCOUNT_INACTIVE,
                service.authenticate("inactive@example.com", "hash"));
    }

    /** StoreConfigurationException during auth returns DATABASE_NOT_CONFIGURED. */
    @Test
    public void authenticateStoreConfigError() {
        InMemoryUserStore failingStore = new InMemoryUserStore() {
            @Override
            public Optional<PersistedUser> findByEmail(String email) throws StoreException {
                throw new StoreConfigurationException("not configured", null);
            }
        };
        JdbcUserService svc = new JdbcUserService(failingStore);
        assertEquals(UserService.LoginStatus.DATABASE_NOT_CONFIGURED,
                svc.authenticate("a@b.com", "pass"));
    }

    /** StoreException during auth returns DATABASE_ERROR. */
    @Test
    public void authenticateStoreError() {
        InMemoryUserStore failingStore = new InMemoryUserStore() {
            @Override
            public Optional<PersistedUser> findByEmail(String email) throws StoreException {
                throw new StoreException("db error");
            }
        };
        JdbcUserService svc = new JdbcUserService(failingStore);
        assertEquals(UserService.LoginStatus.DATABASE_ERROR,
                svc.authenticate("a@b.com", "pass"));
    }

    // -----------------------------------------------------------------------
    // Session management
    // -----------------------------------------------------------------------

    /** Successful login establishes active session. */
    @Test
    public void authenticateEstablishesSession() {
        service.registerUser("session@example.com", "sessuser", "pass123", "pass123");
        service.authenticate("session@example.com", "pass123");
        assertTrue(service.hasActiveSession());
    }

    /** Successful login sets current user email. */
    @Test
    public void authenticateSetsCurrentUserEmail() {
        service.registerUser("session@example.com", "sessuser", "pass123", "pass123");
        service.authenticate("session@example.com", "pass123");
        assertEquals("session@example.com", service.getCurrentUserEmail());
    }

    /** Logout can be called without error after login. */
    @Test
    public void logoutCanBeCalledAfterLogin() {
        service.registerUser("session@example.com", "sessuser", "pass123", "pass123");
        service.authenticate("session@example.com", "pass123");
        service.logout();
    }

    /** getCurrentUser returns null when no session. */
    @Test
    public void getCurrentUserReturnsNullWhenNoSession() {
        assertNull(service.getCurrentUser());
    }

    /** getCurrentUser returns non-null after login. */
    @Test
    public void getCurrentUserReturnsNonNullAfterLogin() {
        service.registerUser("curr@example.com", "curruser", "pass123", "pass123");
        service.authenticate("curr@example.com", "pass123");
        assertNotNull(service.getCurrentUser());
    }

    /** getCurrentUserRole returns correct role after login. */
    @Test
    public void getCurrentUserRoleAfterLogin() {
        service.registerUser("role@example.com", "roleuser", "pass123", "pass123");
        service.authenticate("role@example.com", "pass123");
        assertEquals("Member", service.getCurrentUserRole());
    }

    /** No session returns "Guest" role. */
    @Test
    public void getCurrentUserRoleGuestWhenNoSession() {
        assertEquals("Guest", service.getCurrentUserRole());
    }

    // -----------------------------------------------------------------------
    // Throttle
    // -----------------------------------------------------------------------

    /** Multiple failed logins trigger throttle. */
    @Test
    public void multipleFailedLoginsTriggerThrottle() {
        service.registerUser("throttle@example.com", "throttleuser", "pass123", "pass123");
        String email = "throttle@example.com";
        for (int i = 0; i < 3; i++) {
            service.authenticate(email, "wrong");
        }
        assertEquals(UserService.LoginStatus.THROTTLED,
                service.authenticate(email, "wrong"));
    }

    /** Throttle reports remaining seconds. */
    @Test
    public void getRemainingThrottleSecondsReportsTime() {
        service.registerUser("throttle@example.com", "throttleuser", "pass123", "pass123");
        String email = "throttle@example.com";
        for (int i = 0; i < 3; i++) {
            service.authenticate(email, "wrong");
        }
        assertTrue(service.getRemainingThrottleSeconds(email) > 0);
    }

    /** No throttle for unblocked email returns 0. */
    @Test
    public void getRemainingThrottleSecondsZeroWhenNotBlocked() {
        assertEquals(0, service.getRemainingThrottleSeconds("test@example.com"));
    }

    /** Null email in throttle check returns 0. */
    @Test
    public void getRemainingThrottleSecondsNullEmail() {
        assertEquals(0, service.getRemainingThrottleSeconds(null));
    }

    /** Successful login clears throttle. */
    @Test
    public void successfulLoginClearsThrottle() {
        // First register a user we can log in with
        service.registerUser("clear@example.com", "clearuser", "pass123", "pass123");
        // Fail a few times
        service.authenticate("clear@example.com", "wrong");
        service.authenticate("clear@example.com", "wrong");
        // Now succeed
        assertEquals(UserService.LoginStatus.SUCCESS,
                service.authenticate("clear@example.com", "pass123"));
        assertEquals(0, service.getRemainingThrottleSeconds("clear@example.com"));
    }

    // -----------------------------------------------------------------------
    // inviteUser / revokeUser / restoreUser
    // -----------------------------------------------------------------------

    /** Invite new user returns true. */
    @Test
    public void inviteNewUserReturnsTrue() {
        assertTrue(service.inviteUser("invited@example.com", "Member"));
    }

    /** Invite duplicate user returns false. */
    @Test
    public void inviteDuplicateUserReturnsFalse() {
        service.inviteUser("invited@example.com", "Member");
        assertFalse(service.inviteUser("invited@example.com", "Guest"));
    }

    /** Invited user appears in list. */
    @Test
    public void invitedUserAppearsInList() {
        service.inviteUser("invited@example.com", "Member");
        assertTrue(service.getUsers().stream().anyMatch(u -> "invited@example.com".equals(u.getEmail())));
    }

    /** Revoke non-owner returns true. */
    @Test
    public void revokeNonOwnerReturnsTrue() {
        service.inviteUser("member2@example.com", "Member");
        assertTrue(service.revokeUser("member2@example.com"));
    }

    /** Revoke owner returns false. */
    @Test
    public void revokeOwnerReturnsFalse() {
        assertFalse(service.revokeUser("owner@smarthome.com"));
    }

    /** Revoke unknown user returns false. */
    @Test
    public void revokeUnknownUserReturnsFalse() {
        assertFalse(service.revokeUser("nonexistent@example.com"));
    }

    /** Restore non-owner returns true. */
    @Test
    public void restoreNonOwnerReturnsTrue() {
        service.inviteUser("restore@example.com", "Member");
        service.revokeUser("restore@example.com");
        assertTrue(service.restoreUser("restore@example.com"));
    }

    /** Restore owner returns false. */
    @Test
    public void restoreOwnerReturnsFalse() {
        assertFalse(service.restoreUser("owner@smarthome.com"));
    }

    /** Revoke user changes their status. */
    @Test
    public void revokeUserChangesStatus() {
        service.registerUser("revoke@example.com", "revokeuser", "pass123", "pass123");
        service.revokeUser("revoke@example.com");
        User revoked = service.getUsers().stream()
                .filter(u -> "revoke@example.com".equals(u.getEmail()))
                .findFirst().orElse(null);
        assertNotNull(revoked);
        assertEquals("Revoked", revoked.getStatus());
    }

    // -----------------------------------------------------------------------
    // Convenience methods
    // -----------------------------------------------------------------------

    /** register() returns true on success. */
    @Test
    public void registerReturnsTrueOnSuccess() {
        assertTrue(service.register("reg@example.com", "reguser", "pass123", "pass123"));
    }

    /** login() returns true on success. */
    @Test
    public void loginReturnsTrueOnSuccess() {
        service.registerUser("login@example.com", "loginuser", "pass123", "pass123");
        assertTrue(service.login("login@example.com", "pass123"));
    }

    /** isOwner returns true for owner. */
    @Test
    public void isOwnerReturnsTrueForOwner() throws Exception {
        // The owner user is added by initializeDefaultUsers but needs to be in the store to authenticate
        store.save(new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com", "owner", "password123", "Owner", "Active"));
        service.authenticate("owner@smarthome.com", "password123");
        assertTrue(service.isOwner());
    }

    /** isOwner returns false for member. */
    @Test
    public void isOwnerReturnsFalseForMember() {
        service.registerUser("member@test.com", "member", "pass123", "pass123");
        service.authenticate("member@test.com", "pass123");
        assertFalse(service.isOwner());
    }

    /** canManageSystem mirrors isOwner. */
    @Test
    public void canManageSystemMirrorsIsOwner() throws Exception {
        store.save(new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com", "owner", "password123", "Owner", "Active"));
        service.authenticate("owner@smarthome.com", "password123");
        assertTrue(service.canManageSystem());
    }

    // -----------------------------------------------------------------------
    // In-memory store stub
    // -----------------------------------------------------------------------

    /**
     * Simple in-memory implementation of UserRegistrationStore for testing.
     */
    @SuppressWarnings("PMD.ShortClassName")
    private static class InMemoryUserStore implements UserRegistrationStore {

        private final List<PersistedUser> users = new ArrayList<>();

        @Override
        public boolean emailExists(String normalizedEmail) throws StoreException {
            return users.stream().anyMatch(u -> u.email().equalsIgnoreCase(normalizedEmail));
        }

        @Override
        public Optional<PersistedUser> findByEmail(String normalizedEmail) throws StoreException {
            return users.stream().filter(u -> u.email().equalsIgnoreCase(normalizedEmail)).findFirst();
        }

        @Override
        public List<PersistedUser> findAllUsers() throws StoreException {
            return List.copyOf(users);
        }

        @Override
        public void save(PersistedUser user) throws StoreException {
            if (emailExists(user.email())) {
                throw new DuplicateEmailException("Duplicate: " + user.email(), null);
            }
            users.add(user);
        }

        @Override
        public void updateLastLogin(String normalizedEmail) throws StoreException {
            // no-op for in-memory testing
        }
    }
}
