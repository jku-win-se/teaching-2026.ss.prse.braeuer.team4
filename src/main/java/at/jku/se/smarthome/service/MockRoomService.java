package at.jku.se.smarthome.service;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock Room Service providing room management functionality.
 */
public class MockRoomService {
    
    private static MockRoomService instance;
    private final ObservableList<Room> rooms;
    private final MockUserService userService = MockUserService.getInstance();
    private final MockLogService logService = MockLogService.getInstance();
    
    private MockRoomService() {
        this.rooms = FXCollections.observableArrayList();
        // initializeMockRooms(); // Removed hardcoded data
    }
    
    public static synchronized MockRoomService getInstance() {
        if (instance == null) {
            instance = new MockRoomService();
        }
        return instance;
    }

    /**
     * Resets the singleton for unit testing.
     * Must NOT be called from production code.
     */
    static synchronized void resetForTesting() {
        instance = null;
    }

    /**
     * Gets all rooms.
     */
    public ObservableList<Room> getRooms() {
        return rooms;
    }
    
    /**
     * Adds a new room.
     */
    public Room addRoom(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null; // Invalid name
        }
        String trimmedName = name.trim();
        if (rooms.stream().anyMatch(r -> r.getName().equalsIgnoreCase(trimmedName))) {
            return null; // Duplicate name
        }
        Room room = new Room(
                "room-" + String.format("%03d", rooms.size() + 1),
                trimmedName,
                0
        );
        rooms.add(room);
        logService.addLogEntry("", trimmedName, "Room created", userService.getCurrentUserEmail());
        return room;
    }
    
    /**
     * Updates a room name.
     */
    public boolean updateRoomName(String roomId, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            return false; // Invalid name
        }
        String trimmedName = newName.trim();
        Room room = rooms.stream()
                .filter(r -> r.getId().equals(roomId))
                .findFirst()
                .orElse(null);
        
        if (room != null && !room.getName().equalsIgnoreCase(trimmedName)) {
            if (rooms.stream().anyMatch(r -> !r.getId().equals(roomId) && r.getName().equalsIgnoreCase(trimmedName))) {
                return false; // Duplicate name
            }
            String oldName = room.getName();
            room.setName(trimmedName);
            logService.addLogEntry("", trimmedName, "Room renamed from " + oldName, userService.getCurrentUserEmail());
            return true;
        }
        return false;
    }
    
    /**
     * Deletes a room.
     */
    public boolean deleteRoom(String roomId) {
        Room room = getRoomById(roomId);
        if (room != null) {
            logService.addLogEntry("", room.getName(), "Room deleted", userService.getCurrentUserEmail());
            return rooms.remove(room);
        }
        return false;
    }
    
    /**
     * Gets a room by ID.
     */
    public Room getRoomById(String roomId) {
        return rooms.stream()
                .filter(r -> r.getId().equals(roomId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets all devices across all rooms.
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
     */
    public Device getDeviceByName(String deviceName) {
        return getAllDevices().stream()
                .filter(device -> deviceName.equals(device.getName()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Adds a device to a room.
     */
    public Device addDeviceToRoom(String roomId, String deviceName, String deviceType) {
        Room room = getRoomById(roomId);
        if (room != null) {
            Device device = new Device(
                    "dev-" + String.format("%03d", 1000 + (int)(Math.random() * 9000)),
                    deviceName,
                    deviceType,
                    room.getName(),
                    true
            );
            room.addDevice(device);
            return device;
        }
        return null;
    }
    
    /**
     * Removes a device from a room.
     */
    public boolean removeDeviceFromRoom(String roomId, String deviceId) {
        Room room = getRoomById(roomId);
        if (room != null) {
            return room.getDevices().removeIf(d -> d.getId().equals(deviceId));
        }
        return false;
    }
    
    /**
     * Renames a device.
     */
    public boolean renameDevice(String roomId, String deviceId, String newName) {
        if (newName == null || newName.isBlank()) {
            return false;
        }
        Room room = getRoomById(roomId);
        if (room != null) {
            Device device = room.getDevices().stream()
                    .filter(d -> d.getId().equals(deviceId))
                    .findFirst()
                    .orElse(null);
            if (device != null) {
                device.setName(newName);
                return true;
            }
        }
        return false;
    }
}
