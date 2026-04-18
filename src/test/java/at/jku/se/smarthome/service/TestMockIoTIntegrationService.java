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
     * Test: connect returns true with valid enabled configuration.
     */
    @Test
    public void connectWithValidEnabledConfigurationReturnsTrue() {
        service.saveConfiguration(true, "broker.local", 1883, "owner", "secret");
        assertTrue(service.connect());
    }

    /**
     * Test: connect enables service.
     */
    @Test
    public void connectEnablesService() {
        service.saveConfiguration(true, "broker.local", 1883, "owner", "secret");
        service.connect();
        assertTrue(service.isEnabled());
    }

    /**
     * Test: connect establishes connection.
     */
    @Test
    public void connectEstablishesConnection() {
        service.saveConfiguration(true, "broker.local", 1883, "owner", "secret");
        service.connect();
        assertTrue(service.isConnected());
    }

    /**
     * Test: connect sets protocol name.
     */
    @Test
    public void connectSetsProtocolName() {
        service.saveConfiguration(true, "broker.local", 1883, "owner", "secret");
        service.connect();
        assertEquals("MQTT", service.getProtocolName());
    }

    /**
     * Test: connect seeds devices.
     */
    @Test
    public void connectSeedsDevices() {
        service.saveConfiguration(true, "broker.local", 1883, "owner", "secret");
        service.connect();
        assertEquals(4, service.getDiscoveredDevices().size());
    }

    /**
     * Test: connect updates last sync.
     */
    @Test
    public void connectUpdatesLastSync() {
        service.saveConfiguration(true, "broker.local", 1883, "owner", "secret");
        service.connect();
        assertNotEquals("Never", service.getLastSync());
    }

    /**
     * Test: disable service clears enabled status.
     */
    @Test
    public void saveConfigurationDisabledClearsEnabledStatus() {
        service.saveConfiguration(true, "broker.local", 1883, "owner", "secret");
        service.connect();
        service.saveConfiguration(false, "broker.local", 1883, null, null);
        assertFalse(service.isEnabled());
    }

    /**
     * Test: disable service disconnects.
     */
    @Test
    public void saveConfigurationDisabledDisconnects() {
        service.saveConfiguration(true, "broker.local", 1883, "owner", "secret");
        service.connect();
        service.saveConfiguration(false, "broker.local", 1883, null, null);
        assertFalse(service.isConnected());
    }

    /**
     * Test: disable service clears devices.
     */
    @Test
    public void saveConfigurationDisabledClearsDevices() {
        service.saveConfiguration(true, "broker.local", 1883, "owner", "secret");
        service.connect();
        service.saveConfiguration(false, "broker.local", 1883, null, null);
        assertEquals(0, service.getDiscoveredDevices().size());
    }

    /**
     * Test: disable service clears username.
     */
    @Test
    public void saveConfigurationDisabledClearsUsername() {
        service.saveConfiguration(true, "broker.local", 1883, "owner", "secret");
        service.connect();
        service.saveConfiguration(false, "broker.local", 1883, null, null);
        assertEquals("", service.getConfiguration().username());
    }

    /**
     * Test: disable service clears password.
     */
    @Test
    public void saveConfigurationDisabledClearsPassword() {
        service.saveConfiguration(true, "broker.local", 1883, "owner", "secret");
        service.connect();
        service.saveConfiguration(false, "broker.local", 1883, null, null);
        assertEquals("", service.getConfiguration().password());
    }

    /**
     * Test: test connection rejects empty broker.
     */
    @Test
    public void testConnectionRejectsEmptyBroker() {
        assertFalse(service.testConnection("", "1883"));
    }

    /**
     * Test: test connection rejects port too high.
     */
    @Test
    public void testConnectionRejectsPortTooHigh() {
        assertFalse(service.testConnection("broker.local", "70000"));
    }

    /**
     * Test: test connection rejects non-numeric port.
     */
    @Test
    public void testConnectionRejectsNonNumericPort() {
        assertFalse(service.testConnection("broker.local", "bad"));
    }

    /**
     * Test: test connection accepts valid configuration.
     */
    @Test
    public void testConnectionAcceptsValidConfiguration() {
        assertTrue(service.testConnection("broker.local", "1883"));
    }

    /**
     * Test: refresh devices without connection fails.
     */
    @Test
    public void refreshDevicesWithoutConnectionFails() {
        assertFalse(service.refreshDevices());
    }
}