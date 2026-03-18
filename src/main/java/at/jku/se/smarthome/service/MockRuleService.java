package at.jku.se.smarthome.service;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Rule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock Rule Service providing rule management functionality.
 */
public class MockRuleService {
    
    private static MockRuleService instance;
    private final ObservableList<Rule> rules;
    private final MockRoomService roomService = MockRoomService.getInstance();
    private final MockNotificationService notificationService = MockNotificationService.getInstance();
    private final MockLogService logService = MockLogService.getInstance();
    
    private MockRuleService() {
        this.rules = FXCollections.observableArrayList();
        initializeMockRules();
    }
    
    public static synchronized MockRuleService getInstance() {
        if (instance == null) {
            instance = new MockRuleService();
        }
        return instance;
    }
    
    private void initializeMockRules() {
        rules.add(new Rule("rule-001", "Morning Routine", "Time", "Clock", "06:00 AM", 
                          "Turn On", "Main Light", true, "Active"));
        rules.add(new Rule("rule-002", "Motion Welcome", "Device State", "Bedroom Light", "State = Active", 
                          "Turn On", "Main Light", true, "Active"));
        rules.add(new Rule("rule-003", "Heat Boost", "Sensor Threshold", "Motion Sensor", 
                          "Value > 0", "Set to 22°C", "Temperature Control", true, "Inactive"));
    }
    
    /**
     * Gets all rules.
     */
    public ObservableList<Rule> getRules() {
        return rules;
    }
    
    /**
     * Adds a new rule.
     */
    public Rule addRule(String name, String triggerType, String sourceDevice, String condition,
                       String action, String targetDevice) {
        Rule rule = new Rule(
                "rule-" + String.format("%03d", rules.size() + 1),
                name,
                triggerType,
                sourceDevice,
                condition,
                action,
                targetDevice,
                true,
                "Active"
        );
        rules.add(rule);
        return rule;
    }
    
    /**
     * Updates a rule.
     */
    public boolean updateRule(String ruleId, String name, String triggerType, String sourceDevice,
                             String condition, String action, String targetDevice) {
        Rule rule = rules.stream()
                .filter(r -> r.getId().equals(ruleId))
                .findFirst()
                .orElse(null);
        
        if (rule != null) {
            rule.setName(name);
            rule.setTriggerType(triggerType);
            rule.setSourceDevice(sourceDevice);
            rule.setCondition(condition);
            rule.setAction(action);
            rule.setTargetDevice(targetDevice);
            return true;
        }
        return false;
    }
    
    /**
     * Toggles a rule's enabled state.
     */
    public boolean toggleRule(String ruleId) {
        Rule rule = rules.stream()
                .filter(r -> r.getId().equals(ruleId))
                .findFirst()
                .orElse(null);
        
        if (rule != null) {
            rule.setEnabled(!rule.isEnabled());
            rule.setStatus(rule.isEnabled() ? "Active" : "Inactive");
            return true;
        }
        return false;
    }
    
    /**
     * Deletes a rule.
     */
    public boolean deleteRule(String ruleId) {
        return rules.removeIf(r -> r.getId().equals(ruleId));
    }

    /**
     * Executes a rule in the mock engine and emits in-app notifications.
     */
    public boolean executeRule(String ruleId) {
        Rule rule = rules.stream()
                .filter(r -> r.getId().equals(ruleId))
                .findFirst()
                .orElse(null);

        if (rule == null) {
            notificationService.addNotification("Rule execution failed: rule not found", "error");
            return false;
        }

        if (!rule.isEnabled()) {
            notificationService.addNotification("Rule execution failed: " + rule.getName() + " is inactive", "error");
            return false;
        }

        Device targetDevice = roomService.getDeviceByName(rule.getTargetDevice());
        if (targetDevice == null) {
            notificationService.addNotification("Rule execution failed: target device missing for " + rule.getName(), "error");
            return false;
        }

        boolean success = applyAction(targetDevice, rule.getAction());
        if (!success) {
            notificationService.addNotification("Rule execution failed: unsupported action in " + rule.getName(), "error");
            return false;
        }

        logService.addLogEntry(targetDevice.getName(), targetDevice.getRoom(), rule.getAction(), "Rule: " + rule.getName());
        notificationService.addNotification("Rule executed: " + rule.getName(), "success");
        return true;
    }

    private boolean applyAction(Device targetDevice, String action) {
        switch (action) {
            case "Turn On":
                targetDevice.setState(true);
                return true;
            case "Turn Off":
                targetDevice.setState(false);
                return true;
            case "Open":
                targetDevice.setState(true);
                return true;
            case "Close":
                targetDevice.setState(false);
                return true;
            case "Notify User":
            case "Trigger Alert":
                return true;
            default:
                if (action.startsWith("Set to ") && action.endsWith("%")) {
                    int brightness = Integer.parseInt(action.substring(7, action.length() - 1));
                    targetDevice.setBrightness(brightness);
                    return true;
                }
                if (action.startsWith("Set to ") && action.endsWith("°C")) {
                    double temperature = Double.parseDouble(action.substring(7, action.length() - 2));
                    targetDevice.setTemperature(temperature);
                    return true;
                }
                return false;
        }
    }
    
    /**
     * Detects conflicts between rules.
     */
    public boolean hasConflicts(String ruleId) {
        return false; // Simplified mock
    }
}
