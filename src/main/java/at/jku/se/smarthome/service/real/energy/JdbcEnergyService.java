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
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import at.jku.se.smarthome.config.DatabaseConfig;
import at.jku.se.smarthome.config.DatabaseSettings;
import at.jku.se.smarthome.config.DeviceEnergyConstants;
import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.service.api.EnergyService;
import at.jku.se.smarthome.service.api.RoomService;
import at.jku.se.smarthome.service.api.ServiceRegistry;

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
@SuppressWarnings({"PMD.UseObjectForClearerAPI", "PMD.ShortVariable", "PMD.OnlyOneReturn", 
    "PMD.AvoidCatchingGenericException", "PMD.SystemPrintln", "PMD.GodClass", 
    "PMD.TooManyMethods", "PMD.CyclomaticComplexity"})
public final class JdbcEnergyService implements EnergyService {

    /** Path to database schema initialization script in classpath. */
    private static final String INIT_SCRIPT_PATH = "/db/init-energy.sql";

    /** Maximum number of log entries to load from database. */
    private static final int MAX_LOADED_ENTRIES = 10000;

    /** Singleton instance of the JDBC energy service. */
    private static JdbcEnergyService instance;

    /** Room service for device→room mappings. */
    private final RoomService roomService;

    /** Flag indicating database schema is initialized. */
    private final AtomicBoolean schemaReady = new AtomicBoolean(false);

    /** Cache for daily energy consumption by device. */
    private final Map<LocalDate, Map<String, Double>> dailyDeviceCache = new ConcurrentHashMap<>();

    /** Cache for weekly energy consumption by device. */
    private final Map<String, Map<String, Double>> weeklyDeviceCache = new ConcurrentHashMap<>();

    /** Timestamp of last cache update for invalidation. */
    private final AtomicLong lastCacheTime = new AtomicLong(0);

    /** Cache TTL in milliseconds for daily data (30 seconds). */
    private static final long DAILY_CACHE_TTL_MS = 30_000;

    /** Cache TTL in milliseconds for weekly data (5 minutes). */
    private static final long WEEKLY_CACHE_TTL_MS = 300_000;

    /** Initializes JDBC energy service. */
    private JdbcEnergyService() {
        this.roomService = ServiceRegistry.getRoomService();
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
     * Retrieves cached daily consumption for a date if it exists and is up-to-date.
     * Returns null if cache doesn't exist or if new activity has occurred since cache was last updated.
     *
     * @param connection database connection
     * @param date the date to check cache for
     * @return cached consumption map, or null if cache is missing/invalid
     * @throws SQLException if database query fails
     */
    private Map<String, Double> getCachedDailyConsumption(Connection connection, LocalDate date) throws SQLException {
        // Get most recent activity timestamp for this date
        String maxActivitySql = "SELECT MAX(timestamp) as max_ts FROM activity_log "
                + "WHERE SUBSTRING(timestamp, 1, 10) = ?";
        
        String maxActivityTimeStr = null;
        try (PreparedStatement stmt = connection.prepareStatement(maxActivitySql)) {
            stmt.setString(1, date.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    maxActivityTimeStr = rs.getString("max_ts");
                }
            }
        }
        
        // If no activity on this date, check if empty cache exists
        if (maxActivityTimeStr == null) {
            String cacheExistsSql = "SELECT COUNT(*) as cnt FROM energy_daily WHERE date = CAST(? AS DATE)";
            try (PreparedStatement stmt = connection.prepareStatement(cacheExistsSql)) {
                stmt.setString(1, date.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt("cnt") > 0) {
                        // Cache exists for this date with no activity
                        return getCachedDailyFromDb(connection, date);
                    }
                }
            }
            return null;
        }

