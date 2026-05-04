package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.Rule;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.mock.MockLogService;
import at.jku.se.smarthome.service.mock.MockNotificationService;
import at.jku.se.smarthome.service.mock.MockRoomService;
import at.jku.se.smarthome.service.real.rule.JdbcRuleService;

/**
 * Unit tests for JdbcRuleService — verifies that automation rules survive a
 * service restart (FR-10/FR-11 persistence) and that the in-memory mirror
 * stays consistent with database state across add/update/delete/toggle.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods",
        "PMD.MethodNamingConventions", "PMD.UnitTestContainsTooManyAsserts",
        "PMD.JUnitTestsShouldIncludeAssert", "PMD.CommentRequired",
        "PMD.TooManyStaticImports"})
public class TestJdbcRuleService {

    /** JDBC URL property. */
    private static final String URL_PROPERTY = "smarthome.db.url";
    /** JDBC user property. */
    private static final String USER_PROPERTY = "smarthome.db.user";
    /** JDBC password property. */
    private static final String PASSWORD_PROPERTY = "smarthome.db.password";

    /** JDBC URL for in-memory test database. */
    private String jdbcUrl;
    /** Service under test. */
    private JdbcRuleService service;

    @Before
    public void setUp() {
        jdbcUrl = "jdbc:h2:mem:rules_" + System.nanoTime()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        System.setProperty(URL_PROPERTY, jdbcUrl);
        System.setProperty(USER_PROPERTY, "sa");
        System.setProperty(PASSWORD_PROPERTY, "");

        MockRoomService.resetForTesting();
        MockLogService.resetForTesting();
        MockNotificationService.resetForTesting();
        JdbcRuleService.resetForTesting();

        MockRoomService.getInstance();
        MockLogService.getInstance();
        // Notification + log services are looked up via ServiceRegistry inside
        // JdbcRuleService; route them to the mock implementations so the rule
        // service does not pull in another JDBC singleton.
        ServiceRegistry.setNotificationServiceForTesting(MockNotificationService.getInstance());
        ServiceRegistry.setLogServiceForTesting(MockLogService.getInstance());

        service = JdbcRuleService.getInstance();
    }

    @After
    public void tearDown() {
        JdbcRuleService.resetForTesting();
        ServiceRegistry.setNotificationServiceForTesting(null);
        ServiceRegistry.setLogServiceForTesting(null);
        System.clearProperty(URL_PROPERTY);
        System.clearProperty(USER_PROPERTY);
        System.clearProperty(PASSWORD_PROPERTY);
    }

    @Test
    public void seededRules_loadFromInitScriptOnFirstStart() {
        assertEquals(3, service.getRules().size());
        Rule first = service.getRules().stream()
                .filter(r -> "rule-001".equals(r.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals("Morning Routine", first.getName());
        assertEquals("Time", first.getTriggerType());
    }

    @Test
    public void addRule_persistsAcrossServiceRestart() {
        Rule created = service.addRule("Late Night", "Time", "Clock", "23:00",
                "Turn Off", "Main Light");
        assertNotNull(created);

        JdbcRuleService.resetForTesting();
        JdbcRuleService restarted = JdbcRuleService.getInstance();

        Rule reloaded = restarted.getRules().stream()
                .filter(r -> created.getId().equals(r.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull("Rule must survive a service restart", reloaded);
        assertEquals("Late Night", reloaded.getName());
        assertTrue(reloaded.isEnabled());
    }

    @Test
    public void updateRule_writesChangesToDatabase() {
        Rule created = service.addRule("Tweak Me", "Time", "Clock", "07:00",
                "Turn On", "Main Light");
        assertNotNull(created);

        boolean updated = service.updateRule(created.getId(), "Tweaked",
                "Time", "Clock", "08:00", "Turn Off", "Main Light");
        assertTrue(updated);

        JdbcRuleService.resetForTesting();
        JdbcRuleService restarted = JdbcRuleService.getInstance();
        Rule reloaded = restarted.getRules().stream()
                .filter(r -> created.getId().equals(r.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals("Tweaked", reloaded.getName());
        assertEquals("08:00", reloaded.getCondition());
        assertEquals("Turn Off", reloaded.getAction());
    }

    @Test
    public void deleteRule_removesFromDatabaseAndMirror() {
        int sizeBefore = service.getRules().size();
        boolean deleted = service.deleteRule("rule-001");
        assertTrue(deleted);
        assertEquals(sizeBefore - 1, service.getRules().size());

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) AS cnt FROM rules WHERE id = 'rule-001'")) {
            if (resultSet.next()) {
                assertEquals(0, resultSet.getInt("cnt"));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    public void deleteRule_unknownId_returnsFalse() {
        assertFalse(service.deleteRule("does-not-exist"));
    }

    @Test
    public void toggleRule_persistsEnabledFlipAcrossRestart() {
        boolean toggled = service.toggleRule("rule-001");
        assertTrue(toggled);
        Rule afterToggle = service.getRules().stream()
                .filter(r -> "rule-001".equals(r.getId()))
                .findFirst()
                .orElseThrow();
        assertFalse(afterToggle.isEnabled());
        assertEquals("Inactive", afterToggle.getStatus());

        JdbcRuleService.resetForTesting();
        JdbcRuleService restarted = JdbcRuleService.getInstance();
        Rule reloaded = restarted.getRules().stream()
                .filter(r -> "rule-001".equals(r.getId()))
                .findFirst()
                .orElseThrow();
        assertFalse("toggled state must persist", reloaded.isEnabled());
        assertEquals("Inactive", reloaded.getStatus());
    }

    @Test
    public void addRule_invalidValidation_returnsNullAndDoesNotPersist() {
        // Empty trigger type is invalid per RuleValidator.
        Rule created = service.addRule("Bad Rule", "", "Clock", "07:00",
                "Turn On", "Main Light");
        assertNull(created);

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) AS cnt FROM rules WHERE name = 'Bad Rule'")) {
            if (resultSet.next()) {
                assertEquals("invalid rule must not be inserted", 0, resultSet.getInt("cnt"));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
