package at.jku.se.smarthome.service.mock;

import java.util.List;
import java.util.Locale;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Scene;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock Scene Service providing smart scene management functionality.
 */
public final class MockSceneService {

    /** Expected parts count when parsing a "device:state" scene definition. */
    private static final int DEVICE_STATE_PART_COUNT = 2;
    
    /** Singleton instance of the mock scene service. */
    private static MockSceneService instance;
    /**
     * Singleton scenes collection.
     */
    private final ObservableList<Scene> scenes;
    /** Room service reference for device lookups. */
    private final MockRoomService roomService = MockRoomService.getInstance();
    /** Log service for activity tracking. */
    private final MockLogService logService = MockLogService.getInstance();
    /** Notification service for scene events. */
    private final MockNotificationService notificationService = MockNotificationService.getInstance();
    
    private MockSceneService() {
        this.scenes = FXCollections.observableArrayList();
        initializeMockScenes();
    }
    
    /**
     * Returns the singleton instance of the mock scene service.
     *
     * @return singleton MockSceneService instance
     */
    public static MockSceneService getInstance() {
        synchronized (MockSceneService.class) {
            if (instance == null) {
                instance = new MockSceneService();
            }
            return instance;
        }
    }

    /**
     * Resets the singleton for unit testing.
     * Must NOT be called from production code.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public static void resetForTesting() {
        synchronized (MockSceneService.class) {
            instance = null;
        }
    }
    
    /**
     * Helper: initializes mock scenes with predefined configurations.
     */
    private void initializeMockScenes() {
        Scene movieScene = new Scene("scene-001", "Movie Night", "Dim all lights and close shutters");
        movieScene.addDeviceState("Dimmer Light: 20%");
        movieScene.addDeviceState("Main Light: OFF");
        movieScene.addDeviceState("Ceiling Light: OFF");
        scenes.add(movieScene);
        
        Scene awayScene = new Scene("scene-002", "Away", "Reduce energy usage while nobody is home");
        awayScene.addDeviceState("Main Light: OFF");
        awayScene.addDeviceState("Dimmer Light: 0%");
        awayScene.addDeviceState("Ceiling Light: OFF");
        awayScene.addDeviceState("Thermostat: 18°C");
        awayScene.addDeviceState("Exhaust Fan: OFF");
        scenes.add(awayScene);
        
        Scene morningScene = new Scene("scene-003", "Morning", "Wake up routine");
        morningScene.addDeviceState("Main Light: ON");
        morningScene.addDeviceState("Dimmer Light: 75%");
        morningScene.addDeviceState("Ceiling Light: ON");
        morningScene.addDeviceState("Temperature Control: 22°C");
        scenes.add(morningScene);
    }
    
    /**
     * Gets all scenes.
     *
     * @return observable list of scenes
     */
    public ObservableList<Scene> getScenes() {
        return scenes;
    }
    
    /**
     * Adds a new scene.
     *
     * @param name display name of the scene
     * @param description summary of the scene behavior
     * @return created scene instance
     */
    public Scene addScene(String name, String description) {
        return addScene(name, description, List.of());
    }

    /**
        * Adds a new scene with explicit device states.
        *
        * @param name display name of the scene
        * @param description summary of the scene behavior
        * @param deviceStates configured device-state definitions
        * @return created scene instance
     */
    public Scene addScene(String name, String description, List<String> deviceStates) {
        Scene scene = new Scene(
                "scene-" + String.format("%03d", scenes.size() + 1),
                name,
                description
        );
        scene.setDeviceStates(deviceStates);
        scenes.add(scene);
        return scene;
    }
    
