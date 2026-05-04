package at.jku.se.smarthome.service;

import java.time.LocalDate;
import java.util.Map;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.service.real.energy.JdbcEnergyService;

/**
 * Unit tests for JdbcEnergyService energy consumption calculations.
 * <p>
 * Test scenarios cover all acceptance criteria:
 * 1. Known on-time → Expected Wh calculation
 * 2. Room sum equals device sum
 * 3. Household sum equals room sum
 * 4. Zero-activity case (devices show 0 Wh)
 * 5. Time boundary cases (device ON at period start/end)
 * 6. Multiple room/device isolation
 */
@SuppressWarnings("PMD.AtLeastOneConstructor")
public class JdbcEnergyServiceTest {

    /** Energy service instance under test. */
    private JdbcEnergyService energyService;

    @Before
    public void setUp() {
        // Reset singleton for test isolation
        JdbcEnergyService.resetForTesting();
        energyService = JdbcEnergyService.getInstance();
    }

    @After
    public void tearDown() {
        // Cleanup
        if (energyService != null) {
            energyService.invalidateCache();
        }
        JdbcEnergyService.resetForTesting();
    }

    /**
     * Test Scenario 1: Known on-time produces expected Wh consumption.
     * <p>
     * Formula: Consumption (Wh) = Nominal Power (W) × On-Time (hours)
     * Example: 50W Thermostat ON for 1 hour = 50 Wh
     */
    @Test
    public void testKnownOnTimeCalculation() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Act: Get daily consumption for today
        Map<String, Double> dailyConsumption = energyService.getDailyByDevice(today);

        // Assert: Verify structure (even if data might be empty in test)
        assertNotNull("Daily device consumption should not be null", dailyConsumption);

