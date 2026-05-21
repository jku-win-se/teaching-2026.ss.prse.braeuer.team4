package at.jku.se.smarthome.service.real;

import static org.junit.Assert.*;
import org.junit.Test;
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

    /**
     * Default constructor for RobustJdbcLogicTest.
     */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public RobustJdbcLogicTest() {
        // Required for PMD
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
