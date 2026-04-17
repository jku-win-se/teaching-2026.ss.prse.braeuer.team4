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

    @Test
    public void registerUserSuccessHashesPasswordAndAddsUser() {
        StubRegistrationStore store = new StubRegistrationStore();
        MockUserService service = new MockUserService(store);

        MockUserService.RegistrationStatus result = service.registerUser(
                " New.User@Example.com ",
                " newuser ",
                "secret123",
                "secret123"
        );

        assertEquals(MockUserService.RegistrationStatus.SUCCESS, result);
        assertNotNull(store.savedUser);
        assertEquals("new.user@example.com", store.savedUser.email());
        assertEquals("newuser", store.savedUser.username());
        assertNotEquals("secret123", store.savedUser.passwordHash());
        assertTrue(BCrypt.checkpw("secret123", store.savedUser.passwordHash()));

        User cachedUser = service.getUsers().stream()
                .filter(user -> "new.user@example.com".equals(user.getEmail()))
                .findFirst()
                .orElse(null);
        assertNotNull(cachedUser);
        assertTrue(service.login("new.user@example.com", "secret123"));
    }

    private static final class StubRegistrationStore implements UserRegistrationStore {
        private boolean emailExists;
        private boolean configurationFailure;
        private PersistedUser savedUser;

        @Override
        public boolean emailExists(String normalizedEmail) throws StoreException {
            if (configurationFailure) {
                throw new StoreConfigurationException("Missing DB configuration");
            }
            return emailExists;
        }

        @Override
        public java.util.Optional<PersistedUser> findByEmail(String normalizedEmail) throws StoreException {
            if (configurationFailure) {
                throw new StoreConfigurationException("Missing DB configuration");
            }
            if (savedUser == null || !savedUser.email().equals(normalizedEmail)) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(savedUser);
        }

        @Override
        public java.util.List<PersistedUser> findAllUsers() throws StoreException {
            if (configurationFailure) {
                throw new StoreConfigurationException("Missing DB configuration");
            }
            return savedUser == null ? java.util.List.of() : java.util.List.of(savedUser);
        }

        @Override
        public void save(PersistedUser user) throws StoreException {
            if (configurationFailure) {
                throw new StoreConfigurationException("Missing DB configuration");
            }
            this.savedUser = user;
        }

        @Override
        public void updateLastLogin(String normalizedEmail) throws StoreException {
            if (configurationFailure) {
                throw new StoreConfigurationException("Missing DB configuration");
            }
        }
    }
}