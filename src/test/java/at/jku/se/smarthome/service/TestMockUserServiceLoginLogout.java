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
public class TestMockUserServiceLoginLogout {

    /**
     * Test: authenticate valid persisted credentials starts session and updates last login.
     */
    @Test
    public void authenticateValidPersistedCredentialsStartsSessionAndUpdatesLastLogin() {
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
        assertTrue(service.hasActiveSession());
        assertEquals("owner@smarthome.com", service.getCurrentUserEmail());
        assertEquals("Owner", service.getCurrentUserRole());
        assertEquals("owner@smarthome.com", store.lastLoginUpdatedEmail);
    }

    /**
     * Test: authenticate invalid password returns generic failure without session.
     */
    @Test
    public void authenticateInvalidPasswordReturnsGenericFailureWithoutSession() {
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
        assertFalse(service.hasActiveSession());
        assertNull(service.getCurrentUserEmail());
    }

    /**
     * Test: authenticate inactive user rejects login.
     */
    @Test
    public void authenticateInactiveUserRejectsLogin() {
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
        assertFalse(service.hasActiveSession());
    }

    /**
     * Test: authenticate repeated failures triggers throttle.
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

        assertEquals(LoginStatus.AUTHENTICATION_FAILED, service.authenticate("owner@smarthome.com", "wrong-1"));
        assertEquals(LoginStatus.AUTHENTICATION_FAILED, service.authenticate("owner@smarthome.com", "wrong-2"));
        assertEquals(LoginStatus.AUTHENTICATION_FAILED, service.authenticate("owner@smarthome.com", "wrong-3"));

        LoginStatus result = service.authenticate("owner@smarthome.com", "secret123");

        assertEquals(LoginStatus.THROTTLED, result);
        assertTrue(service.getRemainingThrottleSeconds("owner@smarthome.com") >= 1);
    }

    /**
     * Test: logout and session expiry clear authenticated state.
     */
    @Test
    public void logoutAndSessionExpiryClearAuthenticatedState() {
        StubAuthStore store = new StubAuthStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "owner@smarthome.com",
                "owner",
                BCrypt.hashpw("secret123", BCrypt.gensalt()),
                "Owner",
                "Active"
        );
        MockUserService service = new MockUserService(store);

        assertEquals(LoginStatus.SUCCESS, service.authenticate("owner@smarthome.com", "secret123"));
        service.logout();

        assertFalse(service.hasActiveSession());
        assertNull(service.getCurrentUserEmail());

        assertEquals(LoginStatus.SUCCESS, service.authenticate("owner@smarthome.com", "secret123"));
        service.expireSessionForTesting();

        assertFalse(service.hasActiveSession());
        assertNull(service.getCurrentUserEmail());
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
            if (persistedUser == null || !persistedUser.email().equals(normalizedEmail)) {
                return Optional.empty();
            }
            return Optional.of(persistedUser);
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