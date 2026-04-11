package at.jku.se.smarthome.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.service.mock.MockLogService;
import at.jku.se.smarthome.service.mock.MockUserService;
import at.jku.se.smarthome.service.real.room.JdbcRoomService;

public class TestJdbcRoomService {

    private static final String URL_PROPERTY = "smarthome.db.url";
    private static final String USER_PROPERTY = "smarthome.db.user";
    private static final String PASSWORD_PROPERTY = "smarthome.db.password";

    private String jdbcUrl;
    private JdbcRoomService service;

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

    @After
    public void tearDown() {
        JdbcRoomService.resetForTesting();
        System.clearProperty(URL_PROPERTY);
        System.clearProperty(USER_PROPERTY);
        System.clearProperty(PASSWORD_PROPERTY);
    }

    @Test
    public void addRenameRemoveDevice_persistsChanges() throws Exception {
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

