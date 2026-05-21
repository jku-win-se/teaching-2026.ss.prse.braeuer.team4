package at.jku.se.smarthome.service.real.mqtt;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import at.jku.se.smarthome.model.IntegrationDevice;
import at.jku.se.smarthome.service.api.IoTIntegrationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Real MQTT integration service using Eclipse Paho MQTT client.
 * 
 * <p>Encapsulates all MQTT broker connectivity behind the IoTIntegrationService interface.
 * Connection failures and network issues are handled gracefully without blocking the UI thread.
 * 
 * <p>This service maintains singleton state and connects/disconnects on demand. All MQTT
 * operations run on background threads to prevent UI blocking.
 */
@SuppressWarnings({"PMD.DoNotUseThreads", "PMD.GodClass", "PMD.TooManyMethods"})
public final class MqttIntegrationService implements IoTIntegrationService, MqttCallback {

    /** Logger for MQTT-related events. */
    private static final Logger LOGGER = LogManager.getLogger(MqttIntegrationService.class);
    
    /** Lock for singleton synchronization. */
    private static final Object INSTANCE_LOCK = new Object();
    /** Singleton instance. */
    private static MqttIntegrationService instance;
    
    /** Date/time formatter for synchronization timestamps. */
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /** Observable list of discovered/paired devices. */
    private final ObservableList<IntegrationDevice> discoveredDevices = FXCollections.observableArrayList();
    
    /** Map of device ID to last known state. */
    private final Map<String, String> deviceStates = new ConcurrentHashMap<>();
    
    /** List of state change listeners. */
    private final List<DeviceStateChangeListener> stateChangeListeners = 
        new CopyOnWriteArrayList<>();
    
    /** MQTT client instance. */
    private MqttClient mqttClient;
    
    /** Enable flag. */
    private boolean enabled;
    /** Connection status flag. */
    private boolean connected;
    /** MQTT broker address. */
    private String broker;
    /** MQTT broker port. */
    private int port;
    /** MQTT broker username. */
    private String username;
    /** MQTT broker password. */
    private String password;
    /** Last synchronization timestamp. */
    private String lastSync;
    
    private MqttIntegrationService() {
        this.broker = "mqtt.example.com";
        this.port = 1883;
        this.username = "";
        this.password = "";
        this.lastSync = "Never";
        this.enabled = false;
        this.connected = false;
    }
    
