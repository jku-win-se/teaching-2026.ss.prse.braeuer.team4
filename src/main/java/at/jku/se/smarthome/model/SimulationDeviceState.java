package at.jku.se.smarthome.model;

import javafx.beans.property.SimpleStringProperty;

/**
 * Represents a simulated device state used only inside the time-lapse replay.
 */
public class SimulationDeviceState {

    private final SimpleStringProperty deviceName;
    private final SimpleStringProperty room;
    private final SimpleStringProperty type;
    private final SimpleStringProperty state;
    private final SimpleStringProperty lastChanged;

    public SimulationDeviceState(String deviceName, String room, String type, String state, String lastChanged) {
        this.deviceName = new SimpleStringProperty(deviceName);
        this.room = new SimpleStringProperty(room);
        this.type = new SimpleStringProperty(type);
        this.state = new SimpleStringProperty(state);
        this.lastChanged = new SimpleStringProperty(lastChanged);
    }

    public String getDeviceName() {
        return deviceName.get();
    }

    public SimpleStringProperty deviceNameProperty() {
        return deviceName;
    }

    public String getRoom() {
        return room.get();
    }

    public SimpleStringProperty roomProperty() {
        return room;
    }

    public String getType() {
        return type.get();
    }

    public SimpleStringProperty typeProperty() {
        return type;
    }

    public String getState() {
        return state.get();
    }

    public void setState(String state) {
        this.state.set(state);
    }

    public SimpleStringProperty stateProperty() {
        return state;
    }

    public String getLastChanged() {
        return lastChanged.get();
    }

    public void setLastChanged(String lastChanged) {
        this.lastChanged.set(lastChanged);
    }

    public SimpleStringProperty lastChangedProperty() {
        return lastChanged;
    }
}