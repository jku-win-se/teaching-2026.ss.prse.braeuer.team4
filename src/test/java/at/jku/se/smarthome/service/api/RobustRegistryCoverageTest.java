package at.jku.se.smarthome.service.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.service.mock.MockEnergyService;
import at.jku.se.smarthome.service.mock.MockLogService;
import at.jku.se.smarthome.service.mock.MockNotificationService;
import at.jku.se.smarthome.service.mock.MockRoomService;
import at.jku.se.smarthome.service.mock.MockRuleService;
import at.jku.se.smarthome.service.mock.MockSceneService;
import at.jku.se.smarthome.service.mock.MockScheduleService;
import at.jku.se.smarthome.service.mock.MockUserService;

/**
 * Robust unit tests for ServiceRegistry.
 */
public class RobustRegistryCoverageTest {

    /**
     * Default constructor for RobustRegistryCoverageTest.
     */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public RobustRegistryCoverageTest() {
        // Required for PMD
    }

    /**
     * Set up tests.
     */
    @Before
    public void setUp() {
        ServiceRegistry.resetForTesting();
        MockUserService.resetForTesting();
        MockRoomService.resetForTesting();
        MockRuleService.resetForTesting();
        MockLogService.resetForTesting();
        MockScheduleService.resetForTesting();
        MockNotificationService.resetForTesting();
        MockEnergyService.resetForTesting();
        MockSceneService.resetForTesting();

        ServiceRegistry.setUserServiceForTesting(MockUserService.getInstance());
        ServiceRegistry.setRoomServiceForTesting(MockRoomService.getInstance());
        ServiceRegistry.setLogServiceForTesting(MockLogService.getInstance());
        ServiceRegistry.setNotificationServiceForTesting(MockNotificationService.getInstance());
        ServiceRegistry.setRuleServiceForTesting(MockRuleService.getInstance());
        ServiceRegistry.setScheduleServiceForTesting(MockScheduleService.getInstance());
        ServiceRegistry.setEnergyServiceForTesting(MockEnergyService.getInstance());
        ServiceRegistry.setSceneServiceForTesting(MockSceneService.getInstance());
    }

    /**
     * Tests registry accessors.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testRegistryAccessors() {
        // Exercise all holders and holders' static initializers
        assertNotNull(ServiceRegistry.getUserService());
        assertNotNull(ServiceRegistry.getRoomService());
        assertNotNull(ServiceRegistry.getRuleService());
        assertNotNull(ServiceRegistry.getLogService());
        assertNotNull(ServiceRegistry.getEnergyService());
        assertNotNull(ServiceRegistry.getScheduleService());
        assertNotNull(ServiceRegistry.getNotificationService());
        assertNotNull(ServiceRegistry.getSceneService());
        
        // Exercise overrides
        assertEquals(MockUserService.getInstance(), ServiceRegistry.getUserService());
        assertEquals(MockRoomService.getInstance(), ServiceRegistry.getRoomService());

        ServiceRegistry.resetForTesting();
    }
}
