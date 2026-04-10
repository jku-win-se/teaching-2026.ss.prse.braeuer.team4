package at.jku.se.smarthome.service.mock;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
public class MockScheduleService implements ScheduleService {
    
    private static MockScheduleService instance;
    private final ObservableList<Schedule> schedules;
    private final MockRoomService roomService = MockRoomService.getInstance();
    private final MockLogService logService = MockLogService.getInstance();
    private final Map<String, LocalDateTime> lastProcessedMinuteByScheduleId = new HashMap<>();
    private ScheduledExecutorService scheduler;

    private static final List<DateTimeFormatter> TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
    );

    private MockScheduleService() {
        this.schedules = FXCollections.observableArrayList();
        initializeMockSchedules();
    }
    
    public static synchronized MockScheduleService getInstance() {
        if (instance == null) {
            instance = new MockScheduleService();
        }
        return instance;
    }

    /**
     * Resets the singleton for unit testing.
     * Must NOT be called from production code.
     */
    public static synchronized void resetForTesting() {
        instance = null;
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
    public synchronized void startRecurringExecution() {
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

    /**
     * Stops periodic recurring schedule processing.
     */
    public synchronized void stopRecurringExecution() {
        if (scheduler == null) {
            return;
        }

        scheduler.shutdownNow();
        scheduler = null;
    }
    
    /**
     * Gets all schedules.
     */
    public ObservableList<Schedule> getSchedules() {
        return schedules;
    }

    /**
     * Gets a schedule by ID.
     */
    public Schedule getScheduleById(String scheduleId) {
        return schedules.stream()
                .filter(schedule -> schedule.getId().equals(scheduleId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets a schedule by display name.
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
            return true;
        }
        return false;
    }

    public boolean updateSchedule(String schedId, String name, String deviceName,
                                  String action, String time, String recurrence) {
        String deviceId = resolveDeviceId(deviceName);
        Schedule schedule = getScheduleById(schedId);
        boolean active = schedule != null && schedule.isActive();
        return updateSchedule(schedId, name, deviceId, deviceName, action, time, recurrence, active);
    }
    
    /**
     * Toggles a schedule's active state.
     */
    public boolean toggleSchedule(String schedId) {
        Schedule schedule = schedules.stream()
                .filter(s -> s.getId().equals(schedId))
                .findFirst()
                .orElse(null);
        
        if (schedule != null) {
            schedule.setActive(!schedule.isActive());
            return true;
        }
        return false;
    }
    
    /**
     * Deletes a schedule.
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
     */
    public boolean executeSchedule(String scheduleId) {
        Schedule schedule = getScheduleById(scheduleId);
        if (schedule == null) return false;
        if (!schedule.isActive()) return false;

        at.jku.se.smarthome.model.Device device = roomService.getDeviceById(schedule.getDeviceId());
        if (device == null) {
            device = roomService.getDeviceByName(schedule.getDevice());
        }
        if (device == null) return false;

        boolean success = applyScheduleAction(device, schedule.getAction());
        if (!success) return false;

        logService.addLogEntry(device.getName(), device.getRoom(),
                schedule.getAction(), "Schedule: " + schedule.getName());
        return true;
    }

    /**
     * Executes all schedules due for the provided minute and returns the number of
     * schedules that successfully triggered.
     */
    public int processDueSchedules(LocalDateTime now) {
        if (now == null) {
            return 0;
        }

        LocalDateTime minute = now.truncatedTo(ChronoUnit.MINUTES);
        int executedCount = 0;

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

        if (!vacationModeService.isActiveOn(date)) {
            return activeSchedules;
        }

        Schedule selectedVacationSchedule = vacationModeService.getSelectedSchedule();
        if (selectedVacationSchedule == null || !selectedVacationSchedule.isActive()) {
            return activeSchedules;
        }

        List<Schedule> effectiveSchedules = new ArrayList<>();
        effectiveSchedules.add(selectedVacationSchedule);
        return effectiveSchedules;
    }

    private boolean isScheduleDue(Schedule schedule, LocalDateTime now) {
        LocalTime scheduledTime = parseScheduledTime(schedule.getTime());
        if (scheduledTime == null || !scheduledTime.equals(now.toLocalTime())) {
            return false;
        }

        return switch (normalizeRecurrence(schedule.getRecurrence())) {
            case "weekdays" -> isWeekday(now.getDayOfWeek());
            case "weekends" -> isWeekend(now.getDayOfWeek());
            case "weekly" -> matchesWeeklyDay(schedule.getTime(), now.getDayOfWeek());
            default -> true;
        };
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
        if (timePattern == null || timePattern.isBlank()) {
            return null;
        }

        String upper = timePattern.toUpperCase(Locale.ENGLISH);
        String[] tokens = upper.split("\\s+");
        for (int index = 0; index < tokens.length; index++) {
            String candidate = tokens[index];
            if (!candidate.contains(":")) {
                continue;
            }

            if (index + 1 < tokens.length && ("AM".equals(tokens[index + 1]) || "PM".equals(tokens[index + 1]))) {
                LocalTime parsed = parseTimeCandidate(candidate + " " + tokens[index + 1]);
                if (parsed != null) {
                    return parsed;
                }
            }

            LocalTime parsed = parseTimeCandidate(candidate);
            if (parsed != null) {
                return parsed;
            }
        }

        return null;
    }

    private LocalTime parseTimeCandidate(String candidate) {
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(candidate.trim(), formatter).truncatedTo(ChronoUnit.MINUTES);
            } catch (DateTimeParseException ignored) {
                // Try the next known schedule time format.
            }
        }
        return null;
    }

    private DayOfWeek extractDayOfWeek(String pattern) {
        if (pattern == null) {
            return null;
        }

        String upper = pattern.toUpperCase(Locale.ENGLISH);
        for (DayOfWeek day : DayOfWeek.values()) {
            String fullName = day.name();
            String shortName = fullName.substring(0, 3);
            if (upper.contains(fullName) || upper.contains(shortName)) {
                return day;
            }
        }
        return null;
    }

    private boolean applyScheduleAction(at.jku.se.smarthome.model.Device device, String action) {
        if (action == null) return false;
        switch (action) {
            case "Turn On":  device.setState(true);  return true;
            case "Turn Off": device.setState(false); return true;
            case "Open":     device.setState(true);  return true;
            case "Close":    device.setState(false); return true;
            default:
                if ("Dimmer".equalsIgnoreCase(device.getType()) && action.matches("Set( to)? \\d+%")) {
                    int brightness = Integer.parseInt(action.replaceAll("[^0-9]", ""));
                    device.setBrightness(brightness);
                    device.setState(brightness > 0);
                    return true;
                }
                if ("Thermostat".equalsIgnoreCase(device.getType()) && action.matches("Set( to)? \\d+(\\.\\d+)?°C")) {
                    double temperature = Double.parseDouble(action.replaceAll("[^0-9.]", ""));
                    device.setTemperature(temperature);
                    device.setState(true);
                    return true;
                }
                return false;
        }
    }

    /**
     * Checks for conflicts.
     */
    public boolean hasConflicts(String schedId) {
        return false; // Simplified mock
    }
}
