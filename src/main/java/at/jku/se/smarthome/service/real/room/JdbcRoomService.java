package at.jku.se.smarthome.service.real.room;

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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import at.jku.se.smarthome.config.DatabaseConfig;
import at.jku.se.smarthome.config.DatabaseSettings;
import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.service.api.RoomService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.api.UserService;
import at.jku.se.smarthome.service.mock.MockLogService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * JDBC-backed RoomService implementation. Persists rooms and devices to the configured database.
 */
public final class JdbcRoomService implements RoomService {

    private static final String INIT_SCRIPT_PATH = "/db/init-rooms.sql";
    private static JdbcRoomService instance;

    private final ObservableList<Room> rooms = FXCollections.observableArrayList();
    private final MockLogService logService = MockLogService.getInstance();
    private final UserService userService = ServiceRegistry.getUserService();
    private final AtomicBoolean schemaReady = new AtomicBoolean(false);

    private JdbcRoomService() {
        refreshRooms();
    }

    /**
     * Returns the singleton instance of the JDBC room service.
     * <p>
     * The instance is lazily created on first access and will load existing
     * rooms/devices from the configured database.
     *
     * @return singleton JdbcRoomService instance
     */
    public static synchronized JdbcRoomService getInstance() {
        if (instance == null) {
            instance = new JdbcRoomService();
        }
        return instance;
    }

    /**
     * Resets the singleton instance for test isolation.
     * <p>
     * Tests should call this between test cases to ensure a fresh instance is
     * created on next {@link #getInstance()} and no state is shared.
     */
    public static synchronized void resetForTesting() {
        instance = null;
    }

    @Override
    /**
     * Returns an observable list of rooms. The list is suitable for direct
     * JavaFX UI binding and is kept in sync with persisted changes performed
     * through this service.
     *
     * @return observable list of rooms
     */
    public ObservableList<Room> getRooms() {
        return rooms;
    }

    @Override
    /**
     * Finds a room by its identifier from the in-memory view.
     *
     * @param roomId unique room identifier
     * @return matching Room or null when not found
     */
    public Room getRoomById(String roomId) {
        return rooms.stream()
                .filter(r -> r.getId().equals(roomId))
                .findFirst()
                .orElse(null);
    }

