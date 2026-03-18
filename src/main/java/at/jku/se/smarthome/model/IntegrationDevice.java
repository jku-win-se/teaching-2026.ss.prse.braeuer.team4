package at.jku.se.smarthome.model;

import javafx.beans.property.SimpleStringProperty;

/**
 * Represents a physical IoT device discovered through the optional integration layer.
 */
public class IntegrationDevice {

    private final SimpleStringProperty name;
    private final SimpleStringProperty type;
    private final SimpleStringProperty topic;
    private final SimpleStringProperty status;

    public IntegrationDevice(String name, String type, String topic, String status) {
        this.name = new SimpleStringProperty(name);
        this.type = new SimpleStringProperty(type);
        this.topic = new SimpleStringProperty(topic);
        this.status = new SimpleStringProperty(status);
    }

    public String getName() {
        return name.get();
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public String getType() {
        return type.get();
    }

    public SimpleStringProperty typeProperty() {
        return type;
    }

    public String getTopic() {
        return topic.get();
    }

    public SimpleStringProperty topicProperty() {
        return topic;
    }

    public String getStatus() {
        return status.get();
    }

    public SimpleStringProperty statusProperty() {
        return status;
    }
}