    /**
     * Returns singleton instance.
     *
     * @return the singleton instance
     */
    public static MqttIntegrationService getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new MqttIntegrationService();
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
            if (instance != null) {
                instance.disconnect();
            }
            instance = null;
        }
    }
    
    @Override
    public String getProtocolName() {
        return "MQTT";
    }
    
    @Override
    public IoTConfiguration getConfiguration() {
        return new IoTConfiguration(enabled, broker, port, username, password);
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public boolean isConnected() {
        return connected;
    }
    
    @Override
    public String getLastSync() {
        return lastSync;
    }
    
    @Override
    public ObservableList<IntegrationDevice> getDiscoveredDevices() {
        return discoveredDevices;
    }
    
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
            deviceStates.clear();
        }
        
        LOGGER.info("IoT configuration saved: enabled={}, broker={}, port={}", enabled, broker, port);
    }
    
    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public boolean testConnection(String brokerAddr, String portValue) {
        if (brokerAddr == null || brokerAddr.isBlank()) {
            return false;
        }
        
        try {
            int parsedPort = Integer.parseInt(portValue);
            // For now, just validate parameters without attempting actual connection.
            // A real implementation might attempt a brief connection here.
            return parsedPort > 0 && parsedPort <= 65535;
        } catch (NumberFormatException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Invalid port number: {}", portValue);
            }
            return false;
        }
    }
    
    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public boolean connect() {
        if (!enabled || broker == null || broker.isBlank() || port <= 0) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Connection conditions not met: enabled={}, broker={}, port={}", enabled, broker, port);
            }
            connected = false;
            return false;
        }
        
        // Perform connection in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                String clientId = "SmartHome-" + System.currentTimeMillis();
                String brokerUrl = "tcp://" + broker + ":" + port;
                
                mqttClient = new MqttClient(brokerUrl, clientId);
                mqttClient.setCallback(this);
                
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                options.setConnectionTimeout(10); // 10 second timeout
                options.setAutomaticReconnect(true);
                
                if (!username.isBlank()) {
                    options.setUserName(username);
                    if (!password.isBlank()) {
                        options.setPassword(password.toCharArray());
                    }
                }
                
                mqttClient.connect(options);
                connected = true;
                lastSync = LocalDateTime.now().format(formatter);
                
                // Subscribe to all discovered devices' state topics
                subscribeToDeviceTopics();
                
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Connected to MQTT broker: {}", brokerUrl);
                }
                Platform.runLater(() -> {
                    // Update UI with connected status
                });
            } catch (MqttException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Failed to connect to MQTT broker: {}", e.getMessage());
                }
                connected = false;
                Platform.runLater(() -> {
                    // Update UI with error status
                });
            }
        }).start();
        
        return connected;
    }
    
    @Override
    public void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            new Thread(() -> {
                try {
                    mqttClient.disconnect();
                    mqttClient.close();
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Disconnected from MQTT broker");
                    }
                } catch (MqttException e) {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error("Error during disconnect: {}", e.getMessage());
                    }
                }
            }).start();
        }
        connected = false;
        deviceStates.clear();
    }
    
    @Override
    @SuppressWarnings({"PMD.OnlyOneReturn", "PMD.AvoidCatchingNPE", "PMD.AvoidCatchingGenericException"})
    public boolean refreshDevices() {
        if (!connected || mqttClient == null || !mqttClient.isConnected()) {
            return false;
        }
        
        new Thread(() -> {
            try {
                // In a real implementation, this might query the broker or resubscribe
                lastSync = LocalDateTime.now().format(formatter);
                subscribeToDeviceTopics();
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Refreshed device list from broker");
                }
            } catch (NullPointerException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Error refreshing devices: {}", e.getMessage());
                }
            }
        }).start();
        
        return true;
    }
    
    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public boolean publishCommand(String deviceId, String command) {
        if (!connected || mqttClient == null || !mqttClient.isConnected()) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Cannot publish command: not connected to broker");
            }
            return false;
        }
        
        new Thread(() -> {
            try {
                String topic = "smarthome/" + deviceId + "/cmd";
                MqttMessage message = new MqttMessage(command.getBytes());
                message.setQos(1); // At least once delivery
                mqttClient.publish(topic, message);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Published command to {}: {}", topic, command);
                }
            } catch (MqttException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Failed to publish command: {}", e.getMessage());
                }
            }
        }).start();
        
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
    
    // MqttCallback implementation
    @Override
    public void connectionLost(Throwable cause) {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("MQTT connection lost: {}", cause.getMessage());
        }
        connected = false;
        Platform.runLater(() -> {
            // Update UI
        });
    }
    
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        LOGGER.debug("Message arrived on {}: {}", topic, payload);
        
        // Extract device ID from topic (format: smarthome/<deviceId>/state)
        String[] parts = topic.split("/");
        if (parts.length >= 3 && "smarthome".equals(parts[0]) && "state".equals(parts[2])) {
            String deviceId = parts[1];
            deviceStates.put(deviceId, payload);
            
            // Notify listeners on UI thread
            Platform.runLater(() -> {
                for (DeviceStateChangeListener listener : stateChangeListeners) {
                    listener.onStateChanged(deviceId, payload);
                }
            });
        }
    }
    
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        LOGGER.debug("Message delivery complete");
    }
    
    /**
     * Subscribes to all discovered devices' state topics.
     */
    private void subscribeToDeviceTopics() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            return;
        }
        
        try {
            // Subscribe to all devices with wildcard
            String topic = "smarthome/+/state";
            mqttClient.subscribe(topic, 1);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Subscribed to topic: {}", topic);
            }
        } catch (MqttException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Failed to subscribe to device topics: {}", e.getMessage());
            }
        }
    }
}
