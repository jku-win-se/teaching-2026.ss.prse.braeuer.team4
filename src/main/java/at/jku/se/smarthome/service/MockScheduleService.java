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
     * Checks for conflicts.
     */
    public boolean hasConflicts(String schedId) {
        return false; // Simplified mock
    }
}
