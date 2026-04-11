package at.jku.se.smarthome.service.mock;

import at.jku.se.smarthome.model.Device;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock Smart Home Service providing simulated device management.
 * 
 * This is a Singleton service that manages a collection of mock devices
 * and provides methods to control them without connecting to a real backend.
 * For now, all changes are in-memory only.
 */
public class MockSmartHomeService {
    
    private static MockSmartHomeService instance;
    private final ObservableList<Device> devices;
    private String currentUser;
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private MockSmartHomeService() {
        this.devices = FXCollections.observableArrayList();
        this.currentUser = "User";
        initializeMockData();
    }
    
    /**
     * Gets the singleton instance of MockSmartHomeService.
     * 
     * @return the singleton instance
     */
    public static synchronized MockSmartHomeService getInstance() {
        if (instance == null) {
            instance = new MockSmartHomeService();
        }
        return instance;
    }
    
    /**
     * Resets the singleton for unit testing.
     * Must NOT be called from production code.
     */
    public static synchronized void resetForTesting() {
        instance = null;
    }

    /**
     * Initializes the service with mock device data.
     */
    private void initializeMockData() {
        devices.add(new Device("dev-001", "Living Room Light", "Switch", "Living Room", true));
        devices.add(new Device("dev-002", "Living Room Thermostat", "Thermostat", "Living Room", true));
        devices.add(new Device("dev-003", "Bedroom Dimmer", "Dimmer", "Bedroom", false));
        devices.add(new Device("dev-004", "Kitchen Light", "Switch", "Kitchen", true));
        devices.add(new Device("dev-005", "Bedroom Thermostat", "Thermostat", "Bedroom", true));
        devices.add(new Device("dev-006", "Garden Blind", "Cover/Blind", "Garden", false));
        devices.add(new Device("dev-007", "Hallway Motion Sensor", "Sensor", "Hallway", true));

        // Set initial brightness and temperature values
        devices.get(2).setBrightness(75);
        devices.get(1).setTemperature(22.5);
        devices.get(4).setTemperature(20.0);
    }
    
    /**
     * Gets all devices as an observable list.
     * 
     * @return observable list of devices
     */
    public ObservableList<Device> getDevices() {
        return devices;
    }
    
    /**
     * Gets a device by its ID.
     * 
     * @param deviceId the device identifier
     * @return the device, or null if not found
     */
    public Device getDeviceById(String deviceId) {
        return devices.stream()
                .filter(d -> d.getId().equals(deviceId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Toggles the power state of a device.
     * 
     * @param deviceId the device identifier
     * @return true if the toggle was successful, false otherwise
     */
    public boolean toggleDevice(String deviceId) {
        Device device = getDeviceById(deviceId);
        if (device != null) {
            device.setState(!device.getState());
            return true;
        }
        return false;
    }
    
    /**
     * Sets the brightness level of a dimmer device.
     * 
     * @param deviceId    the device identifier
     * @param brightness  the brightness level (0-100)
     * @return true if successful, false otherwise
     */
    public boolean setBrightness(String deviceId, int brightness) {
        Device device = getDeviceById(deviceId);
        if (device != null && "Dimmer".equals(device.getType())) {
            if (brightness < 0 || brightness > 100) {
                return false;
            }
            device.setBrightness(brightness);
            if (brightness == 0) {
                device.setState(false);
            } else if (brightness > 0) {
                device.setState(true);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Sets the temperature of a thermostat device.
     * 
     * @param deviceId      the device identifier
     * @param temperature   the target temperature in Celsius
     * @return true if successful, false otherwise
     */
    public boolean setTemperature(String deviceId, double temperature) {
        Device device = getDeviceById(deviceId);
        if (device != null && "Thermostat".equals(device.getType())) {
            if (temperature < 10.0 || temperature > 35.0) {
                return false;
            }
            device.setTemperature(temperature);
            return true;
        }
        return false;
    }
    
    /**
     * Authenticates a user with email and password.
     * Mock implementation accepts any non-empty credentials.
     * 
     * @param email    the user's email
     * @param password the user's password
     * @return true if authentication is successful
     */
    public boolean authenticate(String email, String password) {
        if (email != null && !email.isEmpty() && password != null && !password.isEmpty()) {
            this.currentUser = email.split("@")[0];
            return true;
        }
        return false;
    }
    
    /**
     * Gets the current logged-in user.
     * 
     * @return the current user's display name
     */
    public String getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Logs out the current user.
     */
    public void logout() {
        this.currentUser = "User";
    }

    /**
     * Opens a blind/cover device (sets state to true = OPEN).
     *
     * @param deviceId the device identifier
     * @return true if successful, false if the device is not found or not a Cover/Blind
     */
    public boolean openBlind(String deviceId) {
        Device device = getDeviceById(deviceId);
        if (device != null && "Cover/Blind".equals(device.getType())) {
            device.setState(true);
            return true;
        }
        return false;
    }

    /**
     * Closes a blind/cover device (sets state to false = CLOSED).
     *
     * @param deviceId the device identifier
     * @return true if successful, false if the device is not found or not a Cover/Blind
     */
    public boolean closeBlind(String deviceId) {
        Device device = getDeviceById(deviceId);
        if (device != null && "Cover/Blind".equals(device.getType())) {
            device.setState(false);
            return true;
        }
        return false;
    }

    /**
     * Injects a test value into a sensor device.
     * Any double is accepted — sensors have no inherent range constraint.
     *
     * @param deviceId the device identifier
     * @param value    the value to inject
     * @return true if successful, false if the device is not found or not a Sensor
     */
    public boolean injectSensorValue(String deviceId, double value) {
        Device device = getDeviceById(deviceId);
        if (device != null && "Sensor".equals(device.getType())) {
            device.setTemperature(value);
            return true;
        }
        return false;
    }
}
