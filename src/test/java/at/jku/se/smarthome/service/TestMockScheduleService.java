package at.jku.se.smarthome.service;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.LogEntry;
import at.jku.se.smarthome.service.mock.MockLogService;
import at.jku.se.smarthome.service.mock.MockRoomService;
import at.jku.se.smarthome.service.mock.MockScheduleService;

/**
 * Tests for {@link MockScheduleService#executeSchedule} covering automated logging (FR-08).
 * Also covers FR-09 recurring time-based execution through {@link MockScheduleService#processDueSchedules(LocalDateTime)}.
 *
 * Device names used in tests must match MockRoomService seed data:
 *   "Main Light"        – Switch, Living Room, starts ON
 *   "Bed Light"         – Switch, Bedroom, starts OFF
 *   "Dimmer Light"      – Dimmer, Living Room, starts ON
 *   "Temperature Control" – Thermostat, Bedroom, starts ON
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
        scheduleService.addSchedule("Missing Device", "Does Not Exist", "Turn On", "07:00 AM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Missing Device").getId();

        assertFalse(scheduleService.executeSchedule(schedId));
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
        scheduleService.addSchedule("Dim Scene", "Dimmer Light", "Set to 40%", "18:00", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Dim Scene").getId();

        assertTrue(scheduleService.executeSchedule(schedId));
        Device dimmer = roomService.getDeviceByName("Dimmer Light");
        assertEquals(40, dimmer.getBrightness());
    }

    @Test
    public void executeSchedule_setTemperature_setsThermostatTemperature() {
        scheduleService.addSchedule("Morning Warmup", "Temperature Control", "Set to 22°C", "06:30 AM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Morning Warmup").getId();

        assertTrue(scheduleService.executeSchedule(schedId));
        Device thermostat = roomService.getDeviceByName("Temperature Control");
        assertEquals(22.0, thermostat.getTemperature(), 0.001);
    }

    // -----------------------------------------------------------------------
    // Recurring execution (FR-09)
    // -----------------------------------------------------------------------

    @Test
    public void processDueSchedules_matchingDailyTime_executesOnlyOncePerMinute() {
        scheduleService.addSchedule("Wake Up", "Bed Light", "Turn On", "07:00 AM", "Daily", true);

        Device bed = roomService.getDeviceByName("Bed Light");
        int logsBefore = logService.getLogs().size();

        assertEquals(1, scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 10, 7, 0)));
        assertTrue(bed.getState());
        assertEquals(logsBefore + 1, logService.getLogs().size());

        assertEquals(0, scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 10, 7, 0, 45)));
        assertEquals(logsBefore + 1, logService.getLogs().size());
    }

    @Test
    public void processDueSchedules_weekdaysSchedule_skipsWeekend() {
        scheduleService.addSchedule("Weekday Entry", "Main Light", "Turn Off", "08:15", "Weekdays", true);

        Device main = roomService.getDeviceByName("Main Light");
        main.setState(true);

        assertEquals(0, scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 11, 8, 15)));
        assertTrue(main.getState());

        assertEquals(1, scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 10, 8, 15)));
        assertFalse(main.getState());
    }

    @Test
    public void processDueSchedules_weeklySchedule_requiresConfiguredDay() {
        scheduleService.addSchedule("Weekly Warmup", "Temperature Control", "Set to 24°C", "Fri 09:00 AM", "Weekly", true);

        Device thermostat = roomService.getDeviceByName("Temperature Control");
        thermostat.setTemperature(20.0);

        assertEquals(0, scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 9, 9, 0)));
        assertEquals(20.0, thermostat.getTemperature(), 0.001);

        assertEquals(1, scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 10, 9, 0)));
        assertEquals(24.0, thermostat.getTemperature(), 0.001);
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
        scheduleService.addSchedule("Broken Schedule", "Does Not Exist", "Turn On", "06:00 AM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Broken Schedule").getId();

        int logsBefore = logService.getLogs().size();
        assertFalse(scheduleService.executeSchedule(schedId));
        assertEquals(logsBefore, logService.getLogs().size());
    }
}
