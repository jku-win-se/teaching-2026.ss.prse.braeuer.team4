package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.Rule;
import at.jku.se.smarthome.service.mock.MockLogService;
import at.jku.se.smarthome.service.mock.MockNotificationService;
import at.jku.se.smarthome.service.mock.MockRoomService;
import at.jku.se.smarthome.service.mock.MockRuleService;

public class TestMockRuleService {

    private MockRuleService service;
    private MockRoomService roomService;
    private MockLogService logService;
    private MockNotificationService notificationService;

    @Before
    public void setUp() {
        MockRoomService.resetForTesting();
        MockLogService.resetForTesting();
        MockNotificationService.resetForTesting();
        MockRuleService.resetForTesting();

        roomService = MockRoomService.getInstance();
        logService = MockLogService.getInstance();
        notificationService = MockNotificationService.getInstance();
        service = MockRuleService.getInstance();
    }

    @Test
    public void executeRuleActiveRuleAppliesToTargetActionAndLogs() {
        Rule rule = service.addRule("Shutdown", "Time", "Clock", "23:00", "Turn Off", "Main Light");
        int beforeLogs = logService.getLogs().size();
        int beforeNotifications = notificationService.getNotifications().size();

        assertTrue(service.executeRule(rule.getId()));
        assertFalse(roomService.getDeviceByName("Main Light").getState());
        assertTrue(logService.getLogs().size() > beforeLogs);
        assertTrue(notificationService.getNotifications().size() > beforeNotifications);
    }

    @Test
    public void toggleAndExecuteRuleInactiveRuleFails() {
        Rule rule = service.addRule("Alert", "Sensor Threshold", "Motion Sensor", "Value > 0", "Notify User", "Main Light");
        assertTrue(service.toggleRule(rule.getId()));
        assertEquals("Inactive", rule.getStatus());

        assertFalse(service.executeRule(rule.getId()));
        assertEquals("error", notificationService.getNotifications().get(0).getType());
    }

    @Test
    public void updateAndDeleteRuleMutateRuleCollection() {
        Rule rule = service.addRule("Comfort", "Time", "Clock", "06:00 AM", "Set to 22°C", "Temperature Control");
        assertNotNull(rule);

        assertTrue(service.updateRule(rule.getId(), "Comfort Updated", "Device State", "Bed Light", "State = Active", "Set to 18°C", "Temperature Control"));
        assertEquals("Comfort Updated", rule.getName());
        assertEquals("Device State", rule.getTriggerType());
        assertEquals("Set to 18°C", rule.getAction());
        assertTrue(service.deleteRule(rule.getId()));
        assertFalse(service.deleteRule("missing"));
        assertFalse(service.hasConflicts(rule.getId()));
    }
}