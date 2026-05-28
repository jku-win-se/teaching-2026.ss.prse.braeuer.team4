package at.jku.se.smarthome.service.mock;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.NotificationType;
import at.jku.se.smarthome.model.Rule;
import at.jku.se.smarthome.service.api.NotificationService;
import at.jku.se.smarthome.service.api.RuleService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.rule.RuleEvaluator;
import at.jku.se.smarthome.service.rule.RuleValidator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Mock Rule Service providing rule management functionality.
 */
@SuppressWarnings({"PMD.UseObjectForClearerAPI", "PMD.TooManyMethods", "PMD.CyclomaticComplexity", "PMD.GodClass"})
public final class MockRuleService implements RuleService {
    
    /** Singleton instance of the mock rule service. */
    private static MockRuleService instance;
    /** Observable collection of all rules. */
    private final ObservableList<Rule> rules;
    /** Room service for device lookups. */
    private final MockRoomService roomService = MockRoomService.getInstance();
    /** Notification service for rule events. */
    private final NotificationService notificationService = ServiceRegistry.getNotificationService();
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
    @SuppressWarnings("PMD.NullAssignment")
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
        // Heat Boost relies on Motion Sensor currentValue (default 0.0) so it
        // does not fire by accident; previously it matched because temperature
        // defaulted to 20.0. See FR-11 / Issue #20.
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
        Rule rule = null;
        RuleValidator.Result validation = RuleValidator.validate(triggerType, condition, sourceDevice, roomService);
        if (validation.valid()) {
            rule = new Rule(
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
        }
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
            RuleValidator.Result validation = RuleValidator.validate(triggerType, condition, sourceDevice, roomService);
            if (validation.valid()) {
                rule.setName(name);
                rule.setTriggerType(triggerType);
                rule.setSourceDevice(sourceDevice);
                rule.setCondition(condition);
                rule.setAction(action);
                rule.setTargetDevice(targetDevice);
                updated = true;
            }
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
        return executeRule(ruleId, false);
    }

    @Override
    public boolean executeRule(String ruleId, boolean isManual) {
        boolean executed = false;
        Rule rule = rules.stream()
                .filter(r -> r.getId().equals(ruleId))
                .findFirst()
                .orElse(null);

        if (rule == null) {
            notificationService.addNotification("Rule execution failed: rule not found", NotificationType.FAILURE);
        } else if (rule.isEnabled()) {
            Device sourceDevice = roomService.getDeviceByName(rule.getSourceDevice());
            boolean conditionMet = isManual || new RuleEvaluator().evaluate(rule, sourceDevice);
            if (conditionMet) {
                Device targetDevice = roomService.getDeviceByName(rule.getTargetDevice());
                if (targetDevice != null && applyAction(targetDevice, rule.getAction())) {
                    logService.addLogEntry(targetDevice.getName(), targetDevice.getRoom(), rule.getAction(), "Rule: " + rule.getName());
                    notificationService.addNotification(
                            "Rule '" + rule.getName() + "' executed: " + rule.getAction() + " → " + rule.getTargetDevice(),
                            NotificationType.SUCCESS);
                    executed = true;
                } else if (targetDevice == null) {
                    notificationService.addNotification("Rule execution failed: target device missing for " + rule.getName(), NotificationType.FAILURE);
                } else {
                    notificationService.addNotification("Rule execution failed: unsupported action in " + rule.getName(), NotificationType.FAILURE);
                }
            }
        } else {
            notificationService.addNotification("Rule execution failed: " + rule.getName() + " is inactive", NotificationType.FAILURE);
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
    @SuppressWarnings("PMD.CyclomaticComplexity")
    private boolean applyAction(Device targetDevice, String action) {
        boolean result = false;
        if (action != null) {
            result = switch (action) {
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
                    } else {
                        yield false;
                    }
                }
            };
        }
        return result;
    }
    
