package at.jku.se.smarthome.service.mock;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import at.jku.se.smarthome.model.NotificationEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock in-app notification service.
 */
public final class MockNotificationService {
    
    /** Lock for singleton synchronization. */
    private static final Object INSTANCE_LOCK = new Object();
    /** Singleton instance of the mock notification service. */
    private static MockNotificationService instance;
    /** Observable collection of all notifications. */
    private final ObservableList<NotificationEntry> notifications;
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
    public ObservableList<NotificationEntry> getNotifications() {
        return notifications;
    }
    
    /**
     * Adds a notification to the top of the notifications list.
     *
     * @param message notification message text
     * @param type notification type (info, success, error, warning)
     */
    public void addNotification(String message, String type) {
        notifications.add(0, new NotificationEntry(
                LocalDateTime.now().format(formatter),
                message,
                type
        ));
    }
    
    /**
     * Helper: seeds initial mock notifications.
     */
    private void seedNotifications() {
        addNotification("Dashboard loaded successfully", "info");
        addNotification("All devices are operational", "success");
    }
}
