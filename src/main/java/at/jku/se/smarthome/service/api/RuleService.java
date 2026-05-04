package at.jku.se.smarthome.service.api;

import at.jku.se.smarthome.model.Rule;
import javafx.collections.ObservableList;

/**
 * Defines operations for managing automation rules in the smart-home system.
 */
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public interface RuleService {

    /**
     * Returns all rules currently managed by the service.
     *
     * @return observable list of rules
     */
    ObservableList<Rule> getRules();

    /**
     * Adds a new rule.
     *
     * @param name         display name of the rule
     * @param triggerType  trigger category
     * @param sourceDevice source device or source label
     * @param condition    trigger condition expression
     * @param action       action to execute
     * @param targetDevice target device name
     * @return created rule instance, or null when validation fails
     */
    Rule addRule(String name, String triggerType, String sourceDevice,
                 String condition, String action, String targetDevice);

    /**
     * Updates an existing rule.
     *
     * @param ruleId       identifier of the rule to update
     * @param name         updated rule name
     * @param triggerType  updated trigger category
     * @param sourceDevice updated source device or source label
     * @param condition    updated trigger condition expression
     * @param action       updated action text
     * @param targetDevice updated target device name
     * @return true when the rule exists and the update is valid, otherwise false
     */
    boolean updateRule(String ruleId, String name, String triggerType,
                       String sourceDevice, String condition,
                       String action, String targetDevice);

    /**
     * Deletes a rule.
     *
     * @param ruleId identifier of the rule to delete
     * @return true when the rule existed and was removed, otherwise false
     */
    boolean deleteRule(String ruleId);

    /**
     * Toggles a rule's enabled state.
     *
     * @param ruleId identifier of the rule to toggle
     * @return true when the rule exists and was toggled, otherwise false
     */
    boolean toggleRule(String ruleId);

    /**
     * Executes a rule in the engine and emits in-app notifications.
     *
     * @param ruleId identifier of the rule to execute
     * @return true when execution succeeded, otherwise false
     */
    boolean executeRule(String ruleId);

    /**
     * Detects conflicts between rules.
     *
     * @param ruleId identifier of the rule being validated
     * @return true when a conflict exists, otherwise false
     */
    boolean hasConflicts(String ruleId);
}