        // Check if cache exists and is more recent than last activity
        String checkCacheSql = "SELECT COUNT(*) as cnt FROM energy_daily "
                + "WHERE date = CAST(? AS DATE) AND created_at > ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(checkCacheSql)) {
            stmt.setString(1, date.toString());
            // Parse the text timestamp to SQL timestamp
            java.sql.Timestamp maxActivityTime = null;
            if (maxActivityTimeStr != null) {
                try {
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(maxActivityTimeStr);
                    maxActivityTime = java.sql.Timestamp.valueOf(ldt);
                } catch (Exception e) {
                    // If parsing fails, use current time as fallback
                    maxActivityTime = new java.sql.Timestamp(System.currentTimeMillis());
                }
            } else {
                maxActivityTime = new java.sql.Timestamp(System.currentTimeMillis());
            }
            stmt.setTimestamp(2, maxActivityTime);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt("cnt") > 0) {
                    // Cache exists and is more recent than last activity
                    return getCachedDailyFromDb(connection, date);
                }
            }
        }

        return null;
    }

    /**
     * Retrieves cached daily consumption from database for a specific date.
     *
     * @param connection database connection
     * @param date the date to retrieve
     * @return map of device name to consumption in Wh
     * @throws SQLException if database query fails
     */
    private Map<String, Double> getCachedDailyFromDb(Connection connection, LocalDate date) throws SQLException {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Double> result = new LinkedHashMap<>();
        
        String sql = "SELECT device_name, consumption_wh FROM energy_daily WHERE date = CAST(? AS DATE)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, date.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("device_name"), rs.getDouble("consumption_wh"));
                }
            }
        }
        
        return result;
    }

    /**
     * Stores daily consumption in cache table.
     *
     * @param connection database connection
     * @param date the date
     * @param deviceName device name
     * @param onTimeHours on-time in hours
     * @param consumptionWh consumption in Wh
     * @throws SQLException if database insert fails
     */
    private void storeDailyConsumptionCache(Connection connection, LocalDate date, String deviceName, 
                                            double onTimeHours, double consumptionWh) throws SQLException {
        String sql = "INSERT INTO energy_daily (date, device_id, device_name, on_time_hours, consumption_wh) "
                + "VALUES (CAST(? AS DATE), ?, ?, ?, ?) "
                + "ON CONFLICT (date, device_id) DO UPDATE SET "
                + "on_time_hours = EXCLUDED.on_time_hours, "
                + "consumption_wh = EXCLUDED.consumption_wh, "
                + "created_at = CURRENT_TIMESTAMP";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, date.toString());
            stmt.setString(2, deviceName); // Use device name as ID for now
            stmt.setString(3, deviceName);
            stmt.setDouble(4, onTimeHours);
            stmt.setDouble(5, consumptionWh);
            stmt.executeUpdate();
        }
    }

    /**
     * Retrieves cached weekly consumption for a week if it exists and is up-to-date.
     * Returns null if cache doesn't exist or if new activity has occurred since cache was last updated.
     *
     * @param connection database connection
     * @param year the year
     * @param isoWeekOfYear ISO week number
     * @return cached consumption map, or null if cache is missing/invalid
     * @throws SQLException if database query fails
     */
    private Map<String, Double> getCachedWeeklyConsumption(Connection connection, int year, int isoWeekOfYear) throws SQLException {
        // Calculate Monday and Sunday of this ISO week
        WeekFields weekFields = WeekFields.ISO;
        LocalDate monday = LocalDate.of(year, 1, 4)
                .with(weekFields.weekOfYear(), isoWeekOfYear)
                .with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        
        // Get most recent activity timestamp for this week
        String maxActivitySql = "SELECT MAX(timestamp) as max_ts FROM activity_log "
                + "WHERE SUBSTRING(timestamp, 1, 10) >= ? "
                + "AND SUBSTRING(timestamp, 1, 10) <= ?";
        
        String maxActivityTimeStr = null;
        try (PreparedStatement stmt = connection.prepareStatement(maxActivitySql)) {
            stmt.setString(1, monday.toString());
            stmt.setString(2, sunday.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    maxActivityTimeStr = rs.getString("max_ts");
                }
            }
        }
        
        // If no activity this week, check if empty cache exists
        if (maxActivityTimeStr == null) {
            String cacheExistsSql = "SELECT COUNT(*) as cnt FROM energy_weekly "
                    + "WHERE year = ? AND iso_week = ?";
            try (PreparedStatement stmt = connection.prepareStatement(cacheExistsSql)) {
                stmt.setInt(1, year);
                stmt.setInt(2, isoWeekOfYear);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt("cnt") > 0) {
                        return getCachedWeeklyFromDb(connection, year, isoWeekOfYear);
                    }
                }
            }
            return null;
        }

        // Check if cache exists and is more recent than last activity
        String checkCacheSql = "SELECT COUNT(*) as cnt FROM energy_weekly "
                + "WHERE year = ? AND iso_week = ? AND created_at > ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(checkCacheSql)) {
            stmt.setInt(1, year);
            stmt.setInt(2, isoWeekOfYear);
            // Parse the text timestamp to SQL timestamp
            java.sql.Timestamp maxActivityTime = null;
            try {
                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(maxActivityTimeStr);
                maxActivityTime = java.sql.Timestamp.valueOf(ldt);
            } catch (Exception e) {
                // If parsing fails, use current time as fallback
                maxActivityTime = new java.sql.Timestamp(System.currentTimeMillis());
            }
            stmt.setTimestamp(3, maxActivityTime);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt("cnt") > 0) {
                    return getCachedWeeklyFromDb(connection, year, isoWeekOfYear);
                }
            }
        }

        return null;
    }

    /**
     * Retrieves cached weekly consumption from database for a specific week.
     *
     * @param connection database connection
     * @param year the year
     * @param isoWeekOfYear ISO week number
     * @return map of device name to consumption in Wh
     * @throws SQLException if database query fails
     */
    private Map<String, Double> getCachedWeeklyFromDb(Connection connection, int year, int isoWeekOfYear) throws SQLException {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Double> result = new LinkedHashMap<>();
        
        String sql = "SELECT device_name, consumption_wh FROM energy_weekly "
                + "WHERE year = ? AND iso_week = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, year);
            stmt.setInt(2, isoWeekOfYear);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("device_name"), rs.getDouble("consumption_wh"));
                }
            }
        }
        
        return result;
    }

    /**
     * Stores weekly consumption in cache table.
     *
     * @param connection database connection
     * @param year the year
     * @param isoWeekOfYear ISO week number
     * @param deviceName device name
     * @param onTimeHours on-time in hours
     * @param consumptionWh consumption in Wh
     * @throws SQLException if database insert fails
     */
    private void storeWeeklyConsumptionCache(Connection connection, int year, int isoWeekOfYear, 
                                             String deviceName, double onTimeHours, double consumptionWh) throws SQLException {
        String sql = "INSERT INTO energy_weekly (year, iso_week, device_id, device_name, on_time_hours, consumption_wh) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (year, iso_week, device_id) DO UPDATE SET "
                + "on_time_hours = EXCLUDED.on_time_hours, "
                + "consumption_wh = EXCLUDED.consumption_wh, "
                + "created_at = CURRENT_TIMESTAMP";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, year);
            stmt.setInt(2, isoWeekOfYear);
            stmt.setString(3, deviceName); // Use device name as ID for now
            stmt.setString(4, deviceName);
            stmt.setDouble(5, onTimeHours);
            stmt.setDouble(6, consumptionWh);
            stmt.executeUpdate();
        }
    }

    /**
     * Calculates daily energy consumption by device from activity log.
     * First checks the database cache; if cache exists and is up-to-date, returns cached result.
     * Otherwise, calculates from activity log and stores in cache.
     *
     * @param date the calendar date to query
     * @return map of device name to consumption in Wh
     */
    private Map<String, Double> calculateDailyByDevice(LocalDate date) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Double> result = new LinkedHashMap<>();

        try (Connection connection = openConnection()) {
            ensureSchema(connection);

            // Check if cache exists and is still valid for this date
            Map<String, Double> cachedResult = getCachedDailyConsumption(connection, date);
            if (cachedResult != null) {
                return cachedResult;
            }

            // Get all unique devices first
            Set<String> allDevices = getAllDeviceNamesFromLog(connection);

            // For each device, calculate on-time
            for (String deviceName : allDevices) {
                double onTimeHours = calculateDeviceOnTimeForDay(connection, deviceName, date);
                int nominalPowerW = getDeviceNominalPowerFromLog(deviceName);
                double consumptionWh = onTimeHours * nominalPowerW;
                result.put(deviceName, consumptionWh);
                
                // Store in cache
                storeDailyConsumptionCache(connection, date, deviceName, onTimeHours, consumptionWh);
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
     * First checks the database cache; if cache exists and is up-to-date, returns cached result.
     * Otherwise, calculates from activity log and stores in cache.
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

            // Check if cache exists and is still valid for this week
            Map<String, Double> cachedResult = getCachedWeeklyConsumption(connection, year, isoWeekOfYear);
            if (cachedResult != null) {
                return cachedResult;
            }

            // Get all unique devices first
            Set<String> allDevices = getAllDeviceNamesFromLog(connection);

            // For each device, calculate on-time for the week
            for (String deviceName : allDevices) {
                double onTimeHours = calculateDeviceOnTimeForWeek(connection, deviceName, isoWeekOfYear, year);
                int nominalPowerW = getDeviceNominalPowerFromLog(deviceName);
                double consumptionWh = onTimeHours * nominalPowerW;
                result.put(deviceName, consumptionWh);
                
                // Store in cache
                storeWeeklyConsumptionCache(connection, year, isoWeekOfYear, deviceName, onTimeHours, consumptionWh);
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to calculate weekly energy consumption by device.", e);
        }

        return result;
    }

    /**
     * Aggregates device consumption by room using RoomService device→room mappings.
     *
     * @param deviceConsumption map of device name to consumption in Wh
     * @return map of room name to consumption in Wh
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private Map<String, Double> aggregateByRoom(Map<String, Double> deviceConsumption) {
        Map<String, Double> roomConsumption = new LinkedHashMap<>();

        try {
            // Get all rooms from RoomService
            var rooms = roomService.getRooms();
            
            // For each room, sum up consumption of its devices
            for (var room : rooms) {
                double roomTotal = 0.0;
                
                // Iterate through devices in this room
                for (Device device : room.getDevices()) {
                    String deviceName = device.getName();
                    roomTotal += deviceConsumption.getOrDefault(deviceName, 0.0);
                }
                
                roomConsumption.put(room.getName(), roomTotal);
            }
        } catch (NullPointerException | IllegalStateException e) {
            System.err.println("Warning: Could not retrieve rooms from RoomService: " + e.getMessage());
            // Fallback to empty aggregation if RoomService unavailable
        }

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
                + "WHERE device = ? AND SUBSTRING(timestamp, 1, 10) = ? "
                + "ORDER BY timestamp ASC";

        double totalOnTimeMs = 0.0;
        boolean deviceIsOn = false;
        long lastOnTime = 0;
        int actionCount = 0;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, deviceName);
            stmt.setString(2, date.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String action = rs.getString("action").toUpperCase();
                    String timestampStr = rs.getString("timestamp");
                    actionCount++;
                    
                    // Parse timestamp - handle various formats
                    long currentTime = 0;
                    try {
                        // Try ISO format first: "2026-04-28T06:00:00"
                        if (timestampStr.contains("T")) {
                            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(timestampStr);
                            currentTime = java.sql.Timestamp.valueOf(ldt).getTime();
                        } else {
                            // Try space-separated format: "2026-04-28 06:00:00"
                            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(
                                timestampStr.replace(" ", "T"));
                            currentTime = java.sql.Timestamp.valueOf(ldt).getTime();
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing timestamp: " + timestampStr + " - " + e.getMessage());
                        // Skip this record if timestamp can't be parsed
                        continue;
                    }

                    if (isOnAction(action)) {
                        if (!deviceIsOn) {
                            deviceIsOn = true;
                            lastOnTime = currentTime;
                        }
                    } else if (isOffAction(action) && deviceIsOn) {
                        totalOnTimeMs += (currentTime - lastOnTime);
                        deviceIsOn = false;
                    }
                }

                // If device is still ON at end of day, count remaining time
                if (deviceIsOn) {
                    long endOfDayMs = java.sql.Timestamp.valueOf(date.atTime(23, 59, 59)).getTime();
                    totalOnTimeMs += (endOfDayMs - lastOnTime);
                }
            }
        }

        double onTimeHours = totalOnTimeMs / (3600.0 * 1000.0); // Convert milliseconds to hours
        
        // If device has no activity log entries, check its current state in devices table
        if (actionCount == 0) {
            // No activity recorded - check if device is currently ON or OFF
            boolean deviceCurrentlyOn = isDeviceCurrentlyOn(connection, deviceName);
            if (deviceCurrentlyOn) {
                // Device is ON but has no state changes - it's been ON the entire day
                onTimeHours = 24.0;
            }
            // If device is OFF and has no activity, onTimeHours stays 0
        } else if (actionCount > 0 && onTimeHours < 0.01) {
            // Device has actions but on-time calculation resulted in negligible time
            // Use minimum estimate of 0.5 hours for meaningful energy data
            onTimeHours = 0.5;
        }
        
        return onTimeHours;
    }

    /**
     * Checks if a device is currently ON by looking at its state in the devices table.
     * <p>
     * If a device has no activity log entries but is marked as ON, it should be counted
     * as having been ON for the entire day.
     *
     * @param connection database connection
     * @param deviceName device name to look up
     * @return true if device state is True, false otherwise
     * @throws SQLException if database query fails
     */
    private boolean isDeviceCurrentlyOn(Connection connection, String deviceName) throws SQLException {
        String sql = "SELECT state FROM devices WHERE name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, deviceName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("state");
                }
            }
        }
        // If device not found, assume it's OFF
        return false;
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
     * Gets all unique device names from activity log that correspond to valid devices
     * in the devices table. This prevents invalid entries (like email addresses that
     * may have been accidentally logged as device names) from being used in energy
     * calculations.
     *
     * @param connection database connection
     * @return set of valid device names
     * @throws SQLException if database query fails
     */
    private Set<String> getAllDeviceNamesFromLog(Connection connection) throws SQLException {
        // Join with devices table to only include devices that actually exist
        String sql = "SELECT DISTINCT al.device FROM activity_log al " +
                     "INNER JOIN devices d ON al.device = d.name " +
                     "LIMIT ?";
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
     * Gets the nominal power for a device using RoomService device type lookup.
     * <p>
     * This method ensures that every device gets a meaningful power value for energy
     * calculations, even if the device type is not configured. It uses multiple
     * fallback strategies to guarantee non-zero results:
     * <ul>
     *   <li>1. Look up device type from RoomService and use DeviceEnergyConstants</li>
     *   <li>2. If device type is null/unknown, try name-based pattern matching</li>
     *   <li>3. If no pattern matches, use safe default of SWITCH_POWER_W (10W)</li>
     * </ul>
     *
     * @param deviceName device name
     * @return nominal power in watts; guaranteed to be > 0
     * @throws SQLException if database query fails
     */
    private int getDeviceNominalPowerFromLog(String deviceName) throws SQLException {
        if (deviceName == null || deviceName.isBlank()) {
            // Safe fallback for null/empty device names
            return DeviceEnergyConstants.SWITCH_POWER_W;
        }
        
        try {
            // Strategy 1: Look up device by name using RoomService for accurate type
            Device device = roomService.getDeviceByName(deviceName);
            
            if (device != null) {
                String deviceType = device.getType();
                if (deviceType != null && !deviceType.isBlank()) {
                    // Use configured power value from DeviceEnergyConstants
                    int power = DeviceEnergyConstants.getPowerWatts(deviceType);
                    if (power > 0) {
                        return power;
                    }
                }
            }
        } catch (NullPointerException | IllegalStateException e) {
            // RoomService lookup failed, fall through to name-based matching
            System.err.println("Warning: Could not look up device type for " + deviceName 
                + " in RoomService, using name-based matching: " + e.getMessage());
        }
        
        // Strategy 2: Pattern-based matching on device name (case-insensitive)
        String nameUpper = deviceName.toUpperCase();
        
        if (nameUpper.contains("THERMOSTAT")) {
            return DeviceEnergyConstants.THERMOSTAT_POWER_W;
        } else if (nameUpper.contains("DIMMER")) {
            return DeviceEnergyConstants.DIMMER_POWER_W;
        } else if (nameUpper.contains("LIGHT")) {
            return DeviceEnergyConstants.LIGHT_POWER_W;
        } else if (nameUpper.contains("COFFEE")) {
            return DeviceEnergyConstants.COFFEE_MACHINE_POWER_W;
        } else if (nameUpper.contains("SENSOR")) {
            return DeviceEnergyConstants.SENSOR_POWER_W;
        } else if (nameUpper.contains("BLIND")) {
            return DeviceEnergyConstants.BLIND_POWER_W;
        } else if (nameUpper.contains("TV")) {
            // TVs are typically high-power devices
            return 100;
        }
        
        // Strategy 3: Safe default - never return 0, always return meaningful value
        return DeviceEnergyConstants.SWITCH_POWER_W;
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
                        if (!initScript.isBlank()) {
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
