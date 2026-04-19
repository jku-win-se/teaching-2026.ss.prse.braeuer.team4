package at.jku.se.smarthome.service.api;

import at.jku.se.smarthome.model.Schedule;
import javafx.collections.ObservableList;

/**
 * Defines the scheduling operations exposed to the smart-home UI and services.
 */
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public interface ScheduleService {

    /**
     * Returns all schedules currently managed by the service.
     *
     * @return observable list of schedules
     */
    ObservableList<Schedule> getSchedules();

    /**
     * Finds a schedule by its identifier.
     *
     * @param scheduleId unique schedule identifier
     * @return matching schedule, or null when none exists
     */
    Schedule getScheduleById(String scheduleId);

    /**
     * Finds a schedule by its display name.
     *
     * @param scheduleName schedule name to look up
     * @return matching schedule, or null when none exists
     */
    Schedule getScheduleByName(String scheduleName);

    /**
     * Creates and stores a new schedule.
     *
     * @param name schedule name
     * @param deviceId target device identifier
     * @param deviceName target device display name
     * @param action action to execute
     * @param time execution time pattern
     * @param recurrence recurrence mode
     * @param active whether the schedule starts enabled
     * @return created schedule
     */
    Schedule addSchedule(String name, String deviceId, String deviceName, String action,
                         String time, String recurrence, boolean active);

    /**
     * Updates an existing schedule.
     *
     * @param scheduleId identifier of the schedule to update
     * @param name updated schedule name
     * @param deviceId updated target device identifier
     * @param deviceName updated target device display name
     * @param action updated action to execute
     * @param time updated execution time pattern
     * @param recurrence updated recurrence mode
     * @param active updated enabled state
     * @return true when the schedule was updated, otherwise false
     */
    boolean updateSchedule(String scheduleId, String name, String deviceId, String deviceName,
                           String action, String time, String recurrence, boolean active);

    /**
     * Toggles whether a schedule is active.
     *
     * @param scheduleId identifier of the schedule to toggle
     * @return true when the schedule exists and was toggled, otherwise false
     */
    boolean toggleSchedule(String scheduleId);

    /**
     * Deletes a schedule.
     *
     * @param scheduleId identifier of the schedule to remove
     * @return true when the schedule existed and was removed, otherwise false
     */
    boolean deleteSchedule(String scheduleId);

    /**
     * Executes the configured action for a schedule immediately.
     *
     * @param scheduleId identifier of the schedule to execute
     * @return true when the schedule existed and execution succeeded, otherwise false
     */
    boolean executeSchedule(String scheduleId);

    /**
     * Starts background processing for recurring schedules.
     */
    void startRecurringExecution();

    /**
     * Stops background processing for recurring schedules.
     */
    void stopRecurringExecution();

    /**
     * Checks whether a schedule conflicts with existing schedules.
     *
     * @param scheduleId identifier of the schedule being validated
     * @return true when a conflict exists, otherwise false
     */
    boolean hasConflicts(String scheduleId);
}