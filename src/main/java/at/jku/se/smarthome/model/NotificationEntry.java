package at.jku.se.smarthome.model;

import javafx.beans.property.SimpleStringProperty;

/**
 * Represents an in-app notification message.
 */
public class NotificationEntry {
    
    private final SimpleStringProperty timestamp;
    private final SimpleStringProperty message;
    private final SimpleStringProperty type;
    
    public NotificationEntry(String timestamp, String message, String type) {
        this.timestamp = new SimpleStringProperty(timestamp);
        this.message = new SimpleStringProperty(message);
        this.type = new SimpleStringProperty(type);
    }
    
    public String getTimestamp() {
        return timestamp.get();
    }
    
    public SimpleStringProperty timestampProperty() {
        return timestamp;
    }
    
    public String getMessage() {
        return message.get();
    }
    
    public SimpleStringProperty messageProperty() {
        return message;
    }
    
    public String getType() {
        return type.get();
    }
    
    public SimpleStringProperty typeProperty() {
        return type;
    }
}
