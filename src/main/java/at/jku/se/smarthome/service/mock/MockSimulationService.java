package at.jku.se.smarthome.service.mock;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Rule;
import at.jku.se.smarthome.model.SimulationDeviceState;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock simulation service for replaying a full day without affecting live devices.
 */
public final class MockSimulationService {

    public record SimulationConfiguration(
            LocalTime startTime,
            double initialTemperature,
            double initialHumidity,
            List<Rule> activeRules,
            int speedMultiplier) {
    }

    public record SimulationEvent(
            LocalTime simulatedTime,
            String deviceName,
            String room,
            String resultingState,
            String triggerSource) {
    }

    public record SimulationPlan(
            ObservableList<SimulationDeviceState> simulatedDeviceStates,
            List<SimulationEvent> events) {
    }

    /** Pattern for numeric conditions in rules. */
    private static final Pattern NUMERIC_CONDITION_PATTERN = Pattern.compile("(>=|<=|>|<|=)\\s*(-?\\d+(?:\\.\\d+)?)");
    /** Singleton instance. */
    private static MockSimulationService instance;

    /** Room service for room data. */
    private final MockRoomService roomService = MockRoomService.getInstance();
    /** Time formatter for parsing times. */
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private MockSimulationService() {
    }

    public static synchronized MockSimulationService getInstance() {
        if (instance == null) {
            instance = new MockSimulationService();
        }
        return instance;
    }

    public static synchronized void resetForTesting() {
        instance = null;
    }

    public SimulationPlan buildPlan(SimulationConfiguration configuration) {
        ObservableList<SimulationDeviceState> simulatedDeviceStates = createSimulationSnapshot(configuration.startTime());
        List<SimulationEvent> events = new ArrayList<>();
        int offsetMinutes = 30;

        for (Rule rule : configuration.activeRules()) {
            if (!rule.isEnabled()) {
                continue;
            }

            Device targetDevice = roomService.getDeviceByName(rule.getTargetDevice());
            if (targetDevice == null) {
                continue;
            }

            LocalTime triggerTime = determineTriggerTime(configuration, rule, offsetMinutes);
            if (triggerTime == null) {
                offsetMinutes += 30;
                continue;
            }

            events.add(new SimulationEvent(
                    triggerTime,
                    targetDevice.getName(),
                    targetDevice.getRoom(),
                    toSimulationState(rule.getAction()),
                    "Rule: " + rule.getName()
            ));
            offsetMinutes += 45;
        }

        events.sort(Comparator.comparing(SimulationEvent::simulatedTime));
        return new SimulationPlan(simulatedDeviceStates, events);
    }

    public void applyEvent(ObservableList<SimulationDeviceState> simulatedDeviceStates, SimulationEvent event) {
        simulatedDeviceStates.stream()
                .filter(device -> device.getDeviceName().equals(event.deviceName()))
                .findFirst()
                .ifPresent(device -> {
                    device.setState(event.resultingState());
                    device.setLastChanged(event.simulatedTime().format(timeFormatter));
                });
    }

    public LocalTime parseStartTime(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Start time is required");
        }

        for (String pattern : List.of("HH:mm:ss", "HH:mm")) {
            try {
                return LocalTime.parse(value.trim(), DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new IllegalArgumentException("Use HH:mm or HH:mm:ss for the start time");
    }

    private ObservableList<SimulationDeviceState> createSimulationSnapshot(LocalTime startTime) {
        ObservableList<SimulationDeviceState> snapshot = FXCollections.observableArrayList();
        String initialTimestamp = startTime.format(timeFormatter);

        for (Device device : roomService.getAllDevices()) {
            snapshot.add(new SimulationDeviceState(
                    device.getName(),
                    device.getRoom(),
                    device.getType(),
                    currentLiveState(device),
                    initialTimestamp
            ));
        }

        return snapshot;
    }

    private String currentLiveState(Device device) {
        return switch (device.getType().toLowerCase(Locale.ROOT)) {
            case "dimmer" -> device.getBrightness() + "%";
            case "thermostat" -> String.format(Locale.ROOT, "%.1f°C", device.getTemperature());
            case "cover/blind" -> device.getState() ? "OPEN" : "CLOSED";
            case "sensor" -> device.getState() ? "ACTIVE" : "IDLE";
            default -> device.getState() ? "ON" : "OFF";
        };
    }

    private LocalTime determineTriggerTime(SimulationConfiguration configuration, Rule rule, int fallbackMinutes) {
        return switch (rule.getTriggerType()) {
            case "Time" -> parseRuleTime(rule.getCondition(), configuration.startTime().plusMinutes(fallbackMinutes));
            case "Sensor Threshold" -> evaluateThresholdTrigger(configuration, rule)
                    ? configuration.startTime().plusHours(2).plusMinutes(fallbackMinutes % 20)
                    : null;
            case "Device State" -> configuration.startTime().plusMinutes(fallbackMinutes);
            default -> configuration.startTime().plusMinutes(fallbackMinutes);
        };
    }

    private LocalTime parseRuleTime(String condition, LocalTime fallback) {
        if (condition == null || condition.isBlank()) {
            return fallback;
        }

        for (String pattern : List.of("hh:mm a", "HH:mm", "HH:mm:ss")) {
            try {
                return LocalTime.parse(condition.trim(), DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
            } catch (DateTimeParseException ignored) {
            }
        }
        return fallback;
    }

    private boolean evaluateThresholdTrigger(SimulationConfiguration configuration, Rule rule) {
        double sensorValue = resolveSensorValue(configuration, rule.getSourceDevice(), rule.getCondition());
        Matcher matcher = NUMERIC_CONDITION_PATTERN.matcher(rule.getCondition() == null ? "" : rule.getCondition());
        if (!matcher.find()) {
            return sensorValue > 0;
        }

        String operator = matcher.group(1);
        double threshold = Double.parseDouble(matcher.group(2));

        return switch (operator) {
            case ">" -> sensorValue > threshold;
            case ">=" -> sensorValue >= threshold;
            case "<" -> sensorValue < threshold;
            case "<=" -> sensorValue <= threshold;
            case "=" -> sensorValue == threshold;
            default -> false;
        };
    }

    private double resolveSensorValue(SimulationConfiguration configuration, String sourceDevice, String condition) {
        String normalizedSource = sourceDevice == null ? "" : sourceDevice.toLowerCase(Locale.ROOT);
        String normalizedCondition = condition == null ? "" : condition.toLowerCase(Locale.ROOT);

        if (normalizedSource.contains("humidity") || normalizedCondition.contains("humidity")) {
            return configuration.initialHumidity();
        }
        if (normalizedSource.contains("motion") || normalizedCondition.contains("motion")) {
            return 1.0;
        }
        return configuration.initialTemperature();
    }

    private String toSimulationState(String action) {
        if (action == null || action.isBlank()) {
            return "UNCHANGED";
        }

        String normalized = action.trim();
        return switch (normalized) {
            case "Turn On" -> "ON";
            case "Turn Off" -> "OFF";
            case "Open" -> "OPEN";
            case "Close" -> "CLOSED";
            case "Notify User" -> "NOTIFY";
            case "Trigger Alert" -> "ALERT";
            default -> normalized.replace("Set to ", "");
        };
    }
}