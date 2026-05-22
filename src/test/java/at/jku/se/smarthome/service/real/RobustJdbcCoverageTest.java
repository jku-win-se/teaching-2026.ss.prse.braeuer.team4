package at.jku.se.smarthome.service.real;

import org.junit.After;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.mock.MockEnergyService;
import at.jku.se.smarthome.service.mock.MockLogService;
import at.jku.se.smarthome.service.mock.MockNotificationService;
import at.jku.se.smarthome.service.mock.MockRoomService;
import at.jku.se.smarthome.service.mock.MockRuleService;
import at.jku.se.smarthome.service.mock.MockSceneService;
import at.jku.se.smarthome.service.mock.MockScheduleService;
import at.jku.se.smarthome.service.mock.MockUserService;
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

    /** JDBC URL property. */
    private static final String URL_PROPERTY = "smarthome.db.url";
    /** JDBC user property. */
    private static final String USER_PROPERTY = "smarthome.db.user";
    /** JDBC password property. */
    private static final String PASSWORD_PROPERTY = "smarthome.db.password";

    /**
     * Default constructor for RobustJdbcCoverageTest.
     */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public RobustJdbcCoverageTest() {
        // Required for PMD
    }

    /**
     * Prepare a clean H2-backed registry setup before each test.
     */
    @Before
    public void setUp() {
        String jdbcUrl = "jdbc:h2:mem:robust_jdbc_" + System.nanoTime()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        System.setProperty(URL_PROPERTY, jdbcUrl);
        System.setProperty(USER_PROPERTY, "sa");
        System.setProperty(PASSWORD_PROPERTY, "");

        ServiceRegistry.resetForTesting();
        JdbcUserService.resetForTesting();
        JdbcRoomService.resetForTesting();
        JdbcRuleService.resetForTesting();
        JdbcLogService.resetForTesting();
        JdbcEnergyService.resetForTesting();
        JdbcScheduleService.resetForTesting();
        JdbcNotificationService.resetForTesting();
        JdbcSceneService.resetForTesting();

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
        ServiceRegistry.setRuleServiceForTesting(MockRuleService.getInstance());
        ServiceRegistry.setLogServiceForTesting(MockLogService.getInstance());
        ServiceRegistry.setScheduleServiceForTesting(MockScheduleService.getInstance());
        ServiceRegistry.setNotificationServiceForTesting(MockNotificationService.getInstance());
        ServiceRegistry.setEnergyServiceForTesting(MockEnergyService.getInstance());
        ServiceRegistry.setSceneServiceForTesting(MockSceneService.getInstance());
    }

    /**
     * Reset database configuration after each test.
     */
    @After
    public void tearDown() {
        ServiceRegistry.resetForTesting();
        JdbcUserService.resetForTesting();
        JdbcRoomService.resetForTesting();
        JdbcRuleService.resetForTesting();
        JdbcLogService.resetForTesting();
        JdbcEnergyService.resetForTesting();
        JdbcScheduleService.resetForTesting();
        JdbcNotificationService.resetForTesting();
        JdbcSceneService.resetForTesting();

        System.clearProperty(URL_PROPERTY);
        System.clearProperty(USER_PROPERTY);
        System.clearProperty(PASSWORD_PROPERTY);
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
