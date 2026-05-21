package at.jku.se.smarthome.service.real;

import static org.junit.Assert.*;
import org.junit.Test;
import at.jku.se.smarthome.service.real.auth.JdbcUserService;
import at.jku.se.smarthome.service.real.energy.JdbcEnergyService;
import at.jku.se.smarthome.service.real.log.JdbcLogService;
import at.jku.se.smarthome.service.real.notification.JdbcNotificationService;
import at.jku.se.smarthome.service.real.room.JdbcRoomService;
import at.jku.se.smarthome.service.real.rule.JdbcRuleService;
import at.jku.se.smarthome.service.real.scene.JdbcSceneService;
import at.jku.se.smarthome.service.real.schedule.JdbcScheduleService;

/**
 * Robust unit tests for JDBC services to exercise re-initialization and instance logic.
 * Avoids deep DB operations if possible to maintain stability.
 */
public class RobustJdbcCoverageTest {

    /**
     * Default constructor for RobustJdbcCoverageTest.
     */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public RobustJdbcCoverageTest() {
        // Required for PMD
    }

    /**
     * Tests that all JDBC service instances can be retrieved.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testJdbcServiceInstances() {
        // Just exercise instance logic and basic constructors
        assertNotNull(JdbcUserService.getInstance());
        assertNotNull(JdbcRoomService.getInstance());
        assertNotNull(JdbcRuleService.getInstance());
        assertNotNull(JdbcLogService.getInstance());
        assertNotNull(JdbcEnergyService.getInstance());
        assertNotNull(JdbcScheduleService.getInstance());
        assertNotNull(JdbcNotificationService.getInstance());
        assertNotNull(JdbcSceneService.getInstance());
    }
}
