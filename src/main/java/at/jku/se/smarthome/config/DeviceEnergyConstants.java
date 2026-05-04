package at.jku.se.smarthome.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Centralized source of truth for device nominal power consumption values.
 * <p>
 * Each device type has a documented nominal power in watts (W) that is used
 * to estimate energy consumption from on-time activity logs.
 * <p>
 * Formula: Consumption (Wh) = Nominal Power (W) × On-Time (hours)
 */
public final class DeviceEnergyConstants {

    /** Nominal power for SWITCH device type (W). */
    public static final int SWITCH_POWER_W = 10;

    /** Nominal power for DIMMER device type (W). */
    public static final int DIMMER_POWER_W = 12;

    /** Nominal power for THERMOSTAT device type (W). */
    public static final int THERMOSTAT_POWER_W = 50;

    /** Nominal power for SENSOR device type (W). */
    public static final int SENSOR_POWER_W = 2;

    /** Nominal power for BLIND device type (W). */
    public static final int BLIND_POWER_W = 20;

    /** Nominal power for LIGHT device type (W). */
    public static final int LIGHT_POWER_W = 15;

    /** Nominal power for COFFEE_MACHINE device type (W). */
    public static final int COFFEE_MACHINE_POWER_W = 1500;

    /** Default power for unknown device types (W). */
    private static final int DEFAULT_POWER_W = 10;

    /** Immutable map of device type to nominal power in watts. */
    private static final Map<String, Integer> POWER_MAP;

    static {
        Map<String, Integer> map = new HashMap<>();
        map.put("SWITCH", SWITCH_POWER_W);
        map.put("DIMMER", DIMMER_POWER_W);
        map.put("THERMOSTAT", THERMOSTAT_POWER_W);
        map.put("SENSOR", SENSOR_POWER_W);
        map.put("BLIND", BLIND_POWER_W);
        map.put("LIGHT", LIGHT_POWER_W);
        map.put("COFFEE_MACHINE", COFFEE_MACHINE_POWER_W);
        POWER_MAP = Collections.unmodifiableMap(map);
    }

    /** Private constructor to prevent instantiation. */
    private DeviceEnergyConstants() {
        // Utility class
    }

    /**
     * Returns the nominal power consumption for a device type.
     * <p>
     * If the device type is not recognized, returns DEFAULT_POWER_W (10W).
     *
     * @param deviceType device type identifier (e.g. "SWITCH", "THERMOSTAT")
     * @return nominal power in watts; never negative; DEFAULT_POWER_W if unknown
     */
    public static int getPowerWatts(String deviceType) {
        if (deviceType == null || deviceType.isBlank()) {
            return DEFAULT_POWER_W;
        }
        return POWER_MAP.getOrDefault(deviceType.toUpperCase(), DEFAULT_POWER_W);
    }

    /**
     * Returns all known device types with defined nominal power values.
     *
     * @return immutable set of device type identifiers
     */
    public static Set<String> getAllDeviceTypes() {
        return POWER_MAP.keySet();
    }

    /**
     * Returns the nominal power for a device type, or the default if unknown.
     * <p>
     * This is an alias for {@link #getPowerWatts(String)} for convenience.
     *
     * @param deviceType device type identifier
     * @return nominal power in watts
     */
    public static int getNominalPowerWatts(String deviceType) {
        return getPowerWatts(deviceType);
    }
}
