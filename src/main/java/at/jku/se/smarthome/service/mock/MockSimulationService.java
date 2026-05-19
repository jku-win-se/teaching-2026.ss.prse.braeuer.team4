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
import at.jku.se.smarthome.model.Schedule;
import at.jku.se.smarthome.model.SimulationDeviceState;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock simulation service for replaying a full day without affecting live devices.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.GodClass"})
public final class MockSimulationService {

    /** Trigger source label used for the synthetic day-end marker event. */
    private static final String SIMULATION_DAY_END_SOURCE = "Simulation Day End";

    /** Singleton instance of the mock simulation service. */
    private static MockSimulationService instance;

    /** Room service for room and device data. */
    private final MockRoomService roomService = MockRoomService.getInstance();
    /** Schedule service for simulation schedule playback. */
    private final MockScheduleService scheduleService = MockScheduleService.getInstance();
    /** Date/time formatter for parsing simulation times. */
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    /** Pattern for numeric conditions in rules. */
    private static final Pattern NUMERIC_CONDITION_PATTERN = Pattern.compile("(>=|<=|>|<|=)\\s*(-?\\d+(?:\\.\\d+)?)");

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

    private MockSimulationService() {
    }

    public static MockSimulationService getInstance() {
        synchronized (MockSimulationService.class) {
            if (instance == null) {
                instance = new MockSimulationService();
            }
            return instance;
        }
    }

    /**
     * Resets the singleton for unit testing.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public static void resetForTesting() {
        synchronized (MockSimulationService.class) {
            instance = null;
        }
    }

    /**
     * Builds a complete simulation plan from the provided configuration.
     *
     * @param configuration simulation configuration
     * @return simulation plan with device states and events
     */
    public SimulationPlan buildPlan(SimulationConfiguration configuration) {
        ObservableList<SimulationDeviceState> simulatedDeviceStates = createSimulationSnapshot(configuration.startTime());
        List<PlannedEvent> plannedEvents = new ArrayList<>();
        int offsetMinutes = 30;
        long sequence = 0;

        for (Schedule schedule : scheduleService.getSchedules()) {
            if (!schedule.isActive()) {
                continue;
            }
            Device targetDevice = resolveTargetDevice(schedule.getDevice(), null);
            if (targetDevice == null) {
                continue;
            }
            LocalTime scheduleTime = parseRuleTime(schedule.getTime(), configuration.startTime().plusMinutes(offsetMinutes));
            long scheduleSequence = sequence;
            sequence++;
            plannedEvents.add(new PlannedEvent(
                    new SimulationEvent(
                            scheduleTime,
                            targetDevice.getName(),
                            targetDevice.getRoom(),
                            toSimulationState(schedule.getAction()),
                            "Schedule: " + schedule.getName()
                    ),
                    0,
                    scheduleSequence
            ));
            offsetMinutes += 20;
        }

        for (Rule rule : configuration.activeRules()) {
            if (!rule.isEnabled()) {
                continue;
            }

            Device targetDevice = resolveTargetDevice(rule.getTargetDevice(), rule.getSourceDevice());
            if (targetDevice == null) {
                continue;
            }

            LocalTime triggerTime = determineTriggerTime(configuration, rule, offsetMinutes);
            if (triggerTime == null) {
                offsetMinutes += 30;
                continue;
            }

            long ruleSequence = sequence;
            sequence++;
            plannedEvents.add(new PlannedEvent(
                    new SimulationEvent(
                            triggerTime,
                            targetDevice.getName(),
                            targetDevice.getRoom(),
                            toSimulationState(rule.getAction()),
                            "Rule: " + rule.getName()
                    ),
                    1,
                    ruleSequence
            ));
            offsetMinutes += 45;
        }

        LocalTime dayEnd = LocalTime.MIDNIGHT;
        plannedEvents.add(new PlannedEvent(
                new SimulationEvent(dayEnd, "-", "-", "END_OF_DAY", SIMULATION_DAY_END_SOURCE),
                Integer.MAX_VALUE,
                Long.MAX_VALUE
        ));

        plannedEvents.sort(Comparator
                .<PlannedEvent>comparingLong(event -> elapsedSecondsFromStart(configuration.startTime(), event.event()))
                .thenComparingInt(event -> event.priority())
                .thenComparingLong(event -> event.sequence()));

        List<SimulationEvent> events = plannedEvents.stream().map(PlannedEvent::event).toList();
        return new SimulationPlan(simulatedDeviceStates, events);
    }

    /**
     * Applies a simulation event to the simulated device states.
     *
     * @param simulatedDeviceStates observable list of simulated device states
     * @param event simulation event to apply
     */
    public void applyEvent(ObservableList<SimulationDeviceState> simulatedDeviceStates, SimulationEvent event) {
        simulatedDeviceStates.stream()
                .filter(device -> device.getDeviceName().equals(event.deviceName()))
                .findFirst()
                .ifPresent(device -> {
                    device.setState(event.resultingState());
                    device.setLastChanged(event.simulatedTime().format(timeFormatter));
                });
    }

