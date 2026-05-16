package at.jku.se.smarthome.service;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;

import at.jku.se.smarthome.model.User;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.api.UserService.LoginStatus;
import at.jku.se.smarthome.service.mock.MockLogService;
import at.jku.se.smarthome.service.mock.MockUserService;
import at.jku.se.smarthome.service.real.auth.UserRegistrationStore;

/**
 * FR-20 tests for invite, revoke, and restore in {@link MockUserService}.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods"})
public class TestMockUserServiceInviteRevoke {

    /**
     * Wires in a MockLogService before each test so tryLog does not hit the real JDBC log service.
     */
    @Before
    public void setUp() {
        MockLogService.resetForTesting();
        ServiceRegistry.setLogServiceForTesting(MockLogService.getInstance());
    }

    /**
     * Tears down the MockLogService override after each test.
     */
    @After
    public void tearDown() {
        ServiceRegistry.setLogServiceForTesting(null);
    }

    /**
     * Test: inviteUser with null email returns false.
     */
    @Test
    public void inviteUserNullEmailReturnsFalse() {
        MockUserService service = new MockUserService(new StubStore());
        assertFalse(service.inviteUser(null, "Member"));
    }

    /**
     * Test: inviteUser with blank email returns false.
     */
    @Test
    public void inviteUserBlankEmailReturnsFalse() {
        MockUserService service = new MockUserService(new StubStore());
        assertFalse(service.inviteUser("   ", "Member"));
    }

    /**
     * Test: inviteUser with email missing at-sign returns false.
     */
    @Test
    public void inviteUserNoAtSignReturnsFalse() {
        MockUserService service = new MockUserService(new StubStore());
        assertFalse(service.inviteUser("notanemail", "Member"));
    }

    /**
     * Test: inviteUser with valid new email returns true.
     */
    @Test
    public void inviteUserValidEmailReturnsTrue() {
        MockUserService service = new MockUserService(new StubStore());
        assertTrue(service.inviteUser("newmember@example.com", "Member"));
    }

    /**
     * Test: inviteUser with valid email adds user with Pending status.
     */
    @Test
    public void inviteUserValidEmailAddsPendingUser() {
        MockUserService service = new MockUserService(new StubStore());
        service.inviteUser("newmember@example.com", "Member");
        User found = service.getUsers().stream()
                .filter(u -> "newmember@example.com".equals(u.getEmail()))
                .findFirst()
                .orElse(null);
        assertEquals("Pending", found.getStatus());
    }

    /**
     * Test: inviteUser with valid email adds user with correct role.
     */
    @Test
    public void inviteUserValidEmailAddsCorrectRole() {
        MockUserService service = new MockUserService(new StubStore());
        service.inviteUser("newmember@example.com", "Member");
        User found = service.getUsers().stream()
                .filter(u -> "newmember@example.com".equals(u.getEmail()))
                .findFirst()
                .orElse(null);
        assertEquals("Member", found.getRole());
    }

    /**
     * Test: inviteUser with duplicate email returns false.
     */
    @Test
    public void inviteUserDuplicateEmailReturnsFalse() {
        MockUserService service = new MockUserService(new StubStore());
        service.inviteUser("newmember@example.com", "Member");
        assertFalse(service.inviteUser("newmember@example.com", "Member"));
    }

    /**
     * Test: inviteUser normalizes email to lowercase.
     */
    @Test
    public void inviteUserNormalizesEmailToLowercase() {
        MockUserService service = new MockUserService(new StubStore());
        service.inviteUser("New.Member@EXAMPLE.COM", "Member");
        boolean found = service.getUsers().stream()
                .anyMatch(u -> "new.member@example.com".equals(u.getEmail()));
        assertTrue(found);
    }

    /**
     * Test: revokeUser on a Member returns true.
     * Uses the pre-seeded member@smarthome.com that MockUserService adds at construction.
     */
    @Test
    public void revokeUserMemberReturnsTrue() {
        MockUserService service = new MockUserService(new StubStore());
        assertTrue(service.revokeUser("member@smarthome.com"));
    }

    /**
     * Test: revokeUser on a Member sets status to Revoked.
     */
    @Test
    public void revokeUserMemberSetsStatusRevoked() {
        MockUserService service = new MockUserService(new StubStore());
        service.revokeUser("member@smarthome.com");
        User member = service.getUsers().stream()
                .filter(u -> "member@smarthome.com".equals(u.getEmail()))
                .findFirst()
                .orElseThrow();
        assertEquals("Revoked", member.getStatus());
    }

    /**
     * Test: revokeUser on an Owner returns false.
     */
    @Test
    public void revokeUserOwnerReturnsFalse() {
        MockUserService service = new MockUserService(new StubStore());
        assertFalse(service.revokeUser("owner@smarthome.com"));
    }

    /**
     * Test: revokeUser on the current logged-in user clears the session.
     */
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    @Test
    public void revokeUserCurrentUserClearsSession() {
        StubStore store = new StubStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "member@smarthome.com",
                "member",
                BCrypt.hashpw("pass", BCrypt.gensalt()),
                "Member",
                "Active"
        );
        MockUserService service = new MockUserService(store);
        service.authenticate("member@smarthome.com", "pass");
        assertTrue(service.hasActiveSession());

        service.revokeUser("member@smarthome.com");

        assertFalse(service.hasActiveSession());
    }

    /**
     * Test: revokeUser on the current logged-in user clears the email.
     */
    @Test
    public void revokeUserCurrentUserClearsEmail() {
        StubStore store = new StubStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "member@smarthome.com",
                "member",
                BCrypt.hashpw("pass", BCrypt.gensalt()),
                "Member",
                "Active"
        );
        MockUserService service = new MockUserService(store);
        service.authenticate("member@smarthome.com", "pass");
        service.revokeUser("member@smarthome.com");

        assertNull(service.getCurrentUserEmail());
    }

    /**
     * Test: restoreUser on a revoked Member returns true.
     */
    @Test
    public void restoreUserRevokedMemberReturnsTrue() {
        MockUserService service = new MockUserService(new StubStore());
        service.revokeUser("member@smarthome.com");
        assertTrue(service.restoreUser("member@smarthome.com"));
    }

    /**
     * Test: restoreUser on a revoked Member sets status to Active.
     */
    @Test
    public void restoreUserRevokedMemberSetsStatusActive() {
        MockUserService service = new MockUserService(new StubStore());
        service.revokeUser("member@smarthome.com");
        service.restoreUser("member@smarthome.com");
        User member = service.getUsers().stream()
                .filter(u -> "member@smarthome.com".equals(u.getEmail()))
                .findFirst()
                .orElseThrow();
        assertEquals("Active", member.getStatus());
    }

    /**
     * Test: restoreUser on an Owner returns false.
     */
    @Test
    public void restoreUserOwnerReturnsFalse() {
        MockUserService service = new MockUserService(new StubStore());
        assertFalse(service.restoreUser("owner@smarthome.com"));
    }

    /**
     * Test: a revoked user cannot authenticate.
     */
    @Test
    public void revokedUserCannotAuthenticate() {
        StubStore store = new StubStore();
        store.persistedUser = new UserRegistrationStore.PersistedUser(
                "member@smarthome.com",
                "member",
                BCrypt.hashpw("pass", BCrypt.gensalt()),
                "Member",
                "Revoked"
        );
        MockUserService service = new MockUserService(store);
        LoginStatus result = service.authenticate("member@smarthome.com", "pass");
        assertEquals(LoginStatus.ACCOUNT_INACTIVE, result);
    }

    /**
     * Minimal stub store used across invite/revoke tests.
     */
    private static final class StubStore implements UserRegistrationStore {

        /** Persisted user (optional). */
        private PersistedUser persistedUser;

        @Override
        public boolean emailExists(String normalizedEmail) {
            return persistedUser != null && persistedUser.email().equals(normalizedEmail);
        }

        @Override
        public Optional<PersistedUser> findByEmail(String normalizedEmail) {
            Optional<PersistedUser> result = Optional.empty();
            if (persistedUser != null && persistedUser.email().equals(normalizedEmail)) {
                result = Optional.of(persistedUser);
            }
            return result;
        }

        @Override
        public java.util.List<PersistedUser> findAllUsers() {
            return persistedUser == null ? java.util.List.of() : java.util.List.of(persistedUser);
        }

        @Override
        public void save(PersistedUser user) {
            this.persistedUser = user;
        }

        @Override
        public void updateLastLogin(String normalizedEmail) {
            // no-op for these tests
        }

        @Override
        public void updateStatus(String normalizedEmail, String newStatus) {
            if (persistedUser != null && persistedUser.email().equals(normalizedEmail)) {
                persistedUser = new PersistedUser(
                        persistedUser.email(), persistedUser.username(),
                        persistedUser.passwordHash(), persistedUser.role(), newStatus);
            }
        }
    }
}
