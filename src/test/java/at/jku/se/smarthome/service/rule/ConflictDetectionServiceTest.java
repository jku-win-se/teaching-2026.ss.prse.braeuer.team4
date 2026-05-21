package at.jku.se.smarthome.service.rule;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import at.jku.se.smarthome.model.Schedule;
import at.jku.se.smarthome.model.SchedulingConflict;

/**
 * Unit tests for ConflictDetectionService.
 * Tests all device-type conflict matrices, time-window overlaps, and edge cases.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.UnitTestContainsTooManyAsserts", "PMD.TooManyMethods", "PMD.ExcessivePublicCount"})
public final class ConflictDetectionServiceTest {

    /** The conflict detection service being tested. */
    private final ConflictDetectionService service = new ConflictDetectionService();

    /**
     * Helper to create a test schedule.
     */
    private Schedule createSchedule(String scheduleId, String name, String deviceId, 
                                    String action, String time, String recurrence, boolean active) {
        return new Schedule(scheduleId, name, deviceId, "Device-" + deviceId, action, time, recurrence, active);
    }

    // ==================== Device Type Conflict Tests ====================

    /**
     * Tests that a conflict is detected when one schedule turns a device ON and another turns it OFF at the same time.
     */
    @Test
    public void testSwitchDeviceConflictOnVsOff() {
        Schedule schedule1 = createSchedule("s1", "Turn Light On", "device-1", "ON", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Turn Light Off", "device-1", "OFF", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Should detect ON vs OFF conflict", 1, conflicts.size());
        assertEquals("ON", conflicts.get(0).getCandidateValue());
        assertEquals("OFF", conflicts.get(0).getConflictingValue());
    }

    /**
     * Tests that no conflict is detected when two schedules set the same value (ON) for a device.
     */
    @Test
    public void testSwitchDeviceNoConflictSameValue() {
        Schedule schedule1 = createSchedule("s1", "Turn Light On", "device-1", "ON", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Keep Light On", "device-1", "ON", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Same values should not conflict", 0, conflicts.size());
    }

    /**
     * Tests that a conflict is detected when one schedule opens blinds and another closes them at the same time.
     */
    @Test
    public void testCoverDeviceConflictOpenVsClosed() {
        Schedule schedule1 = createSchedule("s1", "Open Blinds", "device-2", "OPEN", "07:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Close Blinds", "device-2", "CLOSED", "07:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Should detect OPEN vs CLOSED conflict", 1, conflicts.size());
    }

    /**
     * Tests that a conflict is detected when two schedules set different brightness levels for a dimmer.
     */
    @Test
    public void testDimmerDeviceConflictDifferentBrightness() {
        Schedule schedule1 = createSchedule("s1", "Set to 50%", "device-3", "50%", "18:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Set to 75%", "device-3", "75%", "18:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Different brightness values should conflict", 1, conflicts.size());
    }

    /**
     * Tests that a conflict is detected when two schedules set temperatures with a difference greater than 0.5°C.
     */
    @Test
    public void testThermostatDeviceConflictTemperatureDifference() {
        Schedule schedule1 = createSchedule("s1", "Set to 22°C", "device-4", "22°C", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Set to 22.6°C", "device-4", "22.6°C", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Temperature difference > 0.5°C should conflict", 1, conflicts.size());
    }

    /**
     * Tests that no conflict is detected when two schedules set temperatures with a difference of 0.5°C or less.
     */
    @Test
    public void testThermostatDeviceNoConflictSmallDifference() {
        Schedule schedule1 = createSchedule("s1", "Set to 22°C", "device-4", "22.0°C", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Set to 22.3°C", "device-4", "22.3°C", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Temperature difference ≤ 0.5°C should not conflict", 0, conflicts.size());
    }

    // ==================== Time Window Overlap Tests ====================

    /**
     * Tests that daily schedules are always considered to overlap in time.
     */
    @Test
    public void testDailySchedulesAlwaysOverlap() {
        Schedule schedule1 = createSchedule("s1", "Morning Routine", "device-1", "ON", "07:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Evening Routine", "device-1", "OFF", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Daily schedules overlap", 1, conflicts.size());
    }

    /**
     * Tests that schedules on different weekdays do not conflict.
     */
    @Test
    public void testDisjointWeekdaysNoConflict() {
        Schedule schedule1 = createSchedule("s1", "Monday Activity", "device-1", "ON", "Mon 20:00", "weekly", true);
        Schedule schedule2 = createSchedule("s2", "Tuesday Activity", "device-1", "OFF", "Tue 20:00", "weekly", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Different weekdays should not overlap", 0, conflicts.size());
    }

    /**
     * Tests that schedules on the same weekday are considered to overlap in time.
     */
    @Test
    public void testSameWeekdayConflict() {
        Schedule schedule1 = createSchedule("s1", "Monday Morning", "device-1", "ON", "Mon 07:00", "weekly", true);
        Schedule schedule2 = createSchedule("s2", "Monday Evening", "device-1", "OFF", "Mon 20:00", "weekly", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Same weekday should conflict", 1, conflicts.size());
    }

    /**
     * Tests that a daily schedule overlaps with a weekly schedule.
     */
    @Test
    public void testDailyAndWeeklyOverlap() {
        Schedule schedule1 = createSchedule("s1", "Every Day", "device-1", "ON", "08:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Monday Special", "device-1", "OFF", "Mon 08:00", "weekly", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Daily and weekly should overlap", 1, conflicts.size());
    }

    /**
     * Tests that a schedule with 'never' recurrence does not conflict with any other schedule.
     */
    @Test
    public void testNeverRecurrenceNoConflict() {
        Schedule schedule1 = createSchedule("s1", "Disabled", "device-1", "ON", "20:00", "never", true);
        Schedule schedule2 = createSchedule("s2", "Active", "device-1", "OFF", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("'Never' recurrence should not conflict", 0, conflicts.size());
    }

    // ==================== Disabled Schedules Test ====================

    /**
     * Tests that a disabled candidate schedule does not report any conflicts.
     */
    @Test
    public void testDisabledCandidateNoConflict() {
        Schedule schedule1 = createSchedule("s1", "Disabled Light", "device-1", "ON", "20:00", "daily", false);
        Schedule schedule2 = createSchedule("s2", "Active Light", "device-1", "OFF", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Disabled candidate should not report conflicts", 0, conflicts.size());
    }

    /**
     * Tests that a candidate schedule does not conflict with a disabled existing schedule.
     */
    @Test
    public void testDisabledExistingNoConflict() {
        Schedule schedule1 = createSchedule("s1", "Active Light", "device-1", "ON", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Disabled Light", "device-1", "OFF", "20:00", "daily", false);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Disabled existing should not conflict", 0, conflicts.size());
    }

    // ==================== Different Devices Test ====================

    /**
     * Tests that schedules for different devices do not conflict.
     */
    @Test
    public void testDifferentDevicesNoConflict() {
        Schedule schedule1 = createSchedule("s1", "Turn On Light 1", "device-1", "ON", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Turn Off Light 2", "device-2", "OFF", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Different devices should not conflict", 0, conflicts.size());
    }

    // ==================== Multiple Conflicts Test ====================

    /**
     * Tests that multiple conflicts are detected when a candidate schedule conflicts with multiple existing schedules.
     */
    @Test
    public void testMultipleConflicts() {
        Schedule candidate = createSchedule("s1", "Turn Light On", "device-1", "ON", "20:00", "daily", true);
        Schedule conflict1 = createSchedule("s2", "Turn Light Off", "device-1", "OFF", "20:00", "daily", true);
        Schedule conflict2 = createSchedule("s3", "Dim Light to 50%", "device-1", "OFF", "20:00", "daily", true);
        Schedule noConflict = createSchedule("s4", "Control Device 2", "device-2", "ON", "20:00", "daily", true);

        List<Schedule> existing = List.of(conflict1, conflict2, noConflict);
        List<SchedulingConflict> conflicts = service.detectConflicts(candidate, existing);
        
        assertEquals("Should detect both conflicts", 2, conflicts.size());
    }

    // ==================== Edge Cases ====================

    /**
     * Tests that comparing a schedule to itself does not result in a conflict.
     */
    @Test
    public void testSelfComparisonIgnored() {
        Schedule schedule = createSchedule("s1", "Light Control", "device-1", "ON", "20:00", "daily", true);
        
        List<SchedulingConflict> conflicts = service.detectConflicts(schedule, List.of(schedule));
        
        assertEquals("Self-comparison should be ignored", 0, conflicts.size());
    }

    /**
     * Tests that conflict detection is case-insensitive for action values.
     */
    @Test
    public void testCaseInsensitivityOnVsOn() {
        Schedule schedule1 = createSchedule("s1", "Turn Light On", "device-1", "ON", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Turn Light Off", "device-1", "off", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Should handle case variations", 1, conflicts.size());
    }

    /**
     * Tests that no conflicts are reported when there are no existing schedules.
     */
    @Test
    public void testEmptyExistingSchedules() {
        Schedule candidate = createSchedule("s1", "Turn Light On", "device-1", "ON", "20:00", "daily", true);
        
        List<SchedulingConflict> conflicts = service.detectConflicts(candidate, new ArrayList<>());
        
        assertEquals("No existing schedules means no conflicts", 0, conflicts.size());
    }

    // ==================== Additional Coverage Tests ====================

    /**
     * Tests that thermostat schedules with exactly 0.5°C difference do not conflict.
     */
    @Test
    public void testThermostatVariousFormats225Vs22() {
        Schedule schedule1 = createSchedule("s1", "Set to 22.5°C", "device-4", "22.5°C", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Set to 22", "device-4", "22", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        // Exactly 0.5°C difference should NOT conflict (threshold is > 0.5)
        assertEquals("Exactly 0.5°C difference should not conflict", 0, conflicts.size());
    }

    /**
     * Tests that thermostat schedules with plain numeric values are correctly compared.
     */
    @Test
    public void testThermostatPlainNumbers21Vs22() {
        Schedule schedule1 = createSchedule("s1", "Set to 21", "device-4", "21", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Set to 22", "device-4", "22", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Should detect temperature difference", 1, conflicts.size());
    }

    /**
     * Tests that dimmer schedules with different percentage values conflict.
     */
    @Test
    public void testDimmerPercentageVariations() {
        Schedule schedule1 = createSchedule("s1", "Set to 0%", "device-3", "0%", "18:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Set to 100%", "device-3", "100%", "18:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Different brightness values should conflict", 1, conflicts.size());
    }

    /**
     * Tests that dimmer schedules without percentage signs are correctly compared.
     */
    @Test
    public void testDimmerNoPercentageSign() {
        Schedule schedule1 = createSchedule("s1", "Set to 50", "device-3", "50", "18:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Set to 75", "device-3", "75", "18:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Different brightness values (no %) should conflict", 1, conflicts.size());
    }

    /**
     * Tests that weekly schedules with full day names are correctly parsed and compared.
     */
    @Test
    public void testWeeklyDayParsingFullName() {
        Schedule schedule1 = createSchedule("s1", "Monday Activity", "device-1", "ON", "Monday 20:00", "weekly", true);
        Schedule schedule2 = createSchedule("s2", "Monday Activity", "device-1", "OFF", "Monday 20:00", "weekly", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Full day name should be parsed correctly", 1, conflicts.size());
    }

    /**
     * Tests that weekly schedules with short day names are correctly parsed and compared.
     */
    @Test
    public void testWeeklyDayParsingShortForm() {
        Schedule schedule1 = createSchedule("s1", "Tuesday Activity", "device-1", "ON", "Tue 20:00", "weekly", true);
        Schedule schedule2 = createSchedule("s2", "Tuesday Activity", "device-1", "OFF", "Tue 20:00", "weekly", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Short day name should be parsed correctly", 1, conflicts.size());
    }

    /**
     * Tests that weekly schedules without time are correctly parsed and compared.
     */
    @Test
    public void testWeeklyOnlyNoTime() {
        Schedule schedule1 = createSchedule("s1", "Wednesday Activity", "device-1", "ON", "Wednesday", "weekly", true);
        Schedule schedule2 = createSchedule("s2", "Wednesday Activity", "device-1", "OFF", "Wednesday", "weekly", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Day without time should parse correctly", 1, conflicts.size());
    }

    /**
     * Tests that weekly schedules on different days do not conflict.
     */
    @Test
    public void testWeeklyDifferentDaysNoConflict2() {
        Schedule schedule1 = createSchedule("s1", "Friday Activity", "device-1", "ON", "Friday 20:00", "weekly", true);
        Schedule schedule2 = createSchedule("s2", "Saturday Activity", "device-1", "OFF", "Saturday 20:00", "weekly", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Different days should not conflict", 0, conflicts.size());
    }

    /**
     * Tests that cover values are compared case-insensitively.
     */
    @Test
    public void testMixedCoverValues() {
        Schedule schedule1 = createSchedule("s1", "Open Blinds", "device-2", "open", "07:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Close Blinds", "device-2", "CLOSED", "07:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Case-insensitive cover values should conflict", 1, conflicts.size());
    }

    /**
     * Tests that action values with leading/trailing whitespace are correctly compared.
     */
    @Test
    public void testWhitespaceHandling() {
        Schedule schedule1 = createSchedule("s1", "Turn Light On", "device-1", "  ON  ", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Turn Light Off", "device-1", "OFF", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Whitespace should be trimmed", 1, conflicts.size());
    }

    /**
     * Tests that exactly 0.5°C temperature difference is not a conflict.
     */
    @Test
    public void testThermostatExactBoundary05Difference() {
        Schedule schedule1 = createSchedule("s1", "Set to 20°C", "device-4", "20°C", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Set to 20.5°C", "device-4", "20.5°C", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        // Boundary is > 0.5, so exactly 0.5 should NOT conflict
        assertEquals("Exactly 0.5°C difference should not conflict", 0, conflicts.size());
    }

    /**
     * Tests that more than 0.5°C temperature difference results in a conflict.
     */
    @Test
    public void testThermostatBoundary051Difference() {
        Schedule schedule1 = createSchedule("s1", "Set to 20°C", "device-4", "20°C", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Set to 20.51°C", "device-4", "20.51°C", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("More than 0.5°C difference should conflict", 1, conflicts.size());
    }

    /**
     * Tests that invalid temperature formats do not result in conflicts.
     */
    @Test
    public void testInvalidTemperatureFormatNoConflict() {
        Schedule schedule1 = createSchedule("s1", "Set to ABC", "device-4", "ABC", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Set to XYZ", "device-4", "XYZ", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Invalid temperature strings should not conflict", 0, conflicts.size());
    }

    /**
     * Tests that weekly schedules overlap with daily schedules regardless of order.
     */
    @Test
    public void testDailyWeeklyReverseOverlap() {
        Schedule schedule1 = createSchedule("s1", "Monday Only", "device-1", "ON", "Mon 10:00", "weekly", true);
        Schedule schedule2 = createSchedule("s2", "Every Day", "device-1", "OFF", "10:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Weekly then daily should overlap", 1, conflicts.size());
    }

    /**
     * Tests that Sunday is correctly parsed when using its full name.
     */
    @Test
    public void testSundayFullName() {
        Schedule schedule1 = createSchedule("s1", "Sunday Activity 1", "device-1", "ON", "Sunday 20:00", "weekly", true);
        Schedule schedule2 = createSchedule("s2", "Sunday Activity 2", "device-1", "OFF", "Sunday 20:00", "weekly", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Sunday should be parsed correctly", 1, conflicts.size());
    }

    /**
     * Tests that Saturday is correctly parsed when using its short form.
     */
    @Test
    public void testSaturdayShortForm() {
        Schedule schedule1 = createSchedule("s1", "Saturday Activity 1", "device-1", "ON", "Sat 20:00", "weekly", true);
        Schedule schedule2 = createSchedule("s2", "Saturday Activity 2", "device-1", "OFF", "Sat 20:00", "weekly", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Saturday should be parsed correctly", 1, conflicts.size());
    }

    /**
     * Tests that same cover values do not conflict.
     */
    @Test
    public void testCoverNoConflictSameValue() {
        Schedule schedule1 = createSchedule("s1", "Keep Blinds Open 1", "device-2", "OPEN", "07:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Keep Blinds Open 2", "device-2", "OPEN", "07:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Same cover values should not conflict", 0, conflicts.size());
    }

    /**
     * Tests that same dimmer values do not conflict.
     */
    @Test
    public void testDimmerSameValue() {
        Schedule schedule1 = createSchedule("s1", "Set Brightness 50", "device-3", "50%", "18:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Keep Brightness 50", "device-3", "50%", "18:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Same dimmer values should not conflict", 0, conflicts.size());
    }

    // ==================== Mixed Device Type Tests ====================

    /**
     * Tests that actions for different device types (switch vs cover) do not conflict.
     */
    @Test
    public void testMixedTypesSwitchVsCover() {
        Schedule schedule1 = createSchedule("s1", "Turn Light On", "device-1", "ON", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Open Blinds", "device-1", "OPEN", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Switch and cover values should not conflict", 0, conflicts.size());
    }

    /**
     * Tests that actions for different device types (switch vs dimmer) do not conflict.
     */
    @Test
    public void testMixedTypesSwitchVsDimmer() {
        Schedule schedule1 = createSchedule("s1", "Turn Light On", "device-1", "ON", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Set Brightness", "device-1", "50%", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Switch and dimmer values should not conflict", 0, conflicts.size());
    }

    /**
     * Tests that actions for different device types (switch vs thermostat) do not conflict.
     */
    @Test
    public void testMixedTypesSwitchVsThermostat() {
        Schedule schedule1 = createSchedule("s1", "Turn Light On", "device-1", "ON", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Set Temperature", "device-1", "22°C", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Switch and thermostat values should not conflict", 0, conflicts.size());
    }

    /**
     * Tests that actions for different device types (cover vs dimmer) do not conflict.
     */
    @Test
    public void testMixedTypesCoverVsDimmer() {
        Schedule schedule1 = createSchedule("s1", "Open Blinds", "device-2", "OPEN", "07:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Set Brightness", "device-2", "50%", "07:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Cover and dimmer values should not conflict", 0, conflicts.size());
    }

    /**
     * Tests that invalid temperature formats do not result in conflicts even if they look like thermostat values.
     */
    @Test
    public void testInvalidTemperatureFormatIncompatibleValues() {
        // Test the catch block for NumberFormatException
        Schedule schedule1 = createSchedule("s1", "Invalid Temp 1", "device-4", "ABC°C", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Invalid Temp 2", "device-4", "XYZ°C", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        // Both are recognized as thermostat values (they match the pattern) but parsing fails,
        // so it returns false (no conflict)
        assertEquals("Invalid temperature strings should not conflict", 0, conflicts.size());
    }

    /**
     * Tests that dimmer values in decimal format are correctly compared.
     */
    @Test
    public void testDimmerDecimalFormat() {
        Schedule schedule1 = createSchedule("s1", "Set to 0.5", "device-3", "0.5", "18:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Set to 0.75", "device-3", "0.75", "18:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Different decimal brightness values should conflict", 1, conflicts.size());
    }

    /**
     * Tests that thermostat values with extra spaces are correctly compared.
     */
    @Test
    public void testThermostatWithSpaces() {
        Schedule schedule1 = createSchedule("s1", "Set temp with spaces", "device-4", "22 °C", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Set temp normal", "device-4", "22.6°C", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Temperature strings with spaces should be compared", 1, conflicts.size());
    }

    /**
     * Tests that weekly and 'never' schedules do not conflict.
     */
    @Test
    public void testWeeklyNotDailyNoConflictCase() {
        Schedule schedule1 = createSchedule("s1", "Monday Activity", "device-1", "ON", "20:00", "weekly", true);
        Schedule schedule2 = createSchedule("s2", "Everyday Activity", "device-1", "OFF", "20:00", "never", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Weekly and never should not conflict", 0, conflicts.size());
    }

    /**
     * Tests that unknown recurrence patterns do not result in conflicts.
     */
    @Test
    public void testUnparseableRecurrence() {
        Schedule schedule1 = createSchedule("s1", "Unknown pattern", "device-1", "ON", "20:00", "unknown", true);
        Schedule schedule2 = createSchedule("s2", "Normal schedule", "device-1", "OFF", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        // Unknown recurrence should default to "never", so no conflict
        assertEquals("Unknown recurrence should not conflict", 0, conflicts.size());
    }

    /**
     * Tests that daily schedules overlap even if the time string format is invalid.
     */
    @Test
    public void testDailyWithInvalidTimePattern() {
        // This should trigger the catch block in parseRecurrencePattern for "daily" 
        Schedule schedule1 = createSchedule("s1", "Daily invalid", "device-1", "ON", "INVALID", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Normal schedule", "device-1", "OFF", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        // Both should be treated as daily, so they conflict
        assertEquals("Daily schedules (despite invalid time) should overlap", 1, conflicts.size());
    }

    /**
     * Tests that when the recurrence field itself contains a day abbreviation, it is correctly parsed.
     */
    @Test
    public void testRecurrenceAsDirectDayOfWeek() {
        // When recurrence field is directly a day of week abbreviation
        Schedule schedule1 = createSchedule("s1", "Mon activity", "device-1", "ON", "20:00", "Mon", true);
        Schedule schedule2 = createSchedule("s2", "Mon activity", "device-1", "OFF", "20:00", "Mon", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Same weekday in recurrence field should conflict", 1, conflicts.size());
    }

    /**
     * Tests that when the recurrence field itself contains a full day name, it is correctly parsed.
     */
    @Test
    public void testRecurrenceAsFullDayName() {
        // When recurrence field is a full day name
        Schedule schedule1 = createSchedule("s1", "Tuesday activity", "device-1", "ON", "18:00", "Tuesday", true);
        Schedule schedule2 = createSchedule("s2", "Tuesday activity", "device-1", "OFF", "18:00", "Tuesday", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Full day name in recurrence field should conflict", 1, conflicts.size());
    }

    /**
     * Tests that different temperature string formats are correctly compared.
     */
    @Test
    public void testTemperatureParsingWithBothFormats() {
        // Test both temperature regex patterns
        Schedule schedule1 = createSchedule("s1", "Temp format 1", "device-4", "25.0°C", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Temp format 2", "device-4", "20.5 °C", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        assertEquals("Different temperature formats should conflict", 1, conflicts.size());
    }

    /**
     * Tests that unparseable temperature strings (but matching the regex) do not result in conflicts.
     */
    @Test
    public void testNumberFormatExceptionInTemperatureExtraction() {
        // Both values match thermostat pattern but have content that can't be parsed as double
        // After stripping non-numeric chars, "ABC" becomes empty string which fails Double.parseDouble
        Schedule schedule1 = createSchedule("s1", "Bad temp 1", "device-4", "ABC°C", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Bad temp 2", "device-4", "XYZ°C", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        // Both match thermostat pattern but parsing fails, so no conflict
        assertEquals("Unparseable temperatures should return no conflict", 0, conflicts.size());
    }

    /**
     * Tests conflict detection for every day of the week.
     */
    @Test
    public void testAllDayVariations() {
        // Test each day of the week
        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
        for (String day : days) {
            Schedule schedule1 = createSchedule("s1", day + " 1", "device-1", "ON", "20:00", day.toLowerCase(), true);
            Schedule schedule2 = createSchedule("s2", day + " 2", "device-1", "OFF", "20:00", day.toLowerCase(), true);
            
            List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
            assertEquals(day + " schedules should conflict", 1, conflicts.size());
        }
    }

    /**
     * Tests that decimal and percent brightness values are correctly compared.
     */
    @Test
    public void testDimmerDecimalOnly() {
        // Test dimmer value that ONLY matches the second regex "0\.d+"
        Schedule schedule1 = createSchedule("s1", "Decimal dimmer", "device-3", "0.5", "18:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Percent dimmer", "device-3", "50%", "18:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        // Both should be recognized as dimmer values (different device types), so conflict
        assertEquals("Decimal and percent brightness should conflict", 1, conflicts.size());
    }

    /**
     * Tests that values not matching any device type do not result in conflicts.
     */
    @Test
    public void testNonDeviceTypeValue() {
        // Test values that don't match any device type
        Schedule schedule1 = createSchedule("s1", "Generic 1", "device-1", "value1", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Generic 2", "device-1", "value2", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        // Values don't match any device pattern, so no conflict
        assertEquals("Non-matching device values should not conflict", 0, conflicts.size());
    }

    /**
     * Tests that invalid values do not result in conflicts.
     */
    @Test
    public void testThermostatWithInvalidFormat() {
        // Test thermostat with invalid format that throws NumberFormatException
        Schedule schedule1 = createSchedule("s1", "Invalid temp 1", "device-1", "abc", "20:00", "daily", true);
        Schedule schedule2 = createSchedule("s2", "Invalid temp 2", "device-1", "def", "20:00", "daily", true);

        List<SchedulingConflict> conflicts = service.detectConflicts(schedule1, List.of(schedule2));
        
        // Both invalid, so no conflict (they don't match device patterns)
        assertEquals("Invalid values should result in no conflict", 0, conflicts.size());
    }
}
