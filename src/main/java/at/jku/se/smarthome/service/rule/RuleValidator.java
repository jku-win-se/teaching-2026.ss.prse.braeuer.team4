package at.jku.se.smarthome.service.rule;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.service.api.RoomService;

/**
 * Stateless service-layer guard that validates rule trigger type, condition,
 * and source device before a rule is accepted into the system.
 */
public final class RuleValidator {

    /** Time formatter for 12-hour format with AM/PM. */
    private static final DateTimeFormatter TIME_FORMAT_12H =
            DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US);
    /** Time formatter for 24-hour format (HH:mm). */
    private static final DateTimeFormatter TIME_FORMAT_24H =
            DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Validation result carrying a boolean flag and an optional reason.
     *
     * @param valid true when the payload passes all checks
     * @param reason human-readable explanation when invalid
     */
    public record Result(boolean valid, String reason) { }

    /**
     * Validates a prospective rule's trigger type, condition, and source device.
     *
     * @param triggerType the trigger category
     * @param condition   the trigger condition expression
     * @param sourceDevice the source device name (may be null for Time triggers)
     * @param rooms       room service used to resolve devices
     * @return validation result; {@code valid=false} rejects the rule
     */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public static Result validate(String triggerType, String condition,
                                  String sourceDevice, RoomService rooms) {
        if (condition == null || condition.isBlank()) {
            return new Result(false, "condition is required");
        }
        String trimmed = condition.trim();
        return switch (triggerType) {
            case "Time" -> validateTime(trimmed);
            case "Sensor Threshold" -> validateSensorThreshold(trimmed, sourceDevice, rooms);
            case "Device State" -> validateDeviceState(trimmed, sourceDevice, rooms);
            default -> new Result(false, "unknown trigger type: " + triggerType);
        };
    }

    private static Result validateTime(String condition) {
        String timePart = condition;
        int firstSpace = condition.indexOf(' ');
        if (firstSpace > 0) {
            String prefix = condition.substring(0, firstSpace);
            Optional<WeekdaySpec> spec = WeekdaySpec.parse(prefix);
            if (spec.isPresent()) {
                timePart = condition.substring(firstSpace + 1).trim();
            }
        }
        if (parseTime(timePart) == null) {
            return new Result(false, "malformed time expression: " + timePart);
        }
        return new Result(true, null);
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    private static LocalTime parseTime(String text) {
        LocalTime parsedTime = null;
        try {
            parsedTime = LocalTime.parse(text.toUpperCase(), TIME_FORMAT_12H);
        } catch (DateTimeParseException e1) {
            try {
                parsedTime = LocalTime.parse(text, TIME_FORMAT_24H);
            } catch (DateTimeParseException e2) {
                // Keep null to indicate an unparsable time expression.
            }
        }
        return parsedTime;
    }

    private static Result validateSensorThreshold(String condition, String sourceDevice,
                                                   RoomService rooms) {
        String[] parts = condition.split("\\s+");
        if (parts.length != 3 || !"Value".equalsIgnoreCase(parts[0])) {
            return new Result(false, "malformed threshold condition");
        }
        String operator = parts[1];
        if (!isValidOperator(operator)) {
            return new Result(false, "unknown operator: " + operator);
        }
        try {
            Double.parseDouble(parts[2]);
        } catch (NumberFormatException e) {
            return new Result(false, "non-numeric threshold: " + parts[2]);
        }
        Device device = rooms.getDeviceByName(sourceDevice);
        if (device == null) {
            return new Result(false, "source device not found: " + sourceDevice);
        }
        if (!"sensor".equalsIgnoreCase(device.getType())) {
            return new Result(false, "source device is not a sensor: " + sourceDevice);
        }
        return new Result(true, null);
    }

    private static boolean isValidOperator(String operator) {
        return "<".equals(operator) || "<=".equals(operator)
                || "=".equals(operator) || "==".equals(operator)
                || ">=".equals(operator) || ">".equals(operator);
    }

    private static Result validateDeviceState(String condition, String sourceDevice,
                                               RoomService rooms) {
        String normalised = condition.toLowerCase();
        if (!"state = active".equals(normalised) && !"state = inactive".equals(normalised)
                && !"state = on".equals(normalised) && !"state = off".equals(normalised)) {
            return new Result(false, "malformed device state condition");
        }
        if (rooms.getDeviceByName(sourceDevice) == null) {
            return new Result(false, "source device not found: " + sourceDevice);
        }
        return new Result(true, null);
    }
}
