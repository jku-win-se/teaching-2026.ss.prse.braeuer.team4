package at.jku.se.smarthome.model;

import javafx.beans.property.SimpleStringProperty;

/**
 * Represents a physical IoT device discovered through the optional integration layer.
 */
public class IntegrationDevice {

    /** Discovered device name. */
    private final SimpleStringProperty name;
    /** Discovered device type. */
    private final SimpleStringProperty type;
    /** Integration topic or address for the device. */
    private final SimpleStringProperty topic;
    /** Current device status (discovered, connected, etc.). */
    private final SimpleStringProperty status;

    /**
     * Creates an integration device descriptor.
     *
     * @param name discovered device name
     * @param type discovered device type
     * @param topic integration topic or address
     * @param status current discovery or connectivity status
     */
    public IntegrationDevice(String name, String type, String topic, String status) {
        this.name = new SimpleStringProperty(name);
        this.type = new SimpleStringProperty(type);
        this.topic = new SimpleStringProperty(topic);
        this.status = new SimpleStringProperty(status);
    }

    /**
     * Returns the device name.
     *
     * @return discovered device name
     */
    public String getName() {
        return name.get();
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
     * @return discovered device type
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
     * Returns the integration topic or address.
     *
     * @return topic or address string
     */
    public String getTopic() {
        return topic.get();
    }

    /**
     * Exposes the JavaFX topic property.
     *
     * @return topic property
     */
    public SimpleStringProperty topicProperty() {
        return topic;
    }

    /**
     * Returns the current device status.
     *
     * @return discovery or connectivity status
     */
    public String getStatus() {
        return status.get();
    }

    /**
     * Exposes the JavaFX status property.
     *
     * @return status property
     */
    public SimpleStringProperty statusProperty() {
        return status;
    }
}