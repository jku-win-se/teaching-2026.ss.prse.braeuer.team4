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

/**
 * Unit tests for MockRuleService.
 */
public class TestMockRuleService {

    /** Rule service under test. */
    private MockRuleService service;
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
        MockRuleService.resetForTesting();

        roomService = MockRoomService.getInstance();
        logService = MockLogService.getInstance();
        notificationService = MockNotificationService.getInstance();
        service = MockRuleService.getInstance();
    }

    /**
     * Test: execute active rule applies action.
     */
    @Test
    public void executeActiveRuleAppliesAction() {
        Rule rule = service.addRule("Shutdown", "Time", "Clock", "23:00", "Turn Off", "Main Light");
        assertTrue(service.executeRule(rule.getId()));
    }

    /**
     * Test: execute active rule changes device state.
     */
    @Test
    public void executeActiveRuleChangesDeviceState() {
        Rule rule = service.addRule("Shutdown", "Time", "Clock", "23:00", "Turn Off", "Main Light");
        service.executeRule(rule.getId());
        assertFalse(roomService.getDeviceByName("Main Light").getState());
    }

    /**
     * Test: execute active rule creates log entry.
     */
    @Test
    public void executeActiveRuleCreatesLogEntry() {
        Rule rule = service.addRule("Shutdown", "Time", "Clock", "23:00", "Turn Off", "Main Light");
        int beforeLogs = logService.getLogs().size();
        service.executeRule(rule.getId());
        assertTrue(logService.getLogs().size() > beforeLogs);
    }

    /**
     * Test: execute active rule creates notification.
     */
    @Test
    public void executeActiveRuleCreatesNotification() {
        Rule rule = service.addRule("Shutdown", "Time", "Clock", "23:00", "Turn Off", "Main Light");
        int beforeNotifications = notificationService.getNotifications().size();
        service.executeRule(rule.getId());
        assertTrue(notificationService.getNotifications().size() > beforeNotifications);
    }

    /**
     * Test: toggle rule sets inactive status.
     */
    @Test
    public void toggleRuleSetsInactiveStatus() {
        Rule rule = service.addRule("Alert", "Sensor Threshold", "Motion Sensor", "Value > 0", "Notify User", "Main Light");
        service.toggleRule(rule.getId());
        assertEquals("Inactive", rule.getStatus());
    }

    /**
     * Test: execute inactive rule fails.
     */
    @Test
    public void executeInactiveRuleFails() {
        Rule rule = service.addRule("Alert", "Sensor Threshold", "Motion Sensor", "Value > 0", "Notify User", "Main Light");
        service.toggleRule(rule.getId());
        assertFalse(service.executeRule(rule.getId()));
    }

    /**
     * Test: execute inactive rule returns error notification.
     */
    @Test
    public void executeInactiveRuleReturnsErrorNotification() {
        Rule rule = service.addRule("Alert", "Sensor Threshold", "Motion Sensor", "Value > 0", "Notify User", "Main Light");
        service.toggleRule(rule.getId());
        service.executeRule(rule.getId());
        assertEquals("error", notificationService.getNotifications().get(0).getType());
    }

    /**
     * Test: add rule returns non-null.
     */
    @Test
    public void addRuleReturnsNonNull() {
        Rule rule = service.addRule("Comfort", "Time", "Clock", "06:00 AM", "Set to 22°C", "Temperature Control");
        assertNotNull(rule);
    }

    /**
     * Test: update rule changes name.
     */
    @Test
    public void updateRuleChangesName() {
        Rule rule = service.addRule("Comfort", "Time", "Clock", "06:00 AM", "Set to 22°C", "Temperature Control");
        service.updateRule(rule.getId(), "Comfort Updated", "Device State", "Bed Light", "State = Active", "Set to 18°C", "Temperature Control");
        assertEquals("Comfort Updated", rule.getName());
    }

    /**
     * Test: update rule changes trigger type.
     */
    @Test
    public void updateRuleChangesTriggerType() {
        Rule rule = service.addRule("Comfort", "Time", "Clock", "06:00 AM", "Set to 22°C", "Temperature Control");
        service.updateRule(rule.getId(), "Comfort Updated", "Device State", "Bed Light", "State = Active", "Set to 18°C", "Temperature Control");
        assertEquals("Device State", rule.getTriggerType());
    }

    /**
     * Test: update rule changes action.
     */
    @Test
    public void updateRuleChangesAction() {
        Rule rule = service.addRule("Comfort", "Time", "Clock", "06:00 AM", "Set to 22°C", "Temperature Control");
        service.updateRule(rule.getId(), "Comfort Updated", "Device State", "Bed Light", "State = Active", "Set to 18°C", "Temperature Control");
        assertEquals("Set to 18°C", rule.getAction());
    }

    /**
     * Test: delete rule removes from collection.
     */
    @Test
    public void deleteRuleRemovesFromCollection() {
        Rule rule = service.addRule("Comfort", "Time", "Clock", "06:00 AM", "Set to 22°C", "Temperature Control");
        assertTrue(service.deleteRule(rule.getId()));
    }

    /**
     * Test: delete missing rule fails.
     */
    @Test
    public void deleteMissingRuleFails() {
        assertFalse(service.deleteRule("missing"));
    }

    /**
     * Test: delete rule clears conflicts.
     */
    @Test
    public void deleteRuleClearsConflicts() {
        Rule rule = service.addRule("Comfort", "Time", "Clock", "06:00 AM", "Set to 22°C", "Temperature Control");
        service.deleteRule(rule.getId());
        assertFalse(service.hasConflicts(rule.getId()));
    }
}