package at.jku.se.smarthome.service.real.log;

import at.jku.se.smarthome.config.DatabaseConfig;
import at.jku.se.smarthome.config.DatabaseSettings;
import at.jku.se.smarthome.model.LogEntry;
import at.jku.se.smarthome.service.api.LogService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
import java.util.stream.Collectors;

/**
 * JDBC-backed LogService implementation. Persists activity log entries to the configured database.
 */
public final class JdbcLogService implements LogService {

    private static final String INIT_SCRIPT_PATH = "/db/init-activity-log.sql";
    private static final int MAX_LOADED_ENTRIES = 200;
    private static JdbcLogService instance;

    private final ObservableList<LogEntry> logs = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final AtomicBoolean schemaReady = new AtomicBoolean(false);

    private JdbcLogService() {
        refreshLogs();
    }

    /**
     * Returns the singleton instance of the JDBC log service.
     *
     * @return singleton JdbcLogService instance
     */
    public static synchronized JdbcLogService getInstance() {
        if (instance == null) {
            instance = new JdbcLogService();
        }
        return instance;
    }

    /**
     * Resets the singleton instance for test isolation.
     */
    public static synchronized void resetForTesting() {
        instance = null;
    }

    @Override
    public ObservableList<LogEntry> getLogs() {
        return logs;
    }

    @Override
    public void addLogEntry(String device, String room, String action, String actor) {
        String timestamp = LocalDateTime.now().format(formatter);
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO activity_log (timestamp, device, room, action, actor) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setString(1, timestamp);
                stmt.setString(2, device);
                stmt.setString(3, room);
                stmt.setString(4, action);
                stmt.setString(5, actor);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to persist log entry.", e);
        }
        logs.add(0, new LogEntry(timestamp, device, room, action, actor));
    }

    @Override
    public void addLogEntry(String device, String action, String actor) {
        addLogEntry(device, "Unknown", action, actor);
    }

    @Override
    public ObservableList<LogEntry> getLogsByRoom(String room) {
        return FXCollections.observableArrayList(
                logs.stream().filter(l -> l.getRoom().equals(room)).collect(Collectors.toList()));
    }

    @Override
    public ObservableList<LogEntry> getLogsByDevice(String device) {
        return FXCollections.observableArrayList(
                logs.stream().filter(l -> l.getDevice().equals(device)).collect(Collectors.toList()));
    }

    @Override
    public ObservableList<LogEntry> getLogsByActor(String actor) {
        return FXCollections.observableArrayList(
                logs.stream().filter(l -> l.getActor().equals(actor)).collect(Collectors.toList()));
    }

    @Override
    public ObservableList<String> getUniqueDevices() {
        return FXCollections.observableArrayList(
                logs.stream().map(LogEntry::getDevice).distinct().sorted().collect(Collectors.toList()));
    }

    @Override
    public String exportToCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,Device,Room,Action,Actor\n");
        for (LogEntry log : logs) {
            csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    log.getTimestamp(), log.getDevice(), log.getRoom(), log.getAction(), log.getActor()));
        }
        return csv.toString();
    }

    private void refreshLogs() {
        List<LogEntry> loaded = new ArrayList<>();
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT timestamp, device, room, action, actor FROM activity_log ORDER BY id DESC LIMIT ?")) {
                stmt.setInt(1, MAX_LOADED_ENTRIES);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        loaded.add(new LogEntry(
                                rs.getString("timestamp"),
                                rs.getString("device"),
                                rs.getString("room"),
                                rs.getString("action"),
                                rs.getString("actor")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load activity log from the database.", e);
        }
        logs.setAll(loaded);
    }

    /**
     * Helper: opens a database connection using configured settings.
     *
     * @return open database connection
     * @throws SQLException if connection fails
     */
    private Connection openConnection() throws SQLException {
        DatabaseSettings settings = DatabaseConfig.load()
                .orElseThrow(() -> new IllegalStateException("Log database is not configured."));
        return DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password());
    }

    /**
     * Helper: ensures database schema is initialized.
     *
     * @param connection database connection to use
     */
    private void ensureSchema(Connection connection) {
        if (schemaReady.get()) return;
        synchronized (this) {
            if (schemaReady.get()) return;
            try (Statement stmt = connection.createStatement()) {
                for (String sql : loadInitScript().split(";")) {
                    String s = sql.trim();
                    if (!s.isEmpty()) stmt.execute(s);
                }
                schemaReady.set(true);
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to initialize activity_log schema.", e);
            }
        }
    }

    /**
     * Helper: loads database schema initialization script from classpath.
     *
     * @return SQL script content as string
     */
    private String loadInitScript() {
        try (InputStream in = getClass().getResourceAsStream(INIT_SCRIPT_PATH)) {
            if (in == null) throw new IllegalStateException("Log schema script not found at " + INIT_SCRIPT_PATH);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read log schema script.", e);
        }
    }
}
