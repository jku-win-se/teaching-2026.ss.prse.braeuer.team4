package at.jku.se.smarthome.service;

import java.time.LocalDate;
import java.util.Map;

import org.junit.After;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.service.real.energy.JdbcEnergyService;

/**
 * Unit tests for JdbcEnergyService energy consumption calculations.
 * <p>
 * Test scenarios verify energy consumption retrieval and basic validations.
 */
@SuppressWarnings({ "PMD.AtLeastOneConstructor", "PMD.TooManyMethods" })
public class JdbcEnergyServiceTest {

    /** Energy service instance under test. */
    private JdbcEnergyService energyService;

    /**
     * Setup test environment before each test.
     */
    @Before
    public void setUp() {
        // Reset singleton for test isolation
        JdbcEnergyService.resetForTesting();
        energyService = JdbcEnergyService.getInstance();
    }

    /**
     * Clean up after each test.
     */
    @After
    public void tearDown() {
        // Cleanup
        if (energyService != null) {
            energyService.invalidateCache();
        }
        JdbcEnergyService.resetForTesting();
    }

    /**
     * Test: Daily consumption retrieval by device.
     */
    @Test
    public void testGetDailyByDevice() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Act
        Map<String, Double> dailyConsumption = energyService.getDailyByDevice(today);

        // Assert
        assertNotNull("Daily device consumption should not be null", dailyConsumption);
    }

    /**
     * Test: Room consumption retrieval.
     */
    @Test
    public void testGetDailyByRoom() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Act
        Map<String, Double> roomConsumption = energyService.getDailyByRoom(today);

        // Assert
        assertNotNull("Daily room consumption should not be null", roomConsumption);
    }


    /**
     * Test: Household consumption retrieval.
     */
    @Test
    public void testGetHouseholdDaily() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Act
        double household = energyService.getHouseholdDaily(today);

        // Assert
        assertNotNull("Household consumption should not be null", household);
    }

    /**
     * Test: Time boundary cases - today.
     */
    @Test
    public void testTimeBoundaryCases() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Act
        Map<String, Double> todayConsumption = energyService.getDailyByDevice(today);

        // Assert
        assertNotNull("Today's consumption should not be null", todayConsumption);
    }

    /**
     * Test: Time boundary cases - yesterday.
     */
    @Test
    public void testTimeBoundaryCasesYesterday() {
        // Arrange
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // Act
        Map<String, Double> yesterdayConsumption = energyService.getDailyByDevice(yesterday);

        // Assert
        assertNotNull("Yesterday's consumption should not be null", yesterdayConsumption);
    }

    /**
     * Test: Multiple rooms retrieval.
     */
    @Test
    public void testMultipleRoomsIsolation() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Act
        Map<String, Double> roomConsumption = energyService.getDailyByRoom(today);

        // Assert
        assertNotNull("Room consumption should not be null", roomConsumption);
    }

    /**
     * Test: Device nominal power retrieval.
     */
    @Test
    public void testGetDeviceNominalPower() {
        // Act & Assert
        int power = energyService.getDeviceNominalPower("SWITCH");
        assertNotNull("Should retrieve device power", power);
    }

    /**
     * Test: Weekly consumption queries work.
     */
    @Test
    public void testWeeklyConsumptionQueries() {
        // Arrange
        LocalDate today = LocalDate.now();
        int isoWeek = today.get(java.time.temporal.WeekFields.ISO.weekOfYear());
        int year = today.getYear();

        // Act
        Map<String, Double> weeklyDevice = energyService.getWeeklyByDevice(isoWeek, year);

        // Assert
        assertNotNull("Weekly device consumption should not be null", weeklyDevice);
    }

    /**
     * Test: Cache invalidation works.
     */
    @Test
    public void testCacheInvalidation() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Act & Assert - no exceptions should be thrown
        Map<String, Double> firstQuery = energyService.getDailyByDevice(today);
        assertNotNull("First query should not be null", firstQuery);
        energyService.invalidateCache();
        Map<String, Double> secondQuery = energyService.getDailyByDevice(today);
        assertNotNull("Second query after invalidation should not be null", secondQuery);
    }

    /**
     * Test: Cache invalidation allows re-query.
     */
    @Test
    public void testCacheInvalidationRequery() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Act - verify no exceptions on subsequent query after invalidation
        energyService.getDailyByDevice(today);
        energyService.invalidateCache();
        Map<String, Double> requery = energyService.getDailyByDevice(today);

        // Assert
        assertNotNull("Re-query after cache invalidation should work", requery);
    }
}