    /**
        * Activates a scene.
        *
        * @param sceneId identifier of the scene to activate
        * @return true when the scene exists and activation ran, otherwise false
     */
    public boolean activateScene(String sceneId) {
        boolean activated = false;
        Scene scene = scenes.stream()
                .filter(s -> s.getId().equals(sceneId))
                .findFirst()
                .orElse(null);
        
        if (scene != null) {
            int appliedStates = 0;
            int skippedStates = 0;

            for (String deviceState : scene.getDeviceStates()) {
                if (applyDeviceState(scene.getName(), deviceState)) {
                    appliedStates++;
                } else {
                    skippedStates++;
                }
            }

            logService.addLogEntry(
                    "Scene: " + scene.getName(),
                    "Overall",
                    "Activated with " + appliedStates + " applied state" + (appliedStates == 1 ? "" : "s")
                            + (skippedStates > 0 ? " and " + skippedStates + " skipped" : ""),
                    "User"
            );

            if (skippedStates == 0) {
                notificationService.addNotification("Scene '" + scene.getName() + "' activated successfully", "success");
            } else {
                notificationService.addNotification(
                        "Scene '" + scene.getName() + "' activated with " + skippedStates + " skipped device state(s)",
                        "info"
                );
            }
            activated = true;
        }
        return activated;
    }
    
    /**
     * Updates a scene.
     *
     * @param sceneId identifier of the scene to update
     * @param name updated scene name
     * @param description updated scene description
     * @return true when the scene exists and was updated, otherwise false
     */
    public boolean updateScene(String sceneId, String name, String description) {
        return updateScene(sceneId, name, description, List.of());
    }

    /**
        * Updates a scene and replaces its configured device states.
        *
        * @param sceneId identifier of the scene to update
        * @param name updated scene name
        * @param description updated scene description
        * @param deviceStates replacement device-state definitions
        * @return true when the scene exists and was updated, otherwise false
     */
    public boolean updateScene(String sceneId, String name, String description, List<String> deviceStates) {
        boolean updated = false;
        Scene scene = scenes.stream()
                .filter(s -> s.getId().equals(sceneId))
                .findFirst()
                .orElse(null);
        
        if (scene != null) {
            scene.setName(name);
            scene.setDescription(description);
            scene.setDeviceStates(deviceStates);
            updated = true;
        }
        return updated;
    }
    
    /**
     * Deletes a scene.
     *
     * @param sceneId identifier of the scene to delete
     * @return true when the scene existed and was removed, otherwise false
     */
    public boolean deleteScene(String sceneId) {
        return scenes.removeIf(s -> s.getId().equals(sceneId));
    }

    /**
     * Helper: applies device state change based on scene state definition.
     *
     * @param sceneName name of scene being applied
     * @param stateDefinition state definition string
     * @return true if state was applied successfully
     */
    private boolean applyDeviceState(String sceneName, String stateDefinition) {
        boolean applied = false;
        String[] parts = stateDefinition.split(":", DEVICE_STATE_PART_COUNT);
        if (parts.length == DEVICE_STATE_PART_COUNT) {
            String deviceName = parts[0].trim();
            String targetState = parts[1].trim();

            Device device = roomService.getDeviceByName(deviceName);
            if (device != null) {
                applied = applyStateToDevice(device, targetState);
                if (applied) {
                    logService.addLogEntry(
                            device.getName(),
                            device.getRoom(),
                            "Scene '" + sceneName + "' set state to " + targetState,
                            "Scene"
                    );
                }
            }
        }
        return applied;
    }

    /**
     * Helper: applies normalized target state to device (brightness, temperature, or on/off).
     *
     * @param device device to update
     * @param targetState target state string
     * @return true if state was applied successfully
     */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    private boolean applyStateToDevice(Device device, String targetState) {
        boolean applied = false;
        String normalized = targetState.trim().toLowerCase(Locale.ROOT);

        if (normalized.endsWith("%")) {
            try {
                int brightness = Integer.parseInt(normalized.substring(0, normalized.length() - 1).trim());
                device.setBrightness(brightness);
                device.setState(brightness > 0);
                applied = true;
            } catch (NumberFormatException exception) {
                applied = false;
            }
        } else if (normalized.endsWith("°c")) {
            try {
                double temperature = Double.parseDouble(normalized.replace("°c", "").trim());
                device.setTemperature(temperature);
                device.setState(true);
                applied = true;
            } catch (NumberFormatException exception) {
                applied = false;
            }
        } else {
            applied = switch (normalized) {
                case "on", "open", "opened" -> {
                    device.setState(true);
                    yield true;
                }
                case "off", "close", "closed" -> {
                    device.setState(false);
                    yield true;
                }
                default -> false;
            };
        }
        return applied;
    }
}
