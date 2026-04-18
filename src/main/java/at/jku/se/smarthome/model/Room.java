package at.jku.se.smarthome.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Represents a Room in the SmartHome system.
 */
public class Room {
    
    /** Unique room identifier. */
    private final SimpleStringProperty id;
    /** Room display name. */
    private final SimpleStringProperty name;
    /** Count of devices in the room. */
    private final SimpleIntegerProperty deviceCount;
    /** Observable list of devices in the room. */
    private final ObservableList<Device> devices;
    
    /**
     * Creates a room entry.
     *
     * @param id unique room identifier
     * @param name display name of the room
     * @param deviceCount initial device count
     */
    public Room(String id, String name, int deviceCount) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.deviceCount = new SimpleIntegerProperty(deviceCount);
        this.devices = FXCollections.observableArrayList();
    }

    /**
     * Returns the room identifier.
     *
     * @return room identifier
     */
    public String getId() { return id.get(); }
    /**
     * Updates the room identifier.
     *
     * @param id new room identifier
     */
    public void setId(String id) { this.id.set(id); }
    /**
     * Exposes the JavaFX id property.
     *
     * @return id property
     */
    public SimpleStringProperty idProperty() { return id; }

    /**
     * Returns the room name.
     *
     * @return room name
     */
    public String getName() { return name.get(); }
    /**
     * Updates the room name.
     *
     * @param name new room name
     */
    public void setName(String name) { this.name.set(name); }
    /**
     * Exposes the JavaFX name property.
     *
     * @return name property
     */
    public SimpleStringProperty nameProperty() { return name; }

    /**
     * Returns the number of devices assigned to the room.
     *
     * @return device count
     */
    public int getDeviceCount() { return deviceCount.get(); }
    /**
     * Updates the stored device count.
     *
     * @param count new device count
     */
    public void setDeviceCount(int count) { this.deviceCount.set(count); }
    /**
     * Exposes the JavaFX device-count property.
     *
     * @return device-count property
     */
    public SimpleIntegerProperty deviceCountProperty() { return deviceCount; }

    /**
     * Returns the devices assigned to the room.
     *
     * @return observable device list
     */
    public ObservableList<Device> getDevices() { return devices; }

    /**
     * Adds a device to the room and refreshes the count.
     *
     * @param device device to add
     */
    public void addDevice(Device device) {
        devices.add(device);
        updateDeviceCount();
    }

    /**
     * Removes a device from the room and refreshes the count.
     *
     * @param device device to remove
     */
    public void removeDevice(Device device) {
        devices.remove(device);
        updateDeviceCount();
    }
    
    private void updateDeviceCount() {
        deviceCount.set(devices.size());
    }
}
