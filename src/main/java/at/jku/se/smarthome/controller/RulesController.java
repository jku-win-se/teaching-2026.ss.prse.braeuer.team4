package at.jku.se.smarthome.controller;

import java.util.Optional;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.model.Rule;
import at.jku.se.smarthome.service.MockRoomService;
import at.jku.se.smarthome.service.MockRuleService;
import at.jku.se.smarthome.service.MockUserService;
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
public class RulesController {
    
    @FXML
    private TableView<Rule> rulesTable;
    
    @FXML
    private TableColumn<Rule, String> nameColumn;
    
    @FXML
    private TableColumn<Rule, String> triggerColumn;

    @FXML
    private TableColumn<Rule, String> sourceColumn;
    
    @FXML
    private TableColumn<Rule, String> conditionColumn;
    
    @FXML
    private TableColumn<Rule, String> actionColumn;

    @FXML
    private TableColumn<Rule, String> targetColumn;
    
    @FXML
    private TableColumn<Rule, String> statusColumn;

    @FXML
    private Button addRuleBtn;
    
    private final MockRuleService ruleService = MockRuleService.getInstance();
    private final MockRoomService roomService = MockRoomService.getInstance();
    private final MockUserService userService = MockUserService.getInstance();
    
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
    
    @FXML
    private void handleAddRule() {
        if (!userService.canManageSystem()) {
            return;
        }
        Optional<RuleInput> result = showRuleDialog(null);
        result.ifPresent(input -> ruleService.addRule(
                input.name(),
                input.triggerType(),
                input.sourceDevice(),
                input.condition(),
                input.action(),
                input.targetDevice()
        ));
    }

    private void handleEditRule(Rule rule) {
        if (!userService.canManageSystem()) {
            return;
        }
        Optional<RuleInput> result = showRuleDialog(rule);
        result.ifPresent(input -> {
            ruleService.updateRule(
                    rule.getId(),
                    input.name(),
                    input.triggerType(),
                    input.sourceDevice(),
                    input.condition(),
                    input.action(),
                    input.targetDevice()
            );
            rulesTable.refresh();
        });
    }

    private void handleDeleteRule(Rule rule) {
        if (!userService.canManageSystem()) {
            return;
        }
        ruleService.deleteRule(rule.getId());
    }

    private void handleRunRule(Rule rule) {
        ruleService.executeRule(rule.getId());
    }

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

    private boolean isRuleTargetDevice(Device device) {
        return !"sensor".equalsIgnoreCase(device.getType());
    }

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

    private void updateSourceDeviceOptions(ComboBox<String> triggerCombo, ComboBox<String> sourceDeviceCombo, String preferredSource) {
        sourceDeviceCombo.getItems().clear();

        String triggerType = triggerCombo.getValue();
        if ("Time".equals(triggerType)) {
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

    private boolean isValidSourceDevice(String triggerType, Device device) {
        if ("Sensor Threshold".equals(triggerType)) {
            return "sensor".equalsIgnoreCase(device.getType());
        }
        if ("Device State".equals(triggerType)) {
            return true;
        }
        return false;
    }

    private void updateConditionPrompt(ComboBox<String> triggerCombo, TextField conditionField) {
        String triggerType = triggerCombo.getValue();
        if ("Time".equals(triggerType)) {
            conditionField.setPromptText("e.g. 22:00 or Weekdays 07:00");
        } else if ("Sensor Threshold".equals(triggerType)) {
            conditionField.setPromptText("e.g. Value > 28 or Motion = Active");
        } else {
            conditionField.setPromptText("e.g. State = ON");
        }
    }

    private Device getDeviceByName(String deviceName) {
        if (deviceName == null) {
            return null;
        }

        for (Room room : roomService.getRooms()) {
            for (Device device : room.getDevices()) {
                if (deviceName.equals(device.getName())) {
                    return device;
                }
            }
        }
        return null;
    }

    private record RuleInput(
            String name,
            String triggerType,
            String sourceDevice,
            String condition,
            String action,
            String targetDevice) {
    }

    private class RuleActionCell extends TableCell<Rule, Void> {
        private final Button runButton = new Button("Run");
        private final Button editButton = new Button("Edit");
        private final Button deleteButton = new Button("Delete");
        private final HBox container = new HBox(6);

        private RuleActionCell() {
            runButton.setOnAction(event -> handleRunRule(getTableView().getItems().get(getIndex())));
            editButton.setOnAction(event -> handleEditRule(getTableView().getItems().get(getIndex())));
            deleteButton.setOnAction(event -> handleDeleteRule(getTableView().getItems().get(getIndex())));
            deleteButton.setStyle("-fx-text-fill: #e74c3c;");
            container.setAlignment(Pos.CENTER_LEFT);
            container.getChildren().addAll(runButton, editButton, deleteButton);
        }

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
