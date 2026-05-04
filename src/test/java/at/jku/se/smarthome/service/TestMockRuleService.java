package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalTime;

import at.jku.se.smarthome.model.NotificationEntry;
import at.jku.se.smarthome.model.Rule;
import at.jku.se.smarthome.service.api.RuleService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.mock.MockLogService;
import at.jku.se.smarthome.service.mock.MockNotificationService;
import at.jku.se.smarthome.service.mock.MockRoomService;
import at.jku.se.smarthome.service.mock.MockRuleService;

/**
 * Unit tests for MockRuleService.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods",
        "PMD.MethodNamingConventions", "PMD.UnitTestContainsTooManyAsserts"})
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
        Rule rule = service.addRule("Shutdown", "Time", "Clock", nowTime(), "Turn Off", "Main Light");
        assertTrue(service.executeRule(rule.getId()));
    }

    /**
     * Test: execute active rule changes device state.
     */
    @Test
    public void executeActiveRuleChangesDeviceState() {
        Rule rule = service.addRule("Shutdown", "Time", "Clock", nowTime(), "Turn Off", "Main Light");
        service.executeRule(rule.getId());
        assertFalse(roomService.getDeviceByName("Main Light").getState());
    }

    /**
     * Test: execute active rule creates log entry.
     */
    @Test
    public void executeActiveRuleCreatesLogEntry() {
        Rule rule = service.addRule("Shutdown", "Time", "Clock", nowTime(), "Turn Off", "Main Light");
        int beforeLogs = logService.getLogs().size();
        service.executeRule(rule.getId());
        assertTrue(logService.getLogs().size() > beforeLogs);
    }

    /**
     * Test: execute active rule creates notification.
     */
    @Test
    public void executeActiveRuleCreatesNotification() {
        Rule rule = service.addRule("Shutdown", "Time", "Clock", nowTime(), "Turn Off", "Main Light");
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

    /**
     * Test: execute rule does not apply action when condition is not met.
     */
    @Test
    public void executeRule_conditionNotMet_doesNotApplyAction() {
        // Bed Light is off (state = false) in the mock seed
        Rule rule = service.addRule("Bed Check", "Device State", "Bed Light", "State = Active", "Turn On", "Main Light");
        boolean result = service.executeRule(rule.getId());
        assertFalse(result);
        assertTrue(roomService.getDeviceByName("Main Light").getState());
    }

    /**
     * Test: execute rule applies action when condition is met.
     */
    @Test
    public void executeRule_conditionMet_appliesAction() {
        // Main Light is on (state = true) in the mock seed
        Rule rule = service.addRule("Main Check", "Device State", "Main Light", "State = Active", "Turn On", "Bed Light");
        boolean result = service.executeRule(rule.getId());
        assertTrue(result);
        assertTrue(roomService.getDeviceByName("Bed Light").getState());
    }

    /**
     * Test: execute rule emits error notification when condition is not met.
     */
    @Test
    public void executeRule_conditionNotMet_emitsErrorNotification() {
        Rule rule = service.addRule("Bed Check", "Device State", "Bed Light", "State = Active", "Turn On", "Main Light");
        service.executeRule(rule.getId());
        NotificationEntry note = notificationService.getNotifications().get(0);
        assertEquals("error", note.getType());
        assertTrue(note.getMessage().contains("condition not met"));
    }

    /**
     * Test: add rule with invalid trigger type returns null and does not add.
     */
    @Test
    public void addRule_invalidTriggerType_returnsNullAndDoesNotAddRule() {
        int before = service.getRules().size();
        Rule rule = service.addRule("Bad", "Magic", "Clock", "07:00", "Turn On", "Main Light");
        assertEquals(null, rule);
        assertEquals(before, service.getRules().size());
    }

    /**
     * Test: add rule with invalid time condition returns null.
     */
    @Test
    public void addRule_invalidTimeCondition_returnsNull() {
        Rule rule = service.addRule("Bad", "Time", "Clock", "hello world", "Turn On", "Main Light");
        assertEquals(null, rule);
    }

    /**
     * Test: add rule with invalid threshold source returns null.
     */
    @Test
    public void addRule_invalidThresholdSource_returnsNull() {
        // Main Light is a Switch, not a sensor
        Rule rule = service.addRule("Bad", "Sensor Threshold", "Main Light", "Value > 5", "Turn On", "Bed Light");
        assertEquals(null, rule);
    }

    /**
     * Test: update rule with invalid payload returns false and preserves rule.
     */
    @Test
    public void updateRule_invalidPayload_returnsFalseAndPreservesRule() {
        Rule rule = service.addRule("Good", "Time", "Clock", "06:00 AM", "Turn On", "Main Light");
        String originalName = rule.getName();
        String originalCondition = rule.getCondition();
        boolean result = service.updateRule(rule.getId(), "Bad", "Time", "Clock", " Funday 07:00 ", "Turn On", "Main Light");
        assertFalse(result);
        assertEquals(originalName, rule.getName());
        assertEquals(originalCondition, rule.getCondition());
    }

    /**
     * Test: ServiceRegistry returns rule service and setForTesting works.
     */
    @Test
    public void serviceRegistry_returnsRuleService_andSetForTestingWorks() {
        RuleService original = ServiceRegistry.getRuleService();
        assertNotNull(original);
        ServiceRegistry.setRuleServiceForTesting(service);
        assertEquals(service, ServiceRegistry.getRuleService());
        ServiceRegistry.setRuleServiceForTesting(null);
    }

    /**
     * Helper: returns the current time formatted as HH:mm.
     *
     * @return current hour and minute string
     */
    private String nowTime() {
        LocalTime now = LocalTime.now();
        return String.format("%02d:%02d", now.getHour(), now.getMinute());
    }
}