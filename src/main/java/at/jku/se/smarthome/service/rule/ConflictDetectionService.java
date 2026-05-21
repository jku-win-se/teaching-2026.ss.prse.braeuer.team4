package at.jku.se.smarthome.service.rule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import at.jku.se.smarthome.model.Schedule;
import at.jku.se.smarthome.model.SchedulingConflict;

/**
 * Service for detecting scheduling conflicts between rules and schedules.
 * A conflict exists when two enabled automations target the same device
 * with incompatible target values that overlap in time.
 */
public final class ConflictDetectionService {

    /** Time formatter for parsing time patterns. */
    private static final DateTimeFormatter TIME_FORMAT_24H = 
        DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Detects all conflicts between a candidate schedule and existing schedules.
     *
     * @param candidate the schedule being validated
     * @param existingSchedules all existing schedules in the system
     * @return list of detected conflicts (empty if none)
     */
    public List<SchedulingConflict> detectConflicts(Schedule candidate, List<Schedule> existingSchedules) {
        List<SchedulingConflict> conflicts = new ArrayList<>();

        for (Schedule existing : existingSchedules) {
            // Skip self-comparison
            if (candidate.getId().equals(existing.getId())) {
                continue;
            }

            // Skip if either schedule is disabled
            if (!candidate.isActive() || !existing.isActive()) {
                continue;
            }

            // Check if both target the same device
            if (!candidate.getDeviceId().equals(existing.getDeviceId())) {
                continue;
            }

            // Check if time windows overlap
            if (!timeWindowsOverlap(candidate, existing)) {
                continue;
            }

            // Check if target values are incompatible
            if (valuesAreIncompatible(candidate.getAction(), existing.getAction())) {
                SchedulingConflict conflict = createConflict(candidate, existing);
                conflicts.add(conflict);
            }
        }

        return conflicts;
    }

    /**
     * Checks whether two schedule time windows overlap.
     * Supports patterns like "Mon 20:00", "daily 14:30", "never".
     *
     * @param schedule1 first schedule
     * @param schedule2 second schedule
     * @return true if time windows overlap
     */
    private boolean timeWindowsOverlap(Schedule schedule1, Schedule schedule2) {
        // Parse recurrence patterns
        RecurrencePattern pattern1 = parseRecurrencePattern(schedule1.getRecurrence(), schedule1.getTime());
        RecurrencePattern pattern2 = parseRecurrencePattern(schedule2.getRecurrence(), schedule2.getTime());

        // Check if both are "never" — no overlap
        if (pattern1.isNever() || pattern2.isNever()) {
            return false;
        }

        // Daily patterns always overlap
        if (pattern1.isDaily() && pattern2.isDaily()) {
            return true;
        }

        // If one is daily and the other is weekly, check if the weekly day overlaps
        if (pattern1.isDaily() && pattern2.isWeekly()) {
            return true;
        }
        if (pattern1.isWeekly() && pattern2.isDaily()) {
            return true;
        }

        // Both weekly — check if they share a day
        if (pattern1.isWeekly() && pattern2.isWeekly()) {
            return pattern1.dayOfWeek() == pattern2.dayOfWeek();
        }
        
        // All cases covered above; unreachable but included for completeness
        return false;
    }

