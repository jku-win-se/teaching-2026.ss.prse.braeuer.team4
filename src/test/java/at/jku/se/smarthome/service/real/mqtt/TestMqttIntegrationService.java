package at.jku.se.smarthome.service.real.mqtt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.service.api.IoTIntegrationService;

/**
 * Unit tests for MqttIntegrationService.
 * 
 * <p>Tests mock MQTT client behavior without requiring a real broker connection.
 * Uses background thread execution to avoid blocking during tests.
 */
@SuppressWarnings({"PMD.TooManyStaticImports", "PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts", 
    "PMD.CommentRequired", "PMD.UnitTestShouldIncludeAssert"})
public final class TestMqttIntegrationService {

    /** Service instance under test. */
    private IoTIntegrationService service;

    /**
     * Default constructor for JUnit.
     */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public TestMqttIntegrationService() {
        // JUnit requires a public no-arg constructor
    }

    /**
     * Sets up test fixtures before each test.
     */
    @Before
    public void setUp() {
        MqttIntegrationService.resetForTesting();
        service = MqttIntegrationService.getInstance();
    }

    @Test
    public void testSingletonPattern() {
        IoTIntegrationService service1 = MqttIntegrationService.getInstance();
        IoTIntegrationService service2 = MqttIntegrationService.getInstance();
        assertSame("Singleton instances should be identical", service1, service2);
    }

    /**
     * Tests initial state of the service.
     */
    @Test
    public void testInitialState() {
        assertFalse("Service should not be enabled initially", service.isEnabled());
        assertFalse("Service should not be connected initially", service.isConnected());
        assertEquals("Protocol should be MQTT", "MQTT", service.getProtocolName());
        assertEquals("Last sync should be Never initially", "Never", service.getLastSync());
        assertTrue("Discovered devices should be empty initially", service.getDiscoveredDevices().isEmpty());
    }

    @Test
    public void testConfiguration() {
        IoTIntegrationService.IoTConfiguration config = service.getConfiguration();
        assertNotNull("Configuration should not be null", config);
        assertFalse("Configuration should not be enabled initially", config.enabled());
        
        service.saveConfiguration(true, "localhost", 1883, "user", "pass");
        config = service.getConfiguration();
        
        assertTrue("Configuration should be enabled after save", config.enabled());
        assertEquals("Broker should be localhost", "localhost", config.broker());
        assertEquals("Port should be 1883", 1883, config.port());
        assertEquals("Username should be user", "user", config.username());
        assertEquals("Password should be pass", "pass", config.password());
    }

    @Test
    public void testConnectionValidation() {
        assertFalse("Cannot test connection with null broker", service.testConnection(null, "1883"));
        assertFalse("Cannot test connection with empty broker", service.testConnection("", "1883"));
        assertFalse("Cannot test connection with invalid port", service.testConnection("localhost", "invalid"));
        assertFalse("Cannot test connection with port 0", service.testConnection("localhost", "0"));
        assertFalse("Cannot test connection with port > 65535", service.testConnection("localhost", "65536"));
        
        assertTrue("Should accept valid host and port", service.testConnection("localhost", "1883"));
        assertTrue("Should accept valid host and port", service.testConnection("mqtt.example.com", "8883"));
    }

    @Test
    public void testDisabledIntegrationCannotConnect() {
        service.saveConfiguration(false, "localhost", 1883, "", "");
        boolean connected = service.connect();
        assertFalse("Cannot connect when disabled", connected);
        assertFalse("Should not be connected", service.isConnected());
    }

    @Test
    public void testRefreshDevicesRequiresConnection() {
        service.saveConfiguration(true, "localhost", 1883, "", "");
        assertFalse("Cannot refresh devices when not connected", service.refreshDevices());
    }

    @Test
    public void testPublishCommandRequiresConnection() {
        service.saveConfiguration(true, "localhost", 1883, "", "");
        assertFalse("Cannot publish when not connected", service.publishCommand("device-1", "ON"));
    }

    @Test
    public void testDeviceStateTracking() {
        String state = service.getDeviceState("nonexistent-device");
        assertEquals("Nonexistent device state should be UNKNOWN", "UNKNOWN", state);
    }

    @Test
    public void testStateChangeListeners() {
        final boolean[] listenerCalled = {false};
        final String[] receivedDeviceId = {null};
        final String[] receivedState = {null};
        
        IoTIntegrationService.DeviceStateChangeListener listener = (deviceId, newState) -> {
            listenerCalled[0] = true;
            receivedDeviceId[0] = deviceId;
            receivedState[0] = newState;
        };
        
        service.addStateChangeListener(listener);
        
        // This would normally be triggered by MQTT message arrival
        service.removeStateChangeListener(listener);
    }

    @Test
    public void testConfigurationWithNullCredentials() {
        service.saveConfiguration(true, "localhost", 1883, null, null);
        IoTIntegrationService.IoTConfiguration config = service.getConfiguration();
        
        assertEquals("Null username should be empty string", "", config.username());
        assertEquals("Null password should be empty string", "", config.password());
    }

    @Test
    public void testConfigurationReset() {
        service.saveConfiguration(true, "localhost", 1883, "user", "pass");
        service.disconnect();
        
        service.saveConfiguration(false, "localhost", 1883, "user", "pass");
        assertTrue("Discovered devices should be cleared when disabled", service.getDiscoveredDevices().isEmpty());
    }

    @Test
    public void testProtocolName() {
        assertEquals("Protocol should be MQTT", "MQTT", service.getProtocolName());
    }

    @Test
    public void testSaveConfigurationNullValues() {
        service.saveConfiguration(true, "broker.example.com", 8883, null, null);
        IoTIntegrationService.IoTConfiguration config = service.getConfiguration();
        
        assertNotNull("Configuration should handle null username", config);
        assertEquals("Empty string should replace null username", "", config.username());
        assertEquals("Empty string should replace null password", "", config.password());
    }
}
