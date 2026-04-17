package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.service.mock.MockIoTIntegrationService;

/**
 * Tests for {@link MockIoTIntegrationService}.
 */
public class TestMockIoTIntegrationService {

    /** IoT integration service under test. */
    private MockIoTIntegrationService service;

    /**
     * Sets up test fixtures before each test.
     */
    @Before
    public void setUp() {
        MockIoTIntegrationService.resetForTesting();
        service = MockIoTIntegrationService.getInstance();
    }

    /**
     * Test: connect with valid enabled configuration seeds devices.
     */
    @Test
    public void connectWithValidEnabledConfigurationSeedsDevices() {
        service.saveConfiguration(true, "broker.local", 1883, "owner", "secret");

        assertTrue(service.connect());
        assertTrue(service.isEnabled());
        assertTrue(service.isConnected());
        assertEquals("MQTT", service.getProtocolName());
        assertEquals(4, service.getDiscoveredDevices().size());
        assertNotEquals("Never", service.getLastSync());
    }

    /**
     * Test: save configuration disabled disconnects and clears devices.
     */
    @Test
    public void saveConfigurationDisabledDisconnectsAndClearsDevices() {
        service.saveConfiguration(true, "broker.local", 1883, "owner", "secret");
        service.connect();

        service.saveConfiguration(false, "broker.local", 1883, null, null);

        assertFalse(service.isEnabled());
        assertFalse(service.isConnected());
        assertEquals(0, service.getDiscoveredDevices().size());
        assertEquals("", service.getConfiguration().username());
        assertEquals("", service.getConfiguration().password());
    }

    /**
     * Test: connection and refresh validate inputs and connection state.
     */
    @Test
    public void testConnectionAndRefreshValidateInputsAndConnectionState() {
        assertFalse(service.testConnection("", "1883"));
        assertFalse(service.testConnection("broker.local", "70000"));
        assertFalse(service.testConnection("broker.local", "bad"));
        assertTrue(service.testConnection("broker.local", "1883"));
        assertFalse(service.refreshDevices());
    }
}