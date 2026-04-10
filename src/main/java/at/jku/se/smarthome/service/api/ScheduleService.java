package at.jku.se.smarthome.service.api;

import at.jku.se.smarthome.model.Schedule;
import javafx.collections.ObservableList;

public interface ScheduleService {

    ObservableList<Schedule> getSchedules();

    Schedule getScheduleById(String scheduleId);

    Schedule getScheduleByName(String scheduleName);

    Schedule addSchedule(String name, String deviceId, String deviceName, String action,
                         String time, String recurrence, boolean active);

    boolean updateSchedule(String scheduleId, String name, String deviceId, String deviceName,
                           String action, String time, String recurrence, boolean active);

    boolean toggleSchedule(String scheduleId);

    boolean deleteSchedule(String scheduleId);

    boolean executeSchedule(String scheduleId);

    void startRecurringExecution();

    void stopRecurringExecution();

    boolean hasConflicts(String scheduleId);
}