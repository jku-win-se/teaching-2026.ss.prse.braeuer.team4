package at.jku.se.smarthome.service.mock;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import at.jku.se.smarthome.model.Schedule;
import at.jku.se.smarthome.service.api.ScheduleService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock Schedule Service providing schedule management functionality.
 */
@SuppressWarnings({"PMD.DoNotUseThreads", "PMD.TooManyMethods", "PMD.UseObjectForClearerAPI", "PMD.CyclomaticComplexity"})
public final class MockScheduleService implements ScheduleService {
    
    /** Singleton instance. */
    private static MockScheduleService instance;
    /** Observable list of all schedules. */
    private final ObservableList<Schedule> schedules;
    /** Room service for room data. */
    private final MockRoomService roomService = MockRoomService.getInstance();
    /** Log service for activity logging. */
    private final MockLogService logService = MockLogService.getInstance();
    /** Tracks last processed minute for each schedule. */
    private final Map<String, LocalDateTime> lastProcessedMinuteByScheduleId = new ConcurrentHashMap<>();
    /** Executor service for recurring schedule execution. */
    private ScheduledExecutorService scheduler;

    /** Time formatters for parsing various time formats. */
    private static final List<DateTimeFormatter> TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
    );

    /** Private constructor for singleton pattern. */
    private MockScheduleService() {
        this.schedules = FXCollections.observableArrayList();
        initializeMockSchedules();
    }
    
    public static MockScheduleService getInstance() {
        synchronized (MockScheduleService.class) {
            if (instance == null) {
                instance = new MockScheduleService();
            }
            return instance;
        }
    }

    /**
     * Resets the singleton for unit testing.
     * Must NOT be called from production code.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public static void resetForTesting() {
        synchronized (MockScheduleService.class) {
            instance = null;
        }
    }

    private void initializeMockSchedules() {
        schedules.add(new Schedule("sched-001", "Morning Lights", "dev-001", "Main Light", 
                                  "Turn On", "06:00 AM", "Weekdays", true));
        schedules.add(new Schedule("sched-002", "Bedtime", "dev-003", "Bed Light", 
                                  "Turn Off", "11:00 PM", "Daily", true));
        schedules.add(new Schedule("sched-003", "Weekend Relax", "dev-002", "Dimmer Light", 
                                  "Set to 50%", "18:00", "Weekends", false));
    }

    /**
     * Starts periodic recurring schedule processing on the JavaFX application thread.
     */
    public void startRecurringExecution() {
        synchronized (this) {
            if (scheduler != null && !scheduler.isShutdown()) {
                return;
            }

            ThreadFactory threadFactory = runnable -> {
                Thread thread = new Thread(runnable, "mock-schedule-dispatcher");
                thread.setDaemon(true);
                return thread;
            };

            scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
            scheduler.scheduleAtFixedRate(() -> {
                LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
                Platform.runLater(() -> processDueSchedules(now));
            }, 0, 15, TimeUnit.SECONDS);
        }
    }

    /**
     * Stops periodic recurring schedule processing.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public void stopRecurringExecution() {
        synchronized (this) {
            if (scheduler == null) {
                return;
            }

            scheduler.shutdownNow();
            scheduler = null;
        }
    }
    
    /**
     * Gets all schedules.
     *
     * @return observable list of schedules
     */
    public ObservableList<Schedule> getSchedules() {
        return schedules;
    }

    /**
     * Gets a schedule by ID.
     *
     * @param scheduleId identifier of the schedule to retrieve
     * @return matching schedule, or null when none exists
     */
    public Schedule getScheduleById(String scheduleId) {
        return schedules.stream()
                .filter(schedule -> schedule.getId().equals(scheduleId))
                .findFirst()
                .orElse(null);
    }

    /**
        * Gets a schedule by display name.
        *
        * @param scheduleName display name of the schedule to retrieve
        * @return matching schedule, or null when none exists
     */
    public Schedule getScheduleByName(String scheduleName) {
        return schedules.stream()
                .filter(schedule -> schedule.getName().equals(scheduleName))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Adds a new schedule.
     */
    @Override
    public Schedule addSchedule(String name, String deviceId, String deviceName, String action,
                                String time, String recurrence, boolean active) {
        Schedule schedule = new Schedule(
                "sched-" + String.format("%03d", schedules.size() + 1),
                name,
                deviceId,
                deviceName,
                action,
                time,
                recurrence,
                active
        );
        schedules.add(schedule);
        return schedule;
    }

    /**
     * Adds a new schedule by resolving the device identifier from the device name.
     *
     * @param name schedule name
     * @param deviceName target device name
     * @param action action to execute
     * @param time schedule time pattern
     * @param recurrence recurrence mode
     * @param active whether the schedule starts active
     * @return created schedule instance
     */
    public Schedule addSchedule(String name, String deviceName, String action,
                                String time, String recurrence, boolean active) {
        String deviceId = resolveDeviceId(deviceName);
        return addSchedule(name, deviceId, deviceName, action, time, recurrence, active);
    }
    
    /**
     * Updates a schedule.
     */
    @Override
    public boolean updateSchedule(String schedId, String name, String deviceId, String deviceName,
                      String action, String time, String recurrence, boolean active) {
        boolean updated = false;
        Schedule schedule = schedules.stream()
                .filter(s -> s.getId().equals(schedId))
                .findFirst()
                .orElse(null);
        
        if (schedule != null) {
            schedule.setName(name);
            schedule.setDeviceId(deviceId);
            schedule.setDevice(deviceName);
            schedule.setAction(action);
            schedule.setTime(time);
            schedule.setRecurrence(recurrence);
            schedule.setActive(active);
            updated = true;
        }
        return updated;
    }

    /**
     * Updates a schedule by resolving the device identifier from the device name.
     *
     * @param schedId identifier of the schedule to update
     * @param name updated schedule name
     * @param deviceName updated target device name
     * @param action updated action text
     * @param time updated schedule time pattern
     * @param recurrence updated recurrence mode
     * @return true when the schedule exists and was updated, otherwise false
     */
    public boolean updateSchedule(String schedId, String name, String deviceName,
                                  String action, String time, String recurrence) {
        String deviceId = resolveDeviceId(deviceName);
        Schedule schedule = getScheduleById(schedId);
        boolean active = schedule != null && schedule.isActive();
        return updateSchedule(schedId, name, deviceId, deviceName, action, time, recurrence, active);
    }
    
    /**
     * Toggles a schedule's active state.
        *
        * @param schedId identifier of the schedule to toggle
        * @return true when the schedule exists and was toggled, otherwise false
     */
    public boolean toggleSchedule(String schedId) {
        boolean toggled = false;
        Schedule schedule = schedules.stream()
                .filter(s -> s.getId().equals(schedId))
                .findFirst()
                .orElse(null);
        
        if (schedule != null) {
            schedule.setActive(!schedule.isActive());
            toggled = true;
        }
        return toggled;
    }
    
    /**
        * Deletes a schedule.
        *
        * @param schedId identifier of the schedule to delete
        * @return true when the schedule existed and was removed, otherwise false
     */
    public boolean deleteSchedule(String schedId) {
        boolean removed = schedules.removeIf(s -> s.getId().equals(schedId));
        if (removed) {
            lastProcessedMinuteByScheduleId.remove(schedId);
            MockVacationModeService.getInstance().clearIfUsingSchedule(
                    schedId,
                    "Selected vacation schedule was deleted"
            );
        }
        return removed;
    }
    
    /**
     * Executes a schedule: applies its action to the target device and logs the change.
     * Returns false if the schedule is not found, inactive, the device is missing,
     * or the action is not supported.
        *
        * @param scheduleId identifier of the schedule to execute
        * @return true when execution succeeds, otherwise false
     */
    public boolean executeSchedule(String scheduleId) {
        boolean executed = false;
        Schedule schedule = getScheduleById(scheduleId);
        if (schedule != null && schedule.isActive()) {
            at.jku.se.smarthome.model.Device device = roomService.getDeviceById(schedule.getDeviceId());
            if (device == null) {
                device = roomService.getDeviceByName(schedule.getDevice());
            }
            boolean actionApplied = device != null && applyScheduleAction(device, schedule.getAction());
            if (actionApplied) {
                logService.addLogEntry(device.getName(), device.getRoom(),
                        schedule.getAction(), "Schedule: " + schedule.getName());
                executed = true;
            }
        }
        return executed;
    }

    /**
     * Executes all schedules due for the provided minute and returns the number of
     * schedules that successfully triggered.
        *
        * @param now point in time to evaluate
        * @return number of schedules executed for the supplied minute
     */
    public int processDueSchedules(LocalDateTime now) {
        int executedCount = 0;
        if (now != null) {
            LocalDateTime minute = now.truncatedTo(ChronoUnit.MINUTES);

            for (Schedule schedule : getEffectiveSchedules(minute.toLocalDate())) {
                if (!isScheduleDue(schedule, minute)) {
                    continue;
                }
                if (minute.equals(lastProcessedMinuteByScheduleId.get(schedule.getId()))) {
                    continue;
                }

                lastProcessedMinuteByScheduleId.put(schedule.getId(), minute);
                if (executeSchedule(schedule.getId())) {
                    executedCount++;
                }
            }
        }
        return executedCount;
    }

    private String resolveDeviceId(String deviceName) {
        at.jku.se.smarthome.model.Device device = roomService.getDeviceByName(deviceName);
        return device != null ? device.getId() : deviceName;
    }

    private List<Schedule> getEffectiveSchedules(LocalDate date) {
        MockVacationModeService vacationModeService = MockVacationModeService.getInstance();

        List<Schedule> activeSchedules = schedules.stream()
                .filter(Schedule::isActive)
                .toList();

        List<Schedule> effectiveSchedules = activeSchedules;
        if (vacationModeService.isActiveOn(date)) {
            Schedule selectedVacationSchedule = vacationModeService.getSelectedSchedule();
            if (selectedVacationSchedule != null && selectedVacationSchedule.isActive()) {
                effectiveSchedules = new ArrayList<>();
                effectiveSchedules.add(selectedVacationSchedule);
            }
        }
        return effectiveSchedules;
    }

    private boolean isScheduleDue(Schedule schedule, LocalDateTime now) {
        LocalTime scheduledTime = parseScheduledTime(schedule.getTime());
        boolean due = false;
        if (scheduledTime != null && scheduledTime.equals(now.toLocalTime())) {
            due = switch (normalizeRecurrence(schedule.getRecurrence())) {
                case "weekdays" -> isWeekday(now.getDayOfWeek());
                case "weekends" -> isWeekend(now.getDayOfWeek());
                case "weekly" -> matchesWeeklyDay(schedule.getTime(), now.getDayOfWeek());
                default -> true;
            };
        }
        return due;
    }

    private String normalizeRecurrence(String recurrence) {
        return recurrence == null ? "daily" : recurrence.trim().toLowerCase(Locale.ENGLISH);
    }

    private boolean isWeekday(DayOfWeek dayOfWeek) {
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    private boolean isWeekend(DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private boolean matchesWeeklyDay(String timePattern, DayOfWeek currentDay) {
        DayOfWeek parsedDay = extractDayOfWeek(timePattern);
        return parsedDay != null ? parsedDay == currentDay : currentDay == DayOfWeek.MONDAY;
    }

    private LocalTime parseScheduledTime(String timePattern) {
        LocalTime result = null;
        if (timePattern != null && !timePattern.isBlank()) {
            String upper = timePattern.toUpperCase(Locale.ENGLISH);
            String[] tokens = upper.split("\\s+");
            for (int index = 0; index < tokens.length && result == null; index++) {
                String candidate = tokens[index];
                if (!candidate.contains(":")) {
                    continue;
                }

                if (index + 1 < tokens.length && ("AM".equals(tokens[index + 1]) || "PM".equals(tokens[index + 1]))) {
                    result = parseTimeCandidate(candidate + " " + tokens[index + 1]);
                }
                if (result == null) {
                    result = parseTimeCandidate(candidate);
                }
            }
        }
        return result;
    }

    private LocalTime parseTimeCandidate(String candidate) {
        LocalTime result = null;
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                result = LocalTime.parse(candidate.trim(), formatter).truncatedTo(ChronoUnit.MINUTES);
                break;
            } catch (DateTimeParseException ignored) {
                // Try the next known schedule time format.
            }
        }
        return result;
    }

    private DayOfWeek extractDayOfWeek(String pattern) {
        DayOfWeek result = null;
        if (pattern != null) {
            String upper = pattern.toUpperCase(Locale.ENGLISH);
            for (DayOfWeek day : DayOfWeek.values()) {
                String fullName = day.name();
                String shortName = fullName.substring(0, 3);
                if (upper.contains(fullName) || upper.contains(shortName)) {
                    result = day;
                    break;
                }
            }
        }
        return result;
    }

    private boolean applyScheduleAction(at.jku.se.smarthome.model.Device device, String action) {
        boolean applied = false;
        if (action != null) {
            applied = switch (action) {
                case "Turn On" -> {
                    device.setState(true);
                    yield true;
                }
                case "Turn Off" -> {
                    device.setState(false);
                    yield true;
                }
                case "Open" -> {
                    device.setState(true);
                    yield true;
                }
                case "Close" -> {
                    device.setState(false);
                    yield true;
                }
                default -> {
                    if ("Dimmer".equalsIgnoreCase(device.getType()) && action.matches("Set( to)? \\d+%")) {
                        int brightness = Integer.parseInt(action.replaceAll("[^0-9]", ""));
                        device.setBrightness(brightness);
                        device.setState(brightness > 0);
                        yield true;
                    } else if ("Thermostat".equalsIgnoreCase(device.getType()) && action.matches("Set( to)? \\d+(\\.\\d+)?°C")) {
                        double temperature = Double.parseDouble(action.replaceAll("[^0-9.]", ""));
                        device.setTemperature(temperature);
                        device.setState(true);
                        yield true;
                    } else {
                        yield false;
                    }
                }
            };
        }
        return applied;
    }

    /**
     * Checks for conflicts.
     *
     * @param schedId identifier of the schedule being validated
     * @return true when a conflict exists, otherwise false
     */
    public boolean hasConflicts(String schedId) {
        return false; // Simplified mock
    }
}
