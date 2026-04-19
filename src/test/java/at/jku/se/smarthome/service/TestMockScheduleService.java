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
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods"})
public class TestMockScheduleService {


    /** Schedule service under test. */
    private MockScheduleService scheduleService;
    /** Room service for device management. */
    private MockRoomService roomService;
    /** Log service for activity verification. */
    private MockLogService logService;

    /**
     * Sets up test fixtures before each test.
     */
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

    /**
     * Test: execute schedule with unknown ID returns false.
     */
    @Test
    public void executeScheduleUnknownIdReturnsFalse() {
        assertFalse(scheduleService.executeSchedule("does-not-exist"));
    }

    /**
     * Test: execute inactive schedule returns false.
     */
    @Test
    public void executeScheduleInactiveScheduleReturnsFalse() {
        // sched-003 "Weekend Relax" is inactive in seed data
        assertFalse(scheduleService.executeSchedule("sched-003"));
    }

    /**
     * Test: execute schedule with device not found returns false.
     */
    @Test
    public void executeScheduleDeviceNotFoundReturnsFalse() {
        scheduleService.addSchedule("Missing Device", "Does Not Exist", "Turn On", "07:00 AM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Missing Device").getId();

        assertFalse(scheduleService.executeSchedule(schedId));
    }

    // -----------------------------------------------------------------------
    // Turn On
    // -----------------------------------------------------------------------

    /**
     * Test: turn on action execution returns true.
     */
    @Test
    public void executeScheduleTurnOnReturnsTrue() {
        scheduleService.addSchedule("Night On", "Bed Light", "Turn On", "07:00 AM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Night On").getId();
        assertTrue(scheduleService.executeSchedule(schedId));
    }

    /**
     * Test: turn on action sets device state to true.
     */
    @Test
    public void executeScheduleTurnOnSetDeviceStateTrue() {
        // Bed Light starts OFF — schedule turns it ON
        scheduleService.addSchedule("Night On", "Bed Light", "Turn On", "07:00 AM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Night On").getId();

        Device bed = roomService.getDeviceByName("Bed Light");
        scheduleService.executeSchedule(schedId);
        assertTrue(bed.getState());
    }

    // -----------------------------------------------------------------------
    // Turn Off
    // -----------------------------------------------------------------------

    /**
     * Test: turn off action execution returns true.
     */
    @Test
    public void executeScheduleTurnOffReturnsTrue() {
        scheduleService.addSchedule("Night Off", "Main Light", "Turn Off", "11:00 PM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Night Off").getId();
        assertTrue(scheduleService.executeSchedule(schedId));
    }

    /**
     * Test: turn off action sets device state to false.
     */
    @Test
    public void executeScheduleTurnOffSetDeviceStateFalse() {
        // Main Light starts ON — schedule turns it OFF
        scheduleService.addSchedule("Night Off", "Main Light", "Turn Off", "11:00 PM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Night Off").getId();

        Device main = roomService.getDeviceByName("Main Light");
        scheduleService.executeSchedule(schedId);
        assertFalse(main.getState());
    }

    // -----------------------------------------------------------------------
    // Set brightness
    // -----------------------------------------------------------------------

    /**
     * Test: set brightness action execution returns true.
     */
    @Test
    public void executeScheduleSetBrightnessReturnsTrue() {
        scheduleService.addSchedule("Dim Scene", "Dimmer Light", "Set to 40%", "18:00", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Dim Scene").getId();
        assertTrue(scheduleService.executeSchedule(schedId));
    }

    /**
     * Test: set brightness action sets dimmer to specified level.
     */
    @Test
    public void executeScheduleSetBrightnessSetsDimmerLevel() {
        scheduleService.addSchedule("Dim Scene", "Dimmer Light", "Set to 40%", "18:00", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Dim Scene").getId();

        scheduleService.executeSchedule(schedId);
        Device dimmer = roomService.getDeviceByName("Dimmer Light");
        assertEquals(40, dimmer.getBrightness());
    }

    /**
     * Test: set temperature action execution returns true.
     */
    @Test
    public void executeScheduleSetTemperatureReturnsTrue() {
        scheduleService.addSchedule("Morning Warmup", "Temperature Control", "Set to 22°C", "06:30 AM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Morning Warmup").getId();
        assertTrue(scheduleService.executeSchedule(schedId));
    }

    /**
     * Test: set temperature action sets thermostat to specified temperature.
     */
    @Test
    public void executeScheduleSetTemperatureSetsThermostatTemperature() {
        scheduleService.addSchedule("Morning Warmup", "Temperature Control", "Set to 22°C", "06:30 AM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Morning Warmup").getId();

        scheduleService.executeSchedule(schedId);
        Device thermostat = roomService.getDeviceByName("Temperature Control");
        assertEquals(22.0, thermostat.getTemperature(), 0.001);
    }

    // -----------------------------------------------------------------------
    // Recurring execution (FR-09)
    // -----------------------------------------------------------------------

    /**
     * Test: daily schedule matching execution time executes and returns count.
     */
    @Test
    public void processDueSchedulesMatchingDailyTimeReturnsOneWhenMatching() {
        scheduleService.addSchedule("Wake Up", "Bed Light", "Turn On", "07:00 AM", "Daily", true);
        assertEquals(1, scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 10, 7, 0)));
    }

