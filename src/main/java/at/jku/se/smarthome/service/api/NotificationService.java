package at.jku.se.smarthome.service.api;

import at.jku.se.smarthome.model.NotificationEntry;
import at.jku.se.smarthome.model.NotificationType;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.collections.ObservableList;

/**
 * Service interface for managing in-app notifications.
 */
public interface NotificationService {

    /**
     * Returns all notifications, newest first.
     *
     * @return observable list of notification entries
     */
    ObservableList<NotificationEntry> getNotifications();

    /**
     * Creates and prepends a new unread notification.
     *
     * @param message notification message text
     * @param type notification category or severity
     */
    void addNotification(String message, NotificationType type);

    /**
     * Marks a single notification as read, decrementing the unread count.
     * No-op if already read.
     *
     * @param entry notification to mark as read
     */
    void markAsRead(NotificationEntry entry);

    /**
     * Marks all notifications as read and resets the unread count to zero.
     */
    void markAllAsRead();

    /**
     * Returns a read-only property that reflects the current number of unread notifications.
     *
     * @return unread count property
     */
    ReadOnlyIntegerProperty unreadCountProperty();
}
