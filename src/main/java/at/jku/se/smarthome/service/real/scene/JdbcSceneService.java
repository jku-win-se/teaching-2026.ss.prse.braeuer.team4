package at.jku.se.smarthome.service.real.scene;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import at.jku.se.smarthome.config.DatabaseConfig;
import at.jku.se.smarthome.config.DatabaseSettings;
import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.NotificationType;
import at.jku.se.smarthome.model.Scene;
import at.jku.se.smarthome.service.api.LogService;
import at.jku.se.smarthome.service.api.NotificationService;
import at.jku.se.smarthome.service.api.RoomService;
import at.jku.se.smarthome.service.api.SceneService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * JDBC-backed SceneService implementation. Persists smart scenes (FR-17)
 * to the configured database.
 */
@SuppressWarnings({"PMD.UseObjectForClearerAPI", "PMD.TooManyMethods",
        "PMD.CouplingBetweenObjects", "PMD.ExcessiveImports",
        "PMD.AvoidDeeplyNestedIfStmts", "PMD.GodClass",
        "PMD.AvoidInstantiatingObjectsInLoops"})
public final class JdbcSceneService implements SceneService {

    /** Expected parts count when parsing a "device:state" scene definition. */
    private static final int DEVICE_STATE_PART_COUNT = 2;

    /** Path to scene schema initialization script in classpath. */
    private static final String INIT_SCRIPT_PATH = "/db/init-scenes.sql";
    /** Lock for singleton synchronization. */
    private static final Object INSTANCE_LOCK = new Object();
    /** Singleton instance. */
    private static JdbcSceneService instance;

    /** Observable mirror of the scenes table — bound by the UI. */
    private final ObservableList<Scene> scenes = FXCollections.observableArrayList();
    /** Flag indicating database schema is initialized. */
    private final AtomicBoolean schemaReady = new AtomicBoolean(false);

    private JdbcSceneService() {
        refreshScenes();
        seedDemoScenes();
    }

    /**
     * Returns the singleton instance of the JDBC scene service.
     *
     * @return singleton JdbcSceneService instance
     */
    public static JdbcSceneService getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new JdbcSceneService();
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
    public ObservableList<Scene> getScenes() {
        return scenes;
    }

    @Override
    public Scene addScene(String name, String description) {
        return addScene(name, description, List.of());
    }

