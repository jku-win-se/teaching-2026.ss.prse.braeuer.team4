package at.jku.se.smarthome.service;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;

import at.jku.se.smarthome.service.api.UserService.LoginStatus;
import at.jku.se.smarthome.service.mock.MockUserService;
import at.jku.se.smarthome.service.real.auth.UserRegistrationStore;

/**
 * Focused FR-02 tests for secure login and logout behavior.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods"})
public class TestMockUserServiceLoginLogout {


    /**
     * Test: authenticate valid credentials returns SUCCESS.
     */
    @Test
    public void authenticateValidPersistedCredentialsReturnsSuccess() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com",
                "owner",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Owner",
                "Active"
        );
        MockUserService service = new MockUserService(store);

        LoginStatus result = service.authenticate("owner@smarthome.com", "secret123");

        assertEquals(LoginStatus.SUCCESS, result);
    }

    /**
     * Test: authenticate valid credentials starts session.
     */
    @Test
    public void authenticateValidPersistedCredentialsStartsSession() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com",
                "owner",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Owner",
                "Active"
        );
        MockUserService service = new MockUserService(store);

        service.authenticate("owner@smarthome.com", "secret123");

        assertTrue(service.hasActiveSession());
    }

    /**
     * Test: authenticate valid credentials sets user email.
     */
    @Test
    public void authenticateValidPersistedCredentialsSetsEmail() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com",
                "owner",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Owner",
                "Active"
        );
        MockUserService service = new MockUserService(store);

        service.authenticate("owner@smarthome.com", "secret123");

        assertEquals("owner@smarthome.com", service.getCurrentUserEmail());
    }

    /**
     * Test: authenticate valid credentials sets user role.
     */
    @Test
    public void authenticateValidPersistedCredentialsSetsRole() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com",
                "owner",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Owner",
                "Active"
        );
        MockUserService service = new MockUserService(store);

        service.authenticate("owner@smarthome.com", "secret123");

        assertEquals("Owner", service.getCurrentUserRole());
    }

    /**
     * Test: authenticate valid credentials updates last login.
     */
    @Test
    public void authenticateValidPersistedCredentialsUpdatesLastLogin() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com",
                "owner",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Owner",
                "Active"
        );
        MockUserService service = new MockUserService(store);

        service.authenticate("owner@smarthome.com", "secret123");

        assertEquals("owner@smarthome.com", store.lastLoginUpdatedEmail);
    }

    /**
     * Test: authenticate invalid password returns AUTHENTICATION_FAILED.
     */
    @Test
    public void authenticateInvalidPasswordReturnsFailure() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com",
                "owner",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Owner",
                "Active"
        );
        MockUserService service = new MockUserService(store);

        LoginStatus result = service.authenticate("owner@smarthome.com", "wrong-password");

        assertEquals(LoginStatus.AUTHENTICATION_FAILED, result);
    }

    /**
     * Test: authenticate invalid password does not start session.
     */
    @Test
    public void authenticateInvalidPasswordNoSession() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com",
                "owner",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Owner",
                "Active"
        );
        MockUserService service = new MockUserService(store);

        service.authenticate("owner@smarthome.com", "wrong-password");

        assertFalse(service.hasActiveSession());
    }

    /**
     * Test: authenticate invalid password clears user email.
     */
    @Test
    public void authenticateInvalidPasswordClearsEmail() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com",
                "owner",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Owner",
                "Active"
        );
        MockUserService service = new MockUserService(store);

        service.authenticate("owner@smarthome.com", "wrong-password");

        assertNull(service.getCurrentUserEmail());
    }

    /**
     * Test: authenticate inactive user returns ACCOUNT_INACTIVE.
     */
    @Test
    public void authenticateInactiveUserReturnsAccountInactive() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "member@smarthome.com",
                "member",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Member",
                "Revoked"
        );
        MockUserService service = new MockUserService(store);

        LoginStatus result = service.authenticate("member@smarthome.com", "secret123");

        assertEquals(LoginStatus.ACCOUNT_INACTIVE, result);
    }

    /**
     * Test: authenticate inactive user does not start session.
     */
    @Test
    public void authenticateInactiveUserNoSession() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "member@smarthome.com",
                "member",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Member",
                "Revoked"
        );
        MockUserService service = new MockUserService(store);

        service.authenticate("member@smarthome.com", "secret123");

        assertFalse(service.hasActiveSession());
    }

    /**
     * Test: repeated failed attempts trigger throttle on next attempt.
     */
    @Test
    public void authenticateRepeatedFailuresTriggerThrottle() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com",
                "owner",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Owner",
                "Active"
        );
        MockUserService service = new MockUserService(store);

        service.authenticate("owner@smarthome.com", "wrong-1");
        service.authenticate("owner@smarthome.com", "wrong-2");
        service.authenticate("owner@smarthome.com", "wrong-3");

        LoginStatus result = service.authenticate("owner@smarthome.com", "secret123");

        assertEquals(LoginStatus.THROTTLED, result);
    }

    /**
     * Test: throttled authentication sets remaining time.
     */
    @Test
    public void authenticateThrottledSetsRemainingTime() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com",
                "owner",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Owner",
                "Active"
        );
        MockUserService service = new MockUserService(store);

        service.authenticate("owner@smarthome.com", "wrong-1");
        service.authenticate("owner@smarthome.com", "wrong-2");
        service.authenticate("owner@smarthome.com", "wrong-3");
        service.authenticate("owner@smarthome.com", "secret123");

        assertTrue(service.getRemainingThrottleSeconds("owner@smarthome.com") >= 1);
    }

    /**
     * Test: logout clears active session.
     */
    @Test
    public void logoutClearsActiveSession() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com",
                "owner",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Owner",
                "Active"
        );
        MockUserService service = new MockUserService(store);

        service.authenticate("owner@smarthome.com", "secret123");
        service.logout();

        assertFalse(service.hasActiveSession());
    }

    /**
     * Test: logout clears user email.
     */
    @Test
    public void logoutClearsUserEmail() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com",
                "owner",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Owner",
                "Active"
        );
        MockUserService service = new MockUserService(store);

        service.authenticate("owner@smarthome.com", "secret123");
        service.logout();

        assertNull(service.getCurrentUserEmail());
    }

    /**
     * Test: session expiry clears active session.
     */
    @Test
    public void sessionExpiryClearsActiveSession() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com",
                "owner",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Owner",
                "Active"
        );
        MockUserService service = new MockUserService(store);

        service.authenticate("owner@smarthome.com", "secret123");
        service.expireSessionForTesting();

        assertFalse(service.hasActiveSession());
    }

    /**
     * Test: session expiry clears user email.
     */
    @Test
    public void sessionExpiryClearsUserEmail() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com",
                "owner",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Owner",
                "Active"
        );
        MockUserService service = new MockUserService(store);

        service.authenticate("owner@smarthome.com", "secret123");
        service.expireSessionForTesting();

        assertNull(service.getCurrentUserEmail());
    }

    /**
     * Test: session expiry sets user role to Guest.
     */
    @Test
    public void sessionExpirySetRoleToGuest() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com",
                "owner",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Owner",
                "Active"
        );
        MockUserService service = new MockUserService(store);

        service.authenticate("owner@smarthome.com", "secret123");
        service.expireSessionForTesting();

        assertEquals("Guest", service.getCurrentUserRole());
    }

    /**
     * Stub authentication store for testing.
     */
    private static final class StubAuthStore implements UserRegistrationStore {
        /** Persisted user. */
        private UserRegistrationStore.PersistedUser persistedUser;
        /** Last login updated email. */
        private String lastLoginUpdatedEmail;

        /**
         * Checks if email exists.
         */
        @Override
        public boolean emailExists(String normalizedEmail) {
            return persistedUser != null && persistedUser.email().equals(normalizedEmail);
        }

        /**
         * Finds user by email.
         */
        @Override
        public Optional<PersistedUser> findByEmail(String normalizedEmail) {
            Optional<PersistedUser> result = Optional.empty();
            if (persistedUser != null && persistedUser.email().equals(normalizedEmail)) {
                result = Optional.of(persistedUser);
            }
            return result;
        }

        /**
         * Finds all users.
         */
        @Override
        public java.util.List<PersistedUser> findAllUsers() {
            return persistedUser == null ? java.util.List.of() : java.util.List.of(persistedUser);
        }

        /**
         * Saves a user.
         */
        @Override
        public void save(PersistedUser user) {
            this.persistedUser = user;
        }

        /**
         * Updates last login.
         */
        @Override
        public void updateLastLogin(String normalizedEmail) {
            this.lastLoginUpdatedEmail = normalizedEmail;
        }
    }
}