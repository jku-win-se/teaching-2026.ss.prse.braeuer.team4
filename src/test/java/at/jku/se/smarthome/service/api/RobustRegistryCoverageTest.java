package at.jku.se.smarthome.service.api;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import at.jku.se.smarthome.service.mock.*;

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
        ServiceRegistry.setUserServiceForTesting(MockUserService.getInstance());
        assertEquals(MockUserService.getInstance(), ServiceRegistry.getUserService());
        
        ServiceRegistry.setRoomServiceForTesting(MockRoomService.getInstance());
        assertEquals(MockRoomService.getInstance(), ServiceRegistry.getRoomService());

        ServiceRegistry.resetForTesting();
    }
}
