package at.jku.se.smarthome.service.mock;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import at.jku.se.smarthome.model.NotificationEntry;
import at.jku.se.smarthome.model.NotificationType;
import at.jku.se.smarthome.service.api.NotificationService;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock in-app notification service.
 */
public final class MockNotificationService implements NotificationService {

    /** Lock for singleton synchronization. */
    private static final Object INSTANCE_LOCK = new Object();
    /** Singleton instance of the mock notification service. */
    private static MockNotificationService instance;
    /** Observable collection of all notifications. */
    private final ObservableList<NotificationEntry> notifications;
    /** Reactive count of unread notifications. */
    private final IntegerProperty unreadCount = new SimpleIntegerProperty(0);
    /** Time formatter for notification timestamps. */
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private MockNotificationService() {
        notifications = FXCollections.observableArrayList();
        seedNotifications();
    }

    /**
     * Returns the singleton instance of the mock notification service.
     *
     * @return singleton MockNotificationService instance
     */
    public static MockNotificationService getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new MockNotificationService();
            }
            return instance;
        }
    }

    /**
     * Resets the singleton for unit testing.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public static void resetForTesting() {
        synchronized (INSTANCE_LOCK) {
            instance = null;
        }
    }

    /**
     * Returns the observable list of notifications.
     *
     * @return observable list of notification entries
     */
    @Override
    public ObservableList<NotificationEntry> getNotifications() {
        return notifications;
    }

    /**
     * Prepends a new unread notification to the list.
     *
     * @param message notification message text
     * @param type notification category or severity
     */
    @Override
    public void addNotification(String message, NotificationType type) {
        notifications.add(0, new NotificationEntry(
                LocalDateTime.now().format(formatter), message, type));
        unreadCount.set(unreadCount.get() + 1);
    }

    /**
     * Marks a single notification as read. No-op if already read.
     *
     * @param entry notification to mark as read
     */
    @Override
    public void markAsRead(NotificationEntry entry) {
        if (!entry.isRead()) {
            entry.markRead();
            unreadCount.set(Math.max(0, unreadCount.get() - 1));
        }
    }

    /**
     * Marks all notifications as read and resets the unread count to zero.
     */
    @Override
    public void markAllAsRead() {
        notifications.forEach(NotificationEntry::markRead);
        unreadCount.set(0);
    }

    /**
     * Returns a read-only property reflecting the number of unread notifications.
     *
     * @return unread count property
     */
    @Override
    public ReadOnlyIntegerProperty unreadCountProperty() {
        return unreadCount;
    }

    /**
     * Seeds initial notifications as read so the badge starts at zero.
     */
    private void seedNotifications() {
        notifications.add(new NotificationEntry(
                LocalDateTime.now().format(formatter),
                "Dashboard loaded successfully", NotificationType.INFO, true));
        notifications.add(new NotificationEntry(
                LocalDateTime.now().format(formatter),
                "All devices are operational", NotificationType.SUCCESS, true));
    }
}
