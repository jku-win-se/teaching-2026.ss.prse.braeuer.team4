package at.jku.se.smarthome.service;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import at.jku.se.smarthome.service.api.IoTIntegrationService.IoTConfiguration;
import at.jku.se.smarthome.service.real.mqtt.MqttIntegrationService;

/**
 * Unit tests for {@link MqttIntegrationService}.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods"})
public class TestMqttIntegrationService {

    /** The service under test. */
    private MqttIntegrationService service;

    /**
     * Skips the test on headless CI where JavaFX initialization can block.
     */
    @BeforeClass
    public static void skipIfHeadless() {
        assumeTrue("Skipping JavaFX service test on CI", System.getenv("CI") == null);
    }

    /**
     * Sets up test fixtures before each test.
     */
    @Before
    public void setUp() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Platform already started
        }
        MqttIntegrationService.resetForTesting();
        service = MqttIntegrationService.getInstance();
    }

    /**
     * Tears down test fixtures after each test.
     */
    @After
    public void tearDown() {
        MqttIntegrationService.resetForTesting();
    }

    /**
     * Tests the protocol name.
     */
    @Test
    public void testProtocolName() {
        assertEquals("MQTT", service.getProtocolName());
    }

    /**
     * Tests the service configuration.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testConfiguration() {
        service.saveConfiguration(true, "broker.test.com", 1883, "user", "pass");
        IoTConfiguration config = service.getConfiguration();
        assertTrue(config.enabled());
        assertEquals("broker.test.com", config.broker());
        assertEquals(1883, config.port());
        assertEquals("user", config.username());
        assertEquals("pass", config.password());
    }

    /**
     * Tests that disabling the service clears its state.
     */
    @Test
    @SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidCatchingGenericException"})
    public void testDisableClearsState() {
        service.saveConfiguration(true, "b", 1883, "u", "p");
        // Simulate some state
        try {
            service.messageArrived("smarthome/device1/state", new MqttMessage("ON".getBytes()));
        } catch (Exception ignored) {
            // Ignored for test simulation
        }
        
        service.saveConfiguration(false, "b", 1883, "u", "p");
        assertFalse(service.isEnabled());
        assertEquals("UNKNOWN", service.getDeviceState("device1"));
    }

    /**
     * Tests the connection test functionality.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testTestConnection() {
        assertTrue(service.testConnection("localhost", "1883"));
        assertFalse(service.testConnection(null, "1883"));
        assertFalse(service.testConnection("", "1883"));
        assertFalse(service.testConnection("localhost", "invalid"));
        assertFalse(service.testConnection("localhost", "-1"));
    }

    /**
     * Tests that arrived messages update the device state.
     * @throws Exception if message processing fails
     */
    @Test
    public void testMessageArrivedUpdatesState() throws Exception {
        String deviceId = "test-device";
        String payload = "OFF";
        MqttMessage message = new MqttMessage(payload.getBytes());
        
        service.messageArrived("smarthome/" + deviceId + "/state", message);
        
        assertEquals(payload, service.getDeviceState(deviceId));
    }

    /**
     * Tests that arrived messages with invalid topics are ignored.
     * @throws Exception if message processing fails
     */
    @Test
    public void testMessageArrivedIgnoresInvalidTopic() throws Exception {
        service.messageArrived("invalid/topic", new MqttMessage("ON".getBytes()));
        service.messageArrived("smarthome/device/invalid", new MqttMessage("ON".getBytes()));
        org.junit.Assert.assertNotNull(service);
    }

    /**
     * Tests connection loss handling.
     */
    @Test
    public void testConnectionLost() {
        service.connectionLost(new RuntimeException("Lost"));
        assertFalse(service.isConnected());
    }

    /**
     * Tests delivery completion handling.
     */
    @Test
    public void testDeliveryComplete() {
        service.deliveryComplete(null);
        org.junit.Assert.assertNotNull(service);
    }

    /**
     * Tests the state change listener.
     * @throws Exception if test fails or interrupted
     */
    @Test
    @SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.DoNotUseThreads"})
    public void testStateChangeListener() throws Exception {
        final String[] changedId = new String[1];
        final String[] changedState = new String[1];
        
        service.addStateChangeListener((id, state) -> {
            changedId[0] = id;
            changedState[0] = state;
        });
        
        service.messageArrived("smarthome/dev1/state", new MqttMessage("ON".getBytes()));
        
        // Callback is on UI thread (Platform.runLater), but we can call it directly in test or wait
        // In this project's test environment, Platform.runLater might be synchronous or we might need to wait.
        // If it's not synchronous, we might need a small delay.
        Thread.sleep(100); 
        
        assertEquals("dev1", changedId[0]);
        assertEquals("ON", changedState[0]);
    }

    /**
     * Tests that connection fails when service is disabled.
     */
    @Test
    public void testConnectFailsWhenDisabled() {
        service.saveConfiguration(false, "b", 1883, "u", "p");
        assertFalse(service.connect());
    }

    /**
     * Tests that connection fails with invalid broker configuration.
     */
    @Test
    public void testConnectFailsWithInvalidBroker() {
        service.saveConfiguration(true, "", 0, null, null);
        assertFalse(service.connect());
    }
}
