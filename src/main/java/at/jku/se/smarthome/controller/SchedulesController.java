package at.jku.se.smarthome.controller;

import java.util.Optional;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.model.Schedule;
import at.jku.se.smarthome.service.api.ScheduleService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.api.RoomService;
import at.jku.se.smarthome.service.mock.MockUserService;
import at.jku.se.smarthome.service.mock.MockVacationModeService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
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
 * Controller for the schedules view.
 */
public class SchedulesController {
    
    @FXML
    private TableView<Schedule> schedulesTable;
    
    @FXML
    private TableColumn<Schedule, String> nameColumn;
    
    @FXML
    private TableColumn<Schedule, String> deviceColumn;
    
    @FXML
    private TableColumn<Schedule, String> actionColumn;
    
    @FXML
    private TableColumn<Schedule, String> timeColumn;
    
    @FXML
    private TableColumn<Schedule, String> recurrenceColumn;

    @FXML
    private TableColumn<Schedule, Boolean> activeColumn;

    @FXML
    private Button addScheduleBtn;
    
    @FXML
    private Label conflictWarning;
    
    private final ScheduleService scheduleService = ServiceRegistry.getScheduleService();
    private final RoomService roomService = ServiceRegistry.getRoomService();
    private final MockUserService userService = MockUserService.getInstance();
    private final MockVacationModeService vacationModeService = MockVacationModeService.getInstance();
    
    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        deviceColumn.setCellValueFactory(new PropertyValueFactory<>("device"));
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        recurrenceColumn.setCellValueFactory(new PropertyValueFactory<>("recurrence"));
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        activeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "Yes" : "No");
                }
            }
        });

        TableColumn<Schedule, Void> actionsColumn = new TableColumn<>("Actions");
        actionsColumn.setPrefWidth(180);
        actionsColumn.setCellFactory(param -> new ScheduleActionCell());
        schedulesTable.getColumns().add(actionsColumn);

        schedulesTable.setItems(scheduleService.getSchedules());
        updateConflictWarning();

        if (!userService.canManageSystem()) {
            addScheduleBtn.setDisable(true);
        }
    }
    
    @FXML
    private void handleAddSchedule() {
        if (!userService.canManageSystem()) {
            return;
        }
        Optional<ScheduleInput> result = showScheduleDialog(null);
        result.ifPresent(input -> {
            scheduleService.addSchedule(
                    input.name(),
                    input.device().getId(),
                    input.device().getName(),
                    input.action(),
                    input.time(),
                    input.recurrence(),
                    input.active()
            );
            updateConflictWarning();
        });
    }

    private void handleEditSchedule(Schedule schedule) {
        if (!userService.canManageSystem()) {
            return;
        }
        Optional<ScheduleInput> result = showScheduleDialog(schedule);
        result.ifPresent(input -> {
            scheduleService.updateSchedule(
                    schedule.getId(),
                    input.name(),
                    input.device().getId(),
                    input.device().getName(),
                    input.action(),
                    input.time(),
                    input.recurrence(),
                    input.active()
            );
            schedulesTable.refresh();
            updateConflictWarning();
        });
    }

    private void handleDeleteSchedule(Schedule schedule) {
        if (!userService.canManageSystem()) {
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Schedule");
        confirmation.setHeaderText("Delete schedule: " + schedule.getName());
        confirmation.setContentText("This removes the recurring time-based action from the prototype.");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                scheduleService.deleteSchedule(schedule.getId());
                updateConflictWarning();
            }
        });
    }

    private Optional<ScheduleInput> showScheduleDialog(Schedule schedule) {
        Dialog<ScheduleInput> dialog = new Dialog<>();
        dialog.setTitle(schedule == null ? "Add Schedule" : "Edit Schedule");
        dialog.setHeaderText(schedule == null
                ? "Configure an unconditional time-based schedule"
                : "Update the recurring schedule details");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField nameField = new TextField();
        nameField.setPromptText("Weekday Morning Lamp");

        ComboBox<Device> deviceCombo = new ComboBox<>();
        deviceCombo.setPrefWidth(220);
        for (Room room : roomService.getRooms()) {
            for (Device device : room.getDevices()) {
                if (isSchedulableDevice(device)) {
                    deviceCombo.getItems().add(device);
                }
            }
        }
        deviceCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Device device) {
                return device != null ? device.getName() : "";
            }

            @Override
            public Device fromString(String string) {
                return getDeviceByName(string);
            }
        });

        ComboBox<String> actionCombo = new ComboBox<>();
        actionCombo.setPrefWidth(220);
        actionCombo.setPromptText("Select a valid action");

        TextField timeField = new TextField();
        timeField.setPromptText("07:00 AM or Mon 07:00 AM");

        ComboBox<String> recurrenceCombo = new ComboBox<>();
        recurrenceCombo.getItems().addAll("Daily", "Weekdays", "Weekends", "Weekly");
        recurrenceCombo.setPrefWidth(160);

        CheckBox activeCheckBox = new CheckBox("Active schedule");

        if (schedule != null) {
            nameField.setText(schedule.getName());
            deviceCombo.setValue(getDeviceById(schedule.getDeviceId()));
            updateActionOptions(deviceCombo, actionCombo, schedule.getAction());
            timeField.setText(schedule.getTime());
            recurrenceCombo.setValue(schedule.getRecurrence());
            activeCheckBox.setSelected(schedule.isActive());
        } else {
            if (!deviceCombo.getItems().isEmpty()) {
                deviceCombo.setValue(deviceCombo.getItems().get(0));
                updateActionOptions(deviceCombo, actionCombo, null);
            }
            recurrenceCombo.setValue("Daily");
            activeCheckBox.setSelected(true);
        }

        deviceCombo.setOnAction(event -> updateActionOptions(deviceCombo, actionCombo, null));

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Device:"), 0, 1);
        grid.add(deviceCombo, 1, 1);
        grid.add(new Label("Action:"), 0, 2);
        grid.add(actionCombo, 1, 2);
        grid.add(new Label("Time Pattern:"), 0, 3);
        grid.add(timeField, 1, 3);
        grid.add(new Label("Recurrence:"), 0, 4);
        grid.add(recurrenceCombo, 1, 4);
        grid.add(activeCheckBox, 1, 5);

        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.disableProperty().bind(
                nameField.textProperty().isEmpty()
                .or(actionCombo.valueProperty().isNull())
                        .or(timeField.textProperty().isEmpty())
                        .or(deviceCombo.valueProperty().isNull())
                        .or(recurrenceCombo.valueProperty().isNull())
        );

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                return new ScheduleInput(
                        nameField.getText().trim(),
                        deviceCombo.getValue(),
                        actionCombo.getValue(),
                        timeField.getText().trim(),
                        recurrenceCombo.getValue(),
                        activeCheckBox.isSelected()
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private boolean isSchedulableDevice(Device device) {
        return !"sensor".equalsIgnoreCase(device.getType());
    }

    private void updateActionOptions(ComboBox<Device> deviceCombo, ComboBox<String> actionCombo, String preferredAction) {
        actionCombo.getItems().clear();

        Device selectedDevice = deviceCombo.getValue();
        if (selectedDevice == null) {
            return;
        }

        switch (selectedDevice.getType().toLowerCase()) {
            case "switch" -> actionCombo.getItems().addAll("Turn On", "Turn Off");
            case "dimmer" -> actionCombo.getItems().addAll(
                    "Turn On",
                    "Turn Off",
                    "Set to 25%",
                    "Set to 50%",
                    "Set to 75%",
                    "Set to 100%"
            );
            case "thermostat" -> actionCombo.getItems().addAll(
                    "Set to 18°C",
                    "Set to 20°C",
                    "Set to 22°C",
                    "Set to 24°C"
            );
            case "cover/blind" -> actionCombo.getItems().addAll("Open", "Close");
            default -> {
            }
        }

        if (preferredAction != null && actionCombo.getItems().contains(preferredAction)) {
            actionCombo.setValue(preferredAction);
        } else if (!actionCombo.getItems().isEmpty()) {
            actionCombo.setValue(actionCombo.getItems().get(0));
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

    private Device getDeviceById(String deviceId) {
        return deviceId == null ? null : roomService.getDeviceById(deviceId);
    }

    private void updateConflictWarning() {
        if (!vacationModeService.isEnabled()) {
            conflictWarning.setText("");
            return;
        }

        conflictWarning.setStyle("-fx-text-fill: #3498db; -fx-font-size: 12; -fx-font-weight: bold;");
        conflictWarning.setText(vacationModeService.getOverrideSummary());
    }

    private record ScheduleInput(
            String name,
            Device device,
            String action,
            String time,
            String recurrence,
            boolean active) {
    }

    private class ScheduleActionCell extends TableCell<Schedule, Void> {
        private final Button editButton = new Button("Edit");
        private final Button deleteButton = new Button("Delete");
        private final HBox container = new HBox(8);

        private ScheduleActionCell() {
            editButton.setOnAction(event -> handleEditSchedule(getTableView().getItems().get(getIndex())));
            deleteButton.setOnAction(event -> handleDeleteSchedule(getTableView().getItems().get(getIndex())));
            deleteButton.setStyle("-fx-text-fill: #e74c3c;");
            container.setAlignment(Pos.CENTER_LEFT);
            container.getChildren().addAll(editButton, deleteButton);
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
