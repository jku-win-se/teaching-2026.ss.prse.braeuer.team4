package at.jku.se.smarthome.service;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Scene;
import at.jku.se.smarthome.service.mock.MockLogService;
import at.jku.se.smarthome.service.mock.MockNotificationService;
import at.jku.se.smarthome.service.mock.MockRoomService;
import at.jku.se.smarthome.service.mock.MockSceneService;

/**
 * Unit tests for MockSceneService.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods"})
public class TestMockSceneService {


    /** Scene service under test. */
    private MockSceneService service;
    /** Mock room service. */
    private MockRoomService roomService;
    /** Mock log service. */
    private MockLogService logService;
    /** Mock notification service. */
    private MockNotificationService notificationService;

    /**
     * Sets up test fixtures before each test.
     */
    @Before
    public void setUp() {
        MockRoomService.resetForTesting();
        MockLogService.resetForTesting();
        MockNotificationService.resetForTesting();
        MockSceneService.resetForTesting();

        roomService = MockRoomService.getInstance();
        logService = MockLogService.getInstance();
        notificationService = MockNotificationService.getInstance();
        service = MockSceneService.getInstance();
    }

    /**
     * Test: activate scene returns true.
     */
    @Test
    public void activateSceneReturnsTrue() {
        assertTrue(service.activateScene("scene-001"));
    }

    /**
     * Test: activate scene sets device brightness.
     */
    @Test
    public void activateSceneSetsDimmerBrightness() {
        service.activateScene("scene-001");
        Device dimmer = roomService.getDeviceByName("Dimmer Light");
        assertEquals(20, dimmer.getBrightness());
    }

    /**
     * Test: activate scene sets device states.
     */
    @Test
    public void activateSceneSetsDimmerState() {
        service.activateScene("scene-001");
        Device dimmer = roomService.getDeviceByName("Dimmer Light");
        assertTrue(dimmer.getState());
    }

    /**
     * Test: activate scene turns off main light.
     */
    @Test
    public void activateSceneTurnsOffMainLight() {
        service.activateScene("scene-001");
        Device mainLight = roomService.getDeviceByName("Main Light");
        assertFalse(mainLight.getState());
    }

    /**
     * Test: activate scene turns off ceiling light.
     */
    @Test
    public void activateSceneTurnsOffCeilingLight() {
        service.activateScene("scene-001");
        Device ceilingLight = roomService.getDeviceByName("Ceiling Light");
        assertFalse(ceilingLight.getState());
    }

    /**
     * Test: activate scene logs action.
     */
    @Test
    public void activateSceneLogsAction() {
        int beforeLogs = logService.getLogs().size();
        service.activateScene("scene-001");
        assertTrue(logService.getLogs().size() > beforeLogs);
    }

    /**
     * Test: activate scene sends notification.
     */
    @Test
    public void activateSceneSendsNotification() {
        int beforeNotifications = notificationService.getNotifications().size();
        service.activateScene("scene-001");
        assertTrue(notificationService.getNotifications().size() > beforeNotifications);
    }

    /**
     * Test: add scene returns non-null.
     */
    @Test
    public void addSceneReturnsNonNull() {
        Scene scene = service.addScene("Study", "Quiet mode", List.of("Main Light: OFF", "Temperature Control: 21°C"));
        assertNotNull(scene);
    }

    /**
     * Test: add scene sets device states count.
     */
    @Test
    public void addSceneSetsDeviceStatesCount() {
        Scene scene = service.addScene("Study", "Quiet mode", List.of("Main Light: OFF", "Temperature Control: 21°C"));
        assertEquals(2, scene.getDeviceStates().size());
    }

    /**
     * Test: update scene returns true.
     */
    @Test
    public void updateSceneReturnsTrue() {
        Scene scene = service.addScene("Study", "Quiet mode", List.of("Main Light: OFF", "Temperature Control: 21°C"));
        assertTrue(service.updateScene(scene.getId(), "Study Updated", "Warm mode", List.of("Dimmer Light: 50%")));
    }

    /**
     * Test: update scene changes name.
     */
    @Test
    public void updateSceneChangesName() {
        Scene scene = service.addScene("Study", "Quiet mode", List.of("Main Light: OFF", "Temperature Control: 21°C"));
        service.updateScene(scene.getId(), "Study Updated", "Warm mode", List.of("Dimmer Light: 50%"));
        assertEquals("Study Updated", scene.getName());
    }

    /**
     * Test: update scene changes description.
     */
    @Test
    public void updateSceneChangesDescription() {
        Scene scene = service.addScene("Study", "Quiet mode", List.of("Main Light: OFF", "Temperature Control: 21°C"));
        service.updateScene(scene.getId(), "Study Updated", "Warm mode", List.of("Dimmer Light: 50%"));
        assertEquals("Warm mode", scene.getDescription());
    }

    /**
     * Test: update scene changes device states count.
     */
    @Test
    public void updateSceneChangesDeviceStatesCount() {
        Scene scene = service.addScene("Study", "Quiet mode", List.of("Main Light: OFF", "Temperature Control: 21°C"));
        service.updateScene(scene.getId(), "Study Updated", "Warm mode", List.of("Dimmer Light: 50%"));
        assertEquals(1, scene.getDeviceStates().size());
    }

    /**
     * Test: delete scene returns true.
     */
    @Test
    public void deleteSceneReturnsTrue() {
        Scene scene = service.addScene("Study", "Quiet mode", List.of("Main Light: OFF", "Temperature Control: 21°C"));
        assertTrue(service.deleteScene(scene.getId()));
    }

    /**
     * Test: delete non-existent scene returns false.
     */
    @Test
    public void deleteNonExistentSceneReturnsFalse() {
        assertFalse(service.deleteScene("does-not-exist"));
    }
}