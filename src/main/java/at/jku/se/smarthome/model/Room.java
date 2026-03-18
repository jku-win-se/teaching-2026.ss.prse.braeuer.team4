package at.jku.se.smarthome.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Represents a Room in the SmartHome system.
 */
public class Room {
    
    private final SimpleStringProperty id;
    private final SimpleStringProperty name;
    private final SimpleIntegerProperty deviceCount;
    private final ObservableList<Device> devices;
    
    public Room(String id, String name, int deviceCount) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.deviceCount = new SimpleIntegerProperty(deviceCount);
        this.devices = FXCollections.observableArrayList();
    }
    
    public String getId() { return id.get(); }
    public void setId(String id) { this.id.set(id); }
    public SimpleStringProperty idProperty() { return id; }
    
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public SimpleStringProperty nameProperty() { return name; }
    
    public int getDeviceCount() { return deviceCount.get(); }
    public void setDeviceCount(int count) { this.deviceCount.set(count); }
    public SimpleIntegerProperty deviceCountProperty() { return deviceCount; }
    
    public ObservableList<Device> getDevices() { return devices; }
    
    public void addDevice(Device device) {
        devices.add(device);
        updateDeviceCount();
    }
    
    public void removeDevice(Device device) {
        devices.remove(device);
        updateDeviceCount();
    }
    
    private void updateDeviceCount() {
        deviceCount.set(devices.size());
    }
}
