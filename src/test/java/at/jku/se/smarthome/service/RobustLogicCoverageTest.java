package at.jku.se.smarthome.service;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import at.jku.se.smarthome.service.mock.*;
import at.jku.se.smarthome.model.*;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Robust unit tests for logic components to maximize coverage.
 * These tests avoid UI and database dependencies.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals"})
public class RobustLogicCoverageTest {

    /**
     * Public constructor for PMD.
     */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public RobustLogicCoverageTest() {
        // Default constructor
    }

    /**
     * Set up mocks before each test.
     */
    @Before
    public void setUp() {
        MockRoomService.resetForTesting();
        MockRuleService.resetForTesting();
        MockScheduleService.resetForTesting();
        MockUserService.resetForTesting();
        MockLogService.resetForTesting();
        MockNotificationService.resetForTesting();
        MockVacationModeService.resetForTesting();
        MockEnergyService.resetForTesting();
        MockSimulationService.resetForTesting();
        MockIoTIntegrationService.resetForTesting();
        MockSmartHomeService.resetForTesting();
        ServiceRegistry.resetForTesting();
    }

    /**
     * Tests the MockEnergyService.
     */
    @Test
    public void testMockEnergyServiceRobust() {
        MockEnergyService service = MockEnergyService.getInstance();
        
        // Snapshot
        assertNotNull(service.getSnapshot(MockEnergyService.AggregationPeriod.DAY));
        assertNotNull(service.getSnapshot(MockEnergyService.AggregationPeriod.WEEK));
        
        // Daily
        LocalDate date = LocalDate.now();
        Map<String, Double> dailyByDevice = service.getDailyByDevice(date);
        assertFalse(dailyByDevice.isEmpty());
        Map<String, Double> dailyByRoom = service.getDailyByRoom(date);
        assertFalse(dailyByRoom.isEmpty());
        assertTrue(service.getHouseholdDaily(date) > 0);
        
        // Weekly
        Map<String, Double> weeklyByDevice = service.getWeeklyByDevice(1, 2026);
        assertFalse(weeklyByDevice.isEmpty());
        Map<String, Double> weeklyByRoom = service.getWeeklyByRoom(1, 2026);
        assertFalse(weeklyByRoom.isEmpty());
        assertTrue(service.getHouseholdWeekly(1, 2026) > 0);
        
        // Misc
        assertEquals(MockEnergyService.AggregationPeriod.DAY.getDisplayName(), "Day");
        assertTrue(service.getAllDeviceTypes().size() > 0);
        assertTrue(service.getDeviceNominalPower("Switch") >= 0);
    }

    /**
     * Tests the MockIoTIntegrationService.
     */
    @Test
    public void testMockIoTIntegrationServiceRobust() {
        MockIoTIntegrationService service = MockIoTIntegrationService.getInstance();
        assertEquals("MQTT", service.getProtocolName());
        
        service.saveConfiguration(true, "broker", 1883, "user", "pass");
        assertTrue(service.isEnabled());
        assertNotNull(service.getConfiguration());
        
        assertTrue(service.testConnection("broker", "1883"));
        assertFalse(service.testConnection("", "0"));
        
        assertTrue(service.connect());
        assertTrue(service.isConnected());
        assertNotNull(service.getLastSync());
        assertNotEquals("Never", service.getLastSync());
        
        assertFalse(service.getDiscoveredDevices().isEmpty());
        assertTrue(service.refreshDevices());
        
        assertTrue(service.publishCommand("topic", "OFF"));
        assertEquals("OFF", service.getDeviceState("topic"));
        
        service.disconnect();
        assertFalse(service.isConnected());
        
        service.saveConfiguration(false, "b", 0, "u", "p");
        assertFalse(service.isEnabled());
    }

    /**
     * Tests the MockSimulationService.
     */
    @Test
    public void testMockSimulationServiceRobust() {
        MockSimulationService service = MockSimulationService.getInstance();
        
        MockSimulationService.SimulationConfiguration config = new MockSimulationService.SimulationConfiguration(
                LocalTime.of(9, 0), 20.0, 50.0, List.of(), 1
        );
        
        MockSimulationService.SimulationPlan plan = service.buildPlan(config);
        assertNotNull(plan);
        assertNotNull(plan.simulatedDeviceStates());
        assertNotNull(plan.events());
        
        service.applyEvent(plan.simulatedDeviceStates(), new MockSimulationService.SimulationEvent(
                LocalTime.of(10, 0), "Living Room Light", "Living Room", "ON", "Manual"
        ));
        
        assertNotNull(service.parseStartTime("12:34:56"));
    }

    /**
     * Tests the MockSmartHomeService.
     */
    @Test
    public void testMockSmartHomeServiceRobust() {
        MockSmartHomeService service = MockSmartHomeService.getInstance();
        assertFalse(service.getDevices().isEmpty());
        
        String deviceId = "dev-001";
        Device device = service.getDeviceById(deviceId);
        boolean initialState = device.getState();
        service.toggleDevice(deviceId);
        assertNotEquals(initialState, device.getState());
        
        service.setBrightness("dev-003", 25);
        assertEquals(25, service.getDeviceById("dev-003").getBrightness());
        
        service.setTemperature("dev-002", 22.0);
        assertEquals(22.0, service.getDeviceById("dev-002").getTemperature(), 0.1);
        
        assertTrue(service.authenticate("me@me.com", "secret"));
        assertEquals("me", service.getCurrentUser());
        service.logout();
        assertEquals("User", service.getCurrentUser());
        
        assertTrue(service.openBlind("dev-006"));
        assertTrue(service.closeBlind("dev-006"));
        assertTrue(service.injectSensorValue("dev-007", 99.0));
    }
}
