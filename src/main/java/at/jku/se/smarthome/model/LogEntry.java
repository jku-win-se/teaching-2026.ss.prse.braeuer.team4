package at.jku.se.smarthome.model;

import javafx.beans.property.SimpleStringProperty;

/**
 * Represents a Log Entry for activity tracking.
 * Records state changes with timestamp, device, room, action, and actor information.
 */
public class LogEntry {
    
    /** Timestamp of the logged action. */
    private final SimpleStringProperty timestamp;
    /** Name of the device affected. */
    private final SimpleStringProperty device;
    /** Room containing the affected device. */
    private final SimpleStringProperty room;
    /** Action that occurred. */
    private final SimpleStringProperty action;
    /** Actor responsible for triggering the action. */
    private final SimpleStringProperty actor;
    
    /**
        * Creates a log entry with explicit room information.
        *
        * @param timestamp time of the logged action
        * @param device device affected by the action
        * @param room room containing the device
        * @param action action that occurred
        * @param actor actor that triggered the action
     */
    public LogEntry(String timestamp, String device, String room, String action, String actor) {
        this.timestamp = new SimpleStringProperty(timestamp);
        this.device = new SimpleStringProperty(device);
        this.room = new SimpleStringProperty(room);
        this.action = new SimpleStringProperty(action);
        this.actor = new SimpleStringProperty(actor);
    }
    
    /**
     * Creates a log entry when room information is unavailable.
     *
     * @param timestamp time of the logged action
     * @param device device affected by the action
     * @param action action that occurred
     * @param actor actor that triggered the action
     */
    public LogEntry(String timestamp, String device, String action, String actor) {
        this(timestamp, device, "Unknown", action, actor);
    }

    /**
     * Returns the log timestamp.
     *
     * @return timestamp text
     */
    public String getTimestamp() { return timestamp.get(); }
    /**
     * Exposes the JavaFX timestamp property.
     *
     * @return timestamp property
     */
    public SimpleStringProperty timestampProperty() { return timestamp; }

    /**
     * Returns the device name stored in the log entry.
     *
     * @return device name
     */
    public String getDevice() { return device.get(); }
    /**
     * Exposes the JavaFX device property.
     *
     * @return device property
     */
    public SimpleStringProperty deviceProperty() { return device; }

    /**
     * Returns the room name stored in the log entry.
     *
     * @return room name
     */
    public String getRoom() { return room.get(); }
    /**
     * Exposes the JavaFX room property.
     *
     * @return room property
     */
    public SimpleStringProperty roomProperty() { return room; }

    /**
     * Returns the recorded action.
     *
     * @return action description
     */
    public String getAction() { return action.get(); }
    /**
     * Exposes the JavaFX action property.
     *
     * @return action property
     */
    public SimpleStringProperty actionProperty() { return action; }

    /**
     * Returns the actor responsible for the log entry.
     *
     * @return actor name
     */
    public String getActor() { return actor.get(); }
    /**
     * Exposes the JavaFX actor property.
     *
     * @return actor property
     */
    public SimpleStringProperty actorProperty() { return actor; }
}