    /**
     * Checks whether two target values are incompatible for the same device type.
     * Incompatibility rules by device type:
     * - SwitchDevice: "ON" vs "OFF"
     * - DimmerDevice: brightness values that differ by more than 0
     * - ThermostatDevice: temperatures that differ by more than 0.5°C
     * - CoverDevice: "OPEN" vs "CLOSED"
     *
     * @param value1 first target value
     * @param value2 second target value
     * @return true if values are incompatible
     */
    private boolean valuesAreIncompatible(String value1, String value2) {
        // Normalize to uppercase for comparison
        String v1 = value1.trim().toUpperCase();
        String v2 = value2.trim().toUpperCase();

        // Same value is never incompatible
        if (v1.equals(v2)) {
            return false;
        }

        // SwitchDevice: ON vs OFF
        if (isSwitchValue(v1) && isSwitchValue(v2)) {
            return !v1.equals(v2);
        }

        // CoverDevice: OPEN vs CLOSED
        if (isCoverValue(v1) && isCoverValue(v2)) {
            return !v1.equals(v2);
        }

        // DimmerDevice: any different brightness values
        if (isDimmerValue(v1) && isDimmerValue(v2)) {
            return !v1.equals(v2);
        }

        // ThermostatDevice: temperatures differing by more than 0.5°C
        if (isThermostatValue(v1) && isThermostatValue(v2)) {
            try {
                double temp1 = extractTemperatureValue(v1);
                double temp2 = extractTemperatureValue(v2);
                return Math.abs(temp1 - temp2) > 0.5;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return false;
    }

    /**
     * Checks if a value represents a switch state.
     *
     * @param value the value to check
     * @return true if value is "ON" or "OFF"
     */
    private boolean isSwitchValue(String value) {
        return "ON".equals(value) || "OFF".equals(value);
    }

    /**
     * Checks if a value represents a cover position.
     *
     * @param value the value to check
     * @return true if value is "OPEN" or "CLOSED"
     */
    private boolean isCoverValue(String value) {
        return "OPEN".equals(value) || "CLOSED".equals(value);
    }

    /**
     * Checks if a value represents a dimmer brightness (percentage or 0-100).
     *
     * @param value the value to check
     * @return true if value looks like a brightness percentage
     */
    private boolean isDimmerValue(String value) {
        return value.matches("\\d+%?") || value.matches("0\\.\\d+");
    }

    /**
     * Checks if a value represents a thermostat temperature.
     *
     * @param value the value to check
     * @return true if value looks like a temperature
     */
    private boolean isThermostatValue(String value) {
        return value.matches("[0-9]+(\\.\\d+)?°?C?") || value.matches("[0-9]+(\\.\\d+)?\\s*°?C?");
    }

    /**
     * Extracts the numeric temperature value from a temperature string.
     *
     * @param value temperature string (e.g., "22.5°C", "22.5", "22")
     * @return numeric temperature value
     * @throws NumberFormatException if value is not a valid temperature
     */
    private double extractTemperatureValue(String value) {
        // Remove any non-numeric characters except decimal point
        String numeric = value.replaceAll("[^0-9.]", "");
        return Double.parseDouble(numeric);
    }

    /**
     * Creates a SchedulingConflict record for two conflicting schedules.
     *
     * @param candidate the candidate schedule
     * @param existing the existing conflicting schedule
     * @return conflict record
     */
    private SchedulingConflict createConflict(Schedule candidate, Schedule existing) {
        String description = String.format(
            "Conflict: '%s' and '%s' both target %s at the same time with conflicting values (%s vs %s)",
            candidate.getName(),
            existing.getName(),
            candidate.getDevice(),
            candidate.getAction(),
            existing.getAction()
        );

        return new SchedulingConflict(
            UUID.randomUUID().toString(),
            candidate.getId(),
            existing.getId(),
            candidate.getName(),
            existing.getName(),
            candidate.getDeviceId(),
            candidate.getDevice(),
            candidate.getAction(),
            existing.getAction(),
            candidate.getTime(),
            existing.getTime(),
            description
        );
    }

    /**
     * Parses a recurrence pattern string.
     *
     * @param recurrence recurrence mode (e.g., "daily", "Mon", "never")
     * @param timePattern the time pattern (e.g., "20:00", "Mon 20:00")
     * @return parsed recurrence pattern
     */
    private RecurrencePattern parseRecurrencePattern(String recurrence, String timePattern) {
        String rec = recurrence.trim().toLowerCase();

        if ("never".equals(rec)) {
            return new RecurrencePattern(true, false, false, null);
        }

        if ("daily".equals(rec)) {
            try {
                LocalTime time = LocalTime.parse(timePattern.trim(), TIME_FORMAT_24H);
                return new RecurrencePattern(false, true, false, null);
            } catch (Exception e) {
                return new RecurrencePattern(false, true, false, null);
            }
        }

        // Try to parse as day of week (e.g., "Mon", "Monday", "Monday 20:00")
        DayOfWeek day = tryParseDayOfWeek(rec);
        if (day != null) {
            return new RecurrencePattern(false, false, true, day);
        }

        // Try to extract day from the full time pattern
        day = tryParseDayOfWeek(timePattern);
        if (day != null) {
            return new RecurrencePattern(false, false, true, day);
        }

        // Default to never if we can't parse it
        return new RecurrencePattern(true, false, false, null);
    }

    /**
     * Attempts to parse a day of week from a string.
     *
     * @param input input string
     * @return DayOfWeek if found, null otherwise
     */
    private DayOfWeek tryParseDayOfWeek(String input) {
        String upper = input.trim().toUpperCase();
        
        try {
            return DayOfWeek.valueOf(upper);
        } catch (IllegalArgumentException e) {
            // Try short form
            String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
            String[] shortDays = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
            
            for (int i = 0; i < shortDays.length; i++) {
                if (upper.startsWith(shortDays[i])) {
                    return DayOfWeek.valueOf(days[i]);
                }
            }
            
            return null;
        }
    }

    /**
     * Internal record for parsed recurrence pattern.
     */
    private record RecurrencePattern(
        boolean never,
        boolean daily,
        boolean weekly,
        DayOfWeek dayOfWeek
    ) {
        boolean isNever() { return never; }
        boolean isDaily() { return daily; }
        boolean isWeekly() { return weekly; }
    }
}
