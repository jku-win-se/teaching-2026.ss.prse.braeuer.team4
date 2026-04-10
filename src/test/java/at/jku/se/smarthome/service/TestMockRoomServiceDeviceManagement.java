package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
public class TestMockRoomServiceDeviceManagement {

    private MockRoomService service;

    @Before
    public void setUp() {
        MockRoomService.resetForTesting();
        service = MockRoomService.getInstance();
    }

    // -----------------------------------------------------------------------
    // renameDevice
    // -----------------------------------------------------------------------

    /** A valid rename changes the device name. */
    @Test
    public void testRenameDevice_valid() {
        boolean result = service.renameDevice("room-001", "dev-001", "Hallway Lamp");
        assertTrue(result);
        assertEquals("Hallway Lamp", service.getRoomById("room-001")
                .getDevices().stream()
                .filter(d -> d.getId().equals("dev-001"))
                .findFirst().get().getName());
    }

    /** Renaming with a blank name is rejected. */
    @Test
    public void testRenameDevice_blankName_returnsFalse() {
        assertFalse(service.renameDevice("room-001", "dev-001", "   "));
    }

    /** Renaming with an empty string is rejected. */
    @Test
    public void testRenameDevice_emptyName_returnsFalse() {
        assertFalse(service.renameDevice("room-001", "dev-001", ""));
    }

    /** Renaming with null is rejected. */
    @Test
    public void testRenameDevice_nullName_returnsFalse() {
        assertFalse(service.renameDevice("room-001", "dev-001", null));
    }

    /** Renaming a device that does not exist in the room returns false. */
    @Test
    public void testRenameDevice_unknownDeviceId_returnsFalse() {
        assertFalse(service.renameDevice("room-001", "does-not-exist", "New Name"));
    }

    /** Renaming in a room that does not exist returns false. */
    @Test
    public void testRenameDevice_unknownRoomId_returnsFalse() {
        assertFalse(service.renameDevice("does-not-exist", "dev-001", "New Name"));
    }

    /** The original name is unchanged after a failed rename. */
    @Test
    public void testRenameDevice_failedRename_nameUnchanged() {
        service.renameDevice("room-001", "dev-001", "");
        Device d = service.getRoomById("room-001").getDevices().stream()
                .filter(dev -> dev.getId().equals("dev-001"))
                .findFirst().get();
        assertEquals("Main Light", d.getName());
    }

    // -----------------------------------------------------------------------
    // removeDeviceFromRoom
    // -----------------------------------------------------------------------

    /** Removing an existing device returns true and the device is gone. */
    @Test
    public void testRemoveDevice_valid() {
        boolean result = service.removeDeviceFromRoom("room-001", "dev-001");
        assertTrue(result);
        Room room = service.getRoomById("room-001");
        boolean stillExists = room.getDevices().stream()
                .anyMatch(d -> d.getId().equals("dev-001"));
        assertFalse(stillExists);
    }

    /** The other devices in the room are unaffected after a removal. */
    @Test
    public void testRemoveDevice_otherDevicesUntouched() {
        service.removeDeviceFromRoom("room-001", "dev-001");
        Room room = service.getRoomById("room-001");
        boolean dev002Still = room.getDevices().stream()
                .anyMatch(d -> d.getId().equals("dev-002"));
        assertTrue(dev002Still);
    }

    /** Removing with an unknown device ID returns false. */
    @Test
    public void testRemoveDevice_unknownDeviceId_returnsFalse() {
        assertFalse(service.removeDeviceFromRoom("room-001", "does-not-exist"));
    }

    /** Removing from an unknown room ID returns false. */
    @Test
    public void testRemoveDevice_unknownRoomId_returnsFalse() {
        assertFalse(service.removeDeviceFromRoom("does-not-exist", "dev-001"));
    }

    /** Removing the same device twice: second call returns false. */
    @Test
    public void testRemoveDevice_removedTwice_secondReturnsFalse() {
        service.removeDeviceFromRoom("room-001", "dev-001");
        assertFalse(service.removeDeviceFromRoom("room-001", "dev-001"));
    }
}
