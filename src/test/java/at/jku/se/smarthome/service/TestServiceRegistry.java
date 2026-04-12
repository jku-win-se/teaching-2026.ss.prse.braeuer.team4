package at.jku.se.smarthome.service;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.api.RoomService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Basic tests for ServiceRegistry test helpers.
 */
public class TestServiceRegistry {

    @After
    public void tearDown() {
        ServiceRegistry.resetRoomServiceForTesting();
    }

    @Test
    public void setAndResetRoomServiceForTesting_works() {
        RoomService dummy = new RoomService() {
            @Override
            public ObservableList<Room> getRooms() { return FXCollections.observableArrayList(); }

            @Override
            public Room getRoomById(String roomId) { return null; }

            @Override
            public Room addRoom(String name) { return null; }

            @Override
            public boolean updateRoomName(String roomId, String newName) { return false; }

            @Override
            public boolean deleteRoom(String roomId) { return false; }

            @Override
            public ObservableList<Device> getAllDevices() { return FXCollections.observableArrayList(); }

            @Override
            public Device getDeviceById(String deviceId) { return null; }

            @Override
            public Device getDeviceByName(String deviceName) { return null; }

            @Override
            public Device addDeviceToRoom(String roomId, String deviceName, String deviceType) { return null; }

            @Override
            public boolean removeDeviceFromRoom(String roomId, String deviceId) { return false; }

            @Override
            public boolean renameDevice(String roomId, String deviceId, String newName) { return false; }
        };

        ServiceRegistry.setRoomServiceForTesting(dummy);
        RoomService resolved = ServiceRegistry.getRoomService();
        assertNotNull(resolved);
        assertEquals(dummy, resolved);

        ServiceRegistry.resetRoomServiceForTesting();
        // after reset we expect a non-null service created by the registry (may be JDBC-backed)
        RoomService afterReset = ServiceRegistry.getRoomService();
        assertNotNull(afterReset);
    }
}

