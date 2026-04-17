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

public class TestMockSceneService {

    private MockSceneService service;
    private MockRoomService roomService;
    private MockLogService logService;
    private MockNotificationService notificationService;

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

    @Test
    public void activateSceneAppliesToDeviceStatesLogsAndNotifies() {
        int beforeLogs = logService.getLogs().size();
        int beforeNotifications = notificationService.getNotifications().size();

        assertTrue(service.activateScene("scene-001"));

        Device dimmer = roomService.getDeviceByName("Dimmer Light");
        Device mainLight = roomService.getDeviceByName("Main Light");
        Device ceilingLight = roomService.getDeviceByName("Ceiling Light");

        assertEquals(20, dimmer.getBrightness());
        assertTrue(dimmer.getState());
        assertFalse(mainLight.getState());
        assertFalse(ceilingLight.getState());
        assertTrue(logService.getLogs().size() > beforeLogs);
        assertTrue(notificationService.getNotifications().size() > beforeNotifications);
    }

    @Test
    public void addUpdateDeleteSceneManageConfiguredStates() {
        Scene scene = service.addScene("Study", "Quiet mode", List.of("Main Light: OFF", "Temperature Control: 21°C"));
        assertNotNull(scene);
        assertEquals(2, scene.getDeviceStates().size());

        assertTrue(service.updateScene(scene.getId(), "Study Updated", "Warm mode", List.of("Dimmer Light: 50%")));
        assertEquals("Study Updated", scene.getName());
        assertEquals("Warm mode", scene.getDescription());
        assertEquals(1, scene.getDeviceStates().size());

        assertTrue(service.deleteScene(scene.getId()));
        assertFalse(service.deleteScene("does-not-exist"));
    }
}