    /**
     * Detects conflicts between rules.
     *
     * @param ruleId identifier of the rule being validated
     * @return true when a conflict exists, otherwise false
     */
    public boolean hasConflicts(String ruleId) {
        boolean conflictDetected = false;
        Rule candidate = rules.stream()
                .filter(r -> r.getId().equals(ruleId))
                .findFirst()
                .orElse(null);
        if (candidate != null && candidate.isEnabled()) {
            String target = candidate.getTargetDevice();
            String candAction = normalizeValue(candidate.getAction());
            String candTrigger = normalizeTrigger(candidate.getTriggerType());
            String candCondition = normalizeValue(candidate.getCondition());

            for (Rule existing : rules) {
                if (existing.getId().equals(ruleId)) {
                    continue;
                }
                if (!existing.isEnabled()) {
                    continue;
                }
                if (!target.equals(existing.getTargetDevice())) {
                    continue;
                }
                String existTrigger = normalizeTrigger(existing.getTriggerType());
                String existCondition = normalizeValue(existing.getCondition());

                // Very small overlap heuristic: same trigger type and same condition string
                if (!candTrigger.equals(existTrigger) || !candCondition.equals(existCondition)) {
                    continue;
                }

                String existAction = normalizeValue(existing.getAction());
                if (valuesAreIncompatible(candAction, existAction)) {
                    conflictDetected = true;
                    break;
                }
            }
        }
        return conflictDetected;
    }

    // --- conflict helpers (copied/kept small for mock use) ---
    private String normalizeTrigger(String trigger) {
        return trigger == null ? "" : trigger.trim().toLowerCase();
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private boolean valuesAreIncompatible(String value1, String value2) {
        boolean incompatible = false;
        String action1 = canonicalizeAction(value1);
        String action2 = canonicalizeAction(value2);
        if (!action1.equals(action2)) {
            if (isSwitchValue(action1) && isSwitchValue(action2)) {
                incompatible = true;
            } else if (isCoverValue(action1) && isCoverValue(action2)) {
                incompatible = true;
            } else if (isDimmerValue(action1) && isDimmerValue(action2)) {
                incompatible = true;
            } else if (isThermostatValue(action1) && isThermostatValue(action2)) {
                try {
                    double temp1 = extractTemperatureValue(action1);
                    double temp2 = extractTemperatureValue(action2);
                    incompatible = Math.abs(temp1 - temp2) > 0.5;
                } catch (NumberFormatException e) {
                    incompatible = false;
                }
            }
        }
        return incompatible;
    }

    private boolean isSwitchValue(String value) {
        return "ON".equals(value) || "OFF".equals(value);
    }

    private boolean isCoverValue(String value) {
        return "OPEN".equals(value) || "CLOSED".equals(value) || "CLOSE".equals(value);
    }

    private boolean isDimmerValue(String value) {
        return value.matches("\\d+%?") || value.matches("0\\.\\d+");
    }

    private boolean isThermostatValue(String value) {
        return value.matches("[0-9]+(\\\\.[0-9]+)?°?C?") || value.matches("[0-9]+(\\\\.[0-9]+)?\\s*°?C?");
    }

    private double extractTemperatureValue(String value) {
        String numeric = value.replaceAll("[^0-9.]", "");
        return Double.parseDouble(numeric);
    }

    private String canonicalizeAction(String value) {
        String result = "";
        if (value != null) {
            String upperValue = value.trim().toUpperCase();
            if (upperValue.contains("TURN ON")) {
                result = "ON";
            } else if (upperValue.contains("TURN OFF")) {
                result = "OFF";
            } else if (upperValue.contains("OPEN")) {
                result = "OPEN";
            } else if (upperValue.contains("CLOSE") || upperValue.contains("CLOSED")) {
                result = "CLOSED";
            } else if (upperValue.startsWith("SET TO ") && upperValue.endsWith("%")) {
                result = upperValue.substring(7);
            } else if (upperValue.startsWith("SET TO ") && upperValue.endsWith("°C")) {
                result = upperValue.substring(7);
            } else {
                result = upperValue;
            }
        }
        return result;
    }

    @Override
    public void startRecurringExecution() {
        // no-op for mock — rules execute on explicit call only
    }

    @Override
    public void stopRecurringExecution() {
        // no-op for mock
    }
}
