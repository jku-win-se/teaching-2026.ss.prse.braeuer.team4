package at.jku.se.smarthome.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.service.real.auth.JdbcUserRegistrationStore;
import at.jku.se.smarthome.service.real.auth.UserRegistrationStore;

/**
 * Unit tests for JdbcUserRegistrationStore.
 */
public class TestJdbcUserRegistrationStore {

    /** JDBC URL property. */
    private static final String URL_PROPERTY = "smarthome.db.url";
    /** JDBC user property. */
    private static final String USER_PROPERTY = "smarthome.db.user";
    /** JDBC password property. */
    private static final String PASSWORD_PROPERTY = "smarthome.db.password";

    /** User registration store under test. */
    private JdbcUserRegistrationStore store;
    /** JDBC URL for in-memory test database. */
    private String jdbcUrl;

    /**
     * Sets up test fixtures before each test.
     */
    @Before
    public void setUp() {
        jdbcUrl = "jdbc:h2:mem:auth_" + System.nanoTime() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        System.setProperty(URL_PROPERTY, jdbcUrl);
        System.setProperty(USER_PROPERTY, "sa");
        System.setProperty(PASSWORD_PROPERTY, "");
        store = new JdbcUserRegistrationStore();
    }

    /**
     * Tears down test fixtures after each test.
     */
    @After
    public void tearDown() {
        System.clearProperty(URL_PROPERTY);
        System.clearProperty(USER_PROPERTY);
        System.clearProperty(PASSWORD_PROPERTY);
    }

    /**
     * Test: save user persists email.
     */
    @Test
    public void saveUserPersistsEmail() throws Exception {
        UserRegistrationStore.PersistedUser user = new UserRegistrationStore.PersistedUser(
                "owner@example.com",
                "owner",
                "hash",
                "Owner",
                "Active"
        );
        store.save(user);
        assertTrue(store.emailExists("owner@example.com"));
    }

    /**
     * Test: find by email returns present optional.
     */
    @Test
    public void findByEmailReturnsPresentOptional() throws Exception {
        UserRegistrationStore.PersistedUser user = new UserRegistrationStore.PersistedUser(
                "owner@example.com",
                "owner",
                "hash",
                "Owner",
                "Active"
        );
        store.save(user);
        assertTrue(store.findByEmail("owner@example.com").isPresent());
    }

    /**
     * Test: find by email returns correct username.
     */
    @Test
    public void findByEmailReturnsCorrectUsername() throws Exception {
        UserRegistrationStore.PersistedUser user = new UserRegistrationStore.PersistedUser(
                "owner@example.com",
                "owner",
                "hash",
                "Owner",
                "Active"
        );
        store.save(user);
        assertEquals("owner", store.findByEmail("owner@example.com").orElseThrow().username());
    }

    /**
     * Test: find by missing email returns empty optional.
     */
    @Test
    public void findByMissingEmailReturnsEmptyOptional() throws Exception {
        UserRegistrationStore.PersistedUser user = new UserRegistrationStore.PersistedUser(
                "owner@example.com",
                "owner",
                "hash",
                "Owner",
                "Active"
        );
        store.save(user);
        assertFalse(store.findByEmail("missing@example.com").isPresent());
    }

    /**
     * Test: save duplicate email throws exception.
     */
    @Test(expected = UserRegistrationStore.DuplicateEmailException.class)
    public void saveDuplicateEmailThrowsDuplicateEmailException() throws Exception {
        UserRegistrationStore.PersistedUser user = new UserRegistrationStore.PersistedUser(
                "owner@example.com",
                "owner",
                "hash",
                "Owner",
                "Active"
        );

        store.save(user);
        store.save(user);
    }

    /**
     * Test: update last login updates timestamp.
     */
    @Test
    public void updateLastLoginUpdatesTimestamp() throws Exception {
        UserRegistrationStore.PersistedUser user = new UserRegistrationStore.PersistedUser(
                "member@example.com",
                "member",
                "hash",
                "Member",
                "Active"
        );
        store.save(user);

        store.updateLastLogin("member@example.com");

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT last_login_at FROM users WHERE email = 'member@example.com'")) {
            assertTrue(resultSet.next());
        }
    }

    /**
     * Test: update last login persists non-null timestamp.
     */
    @Test
    public void updateLastLoginPersistsNonNullTimestamp() throws Exception {
        UserRegistrationStore.PersistedUser user = new UserRegistrationStore.PersistedUser(
                "member@example.com",
                "member",
                "hash",
                "Member",
                "Active"
        );
        store.save(user);

        store.updateLastLogin("member@example.com");

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT last_login_at FROM users WHERE email = 'member@example.com'")) {
            resultSet.next();
            assertTrue(resultSet.getTimestamp(1) != null);
        }
    }
}