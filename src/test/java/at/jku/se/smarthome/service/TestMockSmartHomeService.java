package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

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
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods"})
public class TestMockSmartHomeService {


    /** Smart home service under test. */
    private MockSmartHomeService service;

    /**
     * Sets up test fixtures before each test.
     */
    @Before
    public void setUp() {
        MockSmartHomeService.resetForTesting();
        service = MockSmartHomeService.getInstance();
    }

    // -----------------------------------------------------------------------
    // toggleDevice
    // -----------------------------------------------------------------------

    /**
     * Test: device that starts ON returns true when toggled.
     */
    @Test
    public void testToggleDeviceOnToOffReturnsTrue() {
        service.toggleDevice("dev-001");
        boolean result = service.toggleDevice("dev-001");
        assertTrue(result);
    }

    /**
     * Test: device that starts ON becomes OFF after toggle.
     */
    @Test
    public void testToggleDeviceOnToOffChangeState() {
        service.toggleDevice("dev-001");
        assertFalse(service.getDeviceById("dev-001").getState());
    }

    /**
     * Test: device that starts OFF returns true when toggled.
     */
    @Test
    public void testToggleDeviceOffToOnReturnsTrue() {
        service.toggleDevice("dev-003");
        boolean result = service.toggleDevice("dev-003");
        assertTrue(result);
    }

    /**
     * Test: device that starts OFF becomes ON after toggle.
     */
    @Test
    public void testToggleDeviceOffToOnChangeState() {
        service.toggleDevice("dev-003");
        assertTrue(service.getDeviceById("dev-003").getState());
    }

    /**
     * Test: unknown device ID returns false and has no side effects.
     */
    @Test
    public void testToggleDeviceUnknownIdReturnsFalse() {
        assertFalse(service.toggleDevice("does-not-exist"));
    }

    // -----------------------------------------------------------------------
    // setBrightness
    // -----------------------------------------------------------------------

    /**
     * Test: setting brightness in valid range returns true.
     */
    @Test
    public void testSetBrightnessValidRangeReturnsTrue() {
        boolean result = service.setBrightness("dev-003", 50);
        assertTrue(result);
    }

    /**
     * Test: brightness in valid range is set on device.
     */
    @Test
    public void testSetBrightnessValidRangeSetsBrightness() {
        service.setBrightness("dev-003", 50);
        assertEquals(50, service.getDeviceById("dev-003").getBrightness());
    }

    /** Setting brightness to 0 turns the dimmer OFF. */
    @Test
    public void testSetBrightnessZeroTurnsDeviceOff() {
        service.setBrightness("dev-003", 0);
        assertFalse(service.getDeviceById("dev-003").getState());
    }

    /** Setting brightness above 0 turns the dimmer ON. */
    @Test
    public void testSetBrightnessAboveZeroTurnsDeviceOn() {
        service.setBrightness("dev-003", 80);
        assertTrue(service.getDeviceById("dev-003").getState());
    }

    /** Brightness > 100 is rejected. */
    @Test
    public void testSetBrightnessAboveMaxReturnsFalse() {
        assertFalse(service.setBrightness("dev-003", 101));
    }

    /** Negative brightness is rejected. */
    @Test
    public void testSetBrightnessNegativeReturnsFalse() {
        assertFalse(service.setBrightness("dev-003", -1));
    }

    /** setBrightness on a non-Dimmer device returns false. */
    @Test
    public void testSetBrightnessWrongTypeReturnsFalse() {
        assertFalse(service.setBrightness("dev-001", 50)); // dev-001 is a Switch
    }

    // -----------------------------------------------------------------------
    // setTemperature
    // -----------------------------------------------------------------------

    /**
     * Test: setting temperature in valid range returns true.
     */
    @Test
    public void testSetTemperatureValidReturnsTrue() {
        boolean result = service.setTemperature("dev-002", 22.5);
        assertTrue(result);
    }

