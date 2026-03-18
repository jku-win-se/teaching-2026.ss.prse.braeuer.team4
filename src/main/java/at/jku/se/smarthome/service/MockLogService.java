package at.jku.se.smarthome.service;

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
     */
    public ObservableList<LogEntry> getLogs() {
        return logs;
    }
    
    /**
     * Adds a new log entry with room information.
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
     * Legacy addLogEntry without room (for backwards compatibility).
     */
    public void addLogEntry(String device, String action, String actor) {
        addLogEntry(device, "Unknown", action, actor);
    }
    
    /**
     * Gets log entries filtered by room.
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
     */
    public ObservableList<LogEntry> getLogsByDevice(String device) {
        return FXCollections.observableArrayList(
            logs.stream()
                .filter(log -> log.getDevice().equals(device))
                .collect(Collectors.toList())
        );
    }
    
    /**
     * Gets log entries filtered by actor (user or rule/automation).
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
     * Exports logs to CSV format.
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
