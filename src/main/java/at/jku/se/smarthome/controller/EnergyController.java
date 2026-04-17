package at.jku.se.smarthome.controller;

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
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

/**
 * Controller for the energy dashboard view.
 */
public class EnergyController {
    
    /** Label displaying total energy usage. */
    @FXML
    private Label totalUsageLabel;
    
    /** Label displaying top energy consumer device. */
    @FXML
    private Label topDeviceLabel;
    
    /** Label displaying top energy consuming room. */
    @FXML
    private Label topRoomLabel;

    /** Label displaying timeline title. */
    @FXML
    private Label timelineTitleLabel;

    /** Label displaying aggregation period hint. */
    @FXML
    private Label aggregationHintLabel;
    
    /** Pie chart displaying energy usage by room. */
    @FXML
    private PieChart roomChart;
    
    /** Bar chart displaying energy usage by device. */
    @FXML
    private BarChart<String, Number> deviceChart;
    
    /** Line chart displaying energy usage over time. */
    @FXML
    private LineChart<String, Number> timelineChart;
    
    /** Toggle button for daily aggregation. */
    @FXML
    private ToggleButton dayToggle;
    
    /** Toggle button for weekly aggregation. */
    @FXML
    private ToggleButton weekToggle;
    
    /** Energy service for consumption data. */
    private final MockEnergyService energyService = MockEnergyService.getInstance();
    /** Current aggregation period for data display. */
    private AggregationPeriod currentPeriod = AggregationPeriod.DAY;
    
    @FXML
    private void initialize() {
        roomChart.setAnimated(false);
        deviceChart.setAnimated(false);
        timelineChart.setAnimated(false);
        refreshDashboard();
    }

    private void refreshDashboard() {
        EnergySnapshot snapshot = energyService.getSnapshot(currentPeriod);
        loadSummaryData(snapshot);
        loadRoomChart(snapshot);
        loadDeviceChart(snapshot);
        loadTimelineChart(snapshot);
        updateToggleState();
    }

    private void loadSummaryData(EnergySnapshot snapshot) {
        totalUsageLabel.setText(String.format("%.1f kWh", snapshot.householdTotal()));
        topDeviceLabel.setText(String.format("%s (%.1f kWh)", snapshot.topDeviceName(), snapshot.topDeviceConsumption()));
        topRoomLabel.setText(String.format("%s (%.1f kWh)", snapshot.topRoomName(), snapshot.topRoomConsumption()));
        aggregationHintLabel.setText("Aggregation: " + snapshot.period().getDisplayName());
    }

    private void loadRoomChart(EnergySnapshot snapshot) {
        ObservableList<PieChart.Data> roomData = FXCollections.observableArrayList();
        snapshot.consumptionByRoom().forEach((room, value) ->
            roomData.add(new PieChart.Data(room, value))
        );
        roomChart.setData(roomData);
        roomChart.setTitle(snapshot.period().getDisplayName() + " Aggregation");
    }

    private void loadDeviceChart(EnergySnapshot snapshot) {
        deviceChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(snapshot.period().getDisplayName() + " Consumption (kWh)");

        snapshot.consumptionByDevice().forEach((device, value) ->
                series.getData().add(new XYChart.Data<>(device, value))
        );

        deviceChart.getData().add(series);
    }

    private void loadTimelineChart(EnergySnapshot snapshot) {
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
    private void handleDayToggle() {
        currentPeriod = AggregationPeriod.DAY;
        refreshDashboard();
    }
    
    @FXML
    private void handleWeekToggle() {
        currentPeriod = AggregationPeriod.WEEK;
        refreshDashboard();
    }
    
    @FXML
    private void handleExport() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Energy Report");
        alert.setHeaderText("Mock export complete");
        alert.setContentText("The " + currentPeriod.getDisplayName().toLowerCase() + " energy report was exported as CSV.");
        alert.showAndWait();
    }
}
