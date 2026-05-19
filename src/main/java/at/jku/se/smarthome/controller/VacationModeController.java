package at.jku.se.smarthome.controller;

import java.time.LocalDate;
import java.time.LocalTime;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Schedule;
import at.jku.se.smarthome.model.VacationModeConfig;
import at.jku.se.smarthome.service.api.RoomService;
import at.jku.se.smarthome.service.api.ScheduleService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.api.UserService;
import at.jku.se.smarthome.service.mock.MockVacationModeService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Controller for the vacation mode view.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.UnusedPrivateMethod", "PMD.TooManyMethods", "unused"})
public class VacationModeController {

    
    /** Date picker for vacation start date. */
    @FXML
    private DatePicker startDatePicker;
    
    /** Date picker for vacation end date. */
    @FXML
    private DatePicker endDatePicker;
    
    /** Combo box for selecting vacation schedule. */
    @FXML
    private ComboBox<Schedule> scheduleCombo;

    /** Combo box for selecting vacation start hour. */
    @FXML
    private ComboBox<String> startTimeCombo;

    /** Combo box for selecting vacation end hour. */
    @FXML
    private ComboBox<String> endTimeCombo;
    
    /** VBox container displaying affected devices. */
    @FXML
    private VBox affectedDevicesBox;

    /** Label displaying current vacation status. */
    @FXML
    private Label currentStatusLabel;

    /** Label displaying selected schedule. */
    @FXML
    private Label selectedScheduleLabel;

    /** Label displaying override summary. */
    @FXML
    private Label overrideSummaryLabel;
    
    /** Button to activate vacation mode. */
    @FXML
    private Button activateBtn;
    
    /** Button to deactivate vacation mode. */
    @FXML
    private Button deactivateBtn;
    
    /** Schedule service instance. */
    private final ScheduleService scheduleService = ServiceRegistry.getScheduleService();
    /** Vacation mode service instance. */
    private final MockVacationModeService vacationModeService = MockVacationModeService.getInstance();
    /** Room service instance. */
    private final RoomService roomService = ServiceRegistry.getRoomService();
    /** User service instance. */
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
        startTimeCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateActionButtons());
        endTimeCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateActionButtons());

        initializeHourOptions();

        refreshFromConfiguration();
    }
    
    @FXML
    @SuppressWarnings("PMD.OnlyOneReturn")
    private void handleActivate() {
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

        LocalTime startTime = parseTime(startTimeCombo.getValue());
        LocalTime endTime = parseTime(endTimeCombo.getValue());
        if (startTime == null || endTime == null) {
            showValidationError("Select a valid start and end hour for vacation mode.");
            return;
        }

        if (endDate.equals(startDate) && endTime.isBefore(startTime)) {
            showValidationError("On the same day, the end hour must be after the start hour.");
            return;
        }
        
        vacationModeService.activateVacationMode(startDate, endDate, startTime, endTime, selectedSchedule, resolveActor());
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

        startDatePicker.setValue(config.getStartDate());
        endDatePicker.setValue(config.getEndDate());
        startTimeCombo.setValue(formatTime(config.getStartTime()));
        endTimeCombo.setValue(formatTime(config.getEndTime()));
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
        boolean canActivate = scheduleCombo.getValue() != null
                && startDatePicker.getValue() != null
                && endDatePicker.getValue() != null
                && startTimeCombo.getValue() != null
                && endTimeCombo.getValue() != null;
        activateBtn.setDisable(!canActivate);
        deactivateBtn.setDisable(!vacationModeService.isEnabled());
    }

    private void initializeHourOptions() {
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                String value = String.format("%02d:%02d", hour, minute);
                startTimeCombo.getItems().add(value);
                endTimeCombo.getItems().add(value);
            }
        }
    }

    private LocalTime parseTime(String value) {
        LocalTime result = null;
        if (value != null && !value.isBlank()) {
            result = LocalTime.parse(value);
        }
        return result;
    }

    private String formatTime(LocalTime value) {
        return value != null ? value.toString() : null;
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
