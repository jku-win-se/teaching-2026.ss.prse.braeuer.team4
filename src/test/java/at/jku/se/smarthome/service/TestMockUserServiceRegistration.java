package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;

import at.jku.se.smarthome.model.User;
import at.jku.se.smarthome.service.mock.MockUserService;
import at.jku.se.smarthome.service.real.auth.UserRegistrationStore;

/**
 * Tests for JDBC-backed registration behavior in {@link MockUserService}.
 */
public class TestMockUserServiceRegistration {

    /**
     * Test: register user password mismatch returns password mismatch.
     */
    @Test
    public void registerUserPasswordMismatchReturnsPasswordMismatch() {
        MockUserService service = new MockUserService(new StubRegistrationStore());

        MockUserService.RegistrationStatus result = service.registerUser(
                "new.user@example.com",
                "newuser",
                "secret123",
                "secret124"
        );

        assertEquals(MockUserService.RegistrationStatus.PASSWORD_MISMATCH, result);
    }

    /**
     * Test: register user duplicate email in store returns duplicate email.
     */
    @Test
    public void registerUserDuplicateEmailInStoreReturnsDuplicateEmail() {
        StubRegistrationStore store = new StubRegistrationStore();
        store.emailExists = true;
        MockUserService service = new MockUserService(store);

        MockUserService.RegistrationStatus result = service.registerUser(
                "new.user@example.com",
                "newuser",
                "secret123",
                "secret123"
        );

        assertEquals(MockUserService.RegistrationStatus.DUPLICATE_EMAIL, result);
    }

    /**
     * Test: register user store not configured returns database not configured.
     */
    @Test
    public void registerUserStoreNotConfiguredReturnsDatabaseNotConfigured() {
        StubRegistrationStore store = new StubRegistrationStore();
        store.configurationFailure = true;
        MockUserService service = new MockUserService(store);

        MockUserService.RegistrationStatus result = service.registerUser(
                "new.user@example.com",
                "newuser",
                "secret123",
                "secret123"
        );

        assertEquals(MockUserService.RegistrationStatus.DATABASE_NOT_CONFIGURED, result);
    }

    /**
     * Test: register user success returns success status.
     */
    @Test
    public void registerUserSuccessReturnsSuccessStatus() {
        StubRegistrationStore store = new StubRegistrationStore();
        MockUserService service = new MockUserService(store);
        MockUserService.RegistrationStatus result = service.registerUser(
                " New.User@Example.com ",
                " newuser ",
                "secret123",
                "secret123"
        );
        assertEquals(MockUserService.RegistrationStatus.SUCCESS, result);
    }

    /**
     * Test: register user success saves user to store.
     */
    @Test
    public void registerUserSuccessSavesUserToStore() {
        StubRegistrationStore store = new StubRegistrationStore();
        MockUserService service = new MockUserService(store);
        service.registerUser(
                " New.User@Example.com ",
                " newuser ",
                "secret123",
                "secret123"
        );
        assertNotNull(store.savedUser);
    }

    /**
     * Test: register user success stores normalized email.
     */
    @Test
    public void registerUserSuccessStoresNormalizedEmail() {
        StubRegistrationStore store = new StubRegistrationStore();
        MockUserService service = new MockUserService(store);
        service.registerUser(
                " New.User@Example.com ",
                " newuser ",
                "secret123",
                "secret123"
        );
        assertEquals("new.user@example.com", store.savedUser.email());
    }

    /**
     * Test: register user success stores trimmed username.
     */
    @Test
    public void registerUserSuccessStoresTrimmedUsername() {
        StubRegistrationStore store = new StubRegistrationStore();
        MockUserService service = new MockUserService(store);
        service.registerUser(
                " New.User@Example.com ",
                " newuser ",
                "secret123",
                "secret123"
        );
        assertEquals("newuser", store.savedUser.username());
    }

    /**
     * Test: register user success hashes password.
     */
    @Test
    public void registerUserSuccessHashesPassword() {
        StubRegistrationStore store = new StubRegistrationStore();
        MockUserService service = new MockUserService(store);
        service.registerUser(
                " New.User@Example.com ",
                " newuser ",
                "secret123",
                "secret123"
        );
        assertNotEquals("secret123", store.savedUser.passwordHash());
    }

    /**
     * Test: register user success password hash verifies successfully.
     */
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    @Test
    public void registerUserSuccessPasswordHashVerifies() {
        StubRegistrationStore store = new StubRegistrationStore();
        MockUserService service = new MockUserService(store);
        service.registerUser(
                " New.User@Example.com ",
                " newuser ",
                "secret123",
                "secret123"
        );
        // Verify password hash can authenticate the plain text password
        assertTrue(BCrypt.checkpw("secret123", store.savedUser.passwordHash()));
    }

    /**
     * Test: register user success caches user.
     */
    @Test
    public void registerUserSuccessCachesUser() {
        StubRegistrationStore store = new StubRegistrationStore();
        MockUserService service = new MockUserService(store);
        service.registerUser(
                " New.User@Example.com ",
                " newuser ",
                "secret123",
                "secret123"
        );
        User cachedUser = service.getUsers().stream()
                .filter(user -> "new.user@example.com".equals(user.getEmail()))
                .findFirst()
                .orElse(null);
        assertNotNull(cachedUser);
    }

    /**
     * Test: register user success enables login.
     */
    @Test
    public void registerUserSuccessEnablesLogin() {
        StubRegistrationStore store = new StubRegistrationStore();
        MockUserService service = new MockUserService(store);
        service.registerUser(
                " New.User@Example.com ",
                " newuser ",
                "secret123",
                "secret123"
        );
        assertTrue(service.login("new.user@example.com", "secret123"));
    }

    /**
     * Stub registration store for testing.
     */
    private static final class StubRegistrationStore implements UserRegistrationStore {
        /** Email exists flag. */
        private boolean emailExists;
        /** Configuration failure flag. */
        private boolean configurationFailure;
        /** Saved persisted user. */
        private PersistedUser savedUser;

        /**
         * Checks if email exists.
         */
        @Override
        public boolean emailExists(String normalizedEmail) throws StoreException {
            if (configurationFailure) {
                throw new StoreConfigurationException("Missing DB configuration");
            }
            return emailExists;
        }

        /**
         * Finds user by email.
         */
        @Override
        public java.util.Optional<PersistedUser> findByEmail(String normalizedEmail) throws StoreException {
            java.util.Optional<PersistedUser> result = java.util.Optional.empty();
            if (!configurationFailure) {
                if (savedUser != null && savedUser.email().equals(normalizedEmail)) {
                    result = java.util.Optional.of(savedUser);
                }
            } else {
                throw new StoreConfigurationException("Missing DB configuration");
            }
            return result;
        }

        /**
         * Finds all users.
         */
        @Override
        public java.util.List<PersistedUser> findAllUsers() throws StoreException {
            if (configurationFailure) {
                throw new StoreConfigurationException("Missing DB configuration");
            }
            return savedUser == null ? java.util.List.of() : java.util.List.of(savedUser);
        }

        /**
         * Saves a user.
         */
        @Override
        public void save(PersistedUser user) throws StoreException {
            if (configurationFailure) {
                throw new StoreConfigurationException("Missing DB configuration");
            }
            this.savedUser = user;
        }

        /**
         * Updates last login.
         */
        @Override
        public void updateLastLogin(String normalizedEmail) throws StoreException {
            if (configurationFailure) {
                throw new StoreConfigurationException("Missing DB configuration");
            }
        }
    }
}