package at.jku.se.smarthome.service.rule;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Rule;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Evaluates whether a rule's condition is satisfied given the current device state or time.
 *
 * <p>This class is stateless and has no service dependencies — all inputs are passed
 * explicitly so the logic can be tested without a running application.</p>
 *
 * <p>Supported trigger types and condition formats:</p>
 * <ul>
 *   <li><b>Time</b> — condition is a time string, e.g. {@code "06:00 AM"}.
 *       Returns true when the current local time matches within the same minute.</li>
 *   <li><b>Device State</b> — condition is {@code "State = Active"} or
 *       {@code "State = Inactive"}. Evaluated against {@code sourceDevice.getState()}.</li>
 *   <li><b>Sensor Threshold</b> — condition is {@code "Value [op] [number]"}, e.g.
 *       {@code "Value > 22.5"}. Evaluated against {@code sourceDevice.getTemperature()}.</li>
 * </ul>
 */
@SuppressWarnings("PMD.AtLeastOneConstructor")
public class RuleEvaluator {


    /** Time formatter for 12-hour format with AM/PM. */
    private static final DateTimeFormatter TIME_FORMAT_12H =
            DateTimeFormatter.ofPattern("hh:mm a");
    /** Time formatter for 24-hour format (HH:mm). */
    private static final DateTimeFormatter TIME_FORMAT_24H =
            DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Evaluates whether the rule's condition is currently satisfied.
     *
     * @param rule         the rule to evaluate
     * @param sourceDevice the source device (may be null for Time triggers)
     * @return true when the condition is met, otherwise false
     */
    public boolean evaluate(Rule rule, Device sourceDevice) {
        boolean result = false;
        if (rule != null) {
            result = switch (rule.getTriggerType()) {
                case "Time" -> evaluateTime(rule.getCondition());
                case "Device State" -> evaluateDeviceState(rule.getCondition(), sourceDevice);
                case "Sensor Threshold" -> evaluateSensorThreshold(rule.getCondition(), sourceDevice);
                default -> false;
            };
        }
        return result;
    }

    private boolean evaluateTime(String condition) {
        boolean result = false;
        if (condition != null && !condition.isBlank()) {
            LocalTime target = parseTime(condition.trim());
            if (target != null) {
                LocalTime now = LocalTime.now();
                result = now.getHour() == target.getHour() && now.getMinute() == target.getMinute();
            }
        }
        return result;
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    private LocalTime parseTime(String text) {
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

    private boolean evaluateDeviceState(String condition, Device sourceDevice) {
        boolean result = false;
        if (condition != null && sourceDevice != null) {
            String normalized = condition.trim().toLowerCase();
            if (normalized.equals("state = active") || normalized.equals("state = on")) {
                result = sourceDevice.getState();
            } else if (normalized.equals("state = inactive") || normalized.equals("state = off")) {
                result = !sourceDevice.getState();
            }
        }
        return result;
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private boolean evaluateSensorThreshold(String condition, Device sourceDevice) {
        boolean result = false;
        if (condition != null && sourceDevice != null) {
            String[] parts = condition.trim().split("\\s+");
            if (parts.length == 3 && parts[0].equalsIgnoreCase("Value")) {
                String operator = parts[1];
                try {
                    double threshold = Double.parseDouble(parts[2]);
                    double sensorValue = sourceDevice.getTemperature();
                    result = switch (operator) {
                        case ">" -> sensorValue > threshold;
                        case ">=" -> sensorValue >= threshold;
                        case "<" -> sensorValue < threshold;
                        case "<=" -> sensorValue <= threshold;
                        case "=", "==" -> Double.compare(sensorValue, threshold) == 0;
                        default -> false;
                    };
                } catch (NumberFormatException e) {
                    result = false;
                }
            }
        }
        return result;
    }
}