    @Override
    /**
     * Creates and persists a new room with the provided display name.
     * <p>
     * The returned Room is also added to the observable in-memory list so UI
     * bindings update automatically.
     *
     * @param name display name of the room
     * @return created Room instance or null when name is invalid
     */
    public Room addRoom(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String trimmed = name.trim();
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            String id = UUID.randomUUID().toString();
            try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO rooms (id, name) VALUES (?, ?)")) {
                stmt.setString(1, id);
                stmt.setString(2, trimmed);
                stmt.executeUpdate();
            }
            Room room = new Room(id, trimmed, 0);
            rooms.add(room);
            logService.addLogEntry("", trimmed, "Room created", userService.getCurrentUserEmail());
            return room;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to persist room.", e);
        }
    }

    @Override
    /**
     * Updates the persisted name of the specified room and refreshes the
     * in-memory Room object and its devices.
     *
     * @param roomId identifier of the room to update
     * @param newName new display name
     * @return true when the update succeeded, otherwise false
     */
    public boolean updateRoomName(String roomId, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            return false;
        }
        String trimmed = newName.trim();
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement stmt = connection.prepareStatement("UPDATE rooms SET name = ? WHERE id = ?")) {
                stmt.setString(1, trimmed);
                stmt.setString(2, roomId);
                if (stmt.executeUpdate() == 0) {
                    return false;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update room.", e);
        }

        Room room = getRoomById(roomId);
        if (room != null) {
            String old = room.getName();
            room.setName(trimmed);
            // update device room names in-memory
            room.getDevices().forEach(d -> d.setRoom(trimmed));
            logService.addLogEntry("", trimmed, "Room renamed from " + old, userService.getCurrentUserEmail());
            return true;
        }
        return false;
    }

    @Override
    /**
     * Deletes the room and all devices assigned to it from the database and
     * removes the Room from the in-memory list.
     *
     * @param roomId identifier of the room to delete
     * @return true when the room existed and was removed, otherwise false
     */
    public boolean deleteRoom(String roomId) {
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            // delete devices first
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM devices WHERE room_id = ?")) {
                stmt.setString(1, roomId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM rooms WHERE id = ?")) {
                stmt.setString(1, roomId);
                if (stmt.executeUpdate() == 0) {
                    return false;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete room.", e);
        }

        Room room = getRoomById(roomId);
        if (room != null) {
            logService.addLogEntry("", room.getName(), "Room deleted", userService.getCurrentUserEmail());
            rooms.remove(room);
            return true;
        }
        return false;
    }

    @Override
    /**
     * Returns a flat observable list of all devices across all rooms. The
     * returned list is a snapshot (new ObservableList) composed from the
     * per-room device lists.
     *
     * @return observable list of all devices
     */
    public ObservableList<Device> getAllDevices() {
        ObservableList<Device> devices = FXCollections.observableArrayList();
        for (Room room : rooms) {
            devices.addAll(room.getDevices());
        }
        return devices;
    }

    @Override
    /**
     * Finds a device by its identifier across all rooms.
     *
     * @param deviceId unique device identifier
     * @return matching Device or null when not found
     */
    public Device getDeviceById(String deviceId) {
        return getAllDevices().stream()
                .filter(d -> d.getId().equals(deviceId))
                .findFirst()
                .orElse(null);
    }

    @Override
    /**
     * Finds a device by its display name across all rooms.
     *
     * @param deviceName device display name
     * @return matching Device or null when not found
     */
    public Device getDeviceByName(String deviceName) {
        return getAllDevices().stream()
                .filter(d -> d.getName().equals(deviceName))
                .findFirst()
                .orElse(null);
    }

    @Override
    /**
     * Adds a new device to the specified room and persists it to the database.
     * The deviceType is validated and normalized. The created Device is added
     * to the in-memory Room device list and returned.
     *
     * @param roomId identifier of the room
     * @param deviceName display name for the device
     * @param deviceType device type (e.g. SWITCH, DIMMER, THERMOSTAT, SENSOR, BLIND)
     * @return created Device or null when inputs are invalid
     */
    public Device addDeviceToRoom(String roomId, String deviceName, String deviceType) {
        if (deviceName == null || deviceName.trim().isEmpty() || deviceType == null || deviceType.trim().isEmpty()) {
            return null;
        }
        Room room = getRoomById(roomId);
        if (room == null) {
            return null;
        }
        String normalizedType = normalizeDeviceType(deviceType);
        if (normalizedType == null) {
            return null; // invalid type
        }

        String id = UUID.randomUUID().toString();
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO devices (id, name, type, room_id, state, brightness, temperature) VALUES (?, ?, ?, ?, ?, ?, ?)") ) {
                stmt.setString(1, id);
                stmt.setString(2, deviceName.trim());
                stmt.setString(3, normalizedType);
                stmt.setString(4, roomId);
                stmt.setBoolean(5, true);
                stmt.setInt(6, 100);
                stmt.setDouble(7, 20.0);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to persist device.", e);
        }

        Device device = new Device(id, deviceName.trim(), normalizedType, room.getName(), true);
        room.addDevice(device);
        logService.addLogEntry(device.getName(), room.getName(), "Device created", userService.getCurrentUserEmail());
        return device;
    }

    @Override
    /**
     * Removes the specified device from the database and the in-memory room
     * device list.
     *
     * @param roomId identifier of the room
     * @param deviceId identifier of the device to remove
     * @return true when the device existed and was removed, otherwise false
     */
    public boolean removeDeviceFromRoom(String roomId, String deviceId) {
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM devices WHERE id = ? AND room_id = ?")) {
                stmt.setString(1, deviceId);
                stmt.setString(2, roomId);
                if (stmt.executeUpdate() == 0) {
                    return false;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete device.", e);
        }

        Room room = getRoomById(roomId);
        if (room != null) {
            boolean removed = room.getDevices().removeIf(d -> d.getId().equals(deviceId));
            return removed;
        }
        return false;
    }

    @Override
    /**
     * Renames a device both in the database and in the in-memory representation.
     *
     * @param roomId identifier of the room
     * @param deviceId identifier of the device to rename
     * @param newName new display name for the device
     * @return true when the device existed and was updated, otherwise false
     */
    public boolean renameDevice(String roomId, String deviceId, String newName) {
        if (newName == null || newName.isBlank()) {
            return false;
        }
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement stmt = connection.prepareStatement("UPDATE devices SET name = ? WHERE id = ? AND room_id = ?")) {
                stmt.setString(1, newName.trim());
                stmt.setString(2, deviceId);
                stmt.setString(3, roomId);
                if (stmt.executeUpdate() == 0) {
                    return false;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to rename device.", e);
        }

        Room room = getRoomById(roomId);
        if (room != null) {
            Device device = room.getDevices().stream().filter(d -> d.getId().equals(deviceId)).findFirst().orElse(null);
            if (device != null) {
                device.setName(newName.trim());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean updateDeviceState(String deviceId, boolean state) {
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE devices SET state = ? WHERE id = ?")) {
                stmt.setBoolean(1, state);
                stmt.setString(2, deviceId);
                if (stmt.executeUpdate() == 0) return false;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update device state.", e);
        }
        Device device = getDeviceById(deviceId);
        if (device != null) {
            device.setState(state);
            return true;
        }
        return false;
    }

    @Override
    public boolean updateDeviceBrightness(String deviceId, int brightness) {
        boolean newState = brightness > 0;
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE devices SET brightness = ?, state = ? WHERE id = ?")) {
                stmt.setInt(1, brightness);
                stmt.setBoolean(2, newState);
                stmt.setString(3, deviceId);
                if (stmt.executeUpdate() == 0) return false;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update device brightness.", e);
        }
        Device device = getDeviceById(deviceId);
        if (device != null) {
            device.setBrightness(brightness);
            device.setState(newState);
            return true;
        }
        return false;
    }

    @Override
    /**
     * Updates the temperature setting of a device in database and in-memory list.
     *
     * @param deviceId identifier of the device
     * @param temperature new temperature value
     * @return true when the device existed and was updated, otherwise false
     */
    public boolean updateDeviceTemperature(String deviceId, double temperature) {
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE devices SET temperature = ? WHERE id = ?")) {
                stmt.setDouble(1, temperature);
                stmt.setString(2, deviceId);
                if (stmt.executeUpdate() == 0) return false;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update device temperature.", e);
        }
        Device device = getDeviceById(deviceId);
        if (device != null) {
            device.setTemperature(temperature);
            return true;
        }
        return false;
    }

    /**
     * Helper: refreshes room list from database.
     */
    private void refreshRooms() {
        List<Room> loaded = new ArrayList<>();
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement stmt = connection.prepareStatement("SELECT id, name FROM rooms ORDER BY name")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String roomId = rs.getString("id");
                        String name = rs.getString("name");
                        Room room = new Room(roomId, name, 0);
                        // load devices for room
                        try (PreparedStatement devStmt = connection.prepareStatement("SELECT id, name, type, state, brightness, temperature FROM devices WHERE room_id = ? ORDER BY name")) {
                            devStmt.setString(1, roomId);
                            try (ResultSet drs = devStmt.executeQuery()) {
                                while (drs.next()) {
                                    Device device = new Device(
                                            drs.getString("id"),
                                            drs.getString("name"),
                                            drs.getString("type"),
                                            name,
                                            drs.getBoolean("state")
                                    );
                                    device.setBrightness(drs.getInt("brightness"));
                                    device.setTemperature(drs.getDouble("temperature"));
                                    room.addDevice(device);
                                }
                            }
                        }
                        loaded.add(room);
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load rooms from the database.", e);
        }

        rooms.setAll(loaded);
    }

    /**
     * Helper: normalizes device type string to standard form.
     *
     * @param deviceType device type string to normalize
     * @return normalized device type or null if invalid
     */
    private String normalizeDeviceType(String deviceType) {
        if (deviceType == null) return null;
        String t = deviceType.trim().toLowerCase(Locale.ENGLISH);
        return switch (t) {
            case "switch", "schalter" -> "Switch";
            case "dimmer" -> "Dimmer";
            case "thermostat" -> "Thermostat";
            case "sensor" -> "Sensor";
            case "blind", "blinds", "shutter", "shutters", "cover", "cover/blind", "coverblind" -> "Cover/Blind";
            default -> null;
        };
    }

    /**
     * Helper: opens a database connection using configured settings.
     *
     * @return open database connection
     * @throws SQLException if connection fails
     */
    private Connection openConnection() throws SQLException {
        DatabaseSettings settings = DatabaseConfig.load()
                .orElseThrow(() -> new IllegalStateException("Room database is not configured."));
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
                throw new IllegalStateException("Failed to initialize rooms schema.", e);
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
            if (in == null) throw new IllegalStateException("Room schema script not found at " + INIT_SCRIPT_PATH);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read room schema script.", e);
        }
    }
}

