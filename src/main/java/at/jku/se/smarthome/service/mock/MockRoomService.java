package at.jku.se.smarthome.service.mock;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.service.api.RoomService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock Room Service providing room management functionality.
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class MockRoomService implements RoomService {

    /** Lock for singleton synchronization. */
    private static final Object INSTANCE_LOCK = new Object();
    /** Singleton instance of the mock room service. */
    private static MockRoomService instance;
    /** Observable list of all rooms. */
    private final ObservableList<Room> rooms;
    /** User service for permission checks. */
    private final MockUserService userService = MockUserService.getInstance();
    /** Log service for activity logging. */
    private final MockLogService logService = MockLogService.getInstance();
    
    private MockRoomService() {
        this.rooms = FXCollections.observableArrayList();
        initializeMockRooms();
    }

    private void initializeMockRooms() {
        Room livingRoom = new Room("room-001", "Living Room", 0);
        livingRoom.addDevice(new Device("dev-001", "Main Light", "Switch", "Living Room", true));
        livingRoom.addDevice(new Device("dev-002", "Dimmer Light", "Dimmer", "Living Room", true));
        rooms.add(livingRoom);

        Room bedroom = new Room("room-002", "Bedroom", 0);
        bedroom.addDevice(new Device("dev-003", "Bed Light", "Switch", "Bedroom", false));
        bedroom.addDevice(new Device("dev-004", "Temperature Control", "Thermostat", "Bedroom", true));
        rooms.add(bedroom);

        Room kitchen = new Room("room-003", "Kitchen", 0);
        kitchen.addDevice(new Device("dev-005", "Ceiling Light", "Switch", "Kitchen", true));
        rooms.add(kitchen);

        Room bathroom = new Room("room-004", "Bathroom", 0);
        bathroom.addDevice(new Device("dev-006", "Exhaust Fan", "Switch", "Bathroom", false));
        rooms.add(bathroom);

        Room hallway = new Room("room-005", "Hallway", 0);
        hallway.addDevice(new Device("dev-007", "Motion Sensor", "Sensor", "Hallway", true));
        hallway.addDevice(new Device("dev-008", "Hallway Blind", "Cover/Blind", "Hallway", false));
        rooms.add(hallway);
    }


    public static MockRoomService getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new MockRoomService();
            }
            return instance;
        }
    }

    /**
     * Resets the singleton for unit testing.
     * Must NOT be called from production code.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public static void resetForTesting() {
        synchronized (INSTANCE_LOCK) {
            instance = null;
        }
    }

    /**
     * Gets all rooms.
     *
     * @return observable list of rooms
     */
    public ObservableList<Room> getRooms() {
        return rooms;
    }
    
    /**
        * Adds a new room.
        *
        * @param name display name for the new room
        * @return created room instance
     */
    public Room addRoom(String name) {
        Room result = null;
        if (name != null && !name.isBlank()) {
            String trimmedName = name.trim();
            boolean duplicate = rooms.stream().anyMatch(r -> r.getName().equalsIgnoreCase(trimmedName));
            if (!duplicate) {
                result = new Room(
                        "room-" + String.format("%03d", rooms.size() + 1),
                        trimmedName,
                        0
                );
                rooms.add(result);
                logService.addLogEntry("", trimmedName, "Room created", userService.getCurrentUserEmail());
            }
        }
        return result;
    }
    
    /**
        * Updates a room name.
        *
        * @param roomId identifier of the room to update
        * @param newName replacement room name
        * @return true when the room exists and was updated, otherwise false
     */
    public boolean updateRoomName(String roomId, String newName) {
        boolean updated = false;
        if (newName != null && !newName.isBlank()) {
            String trimmedName = newName.trim();
            Room room = rooms.stream()
                    .filter(r -> r.getId().equals(roomId))
                    .findFirst()
                    .orElse(null);

            boolean duplicate = rooms.stream()
                    .anyMatch(r -> !r.getId().equals(roomId) && r.getName().equalsIgnoreCase(trimmedName));
            if (room != null && !room.getName().equalsIgnoreCase(trimmedName) && !duplicate) {
                String oldName = room.getName();
                room.setName(trimmedName);
                logService.addLogEntry("", trimmedName, "Room renamed from " + oldName, userService.getCurrentUserEmail());
                updated = true;
            }
        }
        return updated;
    }
    
    /**
     * Deletes a room.
     *
     * @param roomId identifier of the room to remove
     * @return true when the room existed and was removed, otherwise false
     */
    public boolean deleteRoom(String roomId) {
        boolean deleted = false;
        Room room = getRoomById(roomId);
        if (room != null) {
            logService.addLogEntry("", room.getName(), "Room deleted", userService.getCurrentUserEmail());
            deleted = rooms.remove(room);
        }
        return deleted;
    }
    
    /**
        * Gets a room by ID.
        *
        * @param roomId identifier of the room to retrieve
        * @return matching room, or null when none exists
     */
    public Room getRoomById(String roomId) {
        return rooms.stream()
                .filter(r -> r.getId().equals(roomId))
                .findFirst()
                .orElse(null);
    }

    /**
        * Gets all devices across all rooms.
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

    /**
        * Gets a device by its display name.
        *
        * @param deviceName display name of the device to retrieve
        * @return matching device, or null when none exists
     */
    public Device getDeviceByName(String deviceName) {
        return getAllDevices().stream()
                .filter(device -> deviceName.equals(device.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets a device by its identifier.
     *
     * @param deviceId identifier of the device to retrieve
     * @return matching device, or null when none exists
     */
    public Device getDeviceById(String deviceId) {
        return getAllDevices().stream()
                .filter(device -> deviceId.equals(device.getId()))
                .findFirst()
                .orElse(null);
    }
    
    /**
        * Adds a device to a room.
        *
        * @param roomId identifier of the target room
        * @param deviceName display name for the new device
        * @param deviceType type of the new device
        * @return created device, or null when the room does not exist
     */
    public Device addDeviceToRoom(String roomId, String deviceName, String deviceType) {
        Device result = null;
        Room room = getRoomById(roomId);
        if (room != null) {
            result = new Device(
                    "dev-" + String.format("%03d", 1000 + (int)(Math.random() * 9000)),
                    deviceName,
                    deviceType,
                    room.getName(),
                    true
            );
            room.addDevice(result);
        }
        return result;
    }
    
    /**
        * Removes a device from a room.
        *
        * @param roomId identifier of the room containing the device
        * @param deviceId identifier of the device to remove
        * @return true when the device existed and was removed, otherwise false
     */
    public boolean removeDeviceFromRoom(String roomId, String deviceId) {
        boolean removed = false;
        Room room = getRoomById(roomId);
        if (room != null) {
            removed = room.getDevices().removeIf(d -> d.getId().equals(deviceId));
        }
        return removed;
    }
    
    /**
     * Updates the on/off state of a device (in-memory only).
     *
     * @param deviceId unique device identifier
     * @param state new power state
     * @return true when the device existed and was updated, otherwise false
     */
    public boolean updateDeviceState(String deviceId, boolean state) {
        boolean updated = false;
        Device device = getDeviceById(deviceId);
        if (device != null) {
            device.setState(state);
            updated = true;
        }
        return updated;
    }

    /**
     * Updates the brightness of a device (in-memory only).
     *
     * @param deviceId unique device identifier
     * @param brightness brightness level 0–100
     * @return true when the device existed and was updated, otherwise false
     */
    public boolean updateDeviceBrightness(String deviceId, int brightness) {
        boolean updated = false;
        Device device = getDeviceById(deviceId);
        if (device != null) {
            device.setBrightness(brightness);
            device.setState(brightness > 0);
            updated = true;
        }
        return updated;
    }

    /**
     * Updates the temperature/sensor value of a device (in-memory only).
     *
     * @param deviceId unique device identifier
     * @param temperature new temperature or sensor value
     * @return true when the device existed and was updated, otherwise false
     */
    public boolean updateDeviceTemperature(String deviceId, double temperature) {
        boolean updated = false;
        Device device = getDeviceById(deviceId);
        if (device != null) {
            device.setTemperature(temperature);
            updated = true;
        }
        return updated;
    }

    /**
        * Renames a device.
        *
        * @param roomId identifier of the room containing the device
        * @param deviceId identifier of the device to rename
        * @param newName replacement display name
        * @return true when the device existed and was renamed, otherwise false
     */
    public boolean renameDevice(String roomId, String deviceId, String newName) {
        boolean renamed = false;
        if (newName != null && !newName.isBlank()) {
            Room room = getRoomById(roomId);
            Device device = room == null ? null : room.getDevices().stream()
                    .filter(d -> d.getId().equals(deviceId))
                    .findFirst()
                    .orElse(null);
            if (device != null) {
                device.setName(newName);
                renamed = true;
            }
        }
        return renamed;
    }
}
