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

    public static synchronized MockIoTIntegrationService getInstance() {
        if (instance == null) {
            instance = new MockIoTIntegrationService();
        }
        return instance;
    }

    public static synchronized void resetForTesting() {
        instance = null;
    }

    public String getProtocolName() {
        return "MQTT";
    }

    public ObservableList<IntegrationDevice> getDiscoveredDevices() {
        return discoveredDevices;
    }

    public IoTConfiguration getConfiguration() {
        return new IoTConfiguration(enabled, broker, port, username, password);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getLastSync() {
        return lastSync;
    }

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

    public boolean testConnection(String broker, String portValue) {
        if (broker == null || broker.isBlank()) {
            return false;
        }

        try {
            int parsedPort = Integer.parseInt(portValue);
            return parsedPort > 0 && parsedPort <= 65535;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public boolean connect() {
        if (!enabled || broker == null || broker.isBlank() || port <= 0) {
            connected = false;
            return false;
        }

        connected = true;
        lastSync = LocalDateTime.now().format(formatter);
        seedDiscoveredDevices();
        return true;
    }

    public void disconnect() {
        connected = false;
    }

    public boolean refreshDevices() {
        if (!connected) {
            return false;
        }

        seedDiscoveredDevices();
        lastSync = LocalDateTime.now().format(formatter);
        return true;
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