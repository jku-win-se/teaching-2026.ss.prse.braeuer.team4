package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.service.mock.MockSmartHomeService;

/**
 * Tests for {@link MockSmartHomeService} covering all manual device control operations.
 *
 * Each test method resets the singleton so state does not leak between tests.
 * Device IDs used:
 *   dev-001 = Switch (Living Room Light, starts ON)
 *   dev-002 = Thermostat (Living Room, starts ON)
 *   dev-003 = Dimmer (Bedroom, starts OFF, brightness 75)
 *   dev-004 = Switch (Kitchen, starts ON)
 *   dev-005 = Thermostat (Bedroom, starts ON)
 *   dev-006 = Cover/Blind (Garden, starts CLOSED)
 *   dev-007 = Sensor (Hallway, initial reading 0.0)
 */
public class TestMockSmartHomeService {

    private MockSmartHomeService service;

    @Before
    public void setUp() {
        MockSmartHomeService.resetForTesting();
        service = MockSmartHomeService.getInstance();
    }

    // -----------------------------------------------------------------------
    // toggleDevice
    // -----------------------------------------------------------------------

    /** A device that starts ON should be OFF after one toggle. */
    @Test
    public void testToggleDevice_onToOff() {
        Device d = service.getDeviceById("dev-001");
        assertTrue("pre-condition: dev-001 starts ON", d.getState());

        boolean result = service.toggleDevice("dev-001");

        assertTrue(result);
        assertFalse(service.getDeviceById("dev-001").getState());
    }

    /** A device that starts OFF should be ON after one toggle. */
    @Test
    public void testToggleDevice_offToOn() {
        Device d = service.getDeviceById("dev-003");
        assertFalse("pre-condition: dev-003 starts OFF", d.getState());

        boolean result = service.toggleDevice("dev-003");

        assertTrue(result);
        assertTrue(service.getDeviceById("dev-003").getState());
    }

    /** An unknown device ID returns false and has no side effects. */
    @Test
    public void testToggleDevice_unknownId_returnsFalse() {
        assertFalse(service.toggleDevice("does-not-exist"));
    }

    // -----------------------------------------------------------------------
    // setBrightness
    // -----------------------------------------------------------------------

    /** Brightness within [0, 100] is applied to a Dimmer. */
    @Test
    public void testSetBrightness_validRange() {
        boolean result = service.setBrightness("dev-003", 50);
        assertTrue(result);
        assertEquals(50, service.getDeviceById("dev-003").getBrightness());
    }

    /** Setting brightness to 0 turns the dimmer OFF. */
    @Test
    public void testSetBrightness_zero_turnsDeviceOff() {
        service.setBrightness("dev-003", 0);
        assertFalse(service.getDeviceById("dev-003").getState());
    }

    /** Setting brightness above 0 turns the dimmer ON. */
    @Test
    public void testSetBrightness_aboveZero_turnsDeviceOn() {
        service.setBrightness("dev-003", 80);
        assertTrue(service.getDeviceById("dev-003").getState());
    }

    /** Brightness > 100 is rejected. */
    @Test
    public void testSetBrightness_aboveMax_returnsFalse() {
        assertFalse(service.setBrightness("dev-003", 101));
    }

    /** Negative brightness is rejected. */
    @Test
    public void testSetBrightness_negative_returnsFalse() {
        assertFalse(service.setBrightness("dev-003", -1));
    }

    /** setBrightness on a non-Dimmer device returns false. */
    @Test
    public void testSetBrightness_wrongType_returnsFalse() {
        assertFalse(service.setBrightness("dev-001", 50)); // dev-001 is a Switch
    }

    // -----------------------------------------------------------------------
    // setTemperature
    // -----------------------------------------------------------------------

    /** A temperature in the valid range [10.0, 35.0] is applied. */
    @Test
    public void testSetTemperature_valid() {
        boolean result = service.setTemperature("dev-002", 22.5);
        assertTrue(result);
        assertEquals(22.5, service.getDeviceById("dev-002").getTemperature(), 0.001);
    }

    /** Temperature below 10.0 is rejected. */
    @Test
    public void testSetTemperature_tooLow_returnsFalse() {
        assertFalse(service.setTemperature("dev-002", 9.9));
    }

    /** Temperature above 35.0 is rejected. */
    @Test
    public void testSetTemperature_tooHigh_returnsFalse() {
        assertFalse(service.setTemperature("dev-002", 35.1));
    }

    /** setTemperature on a non-Thermostat device returns false. */
    @Test
    public void testSetTemperature_wrongType_returnsFalse() {
        assertFalse(service.setTemperature("dev-001", 22.0)); // dev-001 is a Switch
    }

    // -----------------------------------------------------------------------
    // openBlind / closeBlind
    // -----------------------------------------------------------------------

    /** Opening a closed blind succeeds and state becomes true (OPEN). */
    @Test
    public void testOpenBlind_valid() {
        boolean result = service.openBlind("dev-006");
        assertTrue(result);
        assertTrue(service.getDeviceById("dev-006").getState());
    }

    /** Closing an open blind succeeds and state becomes false (CLOSED). */
    @Test
    public void testCloseBlind_valid() {
        service.openBlind("dev-006");
        boolean result = service.closeBlind("dev-006");
        assertTrue(result);
        assertFalse(service.getDeviceById("dev-006").getState());
    }

    /** openBlind on a non-Cover/Blind device returns false. */
    @Test
    public void testOpenBlind_wrongType_returnsFalse() {
        assertFalse(service.openBlind("dev-001")); // Switch
    }

    /** closeBlind on an unknown ID returns false. */
    @Test
    public void testCloseBlind_unknownId_returnsFalse() {
        assertFalse(service.closeBlind("does-not-exist"));
    }

    // -----------------------------------------------------------------------
    // injectSensorValue
    // -----------------------------------------------------------------------

    /** Any double value can be injected into a Sensor. */
    @Test
    public void testInjectSensorValue_valid() {
        boolean result = service.injectSensorValue("dev-007", 42.5);
        assertTrue(result);
        assertEquals(42.5, service.getDeviceById("dev-007").getTemperature(), 0.001);
    }

    /** Negative sensor values are also valid (e.g. temperature below zero). */
    @Test
    public void testInjectSensorValue_negativeValue_valid() {
        boolean result = service.injectSensorValue("dev-007", -10.0);
        assertTrue(result);
        assertEquals(-10.0, service.getDeviceById("dev-007").getTemperature(), 0.001);
    }

    /** injectSensorValue on a non-Sensor device returns false. */
    @Test
    public void testInjectSensorValue_wrongType_returnsFalse() {
        assertFalse(service.injectSensorValue("dev-001", 42.0)); // Switch
    }

    /** injectSensorValue on an unknown ID returns false. */
    @Test
    public void testInjectSensorValue_unknownId_returnsFalse() {
        assertFalse(service.injectSensorValue("does-not-exist", 42.0));
    }
}
