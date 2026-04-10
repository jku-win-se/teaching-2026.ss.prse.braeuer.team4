package at.jku.se.smarthome.model;

import javafx.beans.property.SimpleStringProperty;

/**
 * Represents an in-app notification message.
 */
public class NotificationEntry {
    
    private final SimpleStringProperty timestamp;
    private final SimpleStringProperty message;
    private final SimpleStringProperty type;
    
    /**
     * Creates a notification entry.
     *
     * @param timestamp time at which the notification was created
     * @param message notification message text
     * @param type notification category or severity
     */
    public NotificationEntry(String timestamp, String message, String type) {
        this.timestamp = new SimpleStringProperty(timestamp);
        this.message = new SimpleStringProperty(message);
        this.type = new SimpleStringProperty(type);
    }

    /**
     * Returns the notification timestamp.
     *
     * @return timestamp text
     */
    public String getTimestamp() {
        return timestamp.get();
    }

    /**
     * Exposes the JavaFX timestamp property.
     *
     * @return timestamp property
     */
    public SimpleStringProperty timestampProperty() {
        return timestamp;
    }

    /**
     * Returns the notification message.
     *
     * @return message text
     */
    public String getMessage() {
        return message.get();
    }

    /**
     * Exposes the JavaFX message property.
     *
     * @return message property
     */
    public SimpleStringProperty messageProperty() {
        return message;
    }

    /**
     * Returns the notification type.
     *
     * @return notification type
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
}
