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

public class TestJdbcUserRegistrationStore {

    private static final String URL_PROPERTY = "smarthome.db.url";
    private static final String USER_PROPERTY = "smarthome.db.user";
    private static final String PASSWORD_PROPERTY = "smarthome.db.password";

    private JdbcUserRegistrationStore store;
    private String jdbcUrl;

    @Before
    public void setUp() {
        jdbcUrl = "jdbc:h2:mem:auth_" + System.nanoTime() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        System.setProperty(URL_PROPERTY, jdbcUrl);
        System.setProperty(USER_PROPERTY, "sa");
        System.setProperty(PASSWORD_PROPERTY, "");
        store = new JdbcUserRegistrationStore();
    }

    @After
    public void tearDown() {
        System.clearProperty(URL_PROPERTY);
        System.clearProperty(USER_PROPERTY);
        System.clearProperty(PASSWORD_PROPERTY);
    }

    @Test
    public void saveAndFindByEmailRoundTripUser() throws Exception {
        UserRegistrationStore.PersistedUser user = new UserRegistrationStore.PersistedUser(
                "owner@example.com",
                "owner",
                "hash",
                "Owner",
                "Active"
        );

        store.save(user);

        assertTrue(store.emailExists("owner@example.com"));
        assertTrue(store.findByEmail("owner@example.com").isPresent());
        assertEquals("owner", store.findByEmail("owner@example.com").orElseThrow().username());
        assertFalse(store.findByEmail("missing@example.com").isPresent());
    }

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

    @Test
    public void updateLastLoginUpdatesPersistedTimestamp() throws Exception {
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
            assertTrue(resultSet.getTimestamp(1) != null);
        }
    }
}