package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.service.mock.MockIoTIntegrationService;

public class TestMockIoTIntegrationService {

    private MockIoTIntegrationService service;

    @Before
    public void setUp() {
        MockIoTIntegrationService.resetForTesting();
        service = MockIoTIntegrationService.getInstance();
    }

    @Test
    public void connect_withValidEnabledConfiguration_seedsDevices() {
        service.saveConfiguration(true, "broker.local", 1883, "owner", "secret");

        assertTrue(service.connect());
        assertTrue(service.isEnabled());
        assertTrue(service.isConnected());
        assertEquals("MQTT", service.getProtocolName());
        assertEquals(4, service.getDiscoveredDevices().size());
        assertNotEquals("Never", service.getLastSync());
    }

    @Test
    public void saveConfiguration_disabled_disconnectsAndClearsDevices() {
        service.saveConfiguration(true, "broker.local", 1883, "owner", "secret");
        service.connect();

        service.saveConfiguration(false, "broker.local", 1883, null, null);

        assertFalse(service.isEnabled());
        assertFalse(service.isConnected());
        assertEquals(0, service.getDiscoveredDevices().size());
        assertEquals("", service.getConfiguration().username());
        assertEquals("", service.getConfiguration().password());
    }

    @Test
    public void testConnection_andRefresh_validateInputsAndConnectionState() {
        assertFalse(service.testConnection("", "1883"));
        assertFalse(service.testConnection("broker.local", "70000"));
        assertFalse(service.testConnection("broker.local", "bad"));
        assertTrue(service.testConnection("broker.local", "1883"));
        assertFalse(service.refreshDevices());
    }
}