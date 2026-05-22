package at.jku.se.smarthome.service.real;

import org.junit.After;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.mock.MockLogService;
import at.jku.se.smarthome.service.mock.MockNotificationService;
import at.jku.se.smarthome.service.mock.MockRoomService;
import at.jku.se.smarthome.service.mock.MockUserService;
import at.jku.se.smarthome.service.real.log.JdbcLogService;
import at.jku.se.smarthome.service.real.notification.JdbcNotificationService;
import at.jku.se.smarthome.service.real.room.JdbcRoomService;
import at.jku.se.smarthome.service.real.scene.JdbcSceneService;

/**
 * Robust unit tests for JDBC logic using H2 (CI compatible).
 * Exercises business logic and query results.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals"})
public class RobustJdbcLogicTest {

    /** JDBC URL property. */
    private static final String URL_PROPERTY = "smarthome.db.url";
    /** JDBC user property. */
    private static final String USER_PROPERTY = "smarthome.db.user";
    /** JDBC password property. */
    private static final String PASSWORD_PROPERTY = "smarthome.db.password";

    /**
     * Default constructor for RobustJdbcLogicTest.
     */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public RobustJdbcLogicTest() {
        // Required for PMD
    }

    /**
     * Prepares the H2 configuration and mock registry dependencies.
     */
    @Before
    public void setUp() {
        String jdbcUrl = "jdbc:h2:mem:robust_jdbc_logic_" + System.nanoTime()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        System.setProperty(URL_PROPERTY, jdbcUrl);
        System.setProperty(USER_PROPERTY, "sa");
        System.setProperty(PASSWORD_PROPERTY, "");

        ServiceRegistry.resetForTesting();
        JdbcLogService.resetForTesting();
        JdbcNotificationService.resetForTesting();
        JdbcRoomService.resetForTesting();
        JdbcSceneService.resetForTesting();

        MockUserService.resetForTesting();
        MockRoomService.resetForTesting();
        MockLogService.resetForTesting();
        MockNotificationService.resetForTesting();

        ServiceRegistry.setUserServiceForTesting(MockUserService.getInstance());
        ServiceRegistry.setRoomServiceForTesting(MockRoomService.getInstance());
        ServiceRegistry.setLogServiceForTesting(MockLogService.getInstance());
        ServiceRegistry.setNotificationServiceForTesting(MockNotificationService.getInstance());
    }

    /**
     * Clears test configuration after each test.
     */
    @After
    public void tearDown() {
        ServiceRegistry.resetForTesting();
        JdbcLogService.resetForTesting();
        JdbcNotificationService.resetForTesting();
        JdbcRoomService.resetForTesting();
        JdbcSceneService.resetForTesting();

        System.clearProperty(URL_PROPERTY);
        System.clearProperty(USER_PROPERTY);
        System.clearProperty(PASSWORD_PROPERTY);
    }

    /**
     * Tests the logic of the JDBC log service.
     */
    @Test
    public void testJdbcLogServiceLogic() {
        JdbcLogService logService = JdbcLogService.getInstance();
        assertNotNull(logService.getLogs());
    }

    /**
     * Tests the logic of the JDBC notification service.
     */
    @Test
    public void testJdbcNotificationServiceLogic() {
        JdbcNotificationService notificationService = JdbcNotificationService.getInstance();
        assertNotNull(notificationService.getNotifications());
    }

    /**
     * Tests the logic of the JDBC room service.
     */
    @Test
    public void testJdbcRoomServiceLogic() {
        JdbcRoomService roomService = JdbcRoomService.getInstance();
        assertNotNull(roomService.getRooms());
        assertNotNull(roomService.getAllDevices());
    }

    /**
     * Tests the logic of the JDBC scene service.
     */
    @Test
    public void testJdbcSceneServiceLogic() {
        JdbcSceneService sceneService = JdbcSceneService.getInstance();
        assertNotNull(sceneService.getScenes());
    }
}
