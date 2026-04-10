package at.jku.se.smarthome.service.mock;

import java.util.LinkedHashMap;
import java.util.Map;

import javafx.scene.chart.XYChart;

/**
 * Mock Energy Service providing day and week energy consumption data.
 */
public class MockEnergyService {

    public enum AggregationPeriod {
        DAY("Day"),
        WEEK("Week");

        private final String displayName;

        AggregationPeriod(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public record EnergySnapshot(
            AggregationPeriod period,
            double householdTotal,
            String topDeviceName,
            double topDeviceConsumption,
            String topRoomName,
            double topRoomConsumption,
            Map<String, Double> consumptionByRoom,
            Map<String, Double> consumptionByDevice,
            XYChart.Series<String, Number> timelineSeries) {
    }

    private static MockEnergyService instance;

    private MockEnergyService() {
    }

    public static synchronized MockEnergyService getInstance() {
        if (instance == null) {
            instance = new MockEnergyService();
        }
        return instance;
    }

    public EnergySnapshot getSnapshot(AggregationPeriod period) {
        Map<String, Double> deviceData = getConsumptionByDevice(period);
        Map<String, Double> roomData = getConsumptionByRoom(period, deviceData);
        double householdTotal = deviceData.values().stream().mapToDouble(Double::doubleValue).sum();

        Map.Entry<String, Double> topDevice = deviceData.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(Map.entry("N/A", 0.0));

        Map.Entry<String, Double> topRoom = roomData.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(Map.entry("N/A", 0.0));

        return new EnergySnapshot(
                period,
                householdTotal,
                topDevice.getKey(),
                topDevice.getValue(),
                topRoom.getKey(),
                topRoom.getValue(),
                roomData,
                deviceData,
                getConsumptionOverTime(period)
        );
    }

    private Map<String, Double> getConsumptionByDevice(AggregationPeriod period) {
        Map<String, Double> data = new LinkedHashMap<>();
        if (period == AggregationPeriod.DAY) {
            data.put("Living Room Light", 4.8);
            data.put("Living Room Thermostat", 10.6);
            data.put("Bedroom Dimmer", 2.7);
            data.put("Kitchen Light", 5.4);
            data.put("Kitchen Coffee Machine", 3.8);
            data.put("Hallway Sensor", 0.9);
        } else {
            data.put("Living Room Light", 32.4);
            data.put("Living Room Thermostat", 74.2);
            data.put("Bedroom Dimmer", 18.1);
            data.put("Kitchen Light", 36.7);
            data.put("Kitchen Coffee Machine", 24.9);
            data.put("Hallway Sensor", 6.3);
        }
        return data;
    }

    private Map<String, Double> getConsumptionByRoom(AggregationPeriod period, Map<String, Double> deviceData) {
        Map<String, Double> roomData = new LinkedHashMap<>();
        roomData.put("Living Room", deviceData.get("Living Room Light") + deviceData.get("Living Room Thermostat"));
        roomData.put("Bedroom", deviceData.get("Bedroom Dimmer"));
        roomData.put("Kitchen", deviceData.get("Kitchen Light") + deviceData.get("Kitchen Coffee Machine"));
        roomData.put("Hallway", deviceData.get("Hallway Sensor"));

        if (period == AggregationPeriod.WEEK) {
            roomData.put("Bathroom", 8.5);
        } else {
            roomData.put("Bathroom", 1.2);
        }

        return roomData;
    }

    private XYChart.Series<String, Number> getConsumptionOverTime(AggregationPeriod period) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(period == AggregationPeriod.DAY ? "Hourly Usage (kWh)" : "Daily Usage (kWh)");

        if (period == AggregationPeriod.DAY) {
            addTimelinePoint(series, "00:00", 1.2);
            addTimelinePoint(series, "04:00", 0.9);
            addTimelinePoint(series, "08:00", 4.1);
            addTimelinePoint(series, "12:00", 5.0);
            addTimelinePoint(series, "16:00", 4.3);
            addTimelinePoint(series, "20:00", 8.6);
            addTimelinePoint(series, "23:00", 4.3);
        } else {
            addTimelinePoint(series, "Mon", 19.8);
            addTimelinePoint(series, "Tue", 21.4);
            addTimelinePoint(series, "Wed", 18.7);
            addTimelinePoint(series, "Thu", 20.3);
            addTimelinePoint(series, "Fri", 22.1);
            addTimelinePoint(series, "Sat", 24.8);
            addTimelinePoint(series, "Sun", 23.4);
        }

        return series;
    }

    private void addTimelinePoint(XYChart.Series<String, Number> series, String label, double value) {
        series.getData().add(new XYChart.Data<>(label, value));
    }
}
