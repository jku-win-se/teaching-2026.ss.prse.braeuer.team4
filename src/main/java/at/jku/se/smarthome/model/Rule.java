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
    
    public String getId() { return id.get(); }
    public SimpleStringProperty idProperty() { return id; }
    
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public SimpleStringProperty nameProperty() { return name; }
    
    public String getTriggerType() { return triggerType.get(); }
    public void setTriggerType(String trigger) { this.triggerType.set(trigger); }
    public SimpleStringProperty triggerTypeProperty() { return triggerType; }

    public String getSourceDevice() { return sourceDevice.get(); }
    public void setSourceDevice(String sourceDevice) { this.sourceDevice.set(sourceDevice); }
    public SimpleStringProperty sourceDeviceProperty() { return sourceDevice; }
    
    public String getCondition() { return condition.get(); }
    public void setCondition(String condition) { this.condition.set(condition); }
    public SimpleStringProperty conditionProperty() { return condition; }
    
    public String getAction() { return action.get(); }
    public void setAction(String action) { this.action.set(action); }
    public SimpleStringProperty actionProperty() { return action; }
    
    public String getTargetDevice() { return targetDevice.get(); }
    public void setTargetDevice(String device) { this.targetDevice.set(device); }
    public SimpleStringProperty targetDeviceProperty() { return targetDevice; }
    
    public boolean isEnabled() { return enabled.get(); }
    public void setEnabled(boolean enabled) { this.enabled.set(enabled); }
    public SimpleBooleanProperty enabledProperty() { return enabled; }
    
    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
    public SimpleStringProperty statusProperty() { return status; }
}
