package at.jku.se.smarthome.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Represents an in-app notification message.
 */
public class NotificationEntry {

    /** Timestamp when the notification was created. */
    private final SimpleStringProperty timestamp;
    /** Notification message text. */
    private final SimpleStringProperty message;
    /** Notification type or severity. */
    private final NotificationType type;
    /** Whether the notification has been read. */
    private final BooleanProperty read;

    /**
     * Creates an unread notification entry.
     *
     * @param timestamp time at which the notification was created
     * @param message notification message text
     * @param type notification category or severity
     */
    public NotificationEntry(String timestamp, String message, NotificationType type) {
        this(timestamp, message, type, false);
    }

    /**
     * Creates a notification entry with an explicit read state (used for seed data).
     *
     * @param timestamp time at which the notification was created
     * @param message notification message text
     * @param type notification category or severity
     * @param read initial read state
     */
    public NotificationEntry(String timestamp, String message, NotificationType type, boolean read) {
        this.timestamp = new SimpleStringProperty(timestamp);
        this.message = new SimpleStringProperty(message);
        this.type = type;
        this.read = new SimpleBooleanProperty(read);
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
    public NotificationType getType() {
        return type;
    }

    /**
     * Returns whether this notification has been read.
     *
     * @return true if read
     */
    public boolean isRead() {
        return read.get();
    }

    /**
     * Marks this notification as read.
     */
    public void markRead() {
        read.set(true);
    }

    /**
     * Exposes the JavaFX read property.
     *
     * @return read property
     */
    public BooleanProperty readProperty() {
        return read;
    }
}
