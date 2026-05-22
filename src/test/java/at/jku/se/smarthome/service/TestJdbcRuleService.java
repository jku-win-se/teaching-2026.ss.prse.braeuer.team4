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
@SuppressWarnings({"PMD.TooManyMethods", "PMD.MethodNamingConventions", 
    "PMD.UnitTestContainsTooManyAsserts", "PMD.CommentRequired", "PMD.TooManyStaticImports"})
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

    /** Default constructor. */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public TestJdbcRuleService() {
        // Default constructor
    }

    /**
     * Set up test fixtures before each test.
     */
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
        ServiceRegistry.setRoomServiceForTesting(MockRoomService.getInstance());
        ServiceRegistry.setNotificationServiceForTesting(MockNotificationService.getInstance());
        ServiceRegistry.setLogServiceForTesting(MockLogService.getInstance());

        service = JdbcRuleService.getInstance();
    }

    /**
     * Tear down test fixtures after each test.
     */
    @After
    public void tearDown() {
        JdbcRuleService.resetForTesting();
        ServiceRegistry.setNotificationServiceForTesting(null);
        ServiceRegistry.setLogServiceForTesting(null);
        System.clearProperty(URL_PROPERTY);
        System.clearProperty(USER_PROPERTY);
        System.clearProperty(PASSWORD_PROPERTY);
    }

    /**
     * Test: seeded rules are loaded from init script on first start.
     */
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

    /**
     * Test: adding rule persists across service restart.
     */
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

    /**
     * Test: updating rule writes changes to database.
     */
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

    /**
     * Test: deleting rule removes it from database and mirror.
     */
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

    /**
     * Test: deleting rule with unknown ID returns false.
     */
    @Test
    public void deleteRule_unknownId_returnsFalse() {
        assertFalse(service.deleteRule("does-not-exist"));
    }

    /**
     * Test: manual execution of rule is successful.
     */
    @Test
    public void executeRule_manual_success() {
        // Find existing "Main Light" device or add it if not found (though it should be there from MockRoomService init)
        at.jku.se.smarthome.model.Device target = ServiceRegistry.getRoomService().getDeviceByName("Main Light");
        if (target == null) {
            target = new at.jku.se.smarthome.model.Device("light-1", "Main Light", "Switch", "Living Room", false);
            ServiceRegistry.getRoomService().getRooms().get(0).addDevice(target);
        }
        target.setState(false);
        
        Rule rule = service.addRule("Test Manual", "Time", "Clock", "12:00", "Turn On", "Main Light");
        boolean executed = service.executeRule(rule.getId(), true);
        
        assertTrue("Manual execution should succeed", executed);
        assertTrue("Target device should be turned on", target.getState());
    }

    /**
     * Test: rule execution fails if condition is not met.
     */
    @Test
    public void executeRule_conditionNotMet_returnsFalse() {
        at.jku.se.smarthome.model.Device target = ServiceRegistry.getRoomService().getDeviceByName("Main Light");
        if (target == null) {
            target = new at.jku.se.smarthome.model.Device("light-1", "Main Light", "Switch", "Living Room", false);
            ServiceRegistry.getRoomService().getRooms().get(0).addDevice(target);
        }
        
        // Use a time format supported by RuleValidator (HH:mm)
        Rule rule = service.addRule("Test Cond", "Time", "Clock", "23:59", "Turn On", "Main Light");
        assertNotNull("Rule should be created", rule);
        boolean executed = service.executeRule(rule.getId(), false);
        
        assertFalse("Execution should fail if condition not met", executed);
    }

    /**
     * Test: conflict detection works for same target with opposite actions.
     */
    @Test
    public void hasConflicts_detectsSameTargetOppositeAction() {
        service.addRule("Rule 1", "Time", "Clock", "12:00", "Turn On", "Light A");
        Rule rule2 = service.addRule("Rule 2", "Time", "Clock", "12:00", "Turn Off", "Light A");
        
        assertTrue("Conflict should be detected for same target, same trigger, different action", 
                service.hasConflicts(rule2.getId()));
    }

    /**
     * Test: applying parameterized action for brightness.
     */
    @Test
    public void applyParameterizedAction_brightness() {
        at.jku.se.smarthome.model.Device target = ServiceRegistry.getRoomService().getDeviceByName("Dimmer Light");
        if (target == null) {
            target = new at.jku.se.smarthome.model.Device("dimmer-1", "Dimmer Light", "Dimmer", "Living Room", false);
            ServiceRegistry.getRoomService().getRooms().get(0).addDevice(target);
        }
        
        Rule rule = service.addRule("Dim", "Time", "Clock", "12:00", "Set to 75%", "Dimmer Light");
        service.executeRule(rule.getId(), true);
        
        assertEquals(75, target.getBrightness());
    }

    /**
     * Test: applying parameterized action for temperature.
     */
    @Test
    public void applyParameterizedAction_temperature() {
        at.jku.se.smarthome.model.Device target = ServiceRegistry.getRoomService().getDeviceByName("Temperature Control");
        if (target == null) {
            target = new at.jku.se.smarthome.model.Device("thermo-1", "Temperature Control", "Thermostat", "Bedroom", false);
            ServiceRegistry.getRoomService().getRooms().get(1).addDevice(target);
        }
        
        Rule rule = service.addRule("Warm", "Time", "Clock", "12:00", "Set to 22.5°C", "Temperature Control");
        service.executeRule(rule.getId(), true);
        
        assertEquals(22.5, target.getTemperature(), 0.01);
    }

    /**
     * Test: starting and stopping recurring execution.
     */
    @Test
    public void startAndStopExecution() {
        service.startRecurringExecution();
        service.stopRecurringExecution();
        assertNotNull(service);
    }
}
