package at.jku.se.smarthome.service.mock;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import at.jku.se.smarthome.model.IntegrationDevice;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock MQTT integration layer for demonstrating optional physical device connectivity.
 */
public final class MockIoTIntegrationService {

    /** Record for IoT configuration. */
    public record IoTConfiguration(/** Enable/disable flag. */ boolean enabled, /** MQTT broker address. */ String broker, /** MQTT broker port. */ int port, /** Username for authentication. */ String username, /** Password for authentication. */ String password) {
    }

    /** Lock for singleton synchronization. */
    private static final Object INSTANCE_LOCK = new Object();
    /** Singleton instance. */
    private static MockIoTIntegrationService instance;

    /** Observable list of discovered devices. */
    private final ObservableList<IntegrationDevice> discoveredDevices = FXCollections.observableArrayList();
    /** Date/time formatter for timestamps. */
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Flag indicating if IoT integration is enabled. */
    private boolean enabled;
    /** Flag indicating if currently connected to broker. */
    private boolean connected;
    /** MQTT broker address. */
    private String broker;
    /** MQTT broker port. */
    private int port;
    /** MQTT broker username. */
    private String username;
    /** MQTT broker password. */
    private String password;
    /** Timestamp of last synchronization. */
    private String lastSync;

    private MockIoTIntegrationService() {
        broker = "mqtt.example.com";
        port = 1883;
        username = "";
        password = "";
        lastSync = "Never";
    }

    /**
     * Returns singleton instance.
     *
     * @return the singleton instance
     */
    public static MockIoTIntegrationService getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new MockIoTIntegrationService();
            }
            return instance;
        }
    }

    /**
     * Resets singleton for testing.
     */
    public static void resetForTesting() {
        synchronized (INSTANCE_LOCK) {
            instance = null;
        }
    }

    /**
     * Returns the protocol name.
     *
     * @return protocol name
     */
    public String getProtocolName() {
        return "MQTT";
    }

    /**
     * Returns list of discovered devices.
     *
     * @return observable list of devices
     */
    public ObservableList<IntegrationDevice> getDiscoveredDevices() {
        return discoveredDevices;
    }

    /**
     * Returns current IoT configuration.
     *
     * @return current configuration
     */
    public IoTConfiguration getConfiguration() {
        return new IoTConfiguration(enabled, broker, port, username, password);
    }

    /**
     * Checks if IoT integration is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Checks if currently connected to broker.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Returns timestamp of last synchronization.
     *
     * @return last sync time string
     */
    public String getLastSync() {
        return lastSync;
    }

    /**
     * Saves new IoT configuration.
     *
     * @param enabled whether IoT is enabled
     * @param broker broker address
     * @param port broker port
     * @param username authentication username
     * @param password authentication password
     */
    public void saveConfiguration(boolean enabled, String broker, int port, String username, String password) {
        this.enabled = enabled;
        this.broker = broker;
        this.port = port;
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;

        if (!enabled) {
            disconnect();
            discoveredDevices.clear();
        }
    }

    /**
     * Tests connection with given broker settings.
     *
     * @param broker broker address
     * @param portValue broker port
     * @return true if connection parameters are valid
     */
    public boolean testConnection(String broker, String portValue) {
        boolean result = false;
        if (broker != null && !broker.isBlank()) {
            try {
                int parsedPort = Integer.parseInt(portValue);
                result = parsedPort > 0 && parsedPort <= 65535;
            } catch (NumberFormatException exception) {
                result = false;
            }
        }
        return result;
    }

    /**
     * Connects to MQTT broker.
     *
     * @return true if connection successful
     */
    public boolean connect() {
        boolean canConnect = enabled && broker != null && !broker.isBlank() && port > 0;
        if (canConnect) {
            connected = true;
            lastSync = LocalDateTime.now().format(formatter);
            seedDiscoveredDevices();
        } else {
            connected = false;
        }
        return connected;
    }

    /**
     * Disconnects from MQTT broker.
     */
    public void disconnect() {
        connected = false;
    }

    /**
     * Refreshes device list from broker.
     *
     * @return true if refresh successful
     */
    public boolean refreshDevices() {
        if (connected) {
            seedDiscoveredDevices();
            lastSync = LocalDateTime.now().format(formatter);
        }
        return connected;
    }

    private void seedDiscoveredDevices() {
        discoveredDevices.setAll(
                new IntegrationDevice("MQTT Living Room Lamp", "Switch", "smarthome/livingroom/lamp/state", "Online"),
                new IntegrationDevice("MQTT Roller Blind", "Cover/Blind", "smarthome/livingroom/blind/state", "Online"),
                new IntegrationDevice("MQTT Bedroom Thermostat", "Thermostat", "smarthome/bedroom/thermostat/setpoint", "Online"),
                new IntegrationDevice("MQTT Kitchen Plug", "Switch", "smarthome/kitchen/plug/state", "Sleeping")
        );
    }
}