    /**
     * Test: temperature in valid range is set on device.
     */
    @Test
    public void testSetTemperatureValidSetsTemperature() {
        service.setTemperature("dev-002", 22.5);
        assertEquals(22.5, service.getDeviceById("dev-002").getTemperature(), 0.001);
    }

    /** Temperature below 10.0 is rejected. */
    @Test
    public void testSetTemperatureTooLowReturnsFalse() {
        assertFalse(service.setTemperature("dev-002", 9.9));
    }

    /** Temperature above 35.0 is rejected. */
    @Test
    public void testSetTemperatureTooHighReturnsFalse() {
        assertFalse(service.setTemperature("dev-002", 35.1));
    }

    /** setTemperature on a non-Thermostat device returns false. */
    @Test
    public void testSetTemperatureWrongTypeReturnsFalse() {
        assertFalse(service.setTemperature("dev-001", 22.0)); // dev-001 is a Switch
    }

    // -----------------------------------------------------------------------
    // openBlind / closeBlind
    // -----------------------------------------------------------------------

    /**
     * Test: opening a closed blind returns true.
     */
    @Test
    public void testOpenBlindValidReturnsTrue() {
        boolean result = service.openBlind("dev-006");
        assertTrue(result);
    }

    /**
     * Test: opening a closed blind sets state to true (OPEN).
     */
    @Test
    public void testOpenBlindValidChangesState() {
        service.openBlind("dev-006");
        assertTrue(service.getDeviceById("dev-006").getState());
    }

    /**
     * Test: closing an open blind returns true.
     */
    @Test
    public void testCloseBlindValidReturnsTrue() {
        service.openBlind("dev-006");
        boolean result = service.closeBlind("dev-006");
        assertTrue(result);
    }

    /**
     * Test: closing an open blind sets state to false (CLOSED).
     */
    @Test
    public void testCloseBlindValidChangesState() {
        service.openBlind("dev-006");
        service.closeBlind("dev-006");
        assertFalse(service.getDeviceById("dev-006").getState());
    }

    /** openBlind on a non-Cover/Blind device returns false. */
    @Test
    public void testOpenBlindWrongTypeReturnsFalse() {
        assertFalse(service.openBlind("dev-001")); // Switch
    }

    /** closeBlind on an unknown ID returns false. */
    @Test
    public void testCloseBlindUnknownIdReturnsFalse() {
        assertFalse(service.closeBlind("does-not-exist"));
    }

    // -----------------------------------------------------------------------
    // injectSensorValue
    // -----------------------------------------------------------------------

    /**
     * Test: injecting valid sensor value returns true.
     */
    @Test
    public void testInjectSensorValueValidReturnsTrue() {
        boolean result = service.injectSensorValue("dev-007", 42.5);
        assertTrue(result);
    }

    /**
     * Test: injecting valid sensor value sets the temperature.
     */
    @Test
    public void testInjectSensorValueValidSetsValue() {
        service.injectSensorValue("dev-007", 42.5);
        assertEquals(42.5, service.getDeviceById("dev-007").getTemperature(), 0.001);
    }

    /**
     * Test: negative sensor values can be injected and return true.
     */
    @Test
    public void testInjectSensorValueNegativeValueReturnsTrue() {
        boolean result = service.injectSensorValue("dev-007", -10.0);
        assertTrue(result);
    }

    /**
     * Test: negative sensor values are set correctly.
     */
    @Test
    public void testInjectSensorValueNegativeValueSetsValue() {
        service.injectSensorValue("dev-007", -10.0);
        assertEquals(-10.0, service.getDeviceById("dev-007").getTemperature(), 0.001);
    }

    /** injectSensorValue on a non-Sensor device returns false. */
    @Test
    public void testInjectSensorValueWrongTypeReturnsFalse() {
        assertFalse(service.injectSensorValue("dev-001", 42.0)); // Switch
    }

    /** injectSensorValue on an unknown ID returns false. */
    @Test
    public void testInjectSensorValueUnknownIdReturnsFalse() {
        assertFalse(service.injectSensorValue("does-not-exist", 42.0));
    }
}
