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

/**
 * Exhaustive unit tests for Mock services and logic components to hit coverage goal.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals"})
public class ExhaustiveLogicCoverageTest {

    /**
     * Default constructor.
     */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public ExhaustiveLogicCoverageTest() {
        // Default constructor
    }

    /**
     * Set up tests.
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
     * Test mock energy service exhaustive.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testMockEnergyServiceExhaustive() {
        MockEnergyService service = MockEnergyService.getInstance();
        assertNotNull(service.getSnapshot(MockEnergyService.AggregationPeriod.DAY));
        assertNotNull(service.getSnapshot(MockEnergyService.AggregationPeriod.WEEK));
        
        assertNotNull(service.getDailyByDevice(LocalDate.now()));
        assertNotNull(service.getWeeklyByDevice(1, 2026));
        assertNotNull(service.getDailyByRoom(LocalDate.now()));
        assertNotNull(service.getWeeklyByRoom(1, 2026));
        
        assertTrue(service.getHouseholdDaily(LocalDate.now()) > 0);
        assertTrue(service.getHouseholdWeekly(1, 2026) > 0);
        
        assertTrue(service.getDeviceNominalPower("Switch") >= 0);
        assertFalse(service.getAllDeviceTypes().isEmpty());
    }

    /**
     * Test mock IoT integration service exhaustive.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testMockIoTIntegrationServiceExhaustive() {
        MockIoTIntegrationService service = MockIoTIntegrationService.getInstance();
        assertEquals("MQTT", service.getProtocolName());
        
        service.saveConfiguration(true, "broker", 1883, "user", "pass");
        assertTrue(service.isEnabled());
        
        assertTrue(service.testConnection("broker", "1883"));
        assertFalse(service.testConnection("", "abc"));
        
        assertTrue(service.connect());
        assertTrue(service.isConnected());
        
        assertFalse(service.getDiscoveredDevices().isEmpty());
        assertTrue(service.refreshDevices());
        
        assertTrue(service.publishCommand("dev1", "ON"));
        assertEquals("ON", service.getDeviceState("dev1"));
        
        service.disconnect();
        assertFalse(service.isConnected());
    }

    /**
     * Test mock simulation service exhaustive.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testMockSimulationServiceExhaustive() {
        MockSimulationService service = MockSimulationService.getInstance();
        
        MockSimulationService.SimulationConfiguration config = new MockSimulationService.SimulationConfiguration(
                LocalTime.of(8, 0), 22.5, 45.0, List.of(), 1
        );
        
        MockSimulationService.SimulationPlan plan = service.buildPlan(config);
        assertNotNull(plan);
        assertFalse(plan.simulatedDeviceStates().isEmpty());
        
        if (!plan.events().isEmpty()) {
            service.applyEvent(plan.simulatedDeviceStates(), plan.events().get(0));
        }
        
        assertNotNull(service.parseStartTime("08:00"));
        assertNotNull(service.parseStartTime("08:00:00"));
    }

    /**
     * Test mock smart home service exhaustive.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testMockSmartHomeServiceExhaustive() {
        MockSmartHomeService service = MockSmartHomeService.getInstance();
        assertFalse(service.getDevices().isEmpty());
        
        Device device = service.getDevices().get(0);
        assertTrue(service.toggleDevice(device.getId()));
        
        // Dimmer
        service.setBrightness("dev-003", 50);
        assertEquals(50, service.getDeviceById("dev-003").getBrightness());
        
        // Thermostat
        service.setTemperature("dev-002", 23.5);
        assertEquals(23.5, service.getDeviceById("dev-002").getTemperature(), 0.1);
        
        assertTrue(service.authenticate("admin@test.com", "pass"));
        assertEquals("admin", service.getCurrentUser());
        service.logout();
        
        assertTrue(service.openBlind("dev-006"));
        assertTrue(service.closeBlind("dev-006"));
        assertTrue(service.injectSensorValue("dev-007", 1.0));
    }
}
