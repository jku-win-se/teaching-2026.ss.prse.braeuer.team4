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
public class MockVacationModeService {

    private static MockVacationModeService instance;

    private final MockRoomService roomService = MockRoomService.getInstance();
    private final MockLogService logService = MockLogService.getInstance();
    private final MockNotificationService notificationService = MockNotificationService.getInstance();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final VacationModeConfig configuration = new VacationModeConfig(false, null, null, null);

    private MockVacationModeService() {
    }

    public static synchronized MockVacationModeService getInstance() {
        if (instance == null) {
            instance = new MockVacationModeService();
        }
        return instance;
    }

    public static synchronized void resetForTesting() {
        instance = null;
    }

    public VacationModeConfig getConfiguration() {
        return configuration;
    }

    public boolean isEnabled() {
        return configuration.isEnabled() && configuration.isConfigured();
    }

    public boolean isActiveOn(LocalDate date) {
        if (!isEnabled() || date == null) {
            return false;
        }
        return !date.isBefore(configuration.getStartDate()) && !date.isAfter(configuration.getEndDate());
    }

    public Schedule getSelectedSchedule() {
        if (configuration.getScheduleId() == null) {
            return null;
        }
        return getScheduleService().getScheduleById(configuration.getScheduleId());
    }

    public List<Schedule> getOverriddenSchedules() {
        Schedule selectedSchedule = getSelectedSchedule();
        String selectedScheduleId = selectedSchedule != null ? selectedSchedule.getId() : null;

        return getScheduleService().getSchedules().stream()
                .filter(Schedule::isActive)
                .filter(schedule -> selectedScheduleId == null || !schedule.getId().equals(selectedScheduleId))
                .toList();
    }

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

    public String getStatusSummary() {
        Schedule selectedSchedule = getSelectedSchedule();
        if (!isEnabled() || selectedSchedule == null) {
            return "Vacation mode is currently disabled.";
        }

        return String.format(
                "Active from %s to %s using '%s'.",
                formatDate(configuration.getStartDate()),
                formatDate(configuration.getEndDate()),
                selectedSchedule.getName()
        );
    }

    public String getOverrideSummary() {
        Schedule selectedSchedule = getSelectedSchedule();
        if (!isEnabled() || selectedSchedule == null) {
            return "When enabled, the chosen schedule becomes the effective schedule for the selected date range.";
        }

        int overriddenSchedules = getOverriddenSchedules().size();
        return String.format(
                "Normal schedules are overridden during the configured period. '%s' is treated as the effective vacation schedule and replaces %d other active schedule(s).",
                selectedSchedule.getName(),
                overriddenSchedules
        );
    }

    private void clearConfiguration() {
        configuration.setEnabled(false);
        configuration.setStartDate(null);
        configuration.setEndDate(null);
        configuration.setScheduleId(null);
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(dateFormatter) : "-";
    }

    private ScheduleService getScheduleService() {
        return ServiceRegistry.getScheduleService();
    }
}