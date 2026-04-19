package at.jku.se.smarthome.service.real.schedule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import at.jku.se.smarthome.config.DatabaseConfig;
import at.jku.se.smarthome.config.DatabaseSettings;
import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Schedule;
import at.jku.se.smarthome.service.api.LogService;
import at.jku.se.smarthome.service.api.RoomService;
import at.jku.se.smarthome.service.api.ScheduleService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.mock.MockNotificationService;
import at.jku.se.smarthome.service.mock.MockVacationModeService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * JDBC-based schedule service for persistent schedule storage and recurring execution.
 */
@SuppressWarnings({"PMD.DoNotUseThreads", "PMD.TooManyMethods", "PMD.UseObjectForClearerAPI", "PMD.CyclomaticComplexity", "PMD.CouplingBetweenObjects", "PMD.ExcessiveImports"})
public final class JdbcScheduleService implements ScheduleService {

    /** Path to schedule initialization SQL script. */
    private static final String INIT_SCRIPT_PATH = "/db/init-schedules.sql";
    /** Time formatters for parsing various time formats. */
    private static final List<DateTimeFormatter> TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
    );
    /** Lock for singleton synchronization. */
    private static final Object INSTANCE_LOCK = new Object();

    /** Singleton instance. */
    private static JdbcScheduleService instance;

    /** Observable list of all schedules. */
    private final ObservableList<Schedule> schedules = FXCollections.observableArrayList();
    /** Room service for room data access. */
    private final RoomService roomService = ServiceRegistry.getRoomService();
    /** Log service for activity logging. */
    private final LogService logService = ServiceRegistry.getLogService();
    /** Notification service for alerts. */
    private final MockNotificationService notificationService = MockNotificationService.getInstance();
    /** Tracks last processed minute for each schedule (prevents duplicate execution). */
    private final Map<String, LocalDateTime> lastProcessedMinuteByScheduleId = new ConcurrentHashMap<>();
    /** Flag indicating database schema is ready. */
    private final AtomicBoolean schemaReady = new AtomicBoolean(false);
    /** Executor service for recurring schedule execution. */
    private ScheduledExecutorService scheduler;

    private JdbcScheduleService() {
        refreshSchedules();
    }

    public static JdbcScheduleService getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new JdbcScheduleService();
            }
            return instance;
        }
    }

    /**
     * Resets the singleton for unit testing.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public static void resetForTesting() {
        synchronized (INSTANCE_LOCK) {
            if (instance != null) {
                instance.stopRecurringExecution();
            }
            instance = null;
        }
    }

    @Override
    public ObservableList<Schedule> getSchedules() {
        return schedules;
    }

    @Override
    public Schedule getScheduleById(String scheduleId) {
        return schedules.stream()
                .filter(schedule -> schedule.getId().equals(scheduleId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Schedule getScheduleByName(String scheduleName) {
        return schedules.stream()
                .filter(schedule -> schedule.getName().equals(scheduleName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Schedule addSchedule(String name, String deviceId, String deviceName, String action,
                                             String time, String recurrence, boolean active) {
        synchronized (this) {
            Schedule schedule = new Schedule(
                    UUID.randomUUID().toString(),
                    name,
                    deviceId,
                    deviceName,
                    action,
                    time,
                    recurrence,
                    active
            );

            try (Connection connection = openConnection()) {
                ensureSchema(connection);
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO scheduled_actions (id, name, device_id, device_name, action, time_pattern, recurrence, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    bindSchedule(statement, schedule);
                    statement.executeUpdate();
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to persist the schedule.", exception);
            }

            schedules.add(schedule);
            return schedule;
        }
    }

    @Override
    public boolean updateSchedule(String scheduleId, String name, String deviceId, String deviceName,
                                               String action, String time, String recurrence, boolean active) {
        synchronized (this) {
            boolean updated = false;
            try (Connection connection = openConnection()) {
                ensureSchema(connection);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE scheduled_actions SET name = ?, device_id = ?, device_name = ?, action = ?, time_pattern = ?, recurrence = ?, active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                    statement.setString(1, name);
                    statement.setString(2, deviceId);
                    statement.setString(3, deviceName);
                    statement.setString(4, action);
                    statement.setString(5, time);
                    statement.setString(6, recurrence);
                    statement.setBoolean(7, active);
                    statement.setString(8, scheduleId);
                    if (statement.executeUpdate() > 0) {
                        updated = true;
                    }
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to update the schedule.", exception);
            }

            if (updated) {
                Schedule schedule = getScheduleById(scheduleId);
                if (schedule != null) {
                    schedule.setName(name);
                    schedule.setDeviceId(deviceId);
                    schedule.setDevice(deviceName);
                    schedule.setAction(action);
                    schedule.setTime(time);
                    schedule.setRecurrence(recurrence);
                    schedule.setActive(active);
                }
            }
            return updated;
        }
    }

    @Override
    public boolean toggleSchedule(String scheduleId) {
        synchronized (this) {
            boolean toggled = false;
            Schedule schedule = getScheduleById(scheduleId);
            if (schedule != null) {
                toggled = updateSchedule(
                        schedule.getId(),
                        schedule.getName(),
                        schedule.getDeviceId(),
                        schedule.getDevice(),
                        schedule.getAction(),
                        schedule.getTime(),
                        schedule.getRecurrence(),
                        !schedule.isActive()
                );
            }
            return toggled;
        }
    }

    @Override
    public boolean deleteSchedule(String scheduleId) {
        synchronized (this) {
            boolean deleted = false;
            try (Connection connection = openConnection()) {
                ensureSchema(connection);
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM scheduled_actions WHERE id = ?")) {
                    statement.setString(1, scheduleId);
                    if (statement.executeUpdate() > 0) {
                        deleted = true;
                    }
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to delete the schedule.", exception);
            }

            if (deleted) {
                schedules.removeIf(schedule -> schedule.getId().equals(scheduleId));
                lastProcessedMinuteByScheduleId.remove(scheduleId);
                MockVacationModeService.getInstance().clearIfUsingSchedule(
                        scheduleId,
                        "Selected vacation schedule was deleted"
                );
            }
            return deleted;
        }
    }

    @Override
    public boolean executeSchedule(String scheduleId) {
        boolean executed = false;
        Schedule schedule = getScheduleById(scheduleId);
        if (schedule != null && schedule.isActive()) {
            Device device = resolveDevice(schedule);
            if (device != null && applyScheduleAction(device, schedule.getAction())) {
                logService.addLogEntry(device.getName(), device.getRoom(), schedule.getAction(), "Schedule: " + schedule.getName());
                notificationService.addNotification("Executed schedule '" + schedule.getName() + "'", "info");
                updateLastTriggered(scheduleId);
                executed = true;
            }
        }
        return executed;
    }

    @Override
    public void startRecurringExecution() {
        synchronized (this) {
            boolean needsStart = scheduler == null || scheduler.isShutdown();
            if (needsStart) {
                ThreadFactory threadFactory = runnable -> {
                    Thread thread = new Thread(runnable, "jdbc-schedule-dispatcher");
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
    }

    @Override
    @SuppressWarnings("PMD.NullAssignment")
    public void stopRecurringExecution() {
        synchronized (this) {
            if (scheduler != null) {
                scheduler.shutdownNow();
                scheduler = null;
            }
        }
    }

    @Override
    public boolean hasConflicts(String scheduleId) {
        return false;
    }

    private void refreshSchedules() {
        List<Schedule> loadedSchedules = new ArrayList<>();

        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, name, device_id, device_name, action, time_pattern, recurrence, active FROM scheduled_actions ORDER BY created_at, name")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        loadedSchedules.add(new Schedule(
                                resultSet.getString("id"),
                                resultSet.getString("name"),
                                resultSet.getString("device_id"),
                                resultSet.getString("device_name"),
                                resultSet.getString("action"),
                                resultSet.getString("time_pattern"),
                                resultSet.getString("recurrence"),
                                resultSet.getBoolean("active")
                        ));
                    }
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load schedules from the database.", exception);
        }

        schedules.setAll(loadedSchedules);
    }

    private int processDueSchedules(LocalDateTime now) {
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

    private List<Schedule> getEffectiveSchedules(LocalDate date) {
        MockVacationModeService vacationModeService = MockVacationModeService.getInstance();

        List<Schedule> activeSchedules = schedules.stream()
                .filter(Schedule::isActive)
                .toList();

        List<Schedule> effectiveSchedules = activeSchedules;
        if (vacationModeService.isActiveOn(date)) {
            Schedule selectedVacationSchedule = vacationModeService.getSelectedSchedule();
            if (selectedVacationSchedule != null && selectedVacationSchedule.isActive()) {
                effectiveSchedules = List.of(selectedVacationSchedule);
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
                // Try next supported format.
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

    private Device resolveDevice(Schedule schedule) {
        Device device = roomService.getDeviceById(schedule.getDeviceId());
        if (device == null) {
            device = roomService.getDeviceByName(schedule.getDevice());
        }
        return device;
    }

    private boolean applyScheduleAction(Device device, String action) {
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

    private void updateLastTriggered(String scheduleId) {
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE scheduled_actions SET last_triggered_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                statement.setString(1, scheduleId);
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            System.getLogger(JdbcScheduleService.class.getName()).log(
                    System.Logger.Level.WARNING,
                    "Schedule action executed but failed to update last-triggered metadata.",
                    exception
            );
        }
    }

    private void bindSchedule(PreparedStatement statement, Schedule schedule) throws SQLException {
        statement.setString(1, schedule.getId());
        statement.setString(2, schedule.getName());
        statement.setString(3, schedule.getDeviceId());
        statement.setString(4, schedule.getDevice());
        statement.setString(5, schedule.getAction());
        statement.setString(6, schedule.getTime());
        statement.setString(7, schedule.getRecurrence());
        statement.setBoolean(8, schedule.isActive());
    }

    private Connection openConnection() throws SQLException {
        DatabaseSettings settings = DatabaseConfig.load()
                .orElseThrow(() -> new IllegalStateException("Schedule database is not configured."));
        return DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password());
    }

    private void ensureSchema(Connection connection) {
        boolean needsSchema = !schemaReady.get();
        if (needsSchema) {
            synchronized (this) {
                if (!schemaReady.get()) {
                    try (Statement statement = connection.createStatement()) {
                        for (String sqlStatement : loadInitScript().split(";")) {
                            String trimmedStatement = sqlStatement.trim();
                            if (!trimmedStatement.isEmpty()) {
                                statement.execute(trimmedStatement);
                            }
                        }
                        schemaReady.set(true);
                    } catch (SQLException exception) {
                        throw new IllegalStateException("Failed to initialize the schedules schema.", exception);
                    }
                }
            }
        }
    }

    private String loadInitScript() {
        try (InputStream inputStream = getClass().getResourceAsStream(INIT_SCRIPT_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Schedule schema script was not found at " + INIT_SCRIPT_PATH + ".");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read the schedule schema script.", exception);
        }
    }
}