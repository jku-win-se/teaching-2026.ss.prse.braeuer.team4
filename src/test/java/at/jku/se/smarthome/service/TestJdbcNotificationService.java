package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.NotificationEntry;
import at.jku.se.smarthome.model.NotificationType;
import at.jku.se.smarthome.service.real.notification.JdbcNotificationService;

/**
 * Unit tests for JdbcNotificationService — verifies that in-app notifications
 * (FR-12) are persisted, that the unread badge stays consistent with database
 * state, and that mark-as-read survives a service restart.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods",
        "PMD.MethodNamingConventions", "PMD.UnitTestContainsTooManyAsserts",
        "PMD.JUnitTestsShouldIncludeAssert", "PMD.CommentRequired"})
public class TestJdbcNotificationService {

    /** JDBC URL property. */
    private static final String URL_PROPERTY = "smarthome.db.url";
    /** JDBC user property. */
    private static final String USER_PROPERTY = "smarthome.db.user";
    /** JDBC password property. */
    private static final String PASSWORD_PROPERTY = "smarthome.db.password";

    /** Service under test. */
    private JdbcNotificationService service;

    @Before
    public void setUp() {
        String jdbcUrl = "jdbc:h2:mem:notif_" + System.nanoTime()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        System.setProperty(URL_PROPERTY, jdbcUrl);
        System.setProperty(USER_PROPERTY, "sa");
        System.setProperty(PASSWORD_PROPERTY, "");

        JdbcNotificationService.resetForTesting();
        service = JdbcNotificationService.getInstance();
    }

    @After
    public void tearDown() {
        JdbcNotificationService.resetForTesting();
        System.clearProperty(URL_PROPERTY);
        System.clearProperty(USER_PROPERTY);
        System.clearProperty(PASSWORD_PROPERTY);
    }

    @Test
    public void freshDatabase_hasNoNotifications() {
        assertTrue("schema does not seed notifications", service.getNotifications().isEmpty());
        assertEquals(0, service.unreadCountProperty().get());
    }

    @Test
    public void addNotification_appearsAtTopAndIncrementsUnread() {
        service.addNotification("First", NotificationType.INFO);
        service.addNotification("Second", NotificationType.SUCCESS);

        assertEquals(2, service.getNotifications().size());
        assertEquals("Second", service.getNotifications().get(0).getMessage());
        assertEquals(2, service.unreadCountProperty().get());
        assertFalse(service.getNotifications().get(0).isRead());
    }

    @Test
    public void addNotification_persistsAcrossRestart() {
        service.addNotification("Persistent", NotificationType.WARNING);

        JdbcNotificationService.resetForTesting();
        JdbcNotificationService restarted = JdbcNotificationService.getInstance();

        assertEquals(1, restarted.getNotifications().size());
        NotificationEntry entry = restarted.getNotifications().get(0);
        assertEquals("Persistent", entry.getMessage());
        assertEquals(NotificationType.WARNING, entry.getType());
        assertFalse("freshly added notification stays unread after restart", entry.isRead());
        assertEquals(1, restarted.unreadCountProperty().get());
    }

    @Test
    public void markAsRead_decrementsUnreadAndPersistsAcrossRestart() {
        service.addNotification("To be read", NotificationType.INFO);
        NotificationEntry entry = service.getNotifications().get(0);
        service.markAsRead(entry);

        assertTrue(entry.isRead());
        assertEquals(0, service.unreadCountProperty().get());

        JdbcNotificationService.resetForTesting();
        JdbcNotificationService restarted = JdbcNotificationService.getInstance();
        assertTrue("read flag survives restart",
                restarted.getNotifications().get(0).isRead());
        assertEquals(0, restarted.unreadCountProperty().get());
    }

    @Test
    public void markAllAsRead_clearsBadgeAndPersistsAcrossRestart() {
        service.addNotification("One", NotificationType.INFO);
        service.addNotification("Two", NotificationType.SUCCESS);
        service.addNotification("Three", NotificationType.FAILURE);
        assertEquals(3, service.unreadCountProperty().get());

        service.markAllAsRead();
        assertEquals(0, service.unreadCountProperty().get());
        assertTrue(service.getNotifications().stream().allMatch(NotificationEntry::isRead));

        JdbcNotificationService.resetForTesting();
        JdbcNotificationService restarted = JdbcNotificationService.getInstance();
        assertEquals(0, restarted.unreadCountProperty().get());
        assertTrue(restarted.getNotifications().stream().allMatch(NotificationEntry::isRead));
    }

    @Test
    public void markAsRead_alreadyReadEntry_isNoOp() {
        service.addNotification("X", NotificationType.INFO);
        NotificationEntry entry = service.getNotifications().get(0);
        service.markAsRead(entry);
        service.markAsRead(entry);
        assertEquals(0, service.unreadCountProperty().get());
    }
}
