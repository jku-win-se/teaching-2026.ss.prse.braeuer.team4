package at.jku.se.smarthome.service.api;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import javafx.collections.ObservableList;

/**
 * Defines room and device operations exposed to the smart-home UI and services.
 */
public interface RoomService {

    ObservableList<Room> getRooms();

    Room getRoomById(String roomId);

    Room addRoom(String name);

    boolean updateRoomName(String roomId, String newName);

    boolean deleteRoom(String roomId);

    ObservableList<Device> getAllDevices();

    Device getDeviceById(String deviceId);

    Device getDeviceByName(String deviceName);

    Device addDeviceToRoom(String roomId, String deviceName, String deviceType);

    boolean removeDeviceFromRoom(String roomId, String deviceId);

    boolean renameDevice(String roomId, String deviceId, String newName);
}

