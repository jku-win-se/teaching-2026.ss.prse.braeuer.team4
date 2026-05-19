package at.jku.se.smarthome.service.mock;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javafx.scene.chart.XYChart;

/**
 * Mock Energy Service providing day and week energy consumption data.
 */
public final class MockEnergyService {

    /** Lock for singleton synchronization. */
    private static final Object INSTANCE_LOCK = new Object();
    /** Singleton instance of the mock energy service. */
    private static MockEnergyService instance;

    /** Enumeration of energy aggregation periods. */
    public enum AggregationPeriod {
        /** Daily aggregation. */
        DAY("Day"),
        /** Weekly aggregation. */
        WEEK("Week");

        /** Display name for the aggregation period. */
        private final String displayName;

        /**
         * Constructs an AggregationPeriod with the given display name.
         *
         * @param displayName human-readable display name
         */
        AggregationPeriod(final String displayName) {
            this.displayName = displayName;
        }

        /**
         * Returns the human-readable display name.
         *
         * @return display name string
         */
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Immutable snapshot of energy consumption metrics for a given aggregation period.
     *
     * @param period               the aggregation period this snapshot covers
     * @param householdTotal       total household consumption in kWh
     * @param topDeviceName        name of the highest-consuming device
     * @param topDeviceConsumption consumption of the top device in kWh
     * @param topRoomName          name of the highest-consuming room
     * @param topRoomConsumption   consumption of the top room in kWh
     * @param consumptionByRoom    map of room name to consumption in kWh
     * @param consumptionByDevice  map of device name to consumption in kWh
     * @param timelineSeries       chart series for consumption over time
     */
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
    public EnergySnapshot getSnapshot(final AggregationPeriod period) {
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

    /**
     * Exports the given energy snapshot as a semicolon-delimited CSV string.
     * Each room appears on its own row sorted alphabetically, followed by a totals row.
     * Fields are double-quoted; numbers use dot decimal notation with one decimal place.
     *
     * @param snapshot the energy snapshot to export
     * @return CSV-formatted string with header row
     */
    public String exportToCSV(final EnergySnapshot snapshot) {
        StringBuilder csv = new StringBuilder(256);
        csv.append("Period;Room;ConsumptionKwh\n");
        snapshot.consumptionByRoom().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> csv.append(String.format(Locale.US, "\"%s\";\"%s\";\"%.1f\"\n",
                        snapshot.period().getDisplayName(),
                        entry.getKey(),
                        entry.getValue())));
        csv.append(String.format(Locale.US, "\"%s\";\"%s\";\"%.1f\"\n",
                snapshot.period().getDisplayName(),
                "Total",
                snapshot.householdTotal()));
        return csv.toString();
    }

    /**
     * Helper: gets energy consumption by device for the aggregation period.
     *
     * @param period aggregation period (DAY or WEEK)
     * @return map of device name to consumption in kWh
     */
    private Map<String, Double> getConsumptionByDevice(final AggregationPeriod period) {
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
     *
     * @param period     aggregation period (DAY or WEEK)
     * @param deviceData device consumption map
     * @return map of room name to consumption in kWh
     */
    private Map<String, Double> getConsumptionByRoom(final AggregationPeriod period,
                                                      final Map<String, Double> deviceData) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
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

    /**
     * Helper: gets consumption timeline series for the aggregation period.
     *
     * @param period aggregation period (DAY or WEEK)
     * @return XYChart series with consumption data points
     */
    private XYChart.Series<String, Number> getConsumptionOverTime(final AggregationPeriod period) {
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
     * @param label  time/day label
     * @param value  consumption value in kWh
     */
    private void addTimelinePoint(final XYChart.Series<String, Number> series,
                                  final String label, final double value) {
        series.getData().add(new XYChart.Data<>(label, value));
    }
}
