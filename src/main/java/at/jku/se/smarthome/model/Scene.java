package at.jku.se.smarthome.model;

import java.util.Collection;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Represents a Scene (set of device states) in the SmartHome system.
 */
public class Scene {
    
    private final SimpleStringProperty id;
    private final SimpleStringProperty name;
    private final SimpleStringProperty description;
    private final ObservableList<String> deviceStates;
    
    public Scene(String id, String name, String description) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.description = new SimpleStringProperty(description);
        this.deviceStates = FXCollections.observableArrayList();
    }
    
    public String getId() { return id.get(); }
    public SimpleStringProperty idProperty() { return id; }
    
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public SimpleStringProperty nameProperty() { return name; }
    
    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }
    public SimpleStringProperty descriptionProperty() { return description; }
    
    public ObservableList<String> getDeviceStates() { return deviceStates; }
    
    public void addDeviceState(String deviceState) { deviceStates.add(deviceState); }
    public void removeDeviceState(String deviceState) { deviceStates.remove(deviceState); }
    public void setDeviceStates(Collection<String> deviceStates) {
        this.deviceStates.setAll(deviceStates);
    }
}
