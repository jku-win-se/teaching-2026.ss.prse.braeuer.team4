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
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.AtLeastOneConstructor"})
public final class ConflictDetectionService {

    /** Literal for 'never' recurrence pattern. */
    private static final String RECURRENCE_NEVER = "never";
    /** Literal for 'daily' recurrence pattern. */
    private static final String RECURRENCE_DAILY = "daily";

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
        boolean overlap = false;
        // Parse recurrence patterns
        RecurrencePattern pattern1 = parseRecurrencePattern(schedule1.getRecurrence(), schedule1.getTime());
        RecurrencePattern pattern2 = parseRecurrencePattern(schedule2.getRecurrence(), schedule2.getTime());

        // Check if both are "never" — no overlap
        if (!pattern1.never() && !pattern2.never()) {
            // Daily patterns always overlap
            if (pattern1.daily() && pattern2.daily()) {
                overlap = true;
            } else if (pattern1.daily() && pattern2.weekly()) {
                // If one is daily and the other is weekly, check if the weekly day overlaps
                overlap = true;
            } else if (pattern1.weekly() && pattern2.daily()) {
                overlap = true;
            } else if (pattern1.weekly() && pattern2.weekly()) {
                // Both weekly — check if they share a day
                overlap = pattern1.dayOfWeek() == pattern2.dayOfWeek();
            }
        }
        
        return overlap;
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
        boolean incompatible = false;

        // Normalize to uppercase for comparison
        String normValue1 = value1.trim().toUpperCase();
        String normValue2 = value2.trim().toUpperCase();

        // Same value is never incompatible
        if (!normValue1.equals(normValue2)) {
            // SwitchDevice: ON vs OFF
            if (isSwitchValue(normValue1) && isSwitchValue(normValue2)) {
                incompatible = true;
            } else if (isCoverValue(normValue1) && isCoverValue(normValue2)) {
                // CoverDevice: OPEN vs CLOSED
                incompatible = true;
            } else if (isDimmerValue(normValue1) && isDimmerValue(normValue2)) {
                // DimmerDevice: any different brightness values
                incompatible = true;
            } else if (isThermostatValue(normValue1) && isThermostatValue(normValue2)) {
                // ThermostatDevice: temperatures differing by more than 0.5°C
                try {
                    double temp1 = extractTemperatureValue(normValue1);
                    double temp2 = extractTemperatureValue(normValue2);
                    incompatible = Math.abs(temp1 - temp2) > 0.5;
                } catch (NumberFormatException e) {
                    incompatible = false;
                }
            }
        }

        return incompatible;
    }

    /**
     * Checks if a value represents a switch state.
     *
     * @param value the value to check
     * @return true if value is "ON" or "OFF"
     */
    private boolean isSwitchValue(String value) {
        return "ON".equals(value) || "OFF".equals(value) || "TURN ON".equals(value) || "TURN OFF".equals(value);
    }

    /**
     * Checks if a value represents a cover position.
     *
     * @param value the value to check
     * @return true if value is "OPEN" or "CLOSED" or "CLOSE"
     */
    private boolean isCoverValue(String value) {
        return "OPEN".equals(value) || "CLOSED".equals(value) || "CLOSE".equals(value);
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
        RecurrencePattern pattern;
        String rec = recurrence.trim().toLowerCase();

        if (RECURRENCE_NEVER.equals(rec)) {
            pattern = new RecurrencePattern(true, false, false, null);
        } else if (RECURRENCE_DAILY.equals(rec)) {
            pattern = new RecurrencePattern(false, true, false, null);
        } else {
            // Try to parse as day of week (e.g., "Mon", "Monday", "Monday 20:00")
            DayOfWeek day = tryParseDayOfWeek(rec);
            if (day == null) {
                // Try to extract day from the full time pattern
                day = tryParseDayOfWeek(timePattern);
            }

            if (day != null) {
                pattern = new RecurrencePattern(false, false, true, day);
            } else {
                // Default to never if we can't parse it
                pattern = new RecurrencePattern(true, false, false, null);
            }
        }

        return pattern;
    }

    /**
     * Attempts to parse a day of week from a string.
     *
     * @param input input string
     * @return DayOfWeek if found, null otherwise
     */
    private DayOfWeek tryParseDayOfWeek(String input) {
        DayOfWeek result = null;
        String upper = input.trim().toUpperCase();
        
        try {
            result = DayOfWeek.valueOf(upper);
        } catch (IllegalArgumentException e) {
            // Try short form
            String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
            String[] shortDays = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
            
            for (int i = 0; i < shortDays.length; i++) {
                if (upper.startsWith(shortDays[i])) {
                    result = DayOfWeek.valueOf(days[i]);
                    break;
                }
            }
        }
        return result;
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
    }
}
