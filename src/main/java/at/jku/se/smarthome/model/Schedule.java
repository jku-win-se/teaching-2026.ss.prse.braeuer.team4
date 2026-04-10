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

    public Schedule(String id, String name, String device, String action,
                   String time, String recurrence, boolean active) {
        this(id, name, device, device, action, time, recurrence, active);
    }
    
    public String getId() { return id.get(); }
    public SimpleStringProperty idProperty() { return id; }
    
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public SimpleStringProperty nameProperty() { return name; }

    public String getDeviceId() { return deviceId.get(); }
    public void setDeviceId(String deviceId) { this.deviceId.set(deviceId); }
    public SimpleStringProperty deviceIdProperty() { return deviceId; }
    
    public String getDevice() { return device.get(); }
    public void setDevice(String device) { this.device.set(device); }
    public SimpleStringProperty deviceProperty() { return device; }
    
    public String getAction() { return action.get(); }
    public void setAction(String action) { this.action.set(action); }
    public SimpleStringProperty actionProperty() { return action; }
    
    public String getTime() { return time.get(); }
    public void setTime(String time) { this.time.set(time); }
    public SimpleStringProperty timeProperty() { return time; }
    
    public String getRecurrence() { return recurrence.get(); }
    public void setRecurrence(String recurrence) { this.recurrence.set(recurrence); }
    public SimpleStringProperty recurrenceProperty() { return recurrence; }
    
    public boolean isActive() { return active.get(); }
    public void setActive(boolean active) { this.active.set(active); }
    public SimpleBooleanProperty activeProperty() { return active; }
}
