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
    
    // Getters and Setters for properties
    
    public String getId() {
        return id.get();
    }
    
    public void setId(String id) {
        this.id.set(id);
    }
    
    public SimpleStringProperty idProperty() {
        return id;
    }
    
    public String getName() {
        return name.get();
    }
    
    public void setName(String name) {
        this.name.set(name);
    }
    
    public SimpleStringProperty nameProperty() {
        return name;
    }
    
    public String getType() {
        return type.get();
    }
    
    public void setType(String type) {
        this.type.set(type);
    }
    
    public SimpleStringProperty typeProperty() {
        return type;
    }
    
    public String getRoom() {
        return room.get();
    }
    
    public void setRoom(String room) {
        this.room.set(room);
    }
    
    public SimpleStringProperty roomProperty() {
        return room;
    }
    
    public boolean getState() {
        return state.get();
    }
    
    public void setState(boolean state) {
        this.state.set(state);
    }
    
    public SimpleBooleanProperty stateProperty() {
        return state;
    }
    
    public int getBrightness() {
        return brightness.get();
    }
    
    public void setBrightness(int brightness) {
        this.brightness.set(brightness);
    }
    
    public SimpleIntegerProperty brightnessProperty() {
        return brightness;
    }
    
    public double getTemperature() {
        return temperature.get();
    }
    
    public void setTemperature(double temperature) {
        this.temperature.set(temperature);
    }
    
    public SimpleDoubleProperty temperatureProperty() {
        return temperature;
    }
    
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
