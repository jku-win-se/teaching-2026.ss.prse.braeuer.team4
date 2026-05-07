package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.service.mock.MockRoomService;

/**
 * Tests for FR-05: remove and rename existing devices via {@link MockRoomService}.
 *
 * Room/device IDs from mock data:
 *   room-001 / Living Room  → dev-001 (Main Light), dev-002 (Dimmer Light)
 *   room-002 / Bedroom      → dev-003 (Bed Light),  dev-004 (Temperature Control)
 *   room-003 / Kitchen      → dev-005 (Ceiling Light)
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods", "PMD.TooManyStaticImports"})
public class TestMockRoomServiceDeviceManagement {


    /** Mock room service for testing. */
    private MockRoomService service;

    /**
     * Sets up test fixtures before each test.
     */
    @Before
    public void setUp() {
        MockRoomService.resetForTesting();
        service = MockRoomService.getInstance();
    }

    // -----------------------------------------------------------------------
    // renameDevice
    // -----------------------------------------------------------------------

    /**
     * Tests valid device rename returns true.
     */
    @Test
    public void testRenameDeviceValidReturnsTrue() {
        boolean result = service.renameDevice("room-001", "dev-001", "Hallway Lamp");
        assertTrue(result);
    }

    /**
     * Tests valid device rename changes the device name.
     */
    @Test
    public void testRenameDeviceValidChangesName() {
        service.renameDevice("room-001", "dev-001", "Hallway Lamp");
        assertEquals("Hallway Lamp", service.getRoomById("room-001")
                .getDevices().stream()
                .filter(d -> d.getId().equals("dev-001"))
                .findFirst().get().getName());
    }

    /** Renaming with a blank name is rejected. */
    @Test
    public void testRenameDeviceBlankNameReturnsFalse() {
        assertFalse(service.renameDevice("room-001", "dev-001", "   "));
    }

    /** Renaming with an empty string is rejected. */
    @Test
    public void testRenameDeviceEmptyNameReturnsFalse() {
        assertFalse(service.renameDevice("room-001", "dev-001", ""));
    }

    /** Renaming with null is rejected. */
    @Test
    public void testRenameDeviceNullNameReturnsFalse() {
        assertFalse(service.renameDevice("room-001", "dev-001", null));
    }

    /** Renaming a device that does not exist in the room returns false. */
    @Test
    public void testRenameDeviceUnknownDeviceIdReturnsFalse() {
        assertFalse(service.renameDevice("room-001", "does-not-exist", "New Name"));
    }

    /** Renaming in a room that does not exist returns false. */
    @Test
    public void testRenameDeviceUnknownRoomIdReturnsFalse() {
        assertFalse(service.renameDevice("does-not-exist", "dev-001", "New Name"));
    }

    /** The original name is unchanged after a failed rename. */
    @Test
    public void testRenameDeviceFailedRenameNameUnchanged() {
        service.renameDevice("room-001", "dev-001", "");
        Device device = service.getRoomById("room-001").getDevices().stream()
                .filter(dev -> dev.getId().equals("dev-001"))
                .findFirst().get();
        assertEquals("Main Light", device.getName());
    }

    // -----------------------------------------------------------------------
    // removeDeviceFromRoom
    // -----------------------------------------------------------------------

    /** Removing an existing device returns true. */
    @Test
    public void testRemoveDeviceValidReturnsTrue() {
        boolean result = service.removeDeviceFromRoom("room-001", "dev-001");
        assertTrue(result);
    }

    /** Removing an existing device removes it from the room. */
    @Test
    public void testRemoveDeviceValidRemovesDevice() {
        service.removeDeviceFromRoom("room-001", "dev-001");
        Room room = service.getRoomById("room-001");
        boolean stillExists = room.getDevices().stream()
                .anyMatch(d -> d.getId().equals("dev-001"));
        assertFalse(stillExists);
    }

    /** The other devices in the room are unaffected after a removal. */
    @Test
    public void testRemoveDeviceOtherDevicesUntouched() {
        service.removeDeviceFromRoom("room-001", "dev-001");
        Room room = service.getRoomById("room-001");
        boolean dev002Still = room.getDevices().stream()
                .anyMatch(d -> d.getId().equals("dev-002"));
        assertTrue(dev002Still);
    }

    /** Removing with an unknown device ID returns false. */
    @Test
    public void testRemoveDeviceUnknownDeviceIdReturnsFalse() {
        assertFalse(service.removeDeviceFromRoom("room-001", "does-not-exist"));
    }

    /** Removing from an unknown room ID returns false. */
    @Test
    public void testRemoveDeviceUnknownRoomIdReturnsFalse() {
        assertFalse(service.removeDeviceFromRoom("does-not-exist", "dev-001"));
    }

    /** Removing the same device twice: second call returns false. */
    @Test
    public void testRemoveDeviceRemovedTwiceSecondReturnsFalse() {
        service.removeDeviceFromRoom("room-001", "dev-001");
        assertFalse(service.removeDeviceFromRoom("room-001", "dev-001"));
    }

    // -----------------------------------------------------------------------
    // addRoom
    // -----------------------------------------------------------------------

    /** Adding a valid room returns non-null. */
    @Test
    public void testAddRoomValidReturnsNonNull() {
        assertNotNull(service.addRoom("Garage"));
    }

    /** Adding a valid room increases room count. */
    @Test
    public void testAddRoomValidIncreasesCount() {
        service.addRoom("Garage");
        assertEquals(6, service.getRooms().size());
    }

    /** Adding a duplicate room name returns null. */
    @Test
    public void testAddRoomDuplicateReturnsNull() {
        assertNull(service.addRoom("Living Room"));
    }

    /** Adding a room with null name returns null. */
    @Test
    public void testAddRoomNullNameReturnsNull() {
        assertNull(service.addRoom(null));
    }

    /** Adding a room with blank name returns null. */
    @Test
    public void testAddRoomBlankNameReturnsNull() {
        assertNull(service.addRoom("   "));
    }

    // -----------------------------------------------------------------------
    // updateRoomName
    // -----------------------------------------------------------------------

    /** Updating room name returns true. */
    @Test
    public void testUpdateRoomNameValidReturnsTrue() {
        assertTrue(service.updateRoomName("room-001", "Lounge"));
    }

    /** Updating room name changes the name. */
    @Test
    public void testUpdateRoomNameChangesName() {
        service.updateRoomName("room-001", "Lounge");
        assertEquals("Lounge", service.getRoomById("room-001").getName());
    }

    /** Updating to same name returns false. */
    @Test
    public void testUpdateRoomNameSameNameReturnsFalse() {
        assertFalse(service.updateRoomName("room-001", "Living Room"));
    }

    /** Updating to duplicate name returns false. */
    @Test
    public void testUpdateRoomNameDuplicateNameReturnsFalse() {
        assertFalse(service.updateRoomName("room-001", "Bedroom"));
    }

    /** Updating with null name returns false. */
    @Test
    public void testUpdateRoomNameNullReturnsFalse() {
        assertFalse(service.updateRoomName("room-001", null));
    }

    /** Updating with blank name returns false. */
    @Test
    public void testUpdateRoomNameBlankReturnsFalse() {
        assertFalse(service.updateRoomName("room-001", "  "));
    }

    /** Updating unknown room returns false. */
    @Test
    public void testUpdateRoomNameUnknownRoomReturnsFalse() {
        assertFalse(service.updateRoomName("room-999", "New Name"));
    }

    // -----------------------------------------------------------------------
    // deleteRoom
    // -----------------------------------------------------------------------

    /** Deleting existing room returns true. */
    @Test
    public void testDeleteRoomReturnsTrue() {
        assertTrue(service.deleteRoom("room-001"));
    }

    /** Deleting existing room removes it. */
    @Test
    public void testDeleteRoomRemovesRoom() {
        service.deleteRoom("room-001");
        assertNull(service.getRoomById("room-001"));
    }

    /** Deleting unknown room returns false. */
    @Test
    public void testDeleteUnknownRoomReturnsFalse() {
        assertFalse(service.deleteRoom("room-999"));
    }

    // -----------------------------------------------------------------------
    // device lookups
    // -----------------------------------------------------------------------

    /** getDeviceById returns correct device. */
    @Test
    public void testGetDeviceByIdReturnsDevice() {
        assertNotNull(service.getDeviceById("dev-001"));
    }

    /** getDeviceById returns null for missing. */
    @Test
    public void testGetDeviceByIdReturnsNullForMissing() {
        assertNull(service.getDeviceById("dev-999"));
    }

    /** getDeviceByName returns correct device. */
    @Test
    public void testGetDeviceByNameReturnsDevice() {
        assertNotNull(service.getDeviceByName("Main Light"));
    }

    /** getDeviceByName returns null for missing. */
    @Test
    public void testGetDeviceByNameReturnsNullForMissing() {
        assertNull(service.getDeviceByName("Nonexistent"));
    }

    /** getAllDevices returns all devices from all rooms. */
    @Test
    public void testGetAllDevicesReturnsAll() {
        assertEquals(8, service.getAllDevices().size());
    }

    // -----------------------------------------------------------------------
    // device state / brightness / temperature
    // -----------------------------------------------------------------------

    /** updateDeviceState returns true for existing device. */
    @Test
    public void testUpdateDeviceStateReturnsTrue() {
        assertTrue(service.updateDeviceState("dev-001", false));
    }

    /** updateDeviceState returns false for missing device. */
    @Test
    public void testUpdateDeviceStateMissingReturnsFalse() {
        assertFalse(service.updateDeviceState("dev-999", true));
    }

    /** updateDeviceBrightness returns true for existing device. */
    @Test
    public void testUpdateDeviceBrightnessReturnsTrue() {
        assertTrue(service.updateDeviceBrightness("dev-002", 75));
    }

    /** updateDeviceBrightness returns false for missing device. */
    @Test
    public void testUpdateDeviceBrightnessMissingReturnsFalse() {
        assertFalse(service.updateDeviceBrightness("dev-999", 50));
    }

    /** updateDeviceTemperature returns true for existing device. */
    @Test
    public void testUpdateDeviceTemperatureReturnsTrue() {
        assertTrue(service.updateDeviceTemperature("dev-004", 22.5));
    }

    /** updateDeviceTemperature returns false for missing device. */
    @Test
    public void testUpdateDeviceTemperatureMissingReturnsFalse() {
        assertFalse(service.updateDeviceTemperature("dev-999", 20.0));
    }

    // -----------------------------------------------------------------------
    // addDeviceToRoom
    // -----------------------------------------------------------------------

    /** Adding a device to existing room returns non-null. */
    @Test
    public void testAddDeviceToRoomReturnsDevice() {
        assertNotNull(service.addDeviceToRoom("room-001", "Lamp", "Switch"));
    }

    /** Adding a device to unknown room returns null. */
    @Test
    public void testAddDeviceToUnknownRoomReturnsNull() {
        assertNull(service.addDeviceToRoom("room-999", "Lamp", "Switch"));
    }
}
