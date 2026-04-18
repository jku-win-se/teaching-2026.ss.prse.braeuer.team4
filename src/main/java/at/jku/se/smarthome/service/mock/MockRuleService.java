package at.jku.se.smarthome.service.mock;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Rule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock Rule Service providing rule management functionality.
 */
public final class MockRuleService {
    
    /** Singleton instance of the mock rule service. */
    private static MockRuleService instance;
    /** Observable collection of all rules. */
    private final ObservableList<Rule> rules;
    /** Room service for device lookups. */
    private final MockRoomService roomService = MockRoomService.getInstance();
    /** Notification service for rule events. */
    private final MockNotificationService notificationService = MockNotificationService.getInstance();
    /** Log service for activity tracking. */
    private final MockLogService logService = MockLogService.getInstance();
    private MockRuleService() {
        this.rules = FXCollections.observableArrayList();
        initializeMockRules();
    }
    
    /**
     * Returns the singleton instance of the mock rule service.
     *
     * @return singleton MockRuleService instance
     */
    public static MockRuleService getInstance() {
        synchronized (MockRuleService.class) {
            if (instance == null) {
                instance = new MockRuleService();
            }
            return instance;
        }
    }

    /**
     * Resets the singleton for unit testing.
     * Must NOT be called from production code.
     */
    public static void resetForTesting() {
        synchronized (MockRuleService.class) {
            instance = null;
        }
    }
    
    /**
     * Helper: initializes mock rules with predefined configurations.
     */
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
     *
     * @return observable list of rules
     */
    public ObservableList<Rule> getRules() {
        return rules;
    }
    
    /**
        * Adds a new rule.
        *
        * @param name display name of the rule
        * @param triggerType trigger category
        * @param sourceDevice source device or source label
        * @param condition trigger condition expression
        * @param action action to execute
        * @param targetDevice target device name
        * @return created rule instance
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
        *
        * @param ruleId identifier of the rule to update
        * @param name updated rule name
        * @param triggerType updated trigger category
        * @param sourceDevice updated source device or source label
        * @param condition updated trigger condition expression
        * @param action updated action text
        * @param targetDevice updated target device name
        * @return true when the rule exists and was updated, otherwise false
     */
    public boolean updateRule(String ruleId, String name, String triggerType, String sourceDevice,
                             String condition, String action, String targetDevice) {
        boolean updated = false;
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
            updated = true;
        }
        return updated;
    }
    
    /**
        * Toggles a rule's enabled state.
        *
        * @param ruleId identifier of the rule to toggle
        * @return true when the rule exists and was toggled, otherwise false
     */
    public boolean toggleRule(String ruleId) {
        boolean toggled = false;
        Rule rule = rules.stream()
                .filter(r -> r.getId().equals(ruleId))
                .findFirst()
                .orElse(null);
        
        if (rule != null) {
            rule.setEnabled(!rule.isEnabled());
            rule.setStatus(rule.isEnabled() ? "Active" : "Inactive");
            toggled = true;
        }
        return toggled;
    }
    
    /**
     * Deletes a rule.
     *
     * @param ruleId identifier of the rule to delete
     * @return true when the rule existed and was removed, otherwise false
     */
    public boolean deleteRule(String ruleId) {
        return rules.removeIf(r -> r.getId().equals(ruleId));
    }

    /**
        * Executes a rule in the mock engine and emits in-app notifications.
        *
        * @param ruleId identifier of the rule to execute
        * @return true when execution succeeded, otherwise false
     */
    public boolean executeRule(String ruleId) {
        boolean executed = false;
        Rule rule = rules.stream()
                .filter(r -> r.getId().equals(ruleId))
                .findFirst()
                .orElse(null);

        if (rule != null && rule.isEnabled()) {
            Device targetDevice = roomService.getDeviceByName(rule.getTargetDevice());
            if (targetDevice != null && applyAction(targetDevice, rule.getAction())) {
                logService.addLogEntry(targetDevice.getName(), targetDevice.getRoom(), rule.getAction(), "Rule: " + rule.getName());
                notificationService.addNotification("Rule executed: " + rule.getName(), "success");
                executed = true;
            } else if (targetDevice == null) {
                notificationService.addNotification("Rule execution failed: target device missing for " + rule.getName(), "error");
            } else {
                notificationService.addNotification("Rule execution failed: unsupported action in " + rule.getName(), "error");
            }
        } else if (rule == null) {
            notificationService.addNotification("Rule execution failed: rule not found", "error");
        } else {
            notificationService.addNotification("Rule execution failed: " + rule.getName() + " is inactive", "error");
        }
        return executed;
    }

    /**
     * Helper: applies action to device.
     *
     * @param targetDevice device to apply action to
     * @param action action string to execute
     * @return true if action was applied successfully
     */
    private boolean applyAction(Device targetDevice, String action) {
        boolean result = switch (action) {
            case "Turn On", "Open" -> {
                targetDevice.setState(true);
                yield true;
            }
            case "Turn Off", "Close" -> {
                targetDevice.setState(false);
                yield true;
            }
            case "Notify User", "Trigger Alert" -> true;
            default -> {
                if (action.startsWith("Set to ") && action.endsWith("%")) {
                    try {
                        int brightness = Integer.parseInt(action.substring(7, action.length() - 1));
                        targetDevice.setBrightness(brightness);
                        yield true;
                    } catch (NumberFormatException e) {
                        yield false;
                    }
                } else if (action.startsWith("Set to ") && action.endsWith("°C")) {
                    try {
                        double temperature = Double.parseDouble(action.substring(7, action.length() - 2));
                        targetDevice.setTemperature(temperature);
                        yield true;
                    } catch (NumberFormatException e) {
                        yield false;
                    }
                }
                yield false;
            }
        };
        return result;
    }
    
    /**
     * Detects conflicts between rules.
     *
     * @param ruleId identifier of the rule being validated
     * @return true when a conflict exists, otherwise false
     */
    public boolean hasConflicts(String ruleId) {
        return false; // Simplified mock
    }
}
