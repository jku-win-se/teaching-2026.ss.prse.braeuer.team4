package at.jku.se.smarthome.controller;

import java.time.LocalDate;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Schedule;
import at.jku.se.smarthome.model.VacationModeConfig;
import at.jku.se.smarthome.service.api.ScheduleService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.api.RoomService;
import at.jku.se.smarthome.service.api.UserService;
import at.jku.se.smarthome.service.mock.MockVacationModeService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Controller for the vacation mode view.
 */
public class VacationModeController {
    
    @FXML
    private ToggleButton vacationToggle;
    
    @FXML
    private DatePicker startDatePicker;
    
    @FXML
    private DatePicker endDatePicker;
    
    @FXML
    private ComboBox<Schedule> scheduleCombo;
    
    @FXML
    private VBox affectedDevicesBox;

    @FXML
    private Label currentStatusLabel;

    @FXML
    private Label selectedScheduleLabel;

    @FXML
    private Label overrideSummaryLabel;
    
    @FXML
    private Button activateBtn;
    
    @FXML
    private Button deactivateBtn;
    
    private final ScheduleService scheduleService = ServiceRegistry.getScheduleService();
    private final MockVacationModeService vacationModeService = MockVacationModeService.getInstance();
    private final RoomService roomService = ServiceRegistry.getRoomService();
    private final UserService userService = ServiceRegistry.getUserService();
    
    @FXML
    private void initialize() {
        scheduleCombo.setItems(scheduleService.getSchedules());
        scheduleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Schedule schedule) {
                return schedule != null ? schedule.getName() : "";
            }

            @Override
            public Schedule fromString(String string) {
                return scheduleService.getScheduleByName(string);
            }
        });

        scheduleCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshDerivedState());
        startDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> updateActionButtons());
        endDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> updateActionButtons());

        refreshFromConfiguration();
    }
    
    @FXML
    private void handleToggleVacation() {
        vacationToggle.setText(vacationToggle.isSelected() ? "ON" : "OFF");
        updateActionButtons();
    }
    
    @FXML
    private void handleActivate() {
        if (!vacationToggle.isSelected()) {
            showValidationError("Enable the vacation mode toggle before applying the override.");
            return;
        }

        Schedule selectedSchedule = scheduleCombo.getValue();
        if (selectedSchedule == null) {
            showValidationError("Select the named schedule that should be applied during vacation mode.");
            return;
        }

        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        if (startDate == null || endDate == null) {
            showValidationError("Select a valid start and end date for vacation mode.");
            return;
        }
        if (endDate.isBefore(startDate)) {
            showValidationError("The end date must be on or after the start date.");
            return;
        }

        vacationModeService.activateVacationMode(startDate, endDate, selectedSchedule, resolveActor());
        refreshFromConfiguration();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Vacation Mode");
        alert.setHeaderText("Vacation Mode Activated");
        alert.setContentText(vacationModeService.getStatusSummary());
        alert.showAndWait();
    }
    
    @FXML
    private void handleDeactivate() {
        vacationModeService.deactivateVacationMode(resolveActor());
        refreshFromConfiguration();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Vacation Mode");
        alert.setHeaderText("Vacation Mode Deactivated");
        alert.setContentText("Normal schedules are now active.");
        alert.showAndWait();
    }

    private void refreshFromConfiguration() {
        VacationModeConfig config = vacationModeService.getConfiguration();
        Schedule selectedSchedule = vacationModeService.getSelectedSchedule();

        vacationToggle.setSelected(vacationModeService.isEnabled());
        vacationToggle.setText(vacationToggle.isSelected() ? "ON" : "OFF");
        startDatePicker.setValue(config.getStartDate());
        endDatePicker.setValue(config.getEndDate());
        scheduleCombo.setValue(selectedSchedule);

        refreshDerivedState();
        updateActionButtons();
    }

    private void refreshDerivedState() {
        Schedule selectedSchedule = scheduleCombo.getValue();
        currentStatusLabel.setText(vacationModeService.getStatusSummary());
        overrideSummaryLabel.setText(vacationModeService.getOverrideSummary());

        if (selectedSchedule == null) {
            selectedScheduleLabel.setText("No schedule selected yet.");
        } else {
            selectedScheduleLabel.setText(String.format(
                    "%s -> %s at %s (%s)",
                    selectedSchedule.getDevice(),
                    selectedSchedule.getAction(),
                    selectedSchedule.getTime(),
                    selectedSchedule.getRecurrence()
            ));
        }

        rebuildAffectedDevices();
        updateActionButtons();
    }

    private void rebuildAffectedDevices() {
        affectedDevicesBox.getChildren().clear();

        Schedule selectedSchedule = scheduleCombo.getValue();
        if (selectedSchedule == null) {
            affectedDevicesBox.getChildren().add(createInfoLabel("No schedule selected"));
            return;
        }

        Device device = roomService.getDeviceByName(selectedSchedule.getDevice());
        affectedDevicesBox.getChildren().add(createInfoLabel("Device: " + selectedSchedule.getDevice()));
        affectedDevicesBox.getChildren().add(createInfoLabel("Action: " + selectedSchedule.getAction()));
        affectedDevicesBox.getChildren().add(createInfoLabel("Time pattern: " + selectedSchedule.getTime()));
        affectedDevicesBox.getChildren().add(createInfoLabel("Recurrence: " + selectedSchedule.getRecurrence()));
        affectedDevicesBox.getChildren().add(createInfoLabel("Room: " + (device != null ? device.getRoom() : "Unknown")));

        int overriddenSchedules = vacationModeService.getOverriddenSchedules().size();
        affectedDevicesBox.getChildren().add(createInfoLabel(
                "Overrides other schedules in range: " + overriddenSchedules
        ));
    }

    private Label createInfoLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #34495e; -fx-font-size: 12;");
        return label;
    }

    private void updateActionButtons() {
        boolean canActivate = vacationToggle.isSelected()
                && scheduleCombo.getValue() != null
                && startDatePicker.getValue() != null
                && endDatePicker.getValue() != null;
        activateBtn.setDisable(!canActivate);
        deactivateBtn.setDisable(!vacationModeService.isEnabled());
    }

    private String resolveActor() {
        String email = userService.getCurrentUserEmail();
        return email != null ? email : "Owner";
    }

    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Vacation Mode");
        alert.setHeaderText("Invalid vacation mode configuration");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
