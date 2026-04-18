package at.jku.se.smarthome.service.mock;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import at.jku.se.smarthome.model.Schedule;
import at.jku.se.smarthome.model.VacationModeConfig;
import at.jku.se.smarthome.service.api.ScheduleService;
import at.jku.se.smarthome.service.api.ServiceRegistry;

/**
 * Mock vacation mode service for FR-21.
 * Stores a selected schedule and date range and exposes the effective override state.
 */
public final class MockVacationModeService {

    /** Lock for singleton synchronization. */
    private static final Object INSTANCE_LOCK = new Object();
    /** Singleton instance. */
    private static MockVacationModeService instance;

    /** Room service for room operations. */
    private final MockRoomService roomService = MockRoomService.getInstance();
    /** Log service for activity logging. */
    private final MockLogService logService = MockLogService.getInstance();
    /** Notification service for alerts. */
    private final MockNotificationService notificationService = MockNotificationService.getInstance();
    /** Date formatter for vacation mode dates. */
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Vacation mode configuration. */
    private final VacationModeConfig configuration = new VacationModeConfig(false, null, null, null);

    /** Private constructor for singleton pattern. */
    private MockVacationModeService() {
    }

    /**
     * Gets the singleton instance.
     *
     * @return the instance
     */
    public static MockVacationModeService getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new MockVacationModeService();
            }
            return instance;
        }
    }

    /**
     * Resets the singleton instance for testing.
     */
    public static void resetForTesting() {
        synchronized (INSTANCE_LOCK) {
            instance = null;
        }
    }

    /**
     * Gets the vacation mode configuration.
     *
     * @return the configuration
     */
    public VacationModeConfig getConfiguration() {
        return configuration;
    }

    /**
     * Checks if vacation mode is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return configuration.isEnabled() && configuration.isConfigured();
    }

    /**
     * Checks if vacation mode is active on the specified date.
     *
     * @param date the date
     * @return true if active on date
     */
    public boolean isActiveOn(LocalDate date) {
        return isEnabled() && date != null && !date.isBefore(configuration.getStartDate()) && !date.isAfter(configuration.getEndDate());
    }

    /**
     * Gets the selected vacation schedule.
     *
     * @return the schedule or null
     */
    public Schedule getSelectedSchedule() {
        return configuration.getScheduleId() != null ? getScheduleService().getScheduleById(configuration.getScheduleId()) : null;
    }

    /**
     * Gets the schedules overridden by vacation mode.
     *
     * @return list of overridden schedules
     */
    public List<Schedule> getOverriddenSchedules() {
        Schedule selectedSchedule = getSelectedSchedule();
        String selectedScheduleId = selectedSchedule != null ? selectedSchedule.getId() : null;

        return getScheduleService().getSchedules().stream()
                .filter(Schedule::isActive)
                .filter(schedule -> selectedScheduleId == null || !schedule.getId().equals(selectedScheduleId))
                .toList();
    }

    /**
     * Activates vacation mode with specified schedule and dates.
     *
     * @param startDate start date
     * @param endDate end date
     * @param schedule the selected schedule
     * @param actor actor triggering activation
     */
    public void activateVacationMode(LocalDate startDate, LocalDate endDate, Schedule schedule, String actor) {
        configuration.setEnabled(true);
        configuration.setStartDate(startDate);
        configuration.setEndDate(endDate);
        configuration.setScheduleId(schedule.getId());

        int overriddenSchedules = getOverriddenSchedules().size();
        String roomName = roomService.getDeviceByName(schedule.getDevice()) != null
                ? roomService.getDeviceByName(schedule.getDevice()).getRoom()
                : "System";

        notificationService.addNotification(
                String.format(
                        "Vacation mode enabled: '%s' overrides %d normal schedule(s) from %s to %s",
                        schedule.getName(),
                        overriddenSchedules,
                        formatDate(startDate),
                        formatDate(endDate)
                ),
                "info"
        );
        logService.addLogEntry(
                schedule.getDevice(),
                roomName,
                String.format(
                        "Vacation mode enabled with schedule '%s' from %s to %s",
                        schedule.getName(),
                        formatDate(startDate),
                        formatDate(endDate)
                ),
                actor
        );
    }

    /**
     * Deactivates vacation mode.
     *
     * @param actor actor triggering deactivation
     */
    public void deactivateVacationMode(String actor) {
        Schedule selectedSchedule = getSelectedSchedule();
        String scheduleName = selectedSchedule != null ? selectedSchedule.getName() : "Unavailable schedule";
        String deviceName = selectedSchedule != null ? selectedSchedule.getDevice() : "Vacation Mode";
        String roomName = selectedSchedule != null && roomService.getDeviceByName(selectedSchedule.getDevice()) != null
                ? roomService.getDeviceByName(selectedSchedule.getDevice()).getRoom()
                : "System";

        notificationService.addNotification(
                "Vacation mode deactivated. Normal schedules are active again.",
                "success"
        );
        logService.addLogEntry(
                deviceName,
                roomName,
                String.format("Vacation mode deactivated for schedule '%s'", scheduleName),
                actor
        );

        clearConfiguration();
    }

    /**
     * Clears vacation mode if using specified schedule.
     *
     * @param scheduleId schedule ID
     * @param reason reason for clearing
     */
    public void clearIfUsingSchedule(String scheduleId, String reason) {
        if (scheduleId == null || !scheduleId.equals(configuration.getScheduleId())) {
            return;
        }

        clearConfiguration();
        notificationService.addNotification(
                "Vacation mode was cleared because the selected schedule is no longer available.",
                "warning"
        );
        logService.addLogEntry("Vacation Mode", "System", reason, "System");
    }

    /**
     * Gets the status summary.
     *
     * @return status summary
     */
    public String getStatusSummary() {
        Schedule selectedSchedule = getSelectedSchedule();
        return (!isEnabled() || selectedSchedule == null)
            ? "Vacation mode is currently disabled."
            : String.format(
                "Active from %s to %s using '%s'.",
                formatDate(configuration.getStartDate()),
                formatDate(configuration.getEndDate()),
                selectedSchedule.getName()
            );
    }

    /**
     * Gets the override summary.
     *
     * @return override summary
     */
    public String getOverrideSummary() {
        Schedule selectedSchedule = getSelectedSchedule();
        String result = "When enabled, the chosen schedule becomes the effective schedule for the selected date range.";
        if (isEnabled() && selectedSchedule != null) {
            int overriddenSchedules = getOverriddenSchedules().size();
            result = String.format(
                "Normal schedules are overridden during the configured period. '%s' is treated as the effective vacation schedule and replaces %d other active schedule(s).",
                selectedSchedule.getName(),
                overriddenSchedules
            );
        }
        return result;
    }

    /**
     * Clears the configuration.
     */
    private void clearConfiguration() {
        configuration.setEnabled(false);
        configuration.setStartDate(null);
        configuration.setEndDate(null);
        configuration.setScheduleId(null);
    }

    /**
     * Formats the date for display.
     *
     * @param date the date
     * @return formatted date
     */
    private String formatDate(LocalDate date) {
        return date != null ? date.format(dateFormatter) : "-";
    }

    /**
     * Gets the schedule service.
     *
     * @return the schedule service
     */
    private ScheduleService getScheduleService() {
        return ServiceRegistry.getScheduleService();
    }
}