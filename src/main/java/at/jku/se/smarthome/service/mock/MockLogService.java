package at.jku.se.smarthome.service.mock;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import at.jku.se.smarthome.model.LogEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock Log Service providing activity logging functionality.
 * Records all manual and automated state changes with detailed information.
 */
public class MockLogService {
    
    private static MockLogService instance;
    private final ObservableList<LogEntry> logs;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private MockLogService() {
        this.logs = FXCollections.observableArrayList();
        initializeMockLogs();
    }
    
    public static synchronized MockLogService getInstance() {
        if (instance == null) {
            instance = new MockLogService();
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

    private void initializeMockLogs() {
        LocalDateTime now = LocalDateTime.now();
        logs.add(new LogEntry(now.minusHours(2).format(formatter), 
                             "Main Light", "Living Room", "Turned ON", "User"));
        logs.add(new LogEntry(now.minusHours(1).format(formatter), 
                             "Dimmer Light", "Bedroom", "Set to 50%", "Rule: Morning Routine"));
        logs.add(new LogEntry(now.minusMinutes(30).format(formatter), 
                             "Temperature Control", "Living Room", "Set to 22.5°C", "User"));
        logs.add(new LogEntry(now.minusMinutes(15).format(formatter), 
                             "Kitchen Light", "Kitchen", "Turned OFF", "Schedule: Evening Shutdown"));
        logs.add(new LogEntry(now.format(formatter), 
                             "Main Light", "Living Room", "Dimmed to 75%", "User"));
    }
    
    /**
     * Gets all log entries.
     *
     * @return observable list of log entries
     */
    public ObservableList<LogEntry> getLogs() {
        return logs;
    }
    
    /**
        * Adds a new log entry with room information.
        *
        * @param device device name recorded in the log entry
        * @param room room name recorded in the log entry
        * @param action action description
        * @param actor actor responsible for the action
     */
    public void addLogEntry(String device, String room, String action, String actor) {
        LogEntry entry = new LogEntry(
                LocalDateTime.now().format(formatter),
                device,
                room,
                action,
                actor
        );
        logs.add(0, entry); // Add to beginning
    }
    
    /**
     * Legacy addLogEntry without room information.
     *
     * @param device device name recorded in the log entry
     * @param action action description
     * @param actor actor responsible for the action
     */
    public void addLogEntry(String device, String action, String actor) {
        addLogEntry(device, "Unknown", action, actor);
    }
    
    /**
        * Gets log entries filtered by room.
        *
        * @param room room name to filter by
        * @return observable list of matching log entries
     */
    public ObservableList<LogEntry> getLogsByRoom(String room) {
        return FXCollections.observableArrayList(
            logs.stream()
                .filter(log -> log.getRoom().equals(room))
                .collect(Collectors.toList())
        );
    }
    
    /**
        * Gets log entries filtered by device.
        *
        * @param device device name to filter by
        * @return observable list of matching log entries
     */
    public ObservableList<LogEntry> getLogsByDevice(String device) {
        return FXCollections.observableArrayList(
            logs.stream()
                .filter(log -> log.getDevice().equals(device))
                .collect(Collectors.toList())
        );
    }
    
    /**
        * Gets log entries filtered by actor.
        *
        * @param actor actor name to filter by
        * @return observable list of matching log entries
     */
    public ObservableList<LogEntry> getLogsByActor(String actor) {
        return FXCollections.observableArrayList(
            logs.stream()
                .filter(log -> log.getActor().equals(actor))
                .collect(Collectors.toList())
        );
    }
    
    /**
        * Gets all unique device names from logs.
        *
        * @return observable list of unique device names
     */
    public ObservableList<String> getUniqueDevices() {
        return FXCollections.observableArrayList(
            logs.stream()
                .map(LogEntry::getDevice)
                .distinct()
                .sorted()
                .collect(Collectors.toList())
        );
    }
    
    /**
        * Exports the current logs to CSV format.
        *
        * @return CSV representation of the current logs
     */
    public String exportToCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,Device,Room,Action,Actor\n");
        
        for (LogEntry log : logs) {
            csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    log.getTimestamp(),
                    log.getDevice(),
                    log.getRoom(),
                    log.getAction(),
                    log.getActor()));
        }
        
        return csv.toString();
    }
}
