package at.jku.se.smarthome.service.real.notification;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import at.jku.se.smarthome.config.DatabaseConfig;
import at.jku.se.smarthome.config.DatabaseSettings;
import at.jku.se.smarthome.model.NotificationEntry;
import at.jku.se.smarthome.model.NotificationType;
import at.jku.se.smarthome.service.api.NotificationService;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * JDBC-backed NotificationService implementation. Persists in-app notifications
 * (FR-12) and keeps a JavaFX-bound observable mirror in sync.
 *
 * <p>Notifications are not seeded in the schema script — the prior mock-only
 * seed entries ("Dashboard loaded successfully") were UI artifacts that only
 * make sense at app start, not as durable rows. The unread-count property is
 * derived once on load and updated synchronously with each mutation.
 */
@SuppressWarnings({"PMD.UseObjectForClearerAPI", "PMD.TooManyMethods",
        "PMD.CouplingBetweenObjects"})
public final class JdbcNotificationService implements NotificationService {

    /** Path to notification schema initialization script in classpath. */
    private static final String INIT_SCRIPT_PATH = "/db/init-notifications.sql";
    /** Maximum number of notifications loaded into the in-memory mirror. */
    private static final int MAX_LOADED_ENTRIES = 200;
    /** Lock for singleton synchronization. */
    private static final Object INSTANCE_LOCK = new Object();
    /** Singleton instance. */
    private static JdbcNotificationService instance;

    /** Observable mirror of recent notifications, newest first. */
    private final ObservableList<NotificationEntry> notifications = FXCollections.observableArrayList();
    /** Reactive count of unread notifications, used for badge bindings. */
    private final IntegerProperty unreadCount = new SimpleIntegerProperty(0);
    /** Time formatter for notification timestamps (matches mock for parity). */
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    /** Flag indicating database schema is initialized. */
    private final AtomicBoolean schemaReady = new AtomicBoolean(false);

    private JdbcNotificationService() {
        refreshNotifications();
    }

    /**
     * Returns the singleton instance of the JDBC notification service.
     *
     * @return singleton JdbcNotificationService instance
     */
    public static JdbcNotificationService getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new JdbcNotificationService();
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

    @Override
    public ObservableList<NotificationEntry> getNotifications() {
        return notifications;
    }

    @Override
    public void addNotification(String message, NotificationType type) {
        String timestamp = LocalDateTime.now().format(formatter);
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO notifications (timestamp, message, notification_type, read_flag)"
                    + " VALUES (?, ?, ?, FALSE)")) {
                statement.setString(1, timestamp);
                statement.setString(2, message);
                statement.setString(3, type.name());
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to persist notification.", exception);
        }
        notifications.add(0, new NotificationEntry(timestamp, message, type));
        unreadCount.set(unreadCount.get() + 1);
    }

    @Override
    public void markAsRead(NotificationEntry entry) {
        if (entry != null && !entry.isRead()) {
            try (Connection connection = openConnection()) {
                ensureSchema(connection);
                // Notifications are identified by (timestamp, message) in the mirror — the
                // primary key is auto-generated and not exposed on NotificationEntry. This
                // matches how the UI invokes markAsRead with the entry object it already holds.
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE notifications SET read_flag = TRUE"
                        + " WHERE timestamp = ? AND message = ? AND read_flag = FALSE")) {
                    statement.setString(1, entry.getTimestamp());
                    statement.setString(2, entry.getMessage());
                    statement.executeUpdate();
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to mark notification as read.", exception);
            }
            entry.markRead();
            unreadCount.set(Math.max(0, unreadCount.get() - 1));
        }
    }

    @Override
    public void markAllAsRead() {
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE notifications SET read_flag = TRUE WHERE read_flag = FALSE")) {
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to mark all notifications as read.", exception);
        }
        notifications.forEach(NotificationEntry::markRead);
        unreadCount.set(0);
    }

    @Override
    public ReadOnlyIntegerProperty unreadCountProperty() {
        return unreadCount;
    }

    private void refreshNotifications() {
        List<NotificationEntry> loaded = new ArrayList<>();
        int unread = 0;
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT timestamp, message, notification_type, read_flag"
                    + " FROM notifications ORDER BY id DESC LIMIT ?")) {
                statement.setInt(1, MAX_LOADED_ENTRIES);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        boolean read = resultSet.getBoolean("read_flag");
                        if (!read) {
                            unread++;
                        }
                        loaded.add(new NotificationEntry(
                                resultSet.getString("timestamp"),
                                resultSet.getString("message"),
                                NotificationType.valueOf(resultSet.getString("notification_type")),
                                read
                        ));
                    }
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load notifications from the database.", exception);
        }
        notifications.setAll(loaded);
        unreadCount.set(unread);
    }

    private Connection openConnection() throws SQLException {
        DatabaseSettings settings = DatabaseConfig.load()
                .orElseThrow(() -> new IllegalStateException("Notification database is not configured."));
        return DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password());
    }

    @SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts")
    private void ensureSchema(Connection connection) {
        if (!schemaReady.get()) {
            synchronized (this) {
                if (!schemaReady.get()) {
                    try (Statement stmt = connection.createStatement()) {
                        for (String sql : loadInitScript().split(";")) {
                            String trimmed = sql.trim();
                            if (!trimmed.isEmpty()) {
                                stmt.execute(trimmed);
                            }
                        }
                        schemaReady.set(true);
                    } catch (SQLException exception) {
                        throw new IllegalStateException(
                                "Failed to initialize notifications schema.", exception);
                    }
                }
            }
        }
    }

    private String loadInitScript() {
        try (InputStream scriptStream = getClass().getResourceAsStream(INIT_SCRIPT_PATH)) {
            if (scriptStream == null) {
                throw new IllegalStateException(
                        "Notification schema script not found at " + INIT_SCRIPT_PATH);
            }
            return new String(scriptStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read notification schema script.", exception);
        }
    }
}
