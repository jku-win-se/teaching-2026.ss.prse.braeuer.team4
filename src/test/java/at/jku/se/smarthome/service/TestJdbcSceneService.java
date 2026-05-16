package at.jku.se.smarthome.service;

import java.util.List;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Scene;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.mock.MockLogService;
import at.jku.se.smarthome.service.mock.MockNotificationService;
import at.jku.se.smarthome.service.mock.MockRoomService;
import at.jku.se.smarthome.service.real.scene.JdbcSceneService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Validates the JDBC-backed scene service implementation (FR-17).
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects",
        "PMD.AtLeastOneConstructor", "PMD.AvoidDuplicateLiterals",
        "PMD.UnitTestContainsTooManyAsserts"})
public class TestJdbcSceneService {

    /** JDBC URL property. */
    private static final String URL_PROPERTY = "smarthome.db.url";
    /** JDBC user property. */
    private static final String USER_PROPERTY = "smarthome.db.user";
    /** JDBC password property. */
    private static final String PASSWORD_PROPERTY = "smarthome.db.password";

    /** Service under test. */
    private JdbcSceneService sceneService;
    /** Mocked room service. */
    private MockRoomService roomService;
    /** Mocked notification service. */
    private MockNotificationService notificationService;
    /** Mocked log service. */
    private MockLogService logService;

    /**
     * Initializes a fresh instance of the JDBC scene service with isolated dependencies.
     */
    @Before
    public void setUp() {
        String jdbcUrl = "jdbc:h2:mem:scenes_" + System.nanoTime()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        System.setProperty(URL_PROPERTY, jdbcUrl);
        System.setProperty(USER_PROPERTY, "sa");
        System.setProperty(PASSWORD_PROPERTY, "");

        MockRoomService.resetForTesting();
        MockNotificationService.resetForTesting();
        MockLogService.resetForTesting();
        JdbcSceneService.resetForTesting();

        roomService = MockRoomService.getInstance();
        notificationService = MockNotificationService.getInstance();
        logService = MockLogService.getInstance();

        ServiceRegistry.setRoomServiceForTesting(roomService);
        ServiceRegistry.setNotificationServiceForTesting(notificationService);
        ServiceRegistry.setLogServiceForTesting(logService);

        sceneService = JdbcSceneService.getInstance();

        // Clear demo scenes for deterministic testing
        for (Scene s : sceneService.getScenes().stream().toList()) {
            sceneService.deleteScene(s.getId());
        }
    }

    /**
     * Resets the singleton instances.
     */
    @After
    public void tearDown() {
        JdbcSceneService.resetForTesting();
        MockRoomService.resetForTesting();
        MockNotificationService.resetForTesting();
        MockLogService.resetForTesting();

        ServiceRegistry.setRoomServiceForTesting(null);
        ServiceRegistry.setNotificationServiceForTesting(null);
        ServiceRegistry.setLogServiceForTesting(null);

        System.clearProperty(URL_PROPERTY);
        System.clearProperty(USER_PROPERTY);
        System.clearProperty(PASSWORD_PROPERTY);
    }

    /**
     * Verifies that adding a scene without device states works.
     */
    @Test
    public void addSceneWithoutDeviceStatesPersistsSuccessfully() {
        Scene scene = sceneService.addScene("Test Scene", "A test scene");
        assertNotNull("Should create scene", scene);
        assertEquals("Name matches", "Test Scene", scene.getName());
        assertTrue("Device states empty", scene.getDeviceStates().isEmpty());

        assertEquals("Scene count is 1", 1, sceneService.getScenes().size());
        assertEquals("Scene matches in list", scene, sceneService.getScenes().get(0));
    }

    /**
     * Verifies that adding a scene with device states works and persists across reloads.
     */
    @Test
    public void addSceneWithDeviceStatesPersistsSuccessfully() {
        List<String> states = List.of("Main Light: ON", "Thermostat: 22°C");
        Scene scene = sceneService.addScene("Test Scene", "A test scene", states);

        assertNotNull("Should create scene", scene);
        assertEquals("States size matches", 2, scene.getDeviceStates().size());
        assertEquals("States content matches", states, scene.getDeviceStates());

        JdbcSceneService.resetForTesting();
        JdbcSceneService refreshedService = JdbcSceneService.getInstance();
        Scene loadedScene = refreshedService.getScenes().get(0);

        assertEquals("States survived reload", states, loadedScene.getDeviceStates());
    }

    /**
     * Verifies that updating a scene name, description and states works.
     */
    @Test
    public void updateSceneChangesPersist() {
        Scene scene = sceneService.addScene("Original", "Original desc", List.of("Device1: OFF"));

        boolean success = sceneService.updateScene(scene.getId(), "Updated", "Updated desc", List.of("Device1: ON", "Device2: OPEN"));

        assertTrue("Update successful", success);
        assertEquals("Name updated", "Updated", scene.getName());
        assertEquals("Desc updated", "Updated desc", scene.getDescription());
        assertEquals("States updated", 2, scene.getDeviceStates().size());

        JdbcSceneService.resetForTesting();
        JdbcSceneService refreshedService = JdbcSceneService.getInstance();
        Scene loadedScene = refreshedService.getScenes().get(0);

        assertEquals("Update survived reload", "Updated", loadedScene.getName());
        assertEquals("States survived reload", 2, loadedScene.getDeviceStates().size());
    }

    /**
     * Verifies that deleting a scene also removes its associated states.
     */
    @Test
    public void deleteSceneRemovesSceneAndStates() {
        Scene scene = sceneService.addScene("To Delete", "Desc", List.of("Device1: OFF"));

        boolean success = sceneService.deleteScene(scene.getId());

        assertTrue("Delete successful", success);

        JdbcSceneService.resetForTesting();
        JdbcSceneService refreshedService = JdbcSceneService.getInstance();

        assertTrue("Delete survived reload", refreshedService.getScenes().stream().noneMatch(s -> s.getId().equals(scene.getId())));
    }

    /**
     * Verifies that activating a scene applies states to devices and logs the action.
     */
    @Test
    public void activateSceneAppliesKnownStates() {
        Device light = roomService.getDeviceByName("Main Light");
        assertNotNull(light);
        light.setState(false);

        int initialNotifications = notificationService.getNotifications().size();
        int initialLogs = logService.getLogs().size();

        Scene scene = sceneService.addScene("Test Scene", "Desc", List.of("Main Light: ON"));

        boolean success = sceneService.activateScene(scene.getId());

        assertTrue("Activation returned true", success);
        assertTrue("Device state changed", light.getState());

        assertEquals("Notification generated", initialNotifications + 1, notificationService.getNotifications().size());
        assertEquals("Log generated", initialLogs + 2, logService.getLogs().size()); // 1 for device, 1 overall
    }

    /**
     * Verifies that activating a non-existent scene returns false.
     */
    @Test
    public void activateSceneUnknownSceneIdReturnsFalse() {
        boolean success = sceneService.activateScene("invalid-id");
        assertFalse("Activation returned false", success);
    }
}
