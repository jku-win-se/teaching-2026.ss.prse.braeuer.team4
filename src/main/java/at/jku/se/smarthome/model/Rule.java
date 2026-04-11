package at.jku.se.smarthome.model;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Represents a Rule in the SmartHome system.
 */
public class Rule {
    
    private final SimpleStringProperty id;
    private final SimpleStringProperty name;
    private final SimpleStringProperty triggerType;
    private final SimpleStringProperty sourceDevice;
    private final SimpleStringProperty condition;
    private final SimpleStringProperty action;
    private final SimpleStringProperty targetDevice;
    private final SimpleBooleanProperty enabled;
    private final SimpleStringProperty status;
    
    /**
     * Creates an automation rule.
     *
     * @param id unique rule identifier
     * @param name display name of the rule
     * @param triggerType trigger category
     * @param sourceDevice trigger source device or source label
     * @param condition trigger condition expression
     * @param action action to perform
     * @param targetDevice target device name
     * @param enabled whether the rule is enabled
     * @param status current status label
     */
    public Rule(String id, String name, String triggerType, String sourceDevice, String condition,
                String action, String targetDevice, boolean enabled, String status) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.triggerType = new SimpleStringProperty(triggerType);
        this.sourceDevice = new SimpleStringProperty(sourceDevice);
        this.condition = new SimpleStringProperty(condition);
        this.action = new SimpleStringProperty(action);
        this.targetDevice = new SimpleStringProperty(targetDevice);
        this.enabled = new SimpleBooleanProperty(enabled);
        this.status = new SimpleStringProperty(status);
    }

    /**
     * Returns the rule identifier.
     *
     * @return rule identifier
     */
    public String getId() { return id.get(); }
    /**
     * Exposes the JavaFX id property.
     *
     * @return id property
     */
    public SimpleStringProperty idProperty() { return id; }

    /**
     * Returns the rule name.
     *
     * @return rule name
     */
    public String getName() { return name.get(); }
    /**
     * Updates the rule name.
     *
     * @param name new rule name
     */
    public void setName(String name) { this.name.set(name); }
    /**
     * Exposes the JavaFX name property.
     *
     * @return name property
     */
    public SimpleStringProperty nameProperty() { return name; }

    /**
     * Returns the trigger type.
     *
     * @return trigger type
     */
    public String getTriggerType() { return triggerType.get(); }
    /**
     * Updates the trigger type.
     *
     * @param trigger updated trigger type
     */
    public void setTriggerType(String trigger) { this.triggerType.set(trigger); }
    /**
     * Exposes the JavaFX trigger-type property.
     *
     * @return trigger-type property
     */
    public SimpleStringProperty triggerTypeProperty() { return triggerType; }

    /**
     * Returns the source device or source label.
     *
     * @return source device name
     */
    public String getSourceDevice() { return sourceDevice.get(); }
    /**
     * Updates the source device or source label.
     *
     * @param sourceDevice updated source device name
     */
    public void setSourceDevice(String sourceDevice) { this.sourceDevice.set(sourceDevice); }
    /**
     * Exposes the JavaFX source-device property.
     *
     * @return source-device property
     */
    public SimpleStringProperty sourceDeviceProperty() { return sourceDevice; }

    /**
     * Returns the rule condition expression.
     *
     * @return condition expression
     */
    public String getCondition() { return condition.get(); }
    /**
     * Updates the rule condition expression.
     *
     * @param condition updated condition expression
     */
    public void setCondition(String condition) { this.condition.set(condition); }
    /**
     * Exposes the JavaFX condition property.
     *
     * @return condition property
     */
    public SimpleStringProperty conditionProperty() { return condition; }

    /**
     * Returns the configured action.
     *
     * @return action text
     */
    public String getAction() { return action.get(); }
    /**
     * Updates the configured action.
     *
     * @param action updated action text
     */
    public void setAction(String action) { this.action.set(action); }
    /**
     * Exposes the JavaFX action property.
     *
     * @return action property
     */
    public SimpleStringProperty actionProperty() { return action; }

    /**
     * Returns the target device name.
     *
     * @return target device name
     */
    public String getTargetDevice() { return targetDevice.get(); }
    /**
     * Updates the target device name.
     *
     * @param device updated target device name
     */
    public void setTargetDevice(String device) { this.targetDevice.set(device); }
    /**
     * Exposes the JavaFX target-device property.
     *
     * @return target-device property
     */
    public SimpleStringProperty targetDeviceProperty() { return targetDevice; }

    /**
     * Returns whether the rule is enabled.
     *
     * @return true when enabled, otherwise false
     */
    public boolean isEnabled() { return enabled.get(); }
    /**
     * Updates whether the rule is enabled.
     *
     * @param enabled new enabled state
     */
    public void setEnabled(boolean enabled) { this.enabled.set(enabled); }
    /**
     * Exposes the JavaFX enabled property.
     *
     * @return enabled property
     */
    public SimpleBooleanProperty enabledProperty() { return enabled; }

    /**
     * Returns the status label.
     *
     * @return status label
     */
    public String getStatus() { return status.get(); }
    /**
     * Updates the status label.
     *
     * @param status updated status label
     */
    public void setStatus(String status) { this.status.set(status); }
    /**
     * Exposes the JavaFX status property.
     *
     * @return status property
     */
    public SimpleStringProperty statusProperty() { return status; }
}
