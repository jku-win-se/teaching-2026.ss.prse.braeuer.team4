package at.jku.se.smarthome.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.api.UserService;
import at.jku.se.smarthome.service.mock.MockEnergyService;
import at.jku.se.smarthome.service.mock.MockEnergyService.AggregationPeriod;
import at.jku.se.smarthome.service.mock.MockEnergyService.EnergySnapshot;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.stage.FileChooser;

/**
 * Controller for the energy dashboard view.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.UnusedPrivateMethod"})
public class EnergyController {

    /** Label displaying total energy usage. */
    @FXML
    @SuppressWarnings("unused")
    private Label totalUsageLabel;

    /** Label displaying top energy consumer device. */
    @FXML
    @SuppressWarnings("unused")
    private Label topDeviceLabel;

    /** Label displaying top energy consuming room. */
    @FXML
    @SuppressWarnings("unused")
    private Label topRoomLabel;

    /** Label displaying timeline title. */
    @FXML
    @SuppressWarnings("unused")
    private Label timelineTitleLabel;

    /** Label displaying aggregation period hint. */
    @FXML
    @SuppressWarnings("unused")
    private Label aggregationHintLabel;

    /** Pie chart displaying energy usage by room. */
    @FXML
    @SuppressWarnings("unused")
    private PieChart roomChart;

    /** Bar chart displaying energy usage by device. */
    @FXML
    @SuppressWarnings("unused")
    private BarChart<String, Number> deviceChart;

    /** Line chart displaying energy usage over time. */
    @FXML
    @SuppressWarnings("unused")
    private LineChart<String, Number> timelineChart;

    /** Toggle button for daily aggregation. */
    @FXML
    @SuppressWarnings("unused")
    private ToggleButton dayToggle;

    /** Toggle button for weekly aggregation. */
    @FXML
    @SuppressWarnings("unused")
    private ToggleButton weekToggle;

    /** Export CSV button — hidden for Members, visible for Owners only (FR-16). */
    @FXML
    @SuppressWarnings("unused")
    private Button exportBtn;

    /** Energy service for consumption data. */
    private final MockEnergyService energyService = MockEnergyService.getInstance();

    /** User service for role-based visibility checks. */
    private final UserService userService = ServiceRegistry.getUserService();

    /** Current aggregation period for data display. */
    private AggregationPeriod currentPeriod = AggregationPeriod.DAY;

    @FXML
    @SuppressWarnings("unused")
    private void initialize() {
        roomChart.setAnimated(false);
        deviceChart.setAnimated(false);
        timelineChart.setAnimated(false);
        refreshDashboard();
        boolean owner = userService.isOwner();
        exportBtn.setVisible(owner);
        exportBtn.setManaged(owner);
    }

    private void refreshDashboard() {
        EnergySnapshot snapshot = energyService.getSnapshot(currentPeriod);
        loadSummaryData(snapshot);
        loadRoomChart(snapshot);
        loadDeviceChart(snapshot);
        loadTimelineChart(snapshot);
        updateToggleState();
    }

    private void loadSummaryData(final EnergySnapshot snapshot) {
        totalUsageLabel.setText(String.format("%.1f kWh", snapshot.householdTotal()));
        topDeviceLabel.setText(String.format("%s (%.1f kWh)", snapshot.topDeviceName(), snapshot.topDeviceConsumption()));
        topRoomLabel.setText(String.format("%s (%.1f kWh)", snapshot.topRoomName(), snapshot.topRoomConsumption()));
        aggregationHintLabel.setText("Aggregation: " + snapshot.period().getDisplayName());
    }

    private void loadRoomChart(final EnergySnapshot snapshot) {
        ObservableList<PieChart.Data> roomData = FXCollections.observableArrayList();
        snapshot.consumptionByRoom().forEach((room, value) ->
            roomData.add(new PieChart.Data(room, value))
        );
        roomChart.setData(roomData);
        roomChart.setTitle(snapshot.period().getDisplayName() + " Aggregation");
    }

    private void loadDeviceChart(final EnergySnapshot snapshot) {
        deviceChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(snapshot.period().getDisplayName() + " Consumption (kWh)");
        snapshot.consumptionByDevice().forEach((device, value) ->
                series.getData().add(new XYChart.Data<>(device, value))
        );
        deviceChart.getData().add(series);
    }

    private void loadTimelineChart(final EnergySnapshot snapshot) {
        timelineChart.getData().clear();
        timelineChart.getData().add(snapshot.timelineSeries());
        timelineTitleLabel.setText(snapshot.period() == AggregationPeriod.DAY
                ? "Daily Consumption Profile"
                : "Weekly Consumption Profile");
        Axis<String> xAxis = timelineChart.getXAxis();
        xAxis.setLabel(snapshot.period() == AggregationPeriod.DAY ? "Hour" : "Day");
        Axis<Number> yAxis = timelineChart.getYAxis();
        yAxis.setLabel("Energy Usage (kWh)");
    }

    private void updateToggleState() {
        boolean isDay = currentPeriod == AggregationPeriod.DAY;
        dayToggle.setSelected(isDay);
        weekToggle.setSelected(!isDay);
        if (isDay) {
            dayToggle.setDisable(true);
            weekToggle.setDisable(false);
        } else {
            dayToggle.setDisable(false);
            weekToggle.setDisable(true);
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleDayToggle() {
        currentPeriod = AggregationPeriod.DAY;
        refreshDashboard();
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleWeekToggle() {
        currentPeriod = AggregationPeriod.WEEK;
        refreshDashboard();
    }

    /**
     * Exports the current energy snapshot (Day or Week) to a user-chosen CSV file (FR-16).
     * Opens a system Save dialog with a default filename of energy-summary_YYYY-MM-DD.csv.
     * If the user cancels the dialog, no action is taken.
     */
    @FXML
    @SuppressWarnings("unused")
    private void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Energy Summary");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        fileChooser.setInitialFileName("energy-summary_" + today + ".csv");

        File file = fileChooser.showSaveDialog(totalUsageLabel.getScene().getWindow());
        if (file == null) {
            return;
        }

        String csv = energyService.exportToCSV(energyService.getSnapshot(currentPeriod));
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.print(csv);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Failed");
            alert.setHeaderText("Could not write CSV file");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
