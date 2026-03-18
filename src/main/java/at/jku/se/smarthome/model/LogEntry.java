package at.jku.se.smarthome.model;

import javafx.beans.property.SimpleStringProperty;

/**
 * Represents a Log Entry for activity tracking.
 * Records state changes with timestamp, device, room, action, and actor information.
 */
public class LogEntry {
    
    private final SimpleStringProperty timestamp;
    private final SimpleStringProperty device;
    private final SimpleStringProperty room;
    private final SimpleStringProperty action;
    private final SimpleStringProperty actor;
    
    /**
     * Constructor with all fields including room.
     */
    public LogEntry(String timestamp, String device, String room, String action, String actor) {
        this.timestamp = new SimpleStringProperty(timestamp);
        this.device = new SimpleStringProperty(device);
        this.room = new SimpleStringProperty(room);
        this.action = new SimpleStringProperty(action);
        this.actor = new SimpleStringProperty(actor);
    }
    
    /**
     * Legacy constructor for backwards compatibility (without room).
     */
    public LogEntry(String timestamp, String device, String action, String actor) {
        this(timestamp, device, "Unknown", action, actor);
    }
    
    public String getTimestamp() { return timestamp.get(); }
    public SimpleStringProperty timestampProperty() { return timestamp; }
    
    public String getDevice() { return device.get(); }
    public SimpleStringProperty deviceProperty() { return device; }
    
    public String getRoom() { return room.get(); }
    public SimpleStringProperty roomProperty() { return room; }
    
    public String getAction() { return action.get(); }
    public SimpleStringProperty actionProperty() { return action; }
    
    public String getActor() { return actor.get(); }
    public SimpleStringProperty actorProperty() { return actor; }
}
