package at.jku.se.smarthome.service;

import at.jku.se.smarthome.model.Schedule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock Schedule Service providing schedule management functionality.
 */
public class MockScheduleService {
    
    private static MockScheduleService instance;
    private final ObservableList<Schedule> schedules;
    private final MockRoomService roomService = MockRoomService.getInstance();
    private final MockLogService logService = MockLogService.getInstance();

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
    static synchronized void resetForTesting() {
        instance = null;
    }

    private void initializeMockSchedules() {
        schedules.add(new Schedule("sched-001", "Morning Lights", "Living Room Light", 
                                  "Turn On", "06:00 AM", "Daily", true));
        schedules.add(new Schedule("sched-002", "Bedtime", "Bedroom Light", 
                                  "Turn Off", "11:00 PM", "Daily", true));
        schedules.add(new Schedule("sched-003", "Weekend Relax", "Living Room Dimmer", 
                                  "Set 50%", "18:00", "Weekly", false));
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
    public Schedule addSchedule(String name, String device, String action, 
                               String time, String recurrence, boolean active) {
        Schedule schedule = new Schedule(
                "sched-" + String.format("%03d", schedules.size() + 1),
                name,
                device,
                action,
                time,
                recurrence,
                active
        );
        schedules.add(schedule);
        return schedule;
    }
    
    /**
     * Updates a schedule.
     */
    public boolean updateSchedule(String schedId, String name, String device, 
                                 String action, String time, String recurrence) {
        Schedule schedule = schedules.stream()
                .filter(s -> s.getId().equals(schedId))
                .findFirst()
                .orElse(null);
        
        if (schedule != null) {
            schedule.setName(name);
            schedule.setDevice(device);
            schedule.setAction(action);
            schedule.setTime(time);
            schedule.setRecurrence(recurrence);
            return true;
        }
        return false;
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

        at.jku.se.smarthome.model.Device device = roomService.getDeviceByName(schedule.getDevice());
        if (device == null) return false;

        boolean success = applyScheduleAction(device, schedule.getAction());
        if (!success) return false;

        logService.addLogEntry(device.getName(), device.getRoom(),
                schedule.getAction(), "Schedule: " + schedule.getName());
        return true;
    }

    private boolean applyScheduleAction(at.jku.se.smarthome.model.Device device, String action) {
        if (action == null) return false;
        switch (action) {
            case "Turn On":  device.setState(true);  return true;
            case "Turn Off": device.setState(false); return true;
            case "Open":     device.setState(true);  return true;
            case "Close":    device.setState(false); return true;
            default:
                if (action.matches("Set \\d+%")) {
                    int brightness = Integer.parseInt(action.substring(4, action.length() - 1));
                    device.setBrightness(brightness);
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