    /**
     * Parses a start time string in various formats.
     *
     * @param value time string (e.g., "06:00:00" or "HH:mm")
     * @return parsed local time
     * @throws IllegalArgumentException if time cannot be parsed
     */
    public LocalTime parseStartTime(String value) {
        LocalTime result = null;
        if (value != null && !value.isBlank()) {
            for (String pattern : List.of("HH:mm:ss", "HH:mm")) {
                try {
                    result = LocalTime.parse(value.trim(), DateTimeFormatter.ofPattern(pattern));
                    break;
                } catch (DateTimeParseException ignored) {
                }
            }
        }

        if (result == null) {
            throw new IllegalArgumentException("Use HH:mm or HH:mm:ss for the start time");
        }
        return result;
    }

    private record PlannedEvent(SimulationEvent event, int priority, long sequence) {
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
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
        LocalTime result = configuration.startTime().plusMinutes(fallbackMinutes);
        if (rule.getTriggerType() != null) {
            result = switch (rule.getTriggerType()) {
                case "Time" -> parseRuleTime(rule.getCondition(), configuration.startTime().plusMinutes(fallbackMinutes));
                case "Sensor Threshold" -> evaluateThresholdTrigger(configuration, rule)
                        ? configuration.startTime().plusHours(2).plusMinutes(fallbackMinutes % 20)
                        : null;
                case "Device State" -> configuration.startTime().plusMinutes(fallbackMinutes);
                default -> configuration.startTime().plusMinutes(fallbackMinutes);
            };
            if (result == null) {
                result = configuration.startTime().plusMinutes(fallbackMinutes);
            }
        }
        return result;
    }

    private Device resolveTargetDevice(String targetDeviceName, String sourceDeviceName) {
        Device target = roomService.getDeviceByName(targetDeviceName);
        if (target == null) {
            target = roomService.getDeviceByName(sourceDeviceName);
        }
        return target;
    }

    private LocalTime parseRuleTime(String condition, LocalTime fallback) {
        LocalTime result = fallback;
        if (condition != null && !condition.isBlank()) {
            for (String pattern : List.of("hh:mm a", "HH:mm", "HH:mm:ss")) {
                try {
                    result = LocalTime.parse(condition.trim(), DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
                    break;
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        return result;
    }

    private boolean evaluateThresholdTrigger(SimulationConfiguration configuration, Rule rule) {
        double sensorValue = resolveSensorValue(configuration, rule.getSourceDevice(), rule.getCondition());
        boolean result = sensorValue > 0;
        Matcher matcher = NUMERIC_CONDITION_PATTERN.matcher(rule.getCondition() == null ? "" : rule.getCondition());
        if (matcher.find()) {
            String operator = matcher.group(1);
            double threshold = Double.parseDouble(matcher.group(2));
            result = switch (operator) {
                case ">" -> sensorValue > threshold;
                case ">=" -> sensorValue >= threshold;
                case "<" -> sensorValue < threshold;
                case "<=" -> sensorValue <= threshold;
                case "=" -> sensorValue == threshold;
                default -> false;
            };
        }
        return result;
    }

    private double resolveSensorValue(SimulationConfiguration configuration, String sourceDevice, String condition) {
        double result = configuration.initialTemperature();
        String normalizedSource = sourceDevice == null ? "" : sourceDevice.toLowerCase(Locale.ROOT);
        String normalizedCondition = condition == null ? "" : condition.toLowerCase(Locale.ROOT);

        if (normalizedSource.contains("humidity") || normalizedCondition.contains("humidity")) {
            result = configuration.initialHumidity();
        } else if (normalizedSource.contains("motion") || normalizedCondition.contains("motion")) {
            result = 1.0;
        }
        return result;
    }

    private String toSimulationState(String action) {
        String result = "UNCHANGED";
        if (action != null && !action.isBlank()) {
            String normalized = action.trim();
            result = switch (normalized) {
                case "Turn On" -> "ON";
                case "Turn Off" -> "OFF";
                case "Open" -> "OPEN";
                case "Close" -> "CLOSED";
                case "Notify User" -> "NOTIFY";
                case "Trigger Alert" -> "ALERT";
                default -> normalized.replace("Set to ", "");
            };
        }
        return result;
    }

    private long elapsedSecondsFromStart(LocalTime startTime, SimulationEvent event) {
        long elapsed = 24 * 60 * 60L;
        if (!SIMULATION_DAY_END_SOURCE.equals(event.triggerSource())) {
            LocalTime eventTime = event.simulatedTime();
            long start = startTime.toSecondOfDay();
            long eventSecond = eventTime.toSecondOfDay();
            elapsed = eventSecond >= start ? eventSecond - start : 24 * 60 * 60L - start + eventSecond;
        }
        return elapsed;
    }
}