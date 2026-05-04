package at.jku.se.smarthome.service.api;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * Defines operations for calculating and querying energy consumption data.
 * <p>
 * The service calculates estimated power consumption based on device on-time
 * derived from activity logs, aggregated at three levels: device, room, and
 * household, over two time scales: daily and weekly.
 * <p>
 * Energy Calculation Formula: Consumption (Wh) = Nominal Power (W) × On-Time (hours)
 * <p>
 * Time Scales:
 * <ul>
 *   <li>Daily: Current calendar day (00:00 to 23:59:59)</li>
 *   <li>Weekly: Current ISO week (Monday 00:00 to current time)</li>
 * </ul>
 */
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public interface EnergyService {

    /**
     * Returns estimated energy consumption per device for a given day.
     * <p>
     * Devices with no activity during the period are included with 0 Wh.
     *
     * @param date the calendar date to query (00:00 to 23:59:59)
     * @return map of device name to consumption in Wh; never null, may be empty
     */
    Map<String, Double> getDailyByDevice(LocalDate date);

    /**
     * Returns estimated energy consumption per device for a given ISO week.
     * <p>
     * Devices with no activity during the period are included with 0 Wh.
     *
     * @param isoWeekOfYear the ISO week number (1-53)
     * @param year the year
     * @return map of device name to consumption in Wh; never null, may be empty
     */
    Map<String, Double> getWeeklyByDevice(int isoWeekOfYear, int year);

    /**
     * Returns estimated energy consumption per room for a given day.
     * <p>
     * Room consumption is the sum of all devices in that room.
     * Rooms with no devices or all devices having zero consumption are included with 0 Wh.
     *
     * @param date the calendar date to query (00:00 to 23:59:59)
     * @return map of room name to consumption in Wh; never null, may be empty
     */
    Map<String, Double> getDailyByRoom(LocalDate date);

    /**
     * Returns estimated energy consumption per room for a given ISO week.
     * <p>
     * Room consumption is the sum of all devices in that room.
     * Rooms with no devices or all devices having zero consumption are included with 0 Wh.
     *
     * @param isoWeekOfYear the ISO week number (1-53)
     * @param year the year
     * @return map of room name to consumption in Wh; never null, may be empty
     */
    Map<String, Double> getWeeklyByRoom(int isoWeekOfYear, int year);

    /**
     * Returns total estimated energy consumption for the household on a given day.
     * <p>
     * Household consumption is the sum of all rooms, which equals the sum of all devices.
     *
     * @param date the calendar date to query (00:00 to 23:59:59)
     * @return total consumption in Wh; never negative
     */
    double getHouseholdDaily(LocalDate date);

    /**
     * Returns total estimated energy consumption for the household for a given ISO week.
     * <p>
     * Household consumption is the sum of all rooms, which equals the sum of all devices.
     *
     * @param isoWeekOfYear the ISO week number (1-53)
     * @param year the year
     * @return total consumption in Wh; never negative
     */
    double getHouseholdWeekly(int isoWeekOfYear, int year);

    /**
     * Returns the nominal power consumption for a device type.
     * <p>
     * This is the baseline power in watts used for energy calculations.
     *
     * @param deviceType device type identifier (e.g. "SWITCH", "THERMOSTAT", "LIGHT")
     * @return nominal power in watts; 0 if device type unknown
     */
    int getDeviceNominalPower(String deviceType);

    /**
     * Returns all known device types with defined nominal power values.
     *
     * @return set of device type identifiers; never null
     */
    Set<String> getAllDeviceTypes();
}
