package at.jku.se.smarthome.service.mock;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import at.jku.se.smarthome.model.IntegrationDevice;
import at.jku.se.smarthome.service.api.IoTIntegrationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock MQTT integration layer for demonstrating optional physical device connectivity.
 * 
 * <p>Implements IoTIntegrationService for testing and demonstration purposes without
 * requiring a real MQTT broker. Simulates device discovery and state changes.
 */
public final class MockIoTIntegrationService implements IoTIntegrationService {

    /** Lock for singleton synchronization. */
    private static final Object INSTANCE_LOCK = new Object();
    /** Singleton instance. */
    private static MockIoTIntegrationService instance;

    /** Observable list of discovered devices. */
    private final ObservableList<IntegrationDevice> discoveredDevices = FXCollections.observableArrayList();
    /** Date/time formatter for timestamps. */
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /** Map of device ID to last known state. */
    private final Map<String, String> deviceStates = new ConcurrentHashMap<>();
    
    /** List of state change listeners. */
    private final List<DeviceStateChangeListener> stateChangeListeners = 
        new CopyOnWriteArrayList<>();

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
    @SuppressWarnings("PMD.NullAssignment")
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
    @Override
    public String getProtocolName() {
        return "MQTT";
    }

    /**
     * Returns list of discovered devices.
     *
     * @return observable list of devices
     */
    @Override
    public ObservableList<IntegrationDevice> getDiscoveredDevices() {
        return discoveredDevices;
    }

    @Override
    public IoTConfiguration getConfiguration() {
        return new IoTConfiguration(enabled, broker, port, username, password);
    }

    /**
     * Checks if IoT integration is enabled.
     *
     * @return true if enabled
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Checks if currently connected to broker.
     *
     * @return true if connected
     */
    @Override
    public boolean isConnected() {
        return connected;
    }

    /**
     * Returns timestamp of last synchronization.
     *
     * @return last sync time string
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
    public void disconnect() {
        connected = false;
    }

    /**
     * Refreshes device list from broker.
     *
     * @return true if refresh successful
     */
    @Override
    public boolean refreshDevices() {
        if (connected) {
            seedDiscoveredDevices();
            lastSync = LocalDateTime.now().format(formatter);
        }
        return connected;
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public boolean publishCommand(String deviceId, String command) {
        if (!connected) {
            return false;
        }
        // Simulate command publishing and update state
        deviceStates.put(deviceId, command);
        for (DeviceStateChangeListener listener : stateChangeListeners) {
            listener.onStateChanged(deviceId, command);
        }
        return true;
    }

    @Override
    public String getDeviceState(String deviceId) {
        return deviceStates.getOrDefault(deviceId, "UNKNOWN");
    }

    @Override
    public void addStateChangeListener(DeviceStateChangeListener listener) {
        stateChangeListeners.add(listener);
    }

    @Override
    public void removeStateChangeListener(DeviceStateChangeListener listener) {
        stateChangeListeners.remove(listener);
    }

    private void seedDiscoveredDevices() {
        discoveredDevices.setAll(
                new IntegrationDevice("MQTT Living Room Lamp", "Switch", "smarthome/livingroom/lamp/state", "Online"),
                new IntegrationDevice("MQTT Roller Blind", "Cover/Blind", "smarthome/livingroom/blind/state", "Online"),
                new IntegrationDevice("MQTT Bedroom Thermostat", "Thermostat", "smarthome/bedroom/thermostat/setpoint", "Online"),
                new IntegrationDevice("MQTT Kitchen Plug", "Switch", "smarthome/kitchen/plug/state", "Sleeping")
        );
        // Initialize device states
        for (IntegrationDevice device : discoveredDevices) {
            deviceStates.put(device.getTopic(), device.getStatus());
        }
    }
}