package at.jku.se.smarthome.model;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Represents a Smart Home Device.
 * 
 * A device has an ID, name, type, current state, and is associated with a room.
 * Supports different device types: Switch, Dimmer, Thermostat.
 */
public class Device {
    
    private final SimpleStringProperty id;
    private final SimpleStringProperty name;
    private final SimpleStringProperty type;
    private final SimpleStringProperty room;
    private final SimpleBooleanProperty state;
    private final SimpleIntegerProperty brightness;
    private final SimpleDoubleProperty temperature;
    
    /**
     * Constructs a Device with the specified parameters.
     * 
     * @param id       unique identifier for the device
     * @param name     display name of the device
     * @param type     device type (Switch, Dimmer, Thermostat)
     * @param room     room where the device is located
     * @param state    initial power state
     */
    public Device(String id, String name, String type, String room, boolean state) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.type = new SimpleStringProperty(type);
        this.room = new SimpleStringProperty(room);
        this.state = new SimpleBooleanProperty(state);
        this.brightness = new SimpleIntegerProperty(100);
        this.temperature = new SimpleDoubleProperty(20.0);
    }
    
    /**
     * Returns the device identifier.
     *
     * @return unique device identifier
     */
    public String getId() {
        return id.get();
    }

    /**
     * Updates the device identifier.
     *
     * @param id new device identifier
     */
    public void setId(String id) {
        this.id.set(id);
    }

    /**
     * Exposes the JavaFX id property.
     *
     * @return id property
     */
    public SimpleStringProperty idProperty() {
        return id;
    }

    /**
     * Returns the device name.
     *
     * @return display name
     */
    public String getName() {
        return name.get();
    }

    /**
     * Updates the device name.
     *
     * @param name new display name
     */
    public void setName(String name) {
        this.name.set(name);
    }

    /**
     * Exposes the JavaFX name property.
     *
     * @return name property
     */
    public SimpleStringProperty nameProperty() {
        return name;
    }

    /**
     * Returns the device type.
     *
     * @return device type
     */
    public String getType() {
        return type.get();
    }

    /**
     * Updates the device type.
     *
     * @param type new device type
     */
    public void setType(String type) {
        this.type.set(type);
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
     * Returns the room name assigned to the device.
     *
     * @return room name
     */
    public String getRoom() {
        return room.get();
    }

    /**
     * Updates the assigned room name.
     *
     * @param room new room name
     */
    public void setRoom(String room) {
        this.room.set(room);
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
     * Returns whether the device is on or active.
     *
     * @return current power or active state
     */
    public boolean getState() {
        return state.get();
    }

    /**
     * Updates the device state.
     *
     * @param state new power or active state
     */
    public void setState(boolean state) {
        this.state.set(state);
    }

    /**
     * Exposes the JavaFX state property.
     *
     * @return state property
     */
    public SimpleBooleanProperty stateProperty() {
        return state;
    }

    /**
     * Returns the dimmer brightness value.
     *
     * @return brightness percentage
     */
    public int getBrightness() {
        return brightness.get();
    }

    /**
     * Updates the dimmer brightness value.
     *
     * @param brightness new brightness percentage
     */
    public void setBrightness(int brightness) {
        this.brightness.set(brightness);
    }

    /**
     * Exposes the JavaFX brightness property.
     *
     * @return brightness property
     */
    public SimpleIntegerProperty brightnessProperty() {
        return brightness;
    }

    /**
     * Returns the thermostat temperature value.
     *
     * @return configured temperature
     */
    public double getTemperature() {
        return temperature.get();
    }

    /**
     * Updates the thermostat temperature value.
     *
     * @param temperature new configured temperature
     */
    public void setTemperature(double temperature) {
        this.temperature.set(temperature);
    }

    /**
     * Exposes the JavaFX temperature property.
     *
     * @return temperature property
     */
    public SimpleDoubleProperty temperatureProperty() {
        return temperature;
    }

    /**
     * Returns a debug-friendly string representation of the device.
     *
     * @return formatted device summary
     */
    @Override
    public String toString() {
        return "Device{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", type='" + getType() + '\'' +
                ", room='" + getRoom() + '\'' +
                ", state=" + getState() +
                '}';
    }
}