    /**
     * Test: daily schedule matching execution time updates device state.
     */
    @Test
    public void processDueSchedulesMatchingDailyTimeUpdatesDeviceState() {
        scheduleService.addSchedule("Wake Up", "Bed Light", "Turn On", "07:00 AM", "Daily", true);

        Device bed = roomService.getDeviceByName("Bed Light");
        scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 10, 7, 0));
        assertTrue(bed.getState());
    }

    /**
     * Test: daily schedule matching execution time logs entry.
     */
    @Test
    public void processDueSchedulesMatchingDailyTimeLogsEntry() {
        scheduleService.addSchedule("Wake Up", "Bed Light", "Turn On", "07:00 AM", "Daily", true);

        int logsBefore = logService.getLogs().size();
        scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 10, 7, 0));
        assertEquals(logsBefore + 1, logService.getLogs().size());
    }

    /**
     * Test: daily schedule executes only once per minute - no duplicate execution.
     */
    @Test
    public void processDueSchedulesNoDuplicateExecutionSameMinute() {
        scheduleService.addSchedule("Wake Up", "Bed Light", "Turn On", "07:00 AM", "Daily", true);

        scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 10, 7, 0));
        assertEquals(0, scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 10, 7, 0, 45)));
    }

    /**
     * Test: weekdays schedule skips weekend - returns zero.
     */
    @Test
    public void processDueSchedulesWeekdaysReturnsZeroOnWeekend() {
        scheduleService.addSchedule("Weekday Entry", "Main Light", "Turn Off", "08:15", "Weekdays", true);
        assertEquals(0, scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 11, 8, 15)));
    }

    /**
     * Test: weekdays schedule preserves device state on weekend.
     */
    @Test
    public void processDueSchedulesWeekdaysPreservesStateOnWeekend() {
        scheduleService.addSchedule("Weekday Entry", "Main Light", "Turn Off", "08:15", "Weekdays", true);

        Device main = roomService.getDeviceByName("Main Light");
        main.setState(true);

        scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 11, 8, 15));
        assertTrue(main.getState());
    }

    /**
     * Test: weekdays schedule executes on weekdays - returns one.
     */
    @Test
    public void processDueSchedulesWeekdaysReturnsOneOnWeekday() {
        scheduleService.addSchedule("Weekday Entry", "Main Light", "Turn Off", "08:15", "Weekdays", true);
        assertEquals(1, scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 10, 8, 15)));
    }

    /**
     * Test: weekdays schedule turns off device on weekdays.
     */
    @Test
    public void processDueSchedulesWeekdaysSwitchesDeviceOnWeekday() {
        scheduleService.addSchedule("Weekday Entry", "Main Light", "Turn Off", "08:15", "Weekdays", true);

        Device main = roomService.getDeviceByName("Main Light");

        scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 10, 8, 15));
        assertFalse(main.getState());
    }

    /**
     * Test: weekly schedule skips non-configured days - returns zero.
     */
    @Test
    public void processDueSchedulesWeeklyReturnsZeroOnNonConfiguredDay() {
        scheduleService.addSchedule("Weekly Warmup", "Temperature Control", "Set to 24°C", "Fri 09:00 AM", "Weekly", true);
        assertEquals(0, scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 9, 9, 0)));
    }

    /**
     * Test: weekly schedule preserves temperature on non-configured day.
     */
    @Test
    public void processDueSchedulesWeeklyPreservesTemperatureOnNonConfiguredDay() {
        scheduleService.addSchedule("Weekly Warmup", "Temperature Control", "Set to 24°C", "Fri 09:00 AM", "Weekly", true);

        Device thermostat = roomService.getDeviceByName("Temperature Control");
        thermostat.setTemperature(20.0);

        scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 9, 9, 0));
        assertEquals(20.0, thermostat.getTemperature(), 0.001);
    }

    /**
     * Test: weekly schedule executes on configured day - returns one.
     */
    @Test
    public void processDueSchedulesWeeklyReturnsOneOnConfiguredDay() {
        scheduleService.addSchedule("Weekly Warmup", "Temperature Control", "Set to 24°C", "Fri 09:00 AM", "Weekly", true);
        assertEquals(1, scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 10, 9, 0)));
    }

    /**
     * Test: weekly schedule sets temperature on configured day.
     */
    @Test
    public void processDueSchedulesWeeklySetsTempOnConfiguredDay() {
        scheduleService.addSchedule("Weekly Warmup", "Temperature Control", "Set to 24°C", "Fri 09:00 AM", "Weekly", true);

        Device thermostat = roomService.getDeviceByName("Temperature Control");

        scheduleService.processDueSchedules(LocalDateTime.of(2026, 4, 10, 9, 0));
        assertEquals(24.0, thermostat.getTemperature(), 0.001);
    }

    // -----------------------------------------------------------------------
    // Logging (FR-08 core requirement)
    // -----------------------------------------------------------------------

    /**
     * Test: schedule execution returns true.
     */
    @Test
    public void executeScheduleReturnsTrue() {
        scheduleService.addSchedule("Log Test", "Main Light", "Turn On", "06:00 AM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Log Test").getId();
        assertTrue(scheduleService.executeSchedule(schedId));
    }

    /**
     * Test: schedule execution creates log entry.
     */
    @Test
    public void executeScheduleCreatesLogEntry() {
        scheduleService.addSchedule("Log Test", "Main Light", "Turn On", "06:00 AM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Log Test").getId();

        int logsBefore = logService.getLogs().size();
        scheduleService.executeSchedule(schedId);
        assertEquals(logsBefore + 1, logService.getLogs().size());
    }

    /**
     * Test: schedule execution logs device name.
     */
    @Test
    public void executeScheduleLogsDeviceName() {
        scheduleService.addSchedule("Log Test", "Main Light", "Turn On", "06:00 AM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Log Test").getId();

        scheduleService.executeSchedule(schedId);
        LogEntry entry = logService.getLogs().get(0); // newest first
        assertEquals("Main Light", entry.getDevice());
    }

    /**
     * Test: schedule execution logs action.
     */
    @Test
    public void executeScheduleLogsAction() {
        scheduleService.addSchedule("Log Test", "Main Light", "Turn On", "06:00 AM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Log Test").getId();

        scheduleService.executeSchedule(schedId);
        LogEntry entry = logService.getLogs().get(0); // newest first
        assertEquals("Turn On", entry.getAction());
    }

    /**
     * Test: schedule execution logs schedule actor.
     */
    @Test
    public void executeScheduleLogsScheduleActor() {
        scheduleService.addSchedule("Log Test", "Main Light", "Turn On", "06:00 AM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Log Test").getId();

        scheduleService.executeSchedule(schedId);
        LogEntry entry = logService.getLogs().get(0); // newest first
        assertEquals("Schedule: Log Test", entry.getActor());
    }

    /**
     * Test: schedule execution logs entry with correct room.
     */
    @Test
    public void executeScheduleLogsEntryWithCorrectRoom() {
        scheduleService.addSchedule("Room Test", "Main Light", "Turn Off", "11:00 PM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Room Test").getId();

        scheduleService.executeSchedule(schedId);
        LogEntry entry = logService.getLogs().get(0);
        assertEquals("Living Room", entry.getRoom());
    }

    /**
     * Test: failed schedule execution returns false.
     */
    @Test
    public void executeScheduleFailedExecutionReturnsFalse() {
        // Device does not exist → executeSchedule returns false
        scheduleService.addSchedule("Broken Schedule", "Does Not Exist", "Turn On", "06:00 AM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Broken Schedule").getId();
        assertFalse(scheduleService.executeSchedule(schedId));
    }

    /**
     * Test: failed schedule execution does not log entry.
     */
    @Test
    public void executeScheduleFailedExecutionDoesNotLog() {
        // Device does not exist → executeSchedule returns false and must not log
        scheduleService.addSchedule("Broken Schedule", "Does Not Exist", "Turn On", "06:00 AM", "Daily", true);
        String schedId = scheduleService.getScheduleByName("Broken Schedule").getId();

        int logsBefore = logService.getLogs().size();
        scheduleService.executeSchedule(schedId);
        assertEquals(logsBefore, logService.getLogs().size());
    }
}
