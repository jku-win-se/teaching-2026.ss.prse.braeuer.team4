package at.jku.se.smarthome.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.service.mock.MockLogService;
import at.jku.se.smarthome.service.mock.MockUserService;
import at.jku.se.smarthome.service.real.room.JdbcRoomService;

/**
 * Integration-style unit tests for the JDBC-backed {@link at.jku.se.smarthome.service.real.room.JdbcRoomService}.
 *
 * Exercises add, rename and remove device operations against an in-memory H2 database to
 * validate persistence and in-memory view synchronization.
 */
public class TestJdbcRoomService {

    /** JDBC URL property. */
    private static final String URL_PROPERTY = "smarthome.db.url";
    /** JDBC user property. */
    private static final String USER_PROPERTY = "smarthome.db.user";
    /** JDBC password property. */
    private static final String PASSWORD_PROPERTY = "smarthome.db.password";

    /** JDBC URL for in-memory test database. */
    private String jdbcUrl;
    /** Room service under test. */
    private JdbcRoomService service;

    /**
     * Sets up test fixtures before each test.
     */
    @Before
    public void setUp() {
        jdbcUrl = "jdbc:h2:mem:rooms_" + System.nanoTime() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        System.setProperty(URL_PROPERTY, jdbcUrl);
        System.setProperty(USER_PROPERTY, "sa");
        System.setProperty(PASSWORD_PROPERTY, "");

        MockLogService.resetForTesting();
        MockUserService.resetForTesting();
        JdbcRoomService.resetForTesting();

        service = JdbcRoomService.getInstance();
    }

    /**
     * Tears down test fixtures after each test.
     */
    @After
    public void tearDown() {
        JdbcRoomService.resetForTesting();
        System.clearProperty(URL_PROPERTY);
        System.clearProperty(USER_PROPERTY);
        System.clearProperty(PASSWORD_PROPERTY);
    }

    /**
     * Test: add, rename, and remove device persists changes.
     */
    @Test
    public void addRenameRemoveDevicePersistsChanges() throws Exception {
        Room room = service.addRoom("Test Room");
        assertNotNull(room);

        Device device = service.addDeviceToRoom(room.getId(), "Test Device", "Switch");
        assertNotNull(device);
        assertEquals(1, service.getAllDevices().size());

        assertTrue(service.renameDevice(room.getId(), device.getId(), "Renamed Device"));
        assertEquals("Renamed Device", service.getDeviceById(device.getId()).getName());

        // verify persisted rows
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM devices")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }

        assertTrue(service.removeDeviceFromRoom(room.getId(), device.getId()));
        assertEquals(0, service.getAllDevices().size());

        // verify DB after removal
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM devices")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }
}

