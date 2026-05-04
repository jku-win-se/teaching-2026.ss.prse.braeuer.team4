package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.NotificationEntry;
import at.jku.se.smarthome.model.NotificationType;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.mock.MockNotificationService;

@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods",
        "PMD.MethodNamingConventions", "PMD.UnitTestContainsTooManyAsserts",
        "PMD.CommentRequired", "PMD.LinguisticNaming"})
public class TestMockNotificationService {

    private MockNotificationService notificationService;

    @Before
    public void setUp() {
        MockNotificationService.resetForTesting();
        notificationService = (MockNotificationService) ServiceRegistry.getNotificationService();
    }

    @Test
    public void addNotification_newEntry_appearsUnreadAndIncrementsBadge() {
        notificationService.addNotification("Test message", NotificationType.INFO);
        NotificationEntry entry = notificationService.getNotifications().get(0);
        assertFalse(entry.isRead());
        assertEquals(1, notificationService.unreadCountProperty().get());
    }

    @Test
    public void addNotification_seedNotifications_startAsRead() {
        assertEquals(0, notificationService.unreadCountProperty().get());
    }

    @Test
    public void markAsRead_unreadEntry_marksReadAndDecrementsCount() {
        notificationService.addNotification("Test", NotificationType.SUCCESS);
        NotificationEntry entry = notificationService.getNotifications().get(0);
        assertEquals(1, notificationService.unreadCountProperty().get());
        notificationService.markAsRead(entry);
        assertTrue(entry.isRead());
        assertEquals(0, notificationService.unreadCountProperty().get());
    }

    @Test
    public void markAsRead_alreadyReadEntry_isNoOp() {
        notificationService.addNotification("Test", NotificationType.SUCCESS);
        NotificationEntry entry = notificationService.getNotifications().get(0);
        notificationService.markAsRead(entry);
        notificationService.markAsRead(entry);
        assertEquals(0, notificationService.unreadCountProperty().get());
    }

    @Test
    public void markAllAsRead_multipleUnread_allReadAndCountZero() {
        notificationService.addNotification("msg1", NotificationType.INFO);
        notificationService.addNotification("msg2", NotificationType.SUCCESS);
        notificationService.addNotification("msg3", NotificationType.FAILURE);
        notificationService.markAllAsRead();
        assertEquals(0, notificationService.unreadCountProperty().get());
        for (NotificationEntry entry : notificationService.getNotifications()) {
            assertTrue(entry.isRead());
        }
    }

    @Test
    public void addNotification_prependsNewestFirst() {
        notificationService.addNotification("first", NotificationType.INFO);
        notificationService.addNotification("second", NotificationType.SUCCESS);
        assertEquals("second", notificationService.getNotifications().get(0).getMessage());
    }

    @Test
    public void getNotifications_returnsObservableList_notNull() {
        assertNotNull(notificationService.getNotifications());
    }
}
