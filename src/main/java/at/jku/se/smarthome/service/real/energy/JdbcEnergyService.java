package at.jku.se.smarthome.service.real.energy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import at.jku.se.smarthome.config.DatabaseConfig;
import at.jku.se.smarthome.config.DatabaseSettings;
import at.jku.se.smarthome.config.DeviceEnergyConstants;
import at.jku.se.smarthome.service.api.EnergyService;

/**
 * JDBC-backed EnergyService implementation.
 * <p>
 * Calculates energy consumption from activity log entries by computing device on-time
 * and multiplying by nominal power values.
 * <p>
 * Energy Calculation Formula: Consumption (Wh) = Nominal Power (W) × On-Time (hours)
 * <p>
 * Aggregates consumption at three levels:
 * <ul>
 *   <li>Device: Individual device consumption</li>
 *   <li>Room: Sum of all devices in room</li>
 *   <li>Household: Sum of all rooms</li>
 * </ul>
 */
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public final class JdbcEnergyService implements EnergyService {

    /** Path to database schema initialization script in classpath. */
    private static final String INIT_SCRIPT_PATH = "/db/init-energy.sql";

    /** Maximum number of log entries to load from database. */
    private static final int MAX_LOADED_ENTRIES = 10000;

    /** Singleton instance of the JDBC energy service. */
    private static JdbcEnergyService instance;

    /** Flag indicating database schema is initialized. */
    private final AtomicBoolean schemaReady = new AtomicBoolean(false);

    /** Cache for daily energy consumption by device. */
    private final Map<LocalDate, Map<String, Double>> dailyDeviceCache = new HashMap<>();

    /** Cache for weekly energy consumption by device. */
    private final Map<String, Map<String, Double>> weeklyDeviceCache = new HashMap<>();

    /** Timestamp of last cache update for invalidation. */
    private final AtomicLong lastCacheTime = new AtomicLong(0);

    /** Cache TTL in milliseconds for daily data (30 seconds). */
    private static final long DAILY_CACHE_TTL_MS = 30_000;

    /** Cache TTL in milliseconds for weekly data (5 minutes). */
    private static final long WEEKLY_CACHE_TTL_MS = 300_000;

    /** Initializes JDBC energy service. */
    private JdbcEnergyService() {
        // Initialize schema on first use
    }

    /**
     * Returns the singleton instance of the JDBC energy service.
     *
     * @return singleton JdbcEnergyService instance
     */
    public static JdbcEnergyService getInstance() {
        synchronized (JdbcEnergyService.class) {
            if (instance == null) {
                instance = new JdbcEnergyService();
            }
            return instance;
        }
    }

    /**
     * Resets the singleton instance for test isolation.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public static void resetForTesting() {
        synchronized (JdbcEnergyService.class) {
            instance = null;
        }
    }

    @Override
    public Map<String, Double> getDailyByDevice(LocalDate date) {
        // Check cache
        if (dailyDeviceCache.containsKey(date) && isCacheValid(DAILY_CACHE_TTL_MS)) {
            return new HashMap<>(dailyDeviceCache.get(date));
        }

        Map<String, Double> result = calculateDailyByDevice(date);
        dailyDeviceCache.put(date, new HashMap<>(result));
        lastCacheTime.set(System.currentTimeMillis());
        return result;
    }

    @Override
    public Map<String, Double> getWeeklyByDevice(int isoWeekOfYear, int year) {
        String cacheKey = year + "-W" + isoWeekOfYear;

        // Check cache
        if (weeklyDeviceCache.containsKey(cacheKey) && isCacheValid(WEEKLY_CACHE_TTL_MS)) {
            return new HashMap<>(weeklyDeviceCache.get(cacheKey));
        }

        Map<String, Double> result = calculateWeeklyByDevice(isoWeekOfYear, year);
        weeklyDeviceCache.put(cacheKey, new HashMap<>(result));
        lastCacheTime.set(System.currentTimeMillis());
        return result;
    }

    @Override
    public Map<String, Double> getDailyByRoom(LocalDate date) {
        Map<String, Double> deviceConsumption = getDailyByDevice(date);
        return aggregateByRoom(deviceConsumption);
    }

    @Override
    public Map<String, Double> getWeeklyByRoom(int isoWeekOfYear, int year) {
        Map<String, Double> deviceConsumption = getWeeklyByDevice(isoWeekOfYear, year);
        return aggregateByRoom(deviceConsumption);
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
     * Calculates daily energy consumption by device from activity log.
     *
     * @param date the calendar date to query
     * @return map of device name to consumption in Wh
     */
    private Map<String, Double> calculateDailyByDevice(LocalDate date) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Double> result = new LinkedHashMap<>();

        try (Connection connection = openConnection()) {
            ensureSchema(connection);

            // Get all unique devices first
            Set<String> allDevices = getAllDeviceNamesFromLog(connection);

            // For each device, calculate on-time
            for (String deviceName : allDevices) {
                double onTimeHours = calculateDeviceOnTimeForDay(connection, deviceName, date);
                int nominalPowerW = getDeviceNominalPowerFromLog(connection, deviceName);
                double consumptionWh = onTimeHours * nominalPowerW;
                result.put(deviceName, consumptionWh);
            }

            // Ensure all known devices are in result (even if no activity)
            for (String deviceName : result.keySet()) {
                if (!result.containsKey(deviceName)) {
                    result.put(deviceName, 0.0);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to calculate daily energy consumption by device.", e);
        }

        return result;
    }

    /**
     * Calculates weekly energy consumption by device from activity log.
     *
     * @param isoWeekOfYear ISO week number
     * @param year the year
     * @return map of device name to consumption in Wh
     */
    private Map<String, Double> calculateWeeklyByDevice(int isoWeekOfYear, int year) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Double> result = new LinkedHashMap<>();

        try (Connection connection = openConnection()) {
            ensureSchema(connection);

            // Get all unique devices first
            Set<String> allDevices = getAllDeviceNamesFromLog(connection);

            // For each device, calculate on-time for the week
            for (String deviceName : allDevices) {
                double onTimeHours = calculateDeviceOnTimeForWeek(connection, deviceName, isoWeekOfYear, year);
                int nominalPowerW = getDeviceNominalPowerFromLog(connection, deviceName);
                double consumptionWh = onTimeHours * nominalPowerW;
                result.put(deviceName, consumptionWh);
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to calculate weekly energy consumption by device.", e);
        }

        return result;
    }

    /**
     * Aggregates device consumption by room.
     *
     * @param deviceConsumption map of device name to consumption in Wh
     * @return map of room name to consumption in Wh
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private Map<String, Double> aggregateByRoom(Map<String, Double> deviceConsumption) {
        Map<String, Double> roomConsumption = new LinkedHashMap<>();

        // TODO: Integrate with RoomService to get device→room mappings
        // For now, use static mappings for mock data
        roomConsumption.put("Living Room",
                deviceConsumption.getOrDefault("Living Room Light", 0.0)
                        + deviceConsumption.getOrDefault("Living Room Thermostat", 0.0));
        roomConsumption.put("Bedroom",
                deviceConsumption.getOrDefault("Bedroom Dimmer", 0.0));
        roomConsumption.put("Kitchen",
                deviceConsumption.getOrDefault("Kitchen Light", 0.0)
                        + deviceConsumption.getOrDefault("Kitchen Coffee Machine", 0.0));
        roomConsumption.put("Hallway",
                deviceConsumption.getOrDefault("Hallway Sensor", 0.0));
        roomConsumption.put("Bathroom",
                deviceConsumption.getOrDefault("Bathroom Light", 0.0));

        return roomConsumption;
    }

    /**
     * Calculates on-time in hours for a device on a specific day.
     *
     * @param connection database connection
     * @param deviceName device name
     * @param date the date to query
     * @return on-time in hours
     * @throws SQLException if database query fails
     */
    private double calculateDeviceOnTimeForDay(Connection connection, String deviceName, LocalDate date)
            throws SQLException {
        // Query: Find all ON/OFF state changes for device on given date
        // Calculate time between ON and OFF events
        String sql = "SELECT timestamp, action FROM activity_log "
                + "WHERE device = ? AND timestamp::date = ?::date "
                + "ORDER BY timestamp ASC";

        double totalOnTimeMs = 0.0;
        boolean deviceIsOn = false;
        long lastOnTime = 0;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, deviceName);
            stmt.setString(2, date.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String action = rs.getString("action").toUpperCase();
                    long currentTime = rs.getTimestamp("timestamp").getTime();

                    if (isOnAction(action)) {
                        if (!deviceIsOn) {
                            deviceIsOn = true;
                            lastOnTime = currentTime;
                        }
                    } else if (isOffAction(action)) {
                        if (deviceIsOn) {
                            totalOnTimeMs += (currentTime - lastOnTime);
                            deviceIsOn = false;
                        }
                    }
                }

                // If device is still ON at end of day, count remaining time
                if (deviceIsOn) {
                    long endOfDayMs = java.sql.Timestamp.valueOf(date.atTime(23, 59, 59)).getTime();
                    totalOnTimeMs += (endOfDayMs - lastOnTime);
                }
            }
        }

        return totalOnTimeMs / (3600.0 * 1000.0); // Convert milliseconds to hours
    }

    /**
     * Calculates on-time in hours for a device during a specific ISO week.
     *
     * @param connection database connection
     * @param deviceName device name
     * @param isoWeekOfYear ISO week number
     * @param year the year
     * @return on-time in hours
     * @throws SQLException if database query fails
     */
    private double calculateDeviceOnTimeForWeek(Connection connection, String deviceName, int isoWeekOfYear, int year)
            throws SQLException {
        double totalOnTimeHours = 0.0;

        // Calculate Monday of this ISO week
        WeekFields weekFields = WeekFields.ISO;
        LocalDate monday = LocalDate.of(year, 1, 4)
                .with(weekFields.weekOfYear(), isoWeekOfYear)
                .with(DayOfWeek.MONDAY);

        // Sum on-time for each day of the week
        for (int i = 0; i < 7; i++) {
            LocalDate dayDate = monday.plusDays(i);
            // Stop at today if we're in current week
            if (dayDate.isAfter(LocalDate.now())) {
                break;
            }
            totalOnTimeHours += calculateDeviceOnTimeForDay(connection, deviceName, dayDate);
        }

        return totalOnTimeHours;
    }

    /**
     * Gets all unique device names from activity log.
     *
     * @param connection database connection
     * @return set of device names
     * @throws SQLException if database query fails
     */
    private Set<String> getAllDeviceNamesFromLog(Connection connection) throws SQLException {
        String sql = "SELECT DISTINCT device FROM activity_log LIMIT ?";
        Set<String> devices = new HashSet<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, MAX_LOADED_ENTRIES);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    devices.add(rs.getString("device"));
                }
            }
        }

        return devices;
    }

    /**
     * Gets the nominal power for a device from the activity log (looks at device type or defaults).
     *
     * @param connection database connection
     * @param deviceName device name
     * @return nominal power in watts
     * @throws SQLException if database query fails
     */
    private int getDeviceNominalPowerFromLog(Connection connection, String deviceName) throws SQLException {
        // TODO: Query device type from RoomService/devices table
        // For now, infer from device name or use defaults

        if (deviceName.contains("Thermostat")) {
            return DeviceEnergyConstants.THERMOSTAT_POWER_W;
        } else if (deviceName.contains("Dimmer")) {
            return DeviceEnergyConstants.DIMMER_POWER_W;
        } else if (deviceName.contains("Light")) {
            return DeviceEnergyConstants.LIGHT_POWER_W;
        } else if (deviceName.contains("Coffee")) {
            return DeviceEnergyConstants.COFFEE_MACHINE_POWER_W;
        } else if (deviceName.contains("Sensor")) {
            return DeviceEnergyConstants.SENSOR_POWER_W;
        } else if (deviceName.contains("Blind")) {
            return DeviceEnergyConstants.BLIND_POWER_W;
        } else {
            return DeviceEnergyConstants.SWITCH_POWER_W;
        }
    }

    /**
     * Helper: checks if action string represents device turning ON.
     *
     * @param action action string from log
     * @return true if action is an ON action
     */
    private boolean isOnAction(String action) {
        String upper = action.toUpperCase();
        return upper.contains("ON") || upper.contains("TURNED_ON") || upper.contains("SWITCH_ON");
    }

    /**
     * Helper: checks if action string represents device turning OFF.
     *
     * @param action action string from log
     * @return true if action is an OFF action
     */
    private boolean isOffAction(String action) {
        String upper = action.toUpperCase();
        return upper.contains("OFF") || upper.contains("TURNED_OFF") || upper.contains("SWITCH_OFF");
    }

    /**
     * Helper: checks if cache is still valid.
     *
     * @param ttlMs time-to-live in milliseconds
     * @return true if cache hasn't expired
     */
    private boolean isCacheValid(long ttlMs) {
        long age = System.currentTimeMillis() - lastCacheTime.get();
        return age < ttlMs;
    }

    /**
     * Helper: opens a database connection using configured settings.
     *
     * @return open database connection
     * @throws SQLException if connection fails
     */
    private Connection openConnection() throws SQLException {
        DatabaseSettings settings = DatabaseConfig.load()
                .orElseThrow(() -> new IllegalStateException("Energy database is not configured."));
        return DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password());
    }

    /**
     * Helper: ensures database schema is initialized.
     *
     * @param connection database connection to use
     */
    @SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts")
    private void ensureSchema(Connection connection) {
        if (!schemaReady.get()) {
            synchronized (this) {
                if (!schemaReady.get()) {
                    try (Statement stmt = connection.createStatement()) {
                        String initScript = loadInitScript();
                        // Only execute if script exists
                        if (!initScript.trim().isEmpty()) {
                            for (String sql : initScript.split(";")) {
                                String statementSql = sql.trim();
                                if (!statementSql.isEmpty()) {
                                    stmt.execute(statementSql);
                                }
                            }
                        }
                        schemaReady.set(true);
                    } catch (SQLException e) {
                        throw new IllegalStateException("Failed to initialize energy database schema.", e);
                    }
                }
            }
        }
    }

    /**
     * Helper: loads initialization script from classpath.
     *
     * @return SQL script content
     */
    private String loadInitScript() {
        try (InputStream is = getClass().getResourceAsStream(INIT_SCRIPT_PATH)) {
            if (is == null) {
                return ""; // Script optional; return empty to skip
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load energy initialization script.", e);
        }
    }

    /**
     * Invalidates the entire cache (for testing or after log modifications).
     */
    public void invalidateCache() {
        dailyDeviceCache.clear();
        weeklyDeviceCache.clear();
        lastCacheTime.set(0);
    }
}
