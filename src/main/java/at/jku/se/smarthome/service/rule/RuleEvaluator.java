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
public class RuleEvaluator {

    private static final DateTimeFormatter TIME_FORMAT_12H =
            DateTimeFormatter.ofPattern("hh:mm a");
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
        if (rule == null) {
            return false;
        }
        switch (rule.getTriggerType()) {
            case "Time":
                return evaluateTime(rule.getCondition());
            case "Device State":
                return evaluateDeviceState(rule.getCondition(), sourceDevice);
            case "Sensor Threshold":
                return evaluateSensorThreshold(rule.getCondition(), sourceDevice);
            default:
                return false;
        }
    }

    private boolean evaluateTime(String condition) {
        if (condition == null || condition.isBlank()) {
            return false;
        }
        LocalTime target = parseTime(condition.trim());
        if (target == null) {
            return false;
        }
        LocalTime now = LocalTime.now();
        return now.getHour() == target.getHour() && now.getMinute() == target.getMinute();
    }

    private LocalTime parseTime(String text) {
        try {
            return LocalTime.parse(text.toUpperCase(), TIME_FORMAT_12H);
        } catch (DateTimeParseException e1) {
            try {
                return LocalTime.parse(text, TIME_FORMAT_24H);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    private boolean evaluateDeviceState(String condition, Device sourceDevice) {
        if (condition == null || sourceDevice == null) {
            return false;
        }
        String normalized = condition.trim().toLowerCase();
        if (normalized.equals("state = active") || normalized.equals("state = on")) {
            return sourceDevice.getState();
        }
        if (normalized.equals("state = inactive") || normalized.equals("state = off")) {
            return !sourceDevice.getState();
        }
        return false;
    }

    private boolean evaluateSensorThreshold(String condition, Device sourceDevice) {
        if (condition == null || sourceDevice == null) {
            return false;
        }
        // Expected format: "Value [op] [number]"
        String[] parts = condition.trim().split("\\s+");
        if (parts.length != 3 || !parts[0].equalsIgnoreCase("Value")) {
            return false;
        }
        String operator = parts[1];
        double threshold;
        try {
            threshold = Double.parseDouble(parts[2]);
        } catch (NumberFormatException e) {
            return false;
        }
        double sensorValue = sourceDevice.getTemperature();
        switch (operator) {
            case ">":  return sensorValue > threshold;
            case ">=": return sensorValue >= threshold;
            case "<":  return sensorValue < threshold;
            case "<=": return sensorValue <= threshold;
            case "=":
            case "==": return Double.compare(sensorValue, threshold) == 0;
            default:   return false;
        }
    }
}
