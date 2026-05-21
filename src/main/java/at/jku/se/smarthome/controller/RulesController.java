package at.jku.se.smarthome.controller;

import java.util.Optional;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.model.Rule;
import at.jku.se.smarthome.service.api.RoomService;
import at.jku.se.smarthome.service.api.RuleService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.api.UserService;
import at.jku.se.smarthome.service.rule.RuleValidator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

/**
 * Controller for the rules view.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.UnusedPrivateMethod", "PMD.TooManyMethods", "PMD.GodClass", "PMD.CyclomaticComplexity", "PMD.CouplingBetweenObjects"})
public class RulesController { // NOPMD - High coupling is inherent in this central controller

    /** Trigger type that does not require a source device. */
    private static final String TRIGGER_TIME = "Time";
    /** Trigger type based on sensor threshold condition. */
    private static final String TRIGGER_SENSOR_THRESHOLD = "Sensor Threshold";
    /** Trigger type based on device state changes. */
    private static final String TRIGGER_DEVICE_STATE = "Device State";
    /** Device type literal for sensor devices. */
    private static final String DEVICE_TYPE_SENSOR = "sensor";
    
    /** Table view for displaying all rules. */
    @FXML
    private TableView<Rule> rulesTable;
    
    /** Column displaying rule name. */
    @FXML
    private TableColumn<Rule, String> nameColumn;
    
    /** Column displaying trigger type. */
    @FXML
    private TableColumn<Rule, String> triggerColumn;

    /** Column displaying source device. */
    @FXML
    private TableColumn<Rule, String> sourceColumn;
    
    /** Column displaying condition. */
    @FXML
    private TableColumn<Rule, String> conditionColumn;
    
    /** Column displaying action. */
    @FXML
    private TableColumn<Rule, String> actionColumn;

    /** Column displaying target device. */
    @FXML
    private TableColumn<Rule, String> targetColumn;
    
    /** Column displaying rule status. */
    @FXML
    private TableColumn<Rule, String> statusColumn;

    /** Button to add new rule. */
    @FXML
    private Button addRuleBtn;
    
    /** Rule service for rule operations. */
    private final RuleService ruleService = ServiceRegistry.getRuleService();
    /** Room service for room data access. */
    private final RoomService roomService = ServiceRegistry.getRoomService();
    /** User service for authorization checks. */
    private final UserService userService = ServiceRegistry.getUserService();
    
    /**
     * Initializes the rules table with columns and loads rules from service.
     */
    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        triggerColumn.setCellValueFactory(new PropertyValueFactory<>("triggerType"));
        sourceColumn.setCellValueFactory(new PropertyValueFactory<>("sourceDevice"));
        conditionColumn.setCellValueFactory(new PropertyValueFactory<>("condition"));
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        targetColumn.setCellValueFactory(new PropertyValueFactory<>("targetDevice"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<Rule, Void> actionsColumn = new TableColumn<>("Actions");
        actionsColumn.setPrefWidth(220);
        actionsColumn.setCellFactory(param -> new RuleActionCell());
        rulesTable.getColumns().add(actionsColumn);

        rulesTable.setItems(ruleService.getRules());

        if (!userService.canManageSystem()) {
            addRuleBtn.setDisable(true);
        }
    }
    
    /**
     * Handles add rule button click, showing dialog to create new rule.
     */
    @FXML
    private void handleAddRule() {
        if (!userService.canManageSystem()) {
            return;
        }
        Optional<RuleInput> result = showRuleDialog(null);
        result.ifPresent(input -> {
            Rule created = ruleService.addRule(
                    input.name(),
                    input.triggerType(),
                    input.sourceDevice(),
                    input.condition(),
                    input.action(),
                    input.targetDevice()
            );
            if (created == null) {
                showRuleValidationError(input.triggerType(), input.condition(), input.sourceDevice());
            } else {
                // Check for conflicts and block save if conflicts exist
                if (ruleService.hasConflicts(created.getId())) {
                    showRuleConflictModal(created, true);
                }
            }
        });
    }

    /**
     * Handles edit rule action, showing dialog to modify existing rule.
     *
     * @param rule rule to edit
     */
    private void handleEditRule(Rule rule) {
        if (!userService.canManageSystem()) {
            return;
        }
        Optional<RuleInput> result = showRuleDialog(rule);
        result.ifPresent(input -> {
            // store previous state to allow revert if conflicts are found
            String prevName = rule.getName();
            String prevTrigger = rule.getTriggerType();
            String prevSource = rule.getSourceDevice();
            String prevCondition = rule.getCondition();
            String prevAction = rule.getAction();
            String prevTarget = rule.getTargetDevice();

            boolean success = ruleService.updateRule(
                    rule.getId(),
                    input.name(),
                    input.triggerType(),
                    input.sourceDevice(),
                    input.condition(),
                    input.action(),
                    input.targetDevice()
            );
            if (success) {
                // After updating, check for conflicts. If present, show modal and revert on cancel.
                if (ruleService.hasConflicts(rule.getId())) {
                    showRuleConflictModal(rule, false, () -> {
                        // revert to previous values
                        ruleService.updateRule(rule.getId(), prevName, prevTrigger, prevSource, prevCondition, prevAction, prevTarget);
                        rulesTable.refresh();
                    });
                } else {
                    rulesTable.refresh();
                }
            } else {
                showRuleValidationError(input.triggerType(), input.condition(), input.sourceDevice());
            }
        });
    }

    /**
     * Handles delete rule action, removing rule from service.
     *
     * @param rule rule to delete
     */
    private void handleDeleteRule(Rule rule) {
        if (!userService.canManageSystem()) {
            return;
        }
        ruleService.deleteRule(rule.getId());
    }

    /**
     * Handles run rule action, executing rule immediately without checking condition.
     *
     * @param rule rule to execute
     */
    private void handleRunRule(Rule rule) {
        ruleService.executeRule(rule.getId(), true);
    }

    private void showRuleValidationError(String triggerType, String condition,
                                         String sourceDevice) {
        RuleValidator.Result result =
                RuleValidator.validate(triggerType, condition, sourceDevice, roomService);
        String detail = result.reason() != null ? result.reason() : "Invalid rule configuration.";
        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Rule not saved");
        alert.setHeaderText("The rule could not be created");
        alert.setContentText(detail);
        alert.showAndWait();
    }

    /**
     * Shows dialog for adding or editing a rule.
     *
     * @param existingRule existing rule to edit, or null for new rule
     * @return optional containing rule input if saved, empty if cancelled
     */
    @SuppressWarnings("PMD.NcssCount")
    private Optional<RuleInput> showRuleDialog(Rule existingRule) {
        Dialog<RuleInput> dialog = new Dialog<>();
        dialog.setTitle(existingRule == null ? "Add Rule" : "Edit Rule");
        dialog.setHeaderText("Configure a rule");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField nameField = new TextField();
        nameField.setPromptText("Evening Hallway Light");

        ComboBox<String> triggerCombo = new ComboBox<>();
        triggerCombo.getItems().addAll("Time", "Sensor Threshold", "Device State");
        triggerCombo.setPrefWidth(220);

        ComboBox<String> sourceDeviceCombo = new ComboBox<>();
        sourceDeviceCombo.setPrefWidth(220);
        sourceDeviceCombo.setPromptText("Select source device");

        TextField conditionField = new TextField();
        conditionField.setPromptText("e.g. 22:00, Value > 28, State = ON");

        ComboBox<String> targetDeviceCombo = new ComboBox<>();
        targetDeviceCombo.setPrefWidth(220);
        for (Room room : roomService.getRooms()) {
            for (Device device : room.getDevices()) {
                if (isRuleTargetDevice(device)) {
                    targetDeviceCombo.getItems().add(device.getName());
                }
            }
        }

        ComboBox<String> actionCombo = new ComboBox<>();
        actionCombo.setPrefWidth(220);
        actionCombo.setPromptText("Select action");

        if (existingRule != null) {
            nameField.setText(existingRule.getName());
            triggerCombo.setValue(existingRule.getTriggerType());
            conditionField.setText(existingRule.getCondition());
            sourceDeviceCombo.setValue(existingRule.getSourceDevice());
            targetDeviceCombo.setValue(existingRule.getTargetDevice());
        } else {
            triggerCombo.setValue("Time");
            if (!targetDeviceCombo.getItems().isEmpty()) {
                targetDeviceCombo.setValue(targetDeviceCombo.getItems().get(0));
            }
        }

        updateSourceDeviceOptions(triggerCombo, sourceDeviceCombo, existingRule == null ? null : existingRule.getSourceDevice());
        updateConditionPrompt(triggerCombo, conditionField);
        updateActionOptions(targetDeviceCombo, actionCombo, existingRule == null ? null : existingRule.getAction());

        triggerCombo.setOnAction(event -> {
            updateSourceDeviceOptions(triggerCombo, sourceDeviceCombo, null);
            updateConditionPrompt(triggerCombo, conditionField);
        });
        targetDeviceCombo.setOnAction(event -> updateActionOptions(targetDeviceCombo, actionCombo, null));

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Trigger:"), 0, 1);
        grid.add(triggerCombo, 1, 1);
        grid.add(new Label("Source Device:"), 0, 2);
        grid.add(sourceDeviceCombo, 1, 2);
        grid.add(new Label("Condition:"), 0, 3);
        grid.add(conditionField, 1, 3);
        grid.add(new Label("Target Device:"), 0, 4);
        grid.add(targetDeviceCombo, 1, 4);
        grid.add(new Label("Action:"), 0, 5);
        grid.add(actionCombo, 1, 4);

        grid.getChildren().remove(actionCombo);
        grid.add(actionCombo, 1, 5);

        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.disableProperty().bind(
                nameField.textProperty().isEmpty()
                        .or(conditionField.textProperty().isEmpty())
                        .or(triggerCombo.valueProperty().isNull())
                        .or(targetDeviceCombo.valueProperty().isNull())
                        .or(actionCombo.valueProperty().isNull())
                        .or(sourceDeviceCombo.disableProperty().not().and(sourceDeviceCombo.valueProperty().isNull()))
        );

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                return new RuleInput(
                        nameField.getText().trim(),
                        triggerCombo.getValue(),
                    sourceDeviceCombo.isDisabled() ? "Clock" : sourceDeviceCombo.getValue(),
                        conditionField.getText().trim(),
                        actionCombo.getValue(),
                    targetDeviceCombo.getValue()
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    /**
     * Helper: checks if device can be a rule target (non-sensor).
     *
     * @param device device to check
     * @return true if device is a valid rule target
     */
    private boolean isRuleTargetDevice(Device device) {
        return !"sensor".equalsIgnoreCase(device.getType());
    }

    /**
     * Helper: updates action options based on selected target device.
     *
     * @param deviceCombo combo box with device selection
     * @param actionCombo combo box to populate with actions
     * @param preferredAction action to select if available
     */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    private void updateActionOptions(ComboBox<String> deviceCombo, ComboBox<String> actionCombo, String preferredAction) {
        actionCombo.getItems().clear();

        Device selectedDevice = getDeviceByName(deviceCombo.getValue());
        if (selectedDevice == null) {
            return;
        }

        switch (selectedDevice.getType().toLowerCase()) {
            case "switch":
                actionCombo.getItems().addAll("Turn On", "Turn Off");
                break;
            case "dimmer":
                actionCombo.getItems().addAll("Turn On", "Turn Off", "Set to 25%", "Set to 50%", "Set to 75%", "Set to 100%");
                break;
            case "thermostat":
                actionCombo.getItems().addAll("Set to 18°C", "Set to 20°C", "Set to 22°C", "Set to 24°C", "Notify User");
                break;
            case "sensor":
                actionCombo.getItems().addAll("Notify User", "Trigger Alert");
                break;
            case "cover/blind":
                actionCombo.getItems().addAll("Open", "Close");
                break;
            default:
                break;
        }

        if (preferredAction != null && actionCombo.getItems().contains(preferredAction)) {
            actionCombo.setValue(preferredAction);
        } else if (!actionCombo.getItems().isEmpty()) {
            actionCombo.setValue(actionCombo.getItems().get(0));
        }
    }

    /**
     * Helper: updates source device options based on trigger type.
     *
     * @param triggerCombo combo box with trigger type selection
     * @param sourceDeviceCombo combo box to populate with source devices
     * @param preferredSource source device to select if available
     */
    private void updateSourceDeviceOptions(ComboBox<String> triggerCombo, ComboBox<String> sourceDeviceCombo, String preferredSource) {
        sourceDeviceCombo.getItems().clear();

        String triggerType = triggerCombo.getValue();
        if (TRIGGER_TIME.equals(triggerType)) {
            sourceDeviceCombo.setDisable(true);
            sourceDeviceCombo.setPromptText("No source device required");
            sourceDeviceCombo.setValue(null);
            return;
        }

        sourceDeviceCombo.setDisable(false);
        for (Room room : roomService.getRooms()) {
            for (Device device : room.getDevices()) {
                if (isValidSourceDevice(triggerType, device)) {
                    sourceDeviceCombo.getItems().add(device.getName());
                }
            }
        }

        if (preferredSource != null && sourceDeviceCombo.getItems().contains(preferredSource)) {
            sourceDeviceCombo.setValue(preferredSource);
        } else if (!sourceDeviceCombo.getItems().isEmpty()) {
            sourceDeviceCombo.setValue(sourceDeviceCombo.getItems().get(0));
        }
    }

    /**
     * Helper: checks if device is valid source for given trigger type.
     *
     * @param triggerType trigger type to check
     * @param device device to validate
     * @return true if device is valid source for trigger type
     */
    private boolean isValidSourceDevice(String triggerType, Device device) {
        boolean valid = false;
        if (TRIGGER_SENSOR_THRESHOLD.equals(triggerType)) {
            valid = DEVICE_TYPE_SENSOR.equalsIgnoreCase(device.getType());
        } else if (TRIGGER_DEVICE_STATE.equals(triggerType)) {
            valid = true;
        }
        return valid;
    }

    /**
     * Helper: updates condition prompt text based on trigger type.
     *
     * @param triggerCombo combo box with trigger type selection
     * @param conditionField text field to update with prompt
     */
    private void updateConditionPrompt(ComboBox<String> triggerCombo, TextField conditionField) {
        String triggerType = triggerCombo.getValue();
        if (TRIGGER_TIME.equals(triggerType)) {
            conditionField.setPromptText("e.g. 22:00 or Weekdays 07:00");
        } else if (TRIGGER_SENSOR_THRESHOLD.equals(triggerType)) {
            conditionField.setPromptText("e.g. Value > 28 or Motion = Active");
        } else {
            conditionField.setPromptText("e.g. State = ON");
        }
    }

    /**
     * Helper: finds device by name across all rooms.
     *
     * @param deviceName name of device to find
     * @return device with matching name, or null if not found
     */
    private Device getDeviceByName(String deviceName) {
        Device result = null;
        if (deviceName != null) {
            for (Room room : roomService.getRooms()) {
                for (Device device : room.getDevices()) {
                    if (deviceName.equals(device.getName())) {
                        result = device;
                        break;
                    }
                }
                if (result != null) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Record for holding rule form input data.
     *
     * @param name rule name
     * @param triggerType type of trigger (Time, Sensor Threshold, Device State)
     * @param sourceDevice source device for trigger
     * @param condition condition for trigger
     * @param action action to perform
     * @param targetDevice target device for action
     */
    private record RuleInput(
            String name,
            String triggerType,
            String sourceDevice,
            String condition,
            String action,
            String targetDevice) {
    }

    /**
     * Shows a simple cancel-only modal when rule conflicts are detected.
     * @param candidate newly created or updated rule
     * @param wasCreated true when this was a newly created rule (delete on cancel)
     */
    private void showRuleConflictModal(Rule candidate, boolean wasCreated) {
        showRuleConflictModal(candidate, wasCreated, null);
    }

    /**
     * Shows a cancel-only modal when rule conflicts are detected. Optionally runs
     * a revert action when the user cancels (used for edits).
     */
    private void showRuleConflictModal(Rule candidate, boolean wasCreated, Runnable onCancelRevert) {
        // Build list of conflicting rules (same target, same trigger/condition, incompatible actions)
        var conflicts = ruleService.getRules().stream()
                .filter(r -> !r.getId().equals(candidate.getId()))
                .filter(Rule::isEnabled)
                .filter(r -> candidate.getTargetDevice() != null && candidate.getTargetDevice().equals(r.getTargetDevice()))
                .filter(r -> normalizeTrigger(r.getTriggerType()).equals(normalizeTrigger(candidate.getTriggerType())))
                .filter(r -> normalizeValue(r.getCondition()).equals(normalizeValue(candidate.getCondition())))
                .filter(r -> valuesAreIncompatible(normalizeValue(r.getAction()), normalizeValue(candidate.getAction())))
                .toList();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Conflicting Rule Detected");
        dialog.setHeaderText("The rule '" + candidate.getName() + "' is conflicting with 1 or more existing rule(s)");

        StringBuilder details = new StringBuilder(Math.max(64, conflicts.size() * 40));
        for (Rule r : conflicts) {
            details.append("• ").append(r.getName()).append(" (Action: ").append(r.getAction()).append(")\n");
        }
        if (details.length() == 0) {
            details.append("A conflict was detected.");
        }

        Label content = new Label(details.toString());
        content.setWrapText(true);
        GridPane pane = new GridPane();
        pane.setPadding(new Insets(10));
        pane.add(content, 0, 0);
        dialog.getDialogPane().setContent(pane);

        // Only allow cancel — user must fix or abandon the change
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(cancelButton);

        var res = dialog.showAndWait();
        if (res.isEmpty() || res.get() == cancelButton) {
            if (wasCreated) {
                ruleService.deleteRule(candidate.getId());
                rulesTable.refresh();
            }
            if (onCancelRevert != null) {
                onCancelRevert.run();
            }
        }
    }

    // small helpers reused for conflict detection in controller
    private String normalizeTrigger(String trigger) {
        return trigger == null ? "" : trigger.trim().toLowerCase();
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private boolean valuesAreIncompatible(String valueA, String valueB) {
        boolean incompatible = false;
        if (valueA != null && valueB != null && !valueA.equals(valueB)) {
            if (isSwitchValue(valueA) && isSwitchValue(valueB)) {
                incompatible = true;
            } else if (isCoverValue(valueA) && isCoverValue(valueB)) {
                incompatible = true;
            } else if (isDimmerValue(valueA) && isDimmerValue(valueB)) {
                incompatible = true;
            } else if (isThermostatValue(valueA) && isThermostatValue(valueB)) {
                try {
                    double temp1 = extractTemperatureValue(valueA);
                    double temp2 = extractTemperatureValue(valueB);
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

    /**
     * Custom table cell for displaying rule action buttons.
     */
    private final class RuleActionCell extends TableCell<Rule, Void> {
        /** Button to run rule. */
        private final Button runButton = new Button("Run");
        /** Button to edit rule. */
        private final Button editButton = new Button("Edit");
        /** Button to delete rule. */
        private final Button deleteButton = new Button("Delete");
        /** Container for action buttons. */
        private final HBox container = new HBox(6);

        /**
         * Constructs action cell with run, edit, and delete buttons.
         */
        private RuleActionCell() {
            super();
            runButton.setOnAction(event -> handleRunRule(getTableView().getItems().get(getIndex())));
            editButton.setOnAction(event -> handleEditRule(getTableView().getItems().get(getIndex())));
            deleteButton.setOnAction(event -> handleDeleteRule(getTableView().getItems().get(getIndex())));
            deleteButton.setStyle("-fx-text-fill: #e74c3c;");
            container.setAlignment(Pos.CENTER_LEFT);
            container.getChildren().addAll(runButton, editButton, deleteButton);
        }

        /**
         * Updates cell to display action buttons or nothing if row is empty.
         *
         * @param item item value (not used for action cell)
         * @param empty true if row is empty
         */
        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                boolean canManage = userService.canManageSystem();
                editButton.setDisable(!canManage);
                deleteButton.setDisable(!canManage);
                setGraphic(container);
            }
        }
    }
}
