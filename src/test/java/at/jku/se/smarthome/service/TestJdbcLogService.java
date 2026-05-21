package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.LogEntry;
import at.jku.se.smarthome.service.real.log.JdbcLogService;

/**
 * Unit tests for {@link JdbcLogService}.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.TooManyStaticImports", "PMD.UnitTestContainsTooManyAsserts"})
public class TestJdbcLogService {

    /** URL property key. */
    private static final String URL_PROPERTY = "smarthome.db.url";
    /** User property key. */
    private static final String USER_PROPERTY = "smarthome.db.user";
    /** Password property key. */
    private static final String PASSWORD_PROPERTY = "smarthome.db.password";

    /** Service instance under test. */
    private JdbcLogService service;

    /** Default constructor. */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public TestJdbcLogService() {
        // Default constructor
    }

    /**
     * Sets up test environment.
     */
    @Before
    public void setUp() {
        String jdbcUrl = "jdbc:h2:mem:log_service_" + System.nanoTime() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        System.setProperty(URL_PROPERTY, jdbcUrl);
        System.setProperty(USER_PROPERTY, "sa");
        System.setProperty(PASSWORD_PROPERTY, "");
        
        JdbcLogService.resetForTesting();
        service = JdbcLogService.getInstance();
    }

    /**
     * Cleans up test environment.
     */
    @After
    public void tearDown() {
        System.clearProperty(URL_PROPERTY);
        System.clearProperty(USER_PROPERTY);
        System.clearProperty(PASSWORD_PROPERTY);
        JdbcLogService.resetForTesting();
    }

    /**
     * Tests singleton behavior.
     */
    @Test
    public void testSingleton() {
        assertNotNull(service);
        assertEquals(service, JdbcLogService.getInstance());
    }

    /**
     * Tests adding log entries.
     */
    @Test
    public void testAddLogEntry() {
        service.addLogEntry("Device A", "Room 1", "Action X", "Actor Y");
        assertEquals(1, service.getLogs().size());
        LogEntry entry = service.getLogs().get(0);
        assertEquals("Device A", entry.getDevice());
        assertEquals("Room 1", entry.getRoom());
        assertEquals("Action X", entry.getAction());
        assertEquals("Actor Y", entry.getActor());
    }

    /**
     * Tests adding log entry with short signature.
     */
    @Test
    public void testAddLogEntryShort() {
        service.addLogEntry("Device B", "Action Y", "Actor Z");
        assertEquals(1, service.getLogs().size());
        assertEquals("Unknown", service.getLogs().get(0).getRoom());
    }

    /**
     * Tests log filtering by room.
     */
    @Test
    public void testGetLogsByRoom() {
        service.addLogEntry("D1", "Room A", "Act", "User");
        service.addLogEntry("D2", "Room B", "Act", "User");
        service.addLogEntry("D3", "Room A", "Act", "User");
        
        assertEquals(2, service.getLogsByRoom("Room A").size());
        assertEquals(1, service.getLogsByRoom("Room B").size());
    }

    /**
     * Tests log filtering by device.
     */
    @Test
    public void testGetLogsByDevice() {
        service.addLogEntry("Device A", "Room 1", "Act", "User");
        service.addLogEntry("Device B", "Room 1", "Act", "User");
        
        assertEquals(1, service.getLogsByDevice("Device A").size());
    }

    /**
     * Tests log filtering by actor.
     */
    @Test
    public void testGetLogsByActor() {
        service.addLogEntry("D1", "R1", "Act", "User A");
        service.addLogEntry("D2", "R1", "Act", "User B");
        
        assertEquals(1, service.getLogsByActor("User A").size());
    }

    /**
     * Tests retrieving unique devices.
     */
    @Test
    public void testGetUniqueDevices() {
        service.addLogEntry("B", "R1", "Act", "User");
        service.addLogEntry("A", "R1", "Act", "User");
        service.addLogEntry("B", "R1", "Act", "User");
        
        List<String> devices = service.getUniqueDevices();
        assertEquals(2, devices.size());
        assertEquals("A", devices.get(0));
        assertEquals("B", devices.get(1));
    }

    /**
     * Tests CSV export functionality.
     */
    @Test
    public void testExportToCSV() {
        service.addLogEntry("Device", "Room", "Action", "Actor");
        String csv = service.exportToCSV();
        assertTrue(csv.contains("Timestamp;Device;Room;Action;Actor"));
        assertTrue(csv.contains("\"Device\";\"Room\";\"Action\";\"Actor\""));
    }

    /**
     * Tests refreshing logs from database.
     */
    @Test
    public void testRefreshLogs() {
        service.addLogEntry("D1", "R1", "A1", "U1");
        
        // Reset and get new instance to force load from DB
        JdbcLogService.resetForTesting();
        JdbcLogService newService = JdbcLogService.getInstance();
        
        assertEquals(1, newService.getLogs().size());
        assertEquals("D1", newService.getLogs().get(0).getDevice());
    }
}
