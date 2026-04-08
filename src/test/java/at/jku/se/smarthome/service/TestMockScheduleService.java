package at.jku.se.smarthome.service;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.LogEntry;
import javafx.collections.ObservableList;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link MockScheduleService#executeSchedule} covering automated logging (FR-08).
 *
 * Device names used in tests must match MockRoomService seed data:
 *   "Main Light"        – Switch, Living Room, starts ON
 *   "Bed Light"         – Switch, Bedroom, starts OFF
 *   "Dimmer Light"      – Dimmer, Living Room, starts ON
 *   "Temperature Control" – Thermostat, Bedroom, starts ON
 *
 * Note: the built-in mock schedules reference "Living Room Light" / "Bedroom Light"
 * which do not exist in MockRoomService seed data — executeSchedule correctly
 * returns false for those (device not found).
 */
public class TestMockScheduleService {

    private MockScheduleService scheduleService;
    private MockRoomService roomService;
    private MockLogService logService;

    @Before
    public void setUp() {
        MockScheduleService.resetForTesting();
        MockRoomService.resetForTesting();
        MockLogService.resetForTesting();
        scheduleService = MockScheduleService.getInstance();
        roomService = MockRoomService.getInstance();
        logService = MockLogService.getInstance();
    }

    // -----------------------------------------------------------------------
    // Guard conditions
    // -----------------------------------------------------------------------

    @Test
    public void executeSchedule_unknownId_returnsFalse() {
        assertFalse(scheduleService.executeSchedule("does-not-exist"));
    }

    @Test
    public void executeSchedule_inactiveSchedule_returnsFalse() {
        // sched-003 "Weekend Relax" is inactive in seed data
        assertFalse(scheduleService.executeSchedule("sched-003"));
    }

    @Test
    public void executeSchedule_deviceNotFound_returnsFalse() {
        // Seed schedules target device names that don't exist in MockRoomService
        assertFalse(scheduleService.executeSchedule("sched-001"));
    }

    // -----------------------------------------------------------------------
    // Turn On
    // -----------------------------------------------------------------------

    @Test
    public void executeSchedule_turnOn_setDeviceStateTrue() {
        // Bed Light starts OFF — schedule turns it ON
        scheduleService.addSchedule("Night On", "Bed Light", "Turn On", "07:00 AM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Night On").getId();

        Device bed = roomService.getDeviceByName("Bed Light");
        assertFalse("pre-condition: Bed Light starts OFF", bed.getState());

        assertTrue(scheduleService.executeSchedule(schedId));
        assertTrue(bed.getState());
    }

    // -----------------------------------------------------------------------
    // Turn Off
    // -----------------------------------------------------------------------

    @Test
    public void executeSchedule_turnOff_setDeviceStateFalse() {
        // Main Light starts ON — schedule turns it OFF
        scheduleService.addSchedule("Night Off", "Main Light", "Turn Off", "11:00 PM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Night Off").getId();

        Device main = roomService.getDeviceByName("Main Light");
        assertTrue("pre-condition: Main Light starts ON", main.getState());

        assertTrue(scheduleService.executeSchedule(schedId));
        assertFalse(main.getState());
    }

    // -----------------------------------------------------------------------
    // Set brightness
    // -----------------------------------------------------------------------

    @Test
    public void executeSchedule_setBrightness_setsDimmerLevel() {
        scheduleService.addSchedule("Dim Scene", "Dimmer Light", "Set 40%", "18:00", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Dim Scene").getId();

        assertTrue(scheduleService.executeSchedule(schedId));
        Device dimmer = roomService.getDeviceByName("Dimmer Light");
        assertEquals(40, dimmer.getBrightness());
    }

    // -----------------------------------------------------------------------
    // Logging (FR-08 core requirement)
    // -----------------------------------------------------------------------

    @Test
    public void executeSchedule_logsEntry_withScheduleActor() {
        scheduleService.addSchedule("Log Test", "Main Light", "Turn On", "06:00 AM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Log Test").getId();

        int logsBefore = logService.getLogs().size();
        assertTrue(scheduleService.executeSchedule(schedId));

        assertEquals(logsBefore + 1, logService.getLogs().size());
        LogEntry entry = logService.getLogs().get(0); // newest first
        assertEquals("Main Light", entry.getDevice());
        assertEquals("Turn On", entry.getAction());
        assertEquals("Schedule: Log Test", entry.getActor());
    }

    @Test
    public void executeSchedule_logsEntry_withCorrectRoom() {
        scheduleService.addSchedule("Room Test", "Main Light", "Turn Off", "11:00 PM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Room Test").getId();

        scheduleService.executeSchedule(schedId);
        LogEntry entry = logService.getLogs().get(0);
        assertEquals("Living Room", entry.getRoom());
    }

    @Test
    public void executeSchedule_failedExecution_doesNotLog() {
        // Device does not exist → executeSchedule returns false and must not log
        int logsBefore = logService.getLogs().size();
        assertFalse(scheduleService.executeSchedule("sched-001")); // device not found
        assertEquals(logsBefore, logService.getLogs().size());
    }
}
