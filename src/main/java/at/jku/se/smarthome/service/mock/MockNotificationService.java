package at.jku.se.smarthome.service.mock;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import at.jku.se.smarthome.model.NotificationEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock in-app notification service.
 */
public class MockNotificationService {
    
    private static MockNotificationService instance;
    private final ObservableList<NotificationEntry> notifications;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private MockNotificationService() {
        notifications = FXCollections.observableArrayList();
        seedNotifications();
    }
    
    public static synchronized MockNotificationService getInstance() {
        if (instance == null) {
            instance = new MockNotificationService();
        }
        return instance;
    }

    public static synchronized void resetForTesting() {
        instance = null;
    }
    
    public ObservableList<NotificationEntry> getNotifications() {
        return notifications;
    }
    
    public void addNotification(String message, String type) {
        notifications.add(0, new NotificationEntry(
                LocalDateTime.now().format(formatter),
                message,
                type
        ));
    }
    
    private void seedNotifications() {
        addNotification("Dashboard loaded successfully", "info");
        addNotification("All devices are operational", "success");
    }
}
