package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.User;
import at.jku.se.smarthome.service.api.UserService.LoginStatus;
import at.jku.se.smarthome.service.api.UserService.RegistrationStatus;
import at.jku.se.smarthome.service.real.auth.JdbcUserRegistrationStore;
import at.jku.se.smarthome.service.real.auth.JdbcUserService;

/**
 * Unit tests for {@link JdbcUserService}.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AtLeastOneConstructor", "PMD.TooManyStaticImports"})
public class TestJdbcUserService {

    /** URL property key. */
    private static final String URL_PROPERTY = "smarthome.db.url";
    /** User property key. */
    private static final String USER_PROPERTY = "smarthome.db.user";
    /** Password property key. */
    private static final String PASSWORD_PROPERTY = "smarthome.db.password";

    /** Service instance under test. */
    private JdbcUserService service;

    /**
     * Default constructor for TestJdbcUserService.
     */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public TestJdbcUserService() {
        // Required for PMD
    }

    /**
     * Sets up test environment.
     */
    @Before
    public void setUp() {
        String jdbcUrl = "jdbc:h2:mem:user_service_" + System.nanoTime() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        System.setProperty(URL_PROPERTY, jdbcUrl);
        System.setProperty(USER_PROPERTY, "sa");
        System.setProperty(PASSWORD_PROPERTY, "");
        
        at.jku.se.smarthome.service.real.log.JdbcLogService.resetForTesting();
        JdbcUserService.resetForTesting();
        service = JdbcUserService.getInstance();
    }

    /**
     * Cleans up test environment.
     */
    @After
    public void tearDown() {
        System.clearProperty(URL_PROPERTY);
        System.clearProperty(USER_PROPERTY);
        System.clearProperty(PASSWORD_PROPERTY);
        at.jku.se.smarthome.service.real.log.JdbcLogService.resetForTesting();
        JdbcUserService.resetForTesting();
    }

    /**
     * Tests singleton behavior.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testSingleton() {
        assertNotNull(service);
        assertEquals(service, JdbcUserService.getInstance());
    }

    /**
     * Tests successful user registration.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testRegisterUserSuccess() {
        RegistrationStatus status = service.registerUser("new@example.com", "newuser", "password123", "password123");
        assertEquals(RegistrationStatus.SUCCESS, status);
        assertTrue(service.getUsers().stream().anyMatch(user -> "new@example.com".equals(user.getEmail())));
    }

    /**
     * Tests registration with invalid input.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testRegisterUserInvalidInput() {
        assertEquals(RegistrationStatus.INVALID_INPUT, service.registerUser(null, "user", "p", "p"));
        assertEquals(RegistrationStatus.INVALID_INPUT, service.registerUser("e", null, "p", "p"));
        assertEquals(RegistrationStatus.INVALID_INPUT, service.registerUser("e", "u", "", "p"));
        assertEquals(RegistrationStatus.INVALID_INPUT, service.registerUser("e", "u", "p", " "));
    }

    /**
     * Tests registration with password mismatch.
     */
    @Test
    public void testRegisterUserPasswordMismatch() {
        RegistrationStatus status = service.registerUser("e@e.com", "u", "p1", "p2");
        assertEquals(RegistrationStatus.PASSWORD_MISMATCH, status);
    }

    /**
     * Tests registration with duplicate email.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testRegisterUserDuplicateEmail() {
        service.registerUser("dup@example.com", "u1", "p", "p");
        RegistrationStatus status = service.registerUser("dup@example.com", "u2", "p", "p");
        assertEquals(RegistrationStatus.DUPLICATE_EMAIL, status);
    }

    /**
     * Tests successful authentication.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testAuthenticateSuccess() {
        service.registerUser("auth@example.com", "user", "pass", "pass");
        LoginStatus status = service.authenticate("auth@example.com", "pass");
        assertEquals(LoginStatus.SUCCESS, status);
        assertTrue(service.hasActiveSession());
        assertEquals("auth@example.com", service.getCurrentUserEmail());
    }

    /**
     * Tests authentication with invalid input.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testAuthenticateInvalidInput() {
        assertEquals(LoginStatus.INVALID_INPUT, service.authenticate(null, "p"));
        assertEquals(LoginStatus.INVALID_INPUT, service.authenticate("e", ""));
    }

    /**
     * Tests failed authentication.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testAuthenticateFailed() {
        service.registerUser("fail@example.com", "user", "pass", "pass");
        assertEquals(LoginStatus.AUTHENTICATION_FAILED, service.authenticate("fail@example.com", "wrong"));
        assertEquals(LoginStatus.AUTHENTICATION_FAILED, service.authenticate("nonexistent@example.com", "pass"));
    }

    /**
     * Tests authentication of inactive account.
     */
    @Test
    public void testAuthenticateInactive() {
        // Invite a user, they are "Pending" by default which is not "Active"
        service.inviteUser("inactive@example.com", "Member");
        assertEquals(LoginStatus.ACCOUNT_INACTIVE, service.authenticate("inactive@example.com", "*INVITED*"));
    }

    /**
     * Tests logout functionality.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testLogout() {
        service.registerUser("out@example.com", "user", "pass", "pass");
        service.authenticate("out@example.com", "pass");
        assertTrue(service.hasActiveSession());
        service.logout();
        assertFalse(service.hasActiveSession());
        assertNull(service.getCurrentUserEmail());
    }

    /**
     * Tests login throttling.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testThrottling() throws InterruptedException {
        String email = "throttle@example.com";
        service.registerUser(email, "user", "pass", "pass");
        
        // 3 failed attempts
        for (int index = 0; index < 3; index++) {
            service.authenticate(email, "wrong");
        }
        
        assertEquals(LoginStatus.THROTTLED, service.authenticate(email, "pass"));
        assertTrue(service.getRemainingThrottleSeconds(email) > 0);
    }

    /**
     * Tests user invitation.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testInviteUser() {
        assertTrue(service.inviteUser("invited@example.com", "Member"));
        assertFalse(service.inviteUser("invited@example.com", "Member")); // Duplicate
        assertFalse(service.inviteUser("invalid", "Member"));
    }

    /**
     * Tests revoking and restoring user access.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testRevokeAndRestoreUser() {
        String email = "revoke@example.com";
        service.registerUser(email, "user", "pass", "pass");
        
        assertTrue(service.revokeUser(email));
        User user = service.getUsers().stream().filter(userItem -> email.equals(userItem.getEmail())).findFirst().orElse(null);
        assertEquals("Revoked", user.getStatus());
        
        assertTrue(service.restoreUser(email));
        assertEquals("Active", user.getStatus());
    }

    /**
     * Tests that revoking the owner fails.
     */
    @Test
    public void testRevokeOwnerFails() {
        assertFalse(service.revokeUser("owner@smarthome.com"));
    }

    /**
     * Tests getting the current user.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testGetCurrentUser() {
        service.registerUser("me@example.com", "me", "pass", "pass");
        service.authenticate("me@example.com", "pass");
        
        User current = service.getCurrentUser();
        assertNotNull(current);
        assertEquals("me@example.com", current.getEmail());
        assertEquals("me", current.getUsername());
    }

    /**
     * Tests registration failure when database is not configured.
     */
    @Test
    public void testRegisterDatabaseError() {
        System.clearProperty(URL_PROPERTY);
        JdbcUserService unconfigured = new JdbcUserService(new JdbcUserRegistrationStore());
        assertEquals(RegistrationStatus.DATABASE_NOT_CONFIGURED, unconfigured.registerUser("e@e.com", "u", "p", "p"));
    }
}
