package at.jku.se.smarthome.service;

import java.util.List;
import java.util.Locale;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Scene;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock Scene Service providing smart scene management functionality.
 */
public class MockSceneService {
    
    private static MockSceneService instance;
    private final ObservableList<Scene> scenes;
    private final MockRoomService roomService = MockRoomService.getInstance();
    private final MockLogService logService = MockLogService.getInstance();
    private final MockNotificationService notificationService = MockNotificationService.getInstance();
    
    private MockSceneService() {
        this.scenes = FXCollections.observableArrayList();
        initializeMockScenes();
    }
    
    public static synchronized MockSceneService getInstance() {
        if (instance == null) {
            instance = new MockSceneService();
        }
        return instance;
    }
    
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
     */
    public ObservableList<Scene> getScenes() {
        return scenes;
    }
    
    /**
     * Adds a new scene.
     */
    public Scene addScene(String name, String description) {
        return addScene(name, description, List.of());
    }

    /**
     * Adds a new scene with explicit device states.
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
     */
    public boolean activateScene(String sceneId) {
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
            return true;
        }
        return false;
    }
    
    /**
     * Updates a scene.
     */
    public boolean updateScene(String sceneId, String name, String description) {
        return updateScene(sceneId, name, description, List.of());
    }

    /**
     * Updates a scene and replaces its configured device states.
     */
    public boolean updateScene(String sceneId, String name, String description, List<String> deviceStates) {
        Scene scene = scenes.stream()
                .filter(s -> s.getId().equals(sceneId))
                .findFirst()
                .orElse(null);
        
        if (scene != null) {
            scene.setName(name);
            scene.setDescription(description);
            scene.setDeviceStates(deviceStates);
            return true;
        }
        return false;
    }
    
    /**
     * Deletes a scene.
     */
    public boolean deleteScene(String sceneId) {
        return scenes.removeIf(s -> s.getId().equals(sceneId));
    }

    private boolean applyDeviceState(String sceneName, String stateDefinition) {
        String[] parts = stateDefinition.split(":", 2);
        if (parts.length != 2) {
            return false;
        }

        String deviceName = parts[0].trim();
        String targetState = parts[1].trim();

        Device device = roomService.getDeviceByName(deviceName);
        if (device == null) {
            return false;
        }

        boolean applied = applyStateToDevice(device, targetState);
        if (applied) {
            logService.addLogEntry(
                    device.getName(),
                    device.getRoom(),
                    "Scene '" + sceneName + "' set state to " + targetState,
                    "Scene"
            );
        }
        return applied;
    }

    private boolean applyStateToDevice(Device device, String targetState) {
        String normalized = targetState.trim().toLowerCase(Locale.ROOT);

        if (normalized.endsWith("%")) {
            try {
                int brightness = Integer.parseInt(normalized.substring(0, normalized.length() - 1).trim());
                device.setBrightness(brightness);
                device.setState(brightness > 0);
                return true;
            } catch (NumberFormatException exception) {
                return false;
            }
        }

        if (normalized.endsWith("°c")) {
            try {
                double temperature = Double.parseDouble(normalized.replace("°c", "").trim());
                device.setTemperature(temperature);
                device.setState(true);
                return true;
            } catch (NumberFormatException exception) {
                return false;
            }
        }

        return switch (normalized) {
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
}