    @Override
    public Scene addScene(String name, String description, List<String> deviceStates) {
        Scene created = null;
        if (name != null && !name.isBlank()) {
            String generatedId = "scene-" + java.util.UUID.randomUUID().toString();
            try (Connection conn = openConnection()) {
                ensureSchema(conn);
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO scenes (id, name, description) VALUES (?, ?, ?)")) {
                        stmt.setString(1, generatedId);
                        stmt.setString(2, name);
                        stmt.setString(3, description);
                        stmt.executeUpdate();
                    }
                    insertDeviceStates(conn, generatedId, deviceStates);
                    conn.commit();
                    created = new Scene(generatedId, name, description);
                    created.setDeviceStates(deviceStates);
                    scenes.add(created);
                } catch (SQLException e) {
                    conn.rollback();
                    System.getLogger(JdbcSceneService.class.getName()).log(
                            System.Logger.Level.ERROR, "Failed to add scene", e);
                }
            } catch (SQLException e) {
                System.getLogger(JdbcSceneService.class.getName()).log(
                        System.Logger.Level.ERROR, "Database connection error", e);
            }
        }
        return created;
    }

    @Override
    public boolean updateScene(String sceneId, String name, String description) {
        return updateScene(sceneId, name, description, List.of());
    }

    @Override
    public boolean updateScene(String sceneId, String name, String description, List<String> deviceStates) {
        boolean success = false;
        try (Connection conn = openConnection()) {
            ensureSchema(conn);
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE scenes SET name = ?, description = ?,"
                        + " updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                    stmt.setString(1, name);
                    stmt.setString(2, description);
                    stmt.setString(3, sceneId);
                    success = stmt.executeUpdate() > 0;
                }
                if (success) {
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "DELETE FROM scene_device_states WHERE scene_id = ?")) {
                        stmt.setString(1, sceneId);
                        stmt.executeUpdate();
                    }
                    insertDeviceStates(conn, sceneId, deviceStates);
                    conn.commit();
                    Scene memScene = scenes.stream()
                            .filter(s -> s.getId().equals(sceneId))
                            .findFirst().orElse(null);
                    if (memScene != null) {
                        memScene.setName(name);
                        memScene.setDescription(description);
                        memScene.setDeviceStates(deviceStates);
                    }
                } else {
                    conn.rollback();
                }
            } catch (SQLException e) {
                conn.rollback();
                System.getLogger(JdbcSceneService.class.getName()).log(
                        System.Logger.Level.ERROR, "Failed to update scene", e);
            }
        } catch (SQLException e) {
            System.getLogger(JdbcSceneService.class.getName()).log(
                    System.Logger.Level.ERROR, "Database connection error", e);
        }
        return success;
    }

    @Override
    public boolean deleteScene(String sceneId) {
        boolean success = false;
        try (Connection conn = openConnection()) {
            ensureSchema(conn);
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM scenes WHERE id = ?")) {
                stmt.setString(1, sceneId);
                if (stmt.executeUpdate() > 0) {
                    scenes.removeIf(s -> s.getId().equals(sceneId));
                    success = true;
                }
            }
        } catch (SQLException e) {
            System.getLogger(JdbcSceneService.class.getName()).log(
                    System.Logger.Level.ERROR, "Failed to delete scene", e);
        }
        return success;
    }

    @Override
    public boolean activateScene(String sceneId) {
        boolean activated = false;
        Scene sceneToActivate = scenes.stream()
                .filter(s -> s.getId().equals(sceneId))
                .findFirst()
                .orElse(null);

        if (sceneToActivate != null) {
            int appliedStates = 0;
            int skippedStates = 0;
            LogService logService = ServiceRegistry.getLogService();
            NotificationService notificationService = ServiceRegistry.getNotificationService();

            for (String deviceState : sceneToActivate.getDeviceStates()) {
                if (applyDeviceState(sceneToActivate.getName(), deviceState, logService)) {
                    appliedStates++;
                } else {
                    skippedStates++;
                }
            }

            logService.addLogEntry(
                    "Scene: " + sceneToActivate.getName(),
                    "Overall",
                    "Activated with " + appliedStates + " applied state"
                            + (appliedStates == 1 ? "" : "s")
                            + (skippedStates > 0 ? " and " + skippedStates + " skipped" : ""),
                    "User"
            );

            if (skippedStates == 0) {
                notificationService.addNotification(
                        "Scene '" + sceneToActivate.getName() + "' activated successfully",
                        NotificationType.SUCCESS);
            } else {
                notificationService.addNotification(
                        "Scene '" + sceneToActivate.getName() + "' activated with "
                                + skippedStates + " skipped device state(s)",
                        NotificationType.INFO);
            }
            activated = true;
        }
        return activated;
    }

    private void insertDeviceStates(Connection conn, String sceneId,
                                    List<String> deviceStates) throws SQLException {
        if (deviceStates != null && !deviceStates.isEmpty()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO scene_device_states (scene_id, device_state, sort_order)"
                    + " VALUES (?, ?, ?)")) {
                for (int i = 0; i < deviceStates.size(); i++) {
                    stmt.setString(1, sceneId);
                    stmt.setString(2, deviceStates.get(i));
                    stmt.setInt(3, i);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        }
    }

    private void refreshScenes() {
        scenes.clear();
        try (Connection conn = openConnection()) {
            ensureSchema(conn);
            try (Statement stmt = conn.createStatement();
                 ResultSet resultSet = stmt.executeQuery(
                         "SELECT id, name, description FROM scenes ORDER BY created_at, id")) {
                while (resultSet.next()) {
                    String sceneId = resultSet.getString("id");
                    String sceneName = resultSet.getString("name");
                    String sceneDescription = resultSet.getString("description");
                    Scene loadedScene = new Scene(sceneId, sceneName, sceneDescription);
                    loadedScene.setDeviceStates(loadDeviceStates(conn, sceneId));
                    scenes.add(loadedScene);
                }
            }
        } catch (SQLException e) {
            System.getLogger(JdbcSceneService.class.getName()).log(
                    System.Logger.Level.ERROR, "Failed to refresh scenes", e);
        }
    }

    private List<String> loadDeviceStates(Connection conn, String sceneId) throws SQLException {
        List<String> states = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT device_state FROM scene_device_states"
                + " WHERE scene_id = ? ORDER BY sort_order, id")) {
            stmt.setString(1, sceneId);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    states.add(resultSet.getString("device_state"));
                }
            }
        }
        return states;
    }

    private void seedDemoScenes() {
        if (!scenes.isEmpty()) {
            return;
        }
        addScene("Movie Night", "Dim all lights and close shutters",
                List.of("Dimmer Light: 20%", "Main Light: OFF", "Ceiling Light: OFF"));
        addScene("Away", "Reduce energy usage while nobody is home",
                List.of("Main Light: OFF", "Dimmer Light: 0%", "Ceiling Light: OFF",
                        "Thermostat: 18°C", "Exhaust Fan: OFF"));
        addScene("Morning", "Wake up routine",
                List.of("Main Light: ON", "Dimmer Light: 75%", "Ceiling Light: ON",
                        "Temperature Control: 22°C"));
    }

    private boolean applyDeviceState(String sceneName, String stateDefinition,
                                     LogService logService) {
        boolean applied = false;
        String[] parts = stateDefinition.split(":", DEVICE_STATE_PART_COUNT);
        if (parts.length == DEVICE_STATE_PART_COUNT) {
            String deviceName = parts[0].trim();
            String targetState = parts[1].trim();
            RoomService roomService = ServiceRegistry.getRoomService();
            Device device = roomService.getDeviceByName(deviceName);
            if (device != null) {
                applied = applyStateToDevice(device, targetState);
                if (applied) {
                    logService.addLogEntry(
                            device.getName(), device.getRoom(),
                            "Scene '" + sceneName + "' set state to " + targetState,
                            "Scene");
                }
            }
        }
        return applied;
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private boolean applyStateToDevice(Device device, String targetState) {
        boolean applied = false;
        String normalized = targetState.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith("%")) {
            try {
                int brightness = Integer.parseInt(
                        normalized.substring(0, normalized.length() - 1).trim());
                device.setBrightness(brightness);
                device.setState(brightness > 0);
                applied = true;
            } catch (NumberFormatException exception) {
                applied = false;
            }
        } else if (normalized.endsWith("°c")) {
            try {
                double temperature = Double.parseDouble(normalized.replace("°c", "").trim());
                device.setTemperature(temperature);
                device.setState(true);
                applied = true;
            } catch (NumberFormatException exception) {
                applied = false;
            }
        } else {
            applied = switch (normalized) {
                case "on", "open", "opened" -> {
                    device.setState(true);
                    yield true;
                }
                case "off", "close", "closed" -> {
                    device.setState(false);
                    yield true;
                }
                default -> false;
            };
        }
        return applied;
    }

    private Connection openConnection() throws SQLException {
        DatabaseSettings settings = DatabaseConfig.load()
                .orElseThrow(() -> new IllegalStateException("Scene database is not configured."));
        return DriverManager.getConnection(
                settings.jdbcUrl(), settings.username(), settings.password());
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    private void ensureSchema(Connection connection) {
        if (!schemaReady.get()) {
            synchronized (INSTANCE_LOCK) {
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
                                "Failed to initialize scene schema.", exception);
                    }
                }
            }
        }
    }

    private String loadInitScript() {
        try (InputStream scriptStream = getClass().getResourceAsStream(INIT_SCRIPT_PATH)) {
            if (scriptStream == null) {
                throw new IllegalStateException(
                        "Scene schema script not found at " + INIT_SCRIPT_PATH);
            }
            return new String(scriptStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read scene schema script.", exception);
        }
    }
}
