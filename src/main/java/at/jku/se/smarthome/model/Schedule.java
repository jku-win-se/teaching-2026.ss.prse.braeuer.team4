package at.jku.se.smarthome.model;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Represents a Schedule in the SmartHome system.
 */
public class Schedule {
    
    private final SimpleStringProperty id;
    private final SimpleStringProperty name;
    private final SimpleStringProperty deviceId;
    private final SimpleStringProperty device;
    private final SimpleStringProperty action;
    private final SimpleStringProperty time;
    private final SimpleStringProperty recurrence;
    private final SimpleBooleanProperty active;
    
    /**
     * Creates a schedule with explicit device identifier and name.
     *
     * @param id unique schedule identifier
     * @param name schedule name
     * @param deviceId target device identifier
     * @param device target device display name
     * @param action action to execute
     * @param time execution time pattern
     * @param recurrence recurrence mode
     * @param active whether the schedule is active
     */
    public Schedule(String id, String name, String deviceId, String device, String action,
                   String time, String recurrence, boolean active) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.deviceId = new SimpleStringProperty(deviceId);
        this.device = new SimpleStringProperty(device);
        this.action = new SimpleStringProperty(action);
        this.time = new SimpleStringProperty(time);
        this.recurrence = new SimpleStringProperty(recurrence);
        this.active = new SimpleBooleanProperty(active);
    }

    /**
     * Creates a schedule using the same value for device identifier and display name.
     *
     * @param id unique schedule identifier
     * @param name schedule name
     * @param device target device identifier and name
     * @param action action to execute
     * @param time execution time pattern
     * @param recurrence recurrence mode
     * @param active whether the schedule is active
     */
    public Schedule(String id, String name, String device, String action,
                   String time, String recurrence, boolean active) {
        this(id, name, device, device, action, time, recurrence, active);
    }

    /**
     * Returns the schedule identifier.
     *
     * @return schedule identifier
     */
    public String getId() { return id.get(); }
    /**
     * Exposes the JavaFX id property.
     *
     * @return id property
     */
    public SimpleStringProperty idProperty() { return id; }

    /**
     * Returns the schedule name.
     *
     * @return schedule name
     */
    public String getName() { return name.get(); }
    /**
     * Updates the schedule name.
     *
     * @param name updated schedule name
     */
    public void setName(String name) { this.name.set(name); }
    /**
     * Exposes the JavaFX name property.
     *
     * @return name property
     */
    public SimpleStringProperty nameProperty() { return name; }

    /**
     * Returns the target device identifier.
     *
     * @return target device identifier
     */
    public String getDeviceId() { return deviceId.get(); }
    /**
     * Updates the target device identifier.
     *
     * @param deviceId updated target device identifier
     */
    public void setDeviceId(String deviceId) { this.deviceId.set(deviceId); }
    /**
     * Exposes the JavaFX device-id property.
     *
     * @return device-id property
     */
    public SimpleStringProperty deviceIdProperty() { return deviceId; }

    /**
     * Returns the target device display name.
     *
     * @return target device name
     */
    public String getDevice() { return device.get(); }
    /**
     * Updates the target device display name.
     *
     * @param device updated target device name
     */
    public void setDevice(String device) { this.device.set(device); }
    /**
     * Exposes the JavaFX device property.
     *
     * @return device property
     */
    public SimpleStringProperty deviceProperty() { return device; }

    /**
     * Returns the configured action.
     *
     * @return action text
     */
    public String getAction() { return action.get(); }
    /**
     * Updates the configured action.
     *
     * @param action updated action text
     */
    public void setAction(String action) { this.action.set(action); }
    /**
     * Exposes the JavaFX action property.
     *
     * @return action property
     */
    public SimpleStringProperty actionProperty() { return action; }

    /**
     * Returns the schedule time pattern.
     *
     * @return time pattern
     */
    public String getTime() { return time.get(); }
    /**
     * Updates the schedule time pattern.
     *
     * @param time updated time pattern
     */
    public void setTime(String time) { this.time.set(time); }
    /**
     * Exposes the JavaFX time property.
     *
     * @return time property
     */
    public SimpleStringProperty timeProperty() { return time; }

    /**
     * Returns the recurrence mode.
     *
     * @return recurrence mode
     */
    public String getRecurrence() { return recurrence.get(); }
    /**
     * Updates the recurrence mode.
     *
     * @param recurrence updated recurrence mode
     */
    public void setRecurrence(String recurrence) { this.recurrence.set(recurrence); }
    /**
     * Exposes the JavaFX recurrence property.
     *
     * @return recurrence property
     */
    public SimpleStringProperty recurrenceProperty() { return recurrence; }

    /**
     * Returns whether the schedule is active.
     *
     * @return true when active, otherwise false
     */
    public boolean isActive() { return active.get(); }
    /**
     * Updates whether the schedule is active.
     *
     * @param active updated active state
     */
    public void setActive(boolean active) { this.active.set(active); }
    /**
     * Exposes the JavaFX active property.
     *
     * @return active property
     */
    public SimpleBooleanProperty activeProperty() { return active; }
}
