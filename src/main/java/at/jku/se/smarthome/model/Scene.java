package at.jku.se.smarthome.model;

import java.util.Collection;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Represents a Scene (set of device states) in the SmartHome system.
 */
public class Scene {
    
    /** Unique scene identifier. */
    private final SimpleStringProperty id;
    /** Scene display name. */
    private final SimpleStringProperty name;
    /** Scene description or purpose. */
    private final SimpleStringProperty description;
    /** Observable list of device state configurations for this scene. */
    private final ObservableList<String> deviceStates;
    
    /**
     * Creates a reusable scene definition.
     *
     * @param id unique scene identifier
     * @param name display name of the scene
     * @param description scene purpose or summary
     */
    public Scene(String id, String name, String description) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.description = new SimpleStringProperty(description);
        this.deviceStates = FXCollections.observableArrayList();
    }

    /**
     * Returns the scene identifier.
     *
     * @return scene identifier
     */
    public String getId() { return id.get(); }
    /**
     * Exposes the JavaFX id property.
     *
     * @return id property
     */
    public SimpleStringProperty idProperty() { return id; }

    /**
     * Returns the scene name.
     *
     * @return scene name
     */
    public String getName() { return name.get(); }
    /**
     * Updates the scene name.
     *
     * @param name updated scene name
     */
    public void setName(String name) { this.name.set(name); }
    /**
     * Exposes the JavaFX name property.
     *
     * @return name property
     */
    public SimpleStringProperty nameProperty() { return name; }

    /**
     * Returns the scene description.
     *
     * @return scene description
     */
    public String getDescription() { return description.get(); }
    /**
     * Updates the scene description.
     *
     * @param description updated description
     */
    public void setDescription(String description) { this.description.set(description); }
    /**
     * Exposes the JavaFX description property.
     *
     * @return description property
     */
    public SimpleStringProperty descriptionProperty() { return description; }

    /**
     * Returns the configured device-state entries.
     *
     * @return observable device-state list
     */
    public ObservableList<String> getDeviceStates() { return deviceStates; }

    /**
     * Adds a device-state entry to the scene.
     *
     * @param deviceState device-state entry to add
     */
    public void addDeviceState(String deviceState) { deviceStates.add(deviceState); }
    /**
     * Removes a device-state entry from the scene.
     *
     * @param deviceState device-state entry to remove
     */
    public void removeDeviceState(String deviceState) { deviceStates.remove(deviceState); }
    /**
     * Replaces all configured device-state entries.
     *
     * @param deviceStates collection of device-state entries to store
     */
    public void setDeviceStates(Collection<String> deviceStates) {
        this.deviceStates.setAll(deviceStates);
    }
}
