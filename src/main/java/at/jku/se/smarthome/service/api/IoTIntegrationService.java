package at.jku.se.smarthome.service.api;

import at.jku.se.smarthome.model.IntegrationDevice;
import javafx.collections.ObservableList;

/**
 * Defines operations for optional MQTT integration with physical IoT devices.
 * 
 * <p>This service encapsulates broker access behind a small interface, allowing the rest
 * of the application to depend on this contract rather than the MQTT implementation details.
 * This design enables both mock testing and real MQTT integration without coupling the
 * rest of the application to a specific broker library.
 * 
 * <p>Topic Convention:
 * <ul>
 *   <li>State Topic: {@code smarthome/<deviceId>/state} - Device publishes its state here</li>
 *   <li>Command Topic: {@code smarthome/<deviceId>/cmd} - App publishes commands here</li>
 * </ul>
 */
public interface IoTIntegrationService {

    /**
     * Configuration record for MQTT broker settings.
     *
     * @param enabled whether IoT integration is enabled
     * @param broker MQTT broker hostname or IP address
     * @param port MQTT broker port
     * @param username optional username for broker authentication
     * @param password optional password for broker authentication
     */
    record IoTConfiguration(boolean enabled, String broker, int port, String username, String password) {
    }

    /**
     * Returns the protocol name (e.g., "MQTT").
     *
     * @return protocol name
     */
    String getProtocolName();

    /**
     * Returns the current IoT configuration.
     *
     * @return current configuration settings
     */
    IoTConfiguration getConfiguration();

    /**
     * Checks if IoT integration is enabled.
     *
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Checks if currently connected to the MQTT broker.
     *
     * @return true if connected, false otherwise
     */
    boolean isConnected();

    /**
     * Returns the timestamp of the last successful synchronization with the broker.
     *
     * @return timestamp string (or "Never" if not yet synchronized)
     */
    String getLastSync();

    /**
     * Returns the list of discovered/paired integration devices.
     *
     * @return observable list of integration devices
     */
    ObservableList<IntegrationDevice> getDiscoveredDevices();

    /**
     * Saves broker configuration settings.
     *
     * <p>This method updates the configuration but does not automatically connect.
     * Call {@link #connect()} to establish a connection after saving.
     *
     * @param enabled whether to enable IoT integration
     * @param broker broker address
     * @param port broker port
     * @param username broker username (optional)
     * @param password broker password (optional)
     */
    void saveConfiguration(boolean enabled, String broker, int port, String username, String password);

    /**
     * Tests if the given broker and port settings are valid.
     *
     * <p>This performs validation without establishing an actual connection.
     * For real implementations, this may attempt a brief connection.
     *
     * @param broker broker address to test
     * @param portValue broker port as a string
     * @return true if parameters appear valid, false otherwise
     */
    boolean testConnection(String broker, String portValue);

    /**
     * Connects to the MQTT broker with current configuration.
     *
     * <p>Connection is only attempted if integration is enabled and broker settings are valid.
     * On successful connection, subscribes to all devices' state topics and updates lastSync.
     *
     * @return true if connection was successful, false otherwise
     */
    boolean connect();

    /**
     * Disconnects from the MQTT broker and cleans up resources.
     */
    void disconnect();

    /**
     * Refreshes the list of discovered devices from the broker.
     *
     * <p>Only successful if currently connected.
     *
     * @return true if refresh was successful, false if not connected or error occurred
     */
    boolean refreshDevices();

    /**
     * Publishes a command to a device's command topic.
     *
     * <p>The command is published to {@code smarthome/<deviceId>/cmd}.
     * Only successful if connected and the device is paired.
     *
     * @param deviceId the device identifier
     * @param command command payload to publish
     * @return true if published successfully, false otherwise
     */
    boolean publishCommand(String deviceId, String command);

    /**
     * Retrieves the current state of a paired device.
     *
     * <p>This may return a cached value if the device has been recently queried.
     *
     * @param deviceId the device identifier
     * @return current state string (e.g., "ON", "OFF", temperature value), or null if not found
     */
    String getDeviceState(String deviceId);

    /**
     * Adds a listener to be notified when a device's state changes via MQTT message.
     *
     * @param listener callback to invoke with deviceId and new state
     */
    void addStateChangeListener(DeviceStateChangeListener listener);

    /**
     * Removes a previously registered state change listener.
     *
     * @param listener the listener to remove
     */
    void removeStateChangeListener(DeviceStateChangeListener listener);

    /**
     * Callback interface for device state changes received from the broker.
     */
    @FunctionalInterface
    interface DeviceStateChangeListener {

        /**
         * Called when a device's state changes.
         *
         * @param deviceId the device identifier
         * @param newState the new state value
         */
        void onStateChanged(String deviceId, String newState);
    }
}
