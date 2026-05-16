package at.jku.se.smarthome.service.api;

import java.util.List;

import at.jku.se.smarthome.model.Scene;
import javafx.collections.ObservableList;

/**
 * Defines operations for managing scenes in the smart-home system.
 */
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public interface SceneService {

    /**
     * Returns all scenes currently managed by the service.
     *
     * @return observable list of scenes
     */
    ObservableList<Scene> getScenes();

    /**
     * Adds a new scene without device states.
     *
     * @param name display name of the scene
     * @param description description of the scene
     * @return created scene instance
     */
    Scene addScene(String name, String description);

    /**
     * Adds a new scene with predefined device states.
     *
     * @param name display name of the scene
     * @param description description of the scene
     * @param deviceStates list of device states in the format "DeviceName: state"
     * @return created scene instance
     */
    Scene addScene(String name, String description, List<String> deviceStates);

    /**
     * Updates an existing scene without changing its device states.
     *
     * @param sceneId identifier of the scene to update
     * @param name updated scene name
     * @param description updated description
     * @return true when the scene exists and was updated, otherwise false
     */
    boolean updateScene(String sceneId, String name, String description);

    /**
     * Updates an existing scene and its device states.
     *
     * @param sceneId identifier of the scene to update
     * @param name updated scene name
     * @param description updated description
     * @param deviceStates updated list of device states
     * @return true when the scene exists and was updated, otherwise false
     */
    boolean updateScene(String sceneId, String name, String description, List<String> deviceStates);

    /**
     * Deletes a scene.
     *
     * @param sceneId identifier of the scene to delete
     * @return true when the scene existed and was removed, otherwise false
     */
    boolean deleteScene(String sceneId);

    /**
     * Activates a scene, applying its device states to the target devices.
     *
     * @param sceneId identifier of the scene to activate
     * @return true when execution succeeded, otherwise false
     */
    boolean activateScene(String sceneId);
}
