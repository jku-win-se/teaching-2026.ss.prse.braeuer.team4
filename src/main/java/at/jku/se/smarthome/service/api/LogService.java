package at.jku.se.smarthome.service.api;

import at.jku.se.smarthome.model.LogEntry;
import javafx.collections.ObservableList;

/**
 * Defines operations for recording and querying activity log entries.
 */
public interface LogService {

    /**
     * Returns all log entries, most recent first.
     *
     * @return observable list of log entries
     */
    ObservableList<LogEntry> getLogs();

    /**
     * Records a new log entry with room information.
     *
     * @param device device name
     * @param room   room name
     * @param action action description
     * @param actor  actor responsible (e.g. "User", "Rule: Morning", "Schedule: Evening")
     */
    void addLogEntry(String device, String room, String action, String actor);

    /**
     * Records a new log entry without room information (room defaults to "Unknown").
     *
     * @param device device name
     * @param action action description
     * @param actor  actor responsible
     */
    void addLogEntry(String device, String action, String actor);

    /**
     * Returns log entries filtered by room name.
     *
     * @param room room name to filter by
     * @return matching log entries
     */
    ObservableList<LogEntry> getLogsByRoom(String room);

    /**
     * Returns log entries filtered by device name.
     *
     * @param device device name to filter by
     * @return matching log entries
     */
    ObservableList<LogEntry> getLogsByDevice(String device);

    /**
     * Returns log entries filtered by actor.
     *
     * @param actor actor name to filter by
     * @return matching log entries
     */
    ObservableList<LogEntry> getLogsByActor(String actor);

    /**
     * Returns all unique device names that appear in the log.
     *
     * @return sorted list of unique device names
     */
    ObservableList<String> getUniqueDevices();

    /**
     * Exports all log entries as a CSV string.
     *
     * @return CSV-formatted string with header row
     */
    String exportToCSV();
}
