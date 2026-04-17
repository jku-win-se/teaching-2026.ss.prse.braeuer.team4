package at.jku.se.smarthome.model;

import javafx.beans.property.SimpleStringProperty;

/**
 * Represents a simulated device state used only inside the time-lapse replay.
 */
public class SimulationDeviceState {

    /** Device display name. */
    private final SimpleStringProperty deviceName;
    /** Room containing device. */
    private final SimpleStringProperty room;
    /** Device type. */
    private final SimpleStringProperty type;
    /** Simulated device state. */
    private final SimpleStringProperty state;
    /** Time of last simulated change. */
    private final SimpleStringProperty lastChanged;

    /**
     * Creates a simulated snapshot entry for a device.
     *
     * @param deviceName device display name
     * @param room room containing the device
     * @param type device type
     * @param state simulated device state
     * @param lastChanged time of the last simulated change
     */
    public SimulationDeviceState(String deviceName, String room, String type, String state, String lastChanged) {
        this.deviceName = new SimpleStringProperty(deviceName);
        this.room = new SimpleStringProperty(room);
        this.type = new SimpleStringProperty(type);
        this.state = new SimpleStringProperty(state);
        this.lastChanged = new SimpleStringProperty(lastChanged);
    }

    /**
     * Returns the device name.
     *
     * @return simulated device name
     */
    public String getDeviceName() {
        return deviceName.get();
    }

    /**
     * Exposes the JavaFX device-name property.
     *
     * @return device-name property
     */
    public SimpleStringProperty deviceNameProperty() {
        return deviceName;
    }

    /**
     * Returns the room name.
     *
     * @return simulated room name
     */
    public String getRoom() {
        return room.get();
    }

    /**
     * Exposes the JavaFX room property.
     *
     * @return room property
     */
    public SimpleStringProperty roomProperty() {
        return room;
    }

    /**
     * Returns the device type.
     *
     * @return simulated device type
     */
    public String getType() {
        return type.get();
    }

    /**
     * Exposes the JavaFX type property.
     *
     * @return type property
     */
    public SimpleStringProperty typeProperty() {
        return type;
    }

    /**
     * Returns the simulated state value.
     *
     * @return simulated state
     */
    public String getState() {
        return state.get();
    }

    /**
     * Updates the simulated state value.
     *
     * @param state new simulated state
     */
    public void setState(String state) {
        this.state.set(state);
    }

    /**
     * Exposes the JavaFX state property.
     *
     * @return state property
     */
    public SimpleStringProperty stateProperty() {
        return state;
    }

    /**
     * Returns the last-changed timestamp.
     *
     * @return last-changed timestamp text
     */
    public String getLastChanged() {
        return lastChanged.get();
    }

    /**
     * Updates the last-changed timestamp.
     *
     * @param lastChanged updated timestamp text
     */
    public void setLastChanged(String lastChanged) {
        this.lastChanged.set(lastChanged);
    }

    /**
     * Exposes the JavaFX last-changed property.
     *
     * @return last-changed property
     */
    public SimpleStringProperty lastChangedProperty() {
        return lastChanged;
    }
}