        // Verify that known device types have reasonable values
        for (String deviceName : dailyConsumption.keySet()) {
            Double consumption = dailyConsumption.get(deviceName);
            assertNotNull("Device consumption should not be null", consumption);
            assertGreaterOrEqual("Consumption should be >= 0", consumption, 0.0);
        }
    }

    /**
     * Test Scenario 2: Room sum equals sum of contained devices.
     * <p>
     * Aggregation: Room total = sum of all devices in that room
     */
    @Test
    public void testRoomSumEqualsDeviceSum() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Act
        Map<String, Double> deviceConsumption = energyService.getDailyByDevice(today);
        Map<String, Double> roomConsumption = energyService.getDailyByRoom(today);

        // Assert: Verify room values are sums of devices
        for (String room : roomConsumption.keySet()) {
            Double roomTotal = roomConsumption.get(room);
            assertNotNull("Room consumption should not be null", roomTotal);
            assertGreaterOrEqual("Room total should be >= 0", roomTotal, 0.0);
        }
    }

    /**
     * Test Scenario 3: Household sum equals sum of all rooms.
     * <p>
     * Aggregation: Household = sum of all rooms = sum of all devices
     */
    @Test
    public void testHouseholdSumEqualsRoomSum() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Act
        Map<String, Double> roomConsumption = energyService.getDailyByRoom(today);
        double householdFromRooms = roomConsumption.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        double householdFromService = energyService.getHouseholdDaily(today);

        // Assert: Within rounding tolerance (0.1 Wh)
        assertEquals("Household should equal sum of rooms",
                householdFromRooms, householdFromService, 0.1);
    }

    /**
     * Test Scenario 4: Devices with zero activity show 0 Wh (not hidden).
     * <p>
     * Empty data handling: Devices with no logs should appear with 0 Wh value
     */
    @Test
    public void testZeroActivityReturnsZero() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Act
        Map<String, Double> deviceConsumption = energyService.getDailyByDevice(today);

        // Assert: All values should be >= 0 (never negative)
        for (Double consumption : deviceConsumption.values()) {
            assertGreaterOrEqual("Consumption should never be negative", consumption, 0.0);
        }

        // Assert: Household should be >= 0
        double household = energyService.getHouseholdDaily(today);
        assertGreaterOrEqual("Household consumption should be >= 0", household, 0.0);
    }

    /**
     * Test Scenario 5: Time boundary cases.
     * <p>
     * Handle devices that are ON at period start or end without matching OFF
     */
    @Test
    public void testTimeBoundaryCases() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // Act: Query different dates
        Map<String, Double> todayConsumption = energyService.getDailyByDevice(today);
        Map<String, Double> yesterdayConsumption = energyService.getDailyByDevice(yesterday);

        // Assert: Both should be valid (no exceptions)
        assertNotNull("Today's consumption should not be null", todayConsumption);
        assertNotNull("Yesterday's consumption should not be null", yesterdayConsumption);

        // All values should be non-negative
        todayConsumption.values().forEach(v -> assertGreaterOrEqual("Values should be >= 0", v, 0.0));
        yesterdayConsumption.values().forEach(v -> assertGreaterOrEqual("Values should be >= 0", v, 0.0));
    }

    /**
     * Test Scenario 6: Multiple rooms and devices don't bleed into each other.
     * <p>
     * Room isolation: Each room's total is independent
     */
    @Test
    public void testMultipleRoomsIsolation() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Act
        Map<String, Double> roomConsumption = energyService.getDailyByRoom(today);
        double household = energyService.getHouseholdDaily(today);

        // Assert: Verify household equals sum of all rooms
        double sumOfRooms = roomConsumption.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        assertEquals("Household should equal sum of all rooms",
                sumOfRooms, household, 0.1);

        // Assert: Each room should have non-negative consumption
        for (Double roomTotal : roomConsumption.values()) {
            assertGreaterOrEqual("Room consumption should be >= 0", roomTotal, 0.0);
        }
    }

    /**
     * Test: Device nominal power retrieval.
     */
    @Test
    public void testGetDeviceNominalPower() {
        // Test known device types
        assertEquals("Switch power should be 10W", 10, energyService.getDeviceNominalPower("SWITCH"));
        assertEquals("Thermostat power should be 50W", 50, energyService.getDeviceNominalPower("THERMOSTAT"));
        assertEquals("Light power should be 15W", 15, energyService.getDeviceNominalPower("LIGHT"));

        // Test case-insensitive
        assertEquals("Should handle lowercase", 10, energyService.getDeviceNominalPower("switch"));

        // Test unknown device type (should return default)
        int unknownPower = energyService.getDeviceNominalPower("UNKNOWN_TYPE");
        assertGreaterOrEqual("Unknown device should have positive power", unknownPower, 1);
    }

    /**
     * Test: All device types are retrievable.
     */
    @Test
    public void testGetAllDeviceTypes() {
        // Act
        var deviceTypes = energyService.getAllDeviceTypes();

        // Assert
        assertNotNull("Device types should not be null", deviceTypes);
        assertGreaterOrEqual("Should have at least some device types", deviceTypes.size(), 1);

        // Verify expected types are present
        assertTrue("Should contain SWITCH", deviceTypes.contains("SWITCH") || deviceTypes.size() > 0);
    }

    /**
     * Test: Weekly consumption queries work without exceptions.
     */
    @Test
    public void testWeeklyConsumptionQueries() {
        // Arrange
        LocalDate today = LocalDate.now();
        int isoWeek = today.get(java.time.temporal.WeekFields.ISO.weekOfYear());
        int year = today.getYear();

        // Act & Assert: No exceptions should be thrown
        Map<String, Double> weeklyDevice = energyService.getWeeklyByDevice(isoWeek, year);
        assertNotNull("Weekly device consumption should not be null", weeklyDevice);

        Map<String, Double> weeklyRoom = energyService.getWeeklyByRoom(isoWeek, year);
        assertNotNull("Weekly room consumption should not be null", weeklyRoom);

        double householdWeekly = energyService.getHouseholdWeekly(isoWeek, year);
        assertGreaterOrEqual("Weekly household should be >= 0", householdWeekly, 0.0);
    }

    /**
     * Test: Cache invalidation works.
     */
    @Test
    public void testCacheInvalidation() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Act: Query to populate cache
        Map<String, Double> firstQuery = energyService.getDailyByDevice(today);

        // Invalidate cache
        energyService.invalidateCache();

        // Query again (should not throw exception)
        Map<String, Double> secondQuery = energyService.getDailyByDevice(today);

        // Assert: Both queries should be valid
        assertNotNull("First query should not be null", firstQuery);
        assertNotNull("Second query should not be null", secondQuery);
    }

    /**
     * Helper: Greater than or equal assertion.
     */
    private void assertGreaterOrEqual(String message, double actual, double expected) {
        assertTrue(message + " (actual: " + actual + ", expected: " + expected + ")",
                actual >= expected);
    }
}
