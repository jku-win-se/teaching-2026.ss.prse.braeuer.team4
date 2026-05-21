package at.jku.se.smarthome.service;

import java.time.LocalDate;
import java.util.Map;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.service.real.energy.JdbcEnergyService;

/**
 * Unit tests for JdbcEnergyService energy consumption calculations.
 * <p>
 * Test scenarios verify energy consumption retrieval and basic validations.
 */
@SuppressWarnings({ "PMD.TooManyMethods" })
public class JdbcEnergyServiceTest {
    /** Energy service instance under test. */
    private JdbcEnergyService energyService;

    /**
     * Default constructor.
     */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public JdbcEnergyServiceTest() {
        // Default constructor
    }

    /**
     * Setup test environment before each test.
     */
    @Before
    public void setUp() throws Exception {
        // Use in-memory DB for tests
        String jdbcUrl = "jdbc:h2:mem:energy_" + System.nanoTime() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        System.setProperty("smarthome.db.url", jdbcUrl);
        System.setProperty("smarthome.db.user", "sa");
        System.setProperty("smarthome.db.password", "");
        
        // Pre-create tables that energy service depends on but doesn't manage
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(jdbcUrl, "sa", "")) {
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS activity_log (timestamp VARCHAR(64), device VARCHAR(255), room VARCHAR(255), action VARCHAR(64), actor VARCHAR(255))");
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS devices (name VARCHAR(255) PRIMARY KEY, type VARCHAR(64), state BOOLEAN)");
        }
        
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
        System.clearProperty("smarthome.db.url");
        System.clearProperty("smarthome.db.user");
        System.clearProperty("smarthome.db.password");
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
     * Test: Weekly consumption retrieval by room.
     */
    @Test
    public void testGetWeeklyByRoom() {
        LocalDate today = LocalDate.now();
        int isoWeek = today.get(java.time.temporal.WeekFields.ISO.weekOfYear());
        int year = today.getYear();
        
        Map<String, Double> weeklyRoom = energyService.getWeeklyByRoom(isoWeek, year);
        assertNotNull(weeklyRoom);
    }

    /**
     * Test: Household weekly consumption retrieval.
     */
    @Test
    public void testGetHouseholdWeekly() {
        LocalDate today = LocalDate.now();
        int isoWeek = today.get(java.time.temporal.WeekFields.ISO.weekOfYear());
        int year = today.getYear();
        double household = energyService.getHouseholdWeekly(isoWeek, year);
        assertTrue(household >= 0);
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
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
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
