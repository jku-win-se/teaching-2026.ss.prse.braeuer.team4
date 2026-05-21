package at.jku.se.smarthome.config;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Robust unit tests for configuration classes.
 */
public class RobustConfigCoverageTest {

    /**
     * Default constructor.
     */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public RobustConfigCoverageTest() {
        // Default constructor
    }

    /**
     * Tests device energy constants.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testDeviceEnergyConstants() {
        assertNotNull(DeviceEnergyConstants.getPowerWatts("SWITCH"));
        assertNotNull(DeviceEnergyConstants.getPowerWatts("DIMMER"));
        assertNotNull(DeviceEnergyConstants.getPowerWatts("THERMOSTAT"));
        assertNotNull(DeviceEnergyConstants.getPowerWatts("SENSOR"));
        assertNotNull(DeviceEnergyConstants.getPowerWatts("BLIND"));
        assertNotNull(DeviceEnergyConstants.getPowerWatts("UNKNOWN"));
        
        assertEquals(10, DeviceEnergyConstants.getNominalPowerWatts("UNKNOWN"));
        assertFalse(DeviceEnergyConstants.getAllDeviceTypes().isEmpty());
    }

    /**
     * Tests database settings.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testDatabaseSettings() {
        DatabaseSettings settings = new DatabaseSettings("url", "user", "pass");
        assertEquals("url", settings.jdbcUrl());
        assertEquals("user", settings.username());
        assertEquals("pass", settings.password());
    }
}
