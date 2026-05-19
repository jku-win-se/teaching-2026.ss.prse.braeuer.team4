package at.jku.se.smarthome.service.mock;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import at.jku.se.smarthome.config.DeviceEnergyConstants;
import at.jku.se.smarthome.service.api.EnergyService;
import javafx.scene.chart.XYChart;

/**
 * Mock Energy Service providing day and week energy consumption data.
 * <p>
 * Implements {@link EnergyService} to provide test/demo data without a database backend.
 */
public final class MockEnergyService implements EnergyService {

    /** Lock for singleton synchronization. */
    private static final Object INSTANCE_LOCK = new Object();
    /** Singleton instance of the mock energy service. */
    private static MockEnergyService instance;

    /** Enumeration of energy aggregation periods. */
    public enum AggregationPeriod {
        DAY("Day"),
        WEEK("Week");

        /** Display name for the aggregation period. */
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

    private MockEnergyService() {
    }

    /**
     * Returns the singleton instance of the mock energy service.
     *
     * @return singleton MockEnergyService instance
     */
    public static MockEnergyService getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new MockEnergyService();
            }
            return instance;
        }
    }

    /**
     * Resets the singleton instance for test isolation.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public static void resetForTesting() {
        synchronized (INSTANCE_LOCK) {
            instance = null;
        }
    }

    /**
     * Returns snapshot of energy consumption data.
     *
     * @param period the aggregation period for consumption data
     * @return energy snapshot with consumption metrics
     */
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

    @Override
    public Map<String, Double> getDailyByDevice(LocalDate date) {
        // Mock: return same data for all dates
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Double> data = new LinkedHashMap<>();
        data.put("Living Room Light", 4.8);
        data.put("Living Room Thermostat", 10.6);
        data.put("Bedroom Dimmer", 2.7);
        data.put("Kitchen Light", 5.4);
        data.put("Kitchen Coffee Machine", 3.8);
        data.put("Hallway Sensor", 0.9);
        return data;
    }

    @Override
    public Map<String, Double> getWeeklyByDevice(int isoWeekOfYear, int year) {
        // Mock: return same data for all weeks
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Double> data = new LinkedHashMap<>();
        data.put("Living Room Light", 32.4);
        data.put("Living Room Thermostat", 74.2);
        data.put("Bedroom Dimmer", 18.1);
        data.put("Kitchen Light", 36.7);
        data.put("Kitchen Coffee Machine", 24.9);
        data.put("Hallway Sensor", 6.3);
        return data;
    }

    @Override
    public Map<String, Double> getDailyByRoom(LocalDate date) {
        Map<String, Double> deviceData = getDailyByDevice(date);
        return aggregateByRoom(deviceData, AggregationPeriod.DAY);
    }

    @Override
    public Map<String, Double> getWeeklyByRoom(int isoWeekOfYear, int year) {
        Map<String, Double> deviceData = getWeeklyByDevice(isoWeekOfYear, year);
        return aggregateByRoom(deviceData, AggregationPeriod.WEEK);
    }

    @Override
    public double getHouseholdDaily(LocalDate date) {
        return getDailyByDevice(date).values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    @Override
    public double getHouseholdWeekly(int isoWeekOfYear, int year) {
        return getWeeklyByDevice(isoWeekOfYear, year).values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    @Override
    public int getDeviceNominalPower(String deviceType) {
        return DeviceEnergyConstants.getPowerWatts(deviceType);
    }

    @Override
    public Set<String> getAllDeviceTypes() {
        return new HashSet<>(DeviceEnergyConstants.getAllDeviceTypes());
    }

    /**
     * Helper: aggregates device consumption by room from device data.
     *
     * @param deviceData device consumption map
     * @param period aggregation period (DAY or WEEK)
     * @return map of room name to consumption in kWh
     */
    private Map<String, Double> aggregateByRoom(Map<String, Double> deviceData, AggregationPeriod period) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Double> roomData = new LinkedHashMap<>();
        roomData.put("Living Room", deviceData.getOrDefault("Living Room Light", 0.0) + deviceData.getOrDefault("Living Room Thermostat", 0.0));
        roomData.put("Bedroom", deviceData.getOrDefault("Bedroom Dimmer", 0.0));
        roomData.put("Kitchen", deviceData.getOrDefault("Kitchen Light", 0.0) + deviceData.getOrDefault("Kitchen Coffee Machine", 0.0));
        roomData.put("Hallway", deviceData.getOrDefault("Hallway Sensor", 0.0));

        if (period == AggregationPeriod.WEEK) {
            roomData.put("Bathroom", 8.5);
        } else {
            roomData.put("Bathroom", 1.2);
        }

        return roomData;
    }

    /**
     * Helper: gets energy consumption by device for the aggregation period.
     * <p>
     * This is used internally by getSnapshot(). For interface usage, see
     * {@link #getDailyByDevice(LocalDate)} or {@link #getWeeklyByDevice(int, int)}.
     *
     * @param period aggregation period (DAY or WEEK)
     * @return map of device name to consumption in kWh
     */
    private Map<String, Double> getConsumptionByDevice(AggregationPeriod period) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
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

    /**
     * Helper: aggregates device consumption by room for the aggregation period.
     * <p>
     * This is used internally by getSnapshot(). For interface usage, see
     * {@link #getDailyByRoom(LocalDate)} or {@link #getWeeklyByRoom(int, int)}.
     *
     * @param period aggregation period (DAY or WEEK)
     * @param deviceData device consumption map
     * @return map of room name to consumption in kWh
     */
    private Map<String, Double> getConsumptionByRoom(AggregationPeriod period, Map<String, Double> deviceData) {
        return aggregateByRoom(deviceData, period);
    }

    /**
     * Helper: gets consumption timeline series for the aggregation period.
     *
     * @param period aggregation period (DAY or WEEK)
     * @return XYChart series with consumption data points
     */
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

    /**
     * Helper: adds a data point to timeline series.
     *
     * @param series timeline series to add point to
     * @param label time/day label
     * @param value consumption value in kWh
     */
    private void addTimelinePoint(XYChart.Series<String, Number> series, String label, double value) {
        series.getData().add(new XYChart.Data<>(label, value));
    }
}
