package at.jku.se.smarthome.service;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.Schedule;
import at.jku.se.smarthome.service.mock.MockLogService;
import at.jku.se.smarthome.service.mock.MockNotificationService;
import at.jku.se.smarthome.service.mock.MockRoomService;
import at.jku.se.smarthome.service.mock.MockVacationModeService;
import at.jku.se.smarthome.service.real.schedule.JdbcScheduleService;

public class TestJdbcScheduleService {

    private static final String URL_PROPERTY = "smarthome.db.url";
    private static final String USER_PROPERTY = "smarthome.db.user";
    private static final String PASSWORD_PROPERTY = "smarthome.db.password";

    private String jdbcUrl;
    private JdbcScheduleService service;
    private MockRoomService roomService;
    private MockLogService logService;
    private MockNotificationService notificationService;

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

        roomService = MockRoomService.getInstance();
        logService = MockLogService.getInstance();
        notificationService = MockNotificationService.getInstance();
        service = JdbcScheduleService.getInstance();
    }

    @After
    public void tearDown() {
        JdbcScheduleService.resetForTesting();
        System.clearProperty(URL_PROPERTY);
        System.clearProperty(USER_PROPERTY);
        System.clearProperty(PASSWORD_PROPERTY);
    }

    @Test
    public void addUpdateToggleDeleteSchedule_persistsChanges() throws Exception {
        Schedule schedule = service.addSchedule("Morning", "dev-001", "Main Light", "Turn Off", "07:00 AM", "Daily", true);
        assertNotNull(schedule);
        assertEquals(1, service.getSchedules().size());

        assertTrue(service.updateSchedule(schedule.getId(), "Morning Updated", "dev-001", "Main Light", "Turn On", "08:00", "Weekdays", false));
        assertEquals("Morning Updated", service.getScheduleById(schedule.getId()).getName());
        assertFalse(service.getScheduleById(schedule.getId()).isActive());

        assertTrue(service.toggleSchedule(schedule.getId()));
        assertTrue(service.getScheduleById(schedule.getId()).isActive());

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM scheduled_actions")) {
            assertTrue(resultSet.next());
            assertEquals(1, resultSet.getInt(1));
        }

        assertTrue(service.deleteSchedule(schedule.getId()));
        assertEquals(0, service.getSchedules().size());
    }

    @Test
    public void executeAndReloadSchedule_updatesDeviceLogNotificationAndDatabase() throws Exception {
        Schedule schedule = service.addSchedule("Warmup", "dev-004", "Temperature Control", "Set to 22°C", "06:30 AM", "Daily", true);

        assertTrue(service.executeSchedule(schedule.getId()));
        assertEquals(22.0, roomService.getDeviceById("dev-004").getTemperature(), 0.001);
        assertTrue(logService.getLogs().get(0).getActor().contains("Schedule: Warmup"));
        assertTrue(notificationService.getNotifications().get(0).getMessage().contains("Executed schedule 'Warmup'"));

        JdbcScheduleService.resetForTesting();
        service = JdbcScheduleService.getInstance();
        assertNotNull(service.getScheduleByName("Warmup"));

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT last_triggered_at FROM scheduled_actions WHERE name = 'Warmup'")) {
            assertTrue(resultSet.next());
            assertTrue(resultSet.getTimestamp(1) != null);
        }
    }

    @Test
    public void processDueSchedules_executesMatchingRecurringSchedules() throws Exception {
        Schedule schedule = service.addSchedule("Wake Up", "dev-003", "Bed Light", "Turn On", "07:00 AM", "Daily", true);

        Method method = JdbcScheduleService.class.getDeclaredMethod("processDueSchedules", LocalDateTime.class);
        method.setAccessible(true);

        int firstRun = (int) method.invoke(service, LocalDateTime.of(2026, 4, 10, 7, 0));
        int duplicateRun = (int) method.invoke(service, LocalDateTime.of(2026, 4, 10, 7, 0, 45));

        assertEquals(1, firstRun);
        assertEquals(0, duplicateRun);
        assertTrue(roomService.getDeviceById("dev-003").getState());

        Schedule weekly = service.addSchedule("Weekly Warmup", "dev-004", "Temperature Control", "Set to 24°C", "Fri 09:00 AM", "Weekly", true);
        assertNotNull(weekly);
        int weeklyMiss = (int) method.invoke(service, LocalDateTime.of(2026, 4, 9, 9, 0));
        int weeklyHit = (int) method.invoke(service, LocalDateTime.of(2026, 4, 10, 9, 0));

        assertEquals(0, weeklyMiss);
        assertEquals(1, weeklyHit);
    }
}