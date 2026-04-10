package at.jku.se.smarthome.service.mock;

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
    
    private MockRoomService() {
        this.rooms = FXCollections.observableArrayList();
        initializeMockRooms();
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
    public static synchronized void resetForTesting() {
        instance = null;
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
        Room room = new Room(
                "room-" + String.format("%03d", rooms.size() + 1),
                name,
                0
        );
        rooms.add(room);
        return room;
    }
    
    /**
     * Updates a room name.
     */
    public boolean updateRoomName(String roomId, String newName) {
        Room room = rooms.stream()
                .filter(r -> r.getId().equals(roomId))
                .findFirst()
                .orElse(null);
        
        if (room != null) {
            room.setName(newName);
            return true;
        }
        return false;
    }
    
    /**
     * Deletes a room.
     */
    public boolean deleteRoom(String roomId) {
        return rooms.removeIf(r -> r.getId().equals(roomId));
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

    public Device getDeviceById(String deviceId) {
        return getAllDevices().stream()
                .filter(device -> deviceId.equals(device.getId()))
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
