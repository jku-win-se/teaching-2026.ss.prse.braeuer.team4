package at.jku.se.smarthome.service.api;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import javafx.collections.ObservableList;

/**
 * Defines operations for managing rooms and devices within the smart-home system.
 * <p>
 * The implementation is expected to provide an ObservableList view for UI binding
 * and to persist changes to the configured data store (e.g. JDBC).
 */
public interface RoomService {

    /**
     * Returns all rooms currently managed by the service.
     *
     * @return observable list of rooms
     */
    ObservableList<Room> getRooms();

    /**
     * Finds a room by its identifier.
     *
     * @param roomId unique room identifier
     * @return matching room, or null when none exists
     */
    Room getRoomById(String roomId);

    /**
     * Creates and stores a new room.
     *
     * @param name display name of the room
     * @return created room
     */
    Room addRoom(String name);

    /**
     * Updates the name of an existing room.
     *
     * @param roomId identifier of the room to update
     * @param newName new display name
     * @return true when the room was updated, otherwise false
     */
    boolean updateRoomName(String roomId, String newName);

    /**
     * Deletes a room and all associated devices.
     *
     * @param roomId identifier of the room to delete
     * @return true when the room existed and was removed, otherwise false
     */
    boolean deleteRoom(String roomId);

    /**
     * Returns all devices across all rooms.
     *
     * @return observable list of devices
     */
    ObservableList<Device> getAllDevices();

    /**
     * Finds a device by its identifier.
     *
     * @param deviceId unique device identifier
     * @return matching device, or null when none exists
     */
    Device getDeviceById(String deviceId);

    /**
     * Finds a device by its display name.
     *
     * @param deviceName device display name
     * @return matching device, or null when none exists
     */
    Device getDeviceByName(String deviceName);

    /**
     * Adds a new device to the specified room.
     *
     * @param roomId identifier of the room
     * @param deviceName display name for the device
     * @param deviceType device type (e.g. SWITCH, DIMMER, THERMOSTAT, SENSOR, BLIND)
     * @return created device
     */
    Device addDeviceToRoom(String roomId, String deviceName, String deviceType);

    /**
     * Removes a device from the specified room.
     *
     * @param roomId identifier of the room
     * @param deviceId identifier of the device to remove
     * @return true when the device existed and was removed, otherwise false
     */
    boolean removeDeviceFromRoom(String roomId, String deviceId);

    /**
     * Renames a device within a room.
     *
     * @param roomId identifier of the room
     * @param deviceId identifier of the device to rename
     * @param newName new display name for the device
     * @return true when the device existed and was updated, otherwise false
     */
    boolean renameDevice(String roomId, String deviceId, String newName);

    /**
     * Updates the on/off state of a device and persists the change.
     *
     * @param deviceId unique device identifier
     * @param state new power state
     * @return true when the device existed and was updated, otherwise false
     */
    boolean updateDeviceState(String deviceId, boolean state);

    /**
     * Updates the brightness of a dimmer device and persists the change.
     * Also sets state to true when brightness &gt; 0, and false when brightness == 0.
     *
     * @param deviceId unique device identifier
     * @param brightness brightness level 0–100
     * @return true when the device existed and was updated, otherwise false
     */
    boolean updateDeviceBrightness(String deviceId, int brightness);

    /**
     * Updates the temperature or sensor reading of a device and persists the change.
     *
     * @param deviceId unique device identifier
     * @param temperature new temperature or sensor value
     * @return true when the device existed and was updated, otherwise false
     */
    boolean updateDeviceTemperature(String deviceId, double temperature);
}

