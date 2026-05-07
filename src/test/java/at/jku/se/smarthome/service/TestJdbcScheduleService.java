package at.jku.se.smarthome.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.Schedule;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.mock.MockLogService;
import at.jku.se.smarthome.service.mock.MockNotificationService;
import at.jku.se.smarthome.service.mock.MockRoomService;
import at.jku.se.smarthome.service.mock.MockVacationModeService;
import at.jku.se.smarthome.service.real.schedule.JdbcScheduleService;

/**
 * Unit tests for JdbcScheduleService.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods"})
public class TestJdbcScheduleService {


    /** JDBC URL property. */
    private static final String URL_PROPERTY = "smarthome.db.url";
    /** JDBC user property. */
    private static final String USER_PROPERTY = "smarthome.db.user";
    /** JDBC password property. */
    private static final String PASSWORD_PROPERTY = "smarthome.db.password";

    /** JDBC URL for in-memory test database. */
    private String jdbcUrl;
    /** Schedule service under test. */
    private JdbcScheduleService service;

    /**
     * Sets up test fixtures before each test.
     */
    @Before
    public void setUp() {
        jdbcUrl = "jdbc:h2:mem:schedule_" + System.nanoTime() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        System.setProperty(URL_PROPERTY, jdbcUrl);
        System.setProperty(USER_PROPERTY, "sa");
        System.setProperty(PASSWORD_PROPERTY, "");

        MockRoomService.resetForTesting();
        MockLogService.resetForTesting();
        MockNotificationService.resetForTesting();
        MockVacationModeService.resetForTesting();
        JdbcScheduleService.resetForTesting();

        MockRoomService.getInstance();
        MockLogService.getInstance();
        MockNotificationService.getInstance();

        ServiceRegistry.resetForTesting();
        ServiceRegistry.setRoomServiceForTesting(MockRoomService.getInstance());
        ServiceRegistry.setLogServiceForTesting(MockLogService.getInstance());
        ServiceRegistry.setNotificationServiceForTesting(MockNotificationService.getInstance());

        service = JdbcScheduleService.getInstance();
        
        // Mock devices are already initialized in MockRoomService.initializeMockRooms()
        // No additional initialization needed for schedule execution tests
    }

    /**
     * Tears down test fixtures after each test.
     */
    @After
    public void tearDown() {
        JdbcScheduleService.resetForTesting();
        System.clearProperty(URL_PROPERTY);
        System.clearProperty(USER_PROPERTY);
        System.clearProperty(PASSWORD_PROPERTY);
    }

    /**
     * Test: add schedule returns non-null.
     */
    @Test
    public void addScheduleReturnsNonNull() throws Exception {
        Schedule schedule = service.addSchedule("Morning", "dev-001", "Main Light", "Turn Off", "07:00 AM", "Daily", true);
        assertNotNull(schedule);
    }

    /**
     * Test: add schedule persists to collection.
     */
    @Test
    public void addSchedulePersistedToCollection() throws Exception {
        service.addSchedule("Morning", "dev-001", "Main Light", "Turn Off", "07:00 AM", "Daily", true);
        assertEquals(1, service.getSchedules().size());
    }

    /**
     * Test: update schedule changes name.
     */
    @Test
    public void updateScheduleChangesName() throws Exception {
        Schedule schedule = service.addSchedule("Morning", "dev-001", "Main Light", "Turn Off", "07:00 AM", "Daily", true);
        service.updateSchedule(schedule.getId(), "Morning Updated", "dev-001", "Main Light", "Turn On", "08:00", "Weekdays", false);
        assertEquals("Morning Updated", service.getScheduleById(schedule.getId()).getName());
    }

    /**
     * Test: update schedule deactivates.
     */
    @Test
    public void updateScheduleDeactivates() throws Exception {
        Schedule schedule = service.addSchedule("Morning", "dev-001", "Main Light", "Turn Off", "07:00 AM", "Daily", true);
        service.updateSchedule(schedule.getId(), "Morning Updated", "dev-001", "Main Light", "Turn On", "08:00", "Weekdays", false);
        assertFalse(service.getScheduleById(schedule.getId()).isActive());
    }

    /**
     * Test: toggle schedule enables inactive schedule.
     */
    @Test
    public void toggleScheduleEnablesInactive() throws Exception {
        Schedule schedule = service.addSchedule("Morning", "dev-001", "Main Light", "Turn Off", "07:00 AM", "Daily", true);
        service.updateSchedule(schedule.getId(), "Morning Updated", "dev-001", "Main Light", "Turn On", "08:00", "Weekdays", false);
        service.toggleSchedule(schedule.getId());
        assertTrue(service.getScheduleById(schedule.getId()).isActive());
    }

    /**
     * Test: delete schedule removes from collection.
     */
    @Test
    public void deleteScheduleRemovesFromCollection() throws Exception {
        Schedule schedule = service.addSchedule("Morning", "dev-001", "Main Light", "Turn Off", "07:00 AM", "Daily", true);
        service.deleteSchedule(schedule.getId());
        assertEquals(0, service.getSchedules().size());
    }

    /**
     * Test: add schedule persists to database.
     */
    @Test
    @SuppressWarnings("PMD.CheckResultSet")
    public void addSchedulePersistedToDatabase() throws Exception {
        service.addSchedule("Warmup", "dev-004", "Temperature Control", "Set to 22°C", "06:30 AM", "Daily", true);

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT id, name FROM scheduled_actions WHERE name = 'Warmup'")) {
            assertTrue(resultSet.next());
        }
    }

    /**
     * Test: reload after reset loads persisted schedule.
     */
    @Test
    public void reloadAfterResetLoadsPersisted() throws Exception {
        service.addSchedule("Warmup", "dev-004", "Temperature Control", "Set to 22°C", "06:30 AM", "Daily", true);

        JdbcScheduleService.resetForTesting();
        service = JdbcScheduleService.getInstance();
        assertNotNull(service.getScheduleByName("Warmup"));
    }

    /**
     * Test: add daily schedule returns non-null.
     */
    @Test
    public void addDailyScheduleReturnsNonNull() throws Exception {
        Schedule schedule = service.addSchedule("Wake Up", "dev-003", "Bed Light", "Turn On", "07:00 AM", "Daily", true);
        assertNotNull(schedule);
    }

    /**
     * Test: add weekly schedule returns non-null.
     */
    @Test
    public void addWeeklyScheduleReturnsNonNull() throws Exception {
        Schedule weekly = service.addSchedule("Weekly Warmup", "dev-004", "Temperature Control", "Set to 24°C", "Fri 09:00 AM", "Weekly", true);
        assertNotNull(weekly);
    }

    /**
     * Test: add multiple recurring schedules in collection.
     */
    @Test
    public void addMultipleRecurringSchedulesInCollection() throws Exception {
        service.addSchedule("Wake Up", "dev-003", "Bed Light", "Turn On", "07:00 AM", "Daily", true);
        service.addSchedule("Weekly Warmup", "dev-004", "Temperature Control", "Set to 24°C", "Fri 09:00 AM", "Weekly", true);
        assertEquals(2, service.getSchedules().size());
    }

    /**
     * Test: multiple schedules persisted to database.
     */
    @Test
    @SuppressWarnings({"PMD.CheckResultSet", "PMD.UnitTestContainsTooManyAsserts"})
    public void multipleSchedulePersistedToDatabase() throws Exception {
        service.addSchedule("Wake Up", "dev-003", "Bed Light", "Turn On", "07:00 AM", "Daily", true);
        service.addSchedule("Weekly Warmup", "dev-004", "Temperature Control", "Set to 24°C", "Fri 09:00 AM", "Weekly", true);

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM scheduled_actions")) {
            assertTrue(resultSet.next());
            assertEquals(2, resultSet.getInt(1));
        }
    }

    // -----------------------------------------------------------------------
    // executeSchedule
    // -----------------------------------------------------------------------

    /** Execute active "Turn On" schedule returns true. */
    @Test
    public void executeScheduleTurnOnReturnsTrue() {
        Schedule schedule = service.addSchedule("Light On", "dev-001", "Main Light", "Turn On", "07:00", "Daily", true);
        assertTrue(service.executeSchedule(schedule.getId()));
    }

    /** Execute active "Turn Off" schedule returns true. */
    @Test
    public void executeScheduleTurnOffReturnsTrue() {
        Schedule schedule = service.addSchedule("Light Off", "dev-001", "Main Light", "Turn Off", "22:00", "Daily", true);
        assertTrue(service.executeSchedule(schedule.getId()));
    }

    /** Execute active "Open" schedule returns true. */
    @Test
    public void executeScheduleOpenReturnsTrue() {
        Schedule schedule = service.addSchedule("Blind Open", "dev-008", "Hallway Blind", "Open", "08:00", "Daily", true);
        assertTrue(service.executeSchedule(schedule.getId()));
    }

    /** Execute active "Close" schedule returns true. */
    @Test
    public void executeScheduleCloseReturnsTrue() {
        Schedule schedule = service.addSchedule("Blind Close", "dev-008", "Hallway Blind", "Close", "20:00", "Daily", true);
        assertTrue(service.executeSchedule(schedule.getId()));
    }

    /** Execute schedule with dimmer action returns true. */
    @Test
    public void executeScheduleDimmerActionReturnsTrue() {
        Schedule schedule = service.addSchedule("Dim 50", "dev-002", "Dimmer Light", "Set to 50%", "18:00", "Daily", true);
        assertTrue(service.executeSchedule(schedule.getId()));
    }

    /** Execute schedule with thermostat action returns true. */
    @Test
    public void executeScheduleThermostatActionReturnsTrue() {
        Schedule schedule = service.addSchedule("Temp 22", "dev-004", "Temperature Control", "Set to 22°C", "06:00", "Daily", true);
        assertTrue(service.executeSchedule(schedule.getId()));
    }

    /** Execute inactive schedule returns false. */
    @Test
    public void executeInactiveScheduleReturnsFalse() {
        Schedule schedule = service.addSchedule("Off", "dev-001", "Main Light", "Turn On", "07:00", "Daily", false);
        assertFalse(service.executeSchedule(schedule.getId()));
    }

    /** Execute unknown schedule ID returns false. */
    @Test
    public void executeUnknownScheduleReturnsFalse() {
        assertFalse(service.executeSchedule("nonexistent-id"));
    }

    /** Execute schedule with unknown device resolves by name. */
    @Test
    public void executeScheduleResolvesDeviceByName() {
        Schedule schedule = service.addSchedule("ByName", "unknown-id", "Main Light", "Turn On", "07:00", "Daily", true);
        assertTrue(service.executeSchedule(schedule.getId()));
    }

    // -----------------------------------------------------------------------
    // toggleSchedule
    // -----------------------------------------------------------------------

    /** Toggle schedule disables active schedule. */
    @Test
    public void toggleScheduleDisablesActive() {
        Schedule schedule = service.addSchedule("Morning", "dev-001", "Main Light", "Turn Off", "07:00", "Daily", true);
        service.toggleSchedule(schedule.getId());
        assertFalse(service.getScheduleById(schedule.getId()).isActive());
    }

    /** Toggle unknown schedule returns false. */
    @Test
    public void toggleUnknownScheduleReturnsFalse() {
        assertFalse(service.toggleSchedule("nonexistent-id"));
    }

    // -----------------------------------------------------------------------
    // lookup methods
    // -----------------------------------------------------------------------

    /** getScheduleByName returns matching schedule. */
    @Test
    public void getScheduleByNameReturnsMatch() {
        service.addSchedule("UniqueName", "dev-001", "Main Light", "Turn On", "07:00", "Daily", true);
        assertNotNull(service.getScheduleByName("UniqueName"));
    }

    /** getScheduleByName returns null for missing. */
    @Test
    public void getScheduleByNameReturnsNullForMissing() {
        assertNull(service.getScheduleByName("DoesNotExist"));
    }

    /** getScheduleById returns null for missing. */
    @Test
    public void getScheduleByIdReturnsNullForMissing() {
        assertNull(service.getScheduleById("nonexistent-id"));
    }

    /** hasConflicts always returns false. */
    @Test
    public void hasConflictsReturnsFalse() {
        Schedule schedule = service.addSchedule("Morning", "dev-001", "Main Light", "Turn On", "07:00", "Daily", true);
        assertFalse(service.hasConflicts(schedule.getId()));
    }

    // -----------------------------------------------------------------------
    // deleteSchedule
    // -----------------------------------------------------------------------

    /** Delete unknown schedule returns false. */
    @Test
    public void deleteUnknownScheduleReturnsFalse() {
        assertFalse(service.deleteSchedule("nonexistent-id"));
    }

    /** Delete persisted schedule removes from database. */
    @Test
    @SuppressWarnings("PMD.CheckResultSet")
    public void deleteScheduleRemovesFromDatabase() throws Exception {
        Schedule schedule = service.addSchedule("ToDelete", "dev-001", "Main Light", "Turn On", "07:00", "Daily", true);
        service.deleteSchedule(schedule.getId());

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM scheduled_actions")) {
            assertTrue(resultSet.next());
            assertEquals(0, resultSet.getInt(1));
        }
    }

    // -----------------------------------------------------------------------
    // startRecurringExecution / stopRecurringExecution
    // -----------------------------------------------------------------------

    /** Start recurring execution can be called without error. */
    @Test
    public void startRecurringExecutionDoesNotThrow() {
        service.startRecurringExecution();
    }

    /** Stop recurring execution can be called without error. */
    @Test
    public void stopRecurringExecutionDoesNotThrow() {
        service.startRecurringExecution();
        service.stopRecurringExecution();
    }

    /** Start recurring execution is idempotent. */
    @Test
    public void startRecurringExecutionIsIdempotent() {
        service.startRecurringExecution();
        service.startRecurringExecution();
    }
}