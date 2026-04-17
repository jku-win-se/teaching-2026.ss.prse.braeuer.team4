package at.jku.se.smarthome.service.api;
import at.jku.se.smarthome.service.real.auth.JdbcUserService;
import at.jku.se.smarthome.service.real.log.JdbcLogService;
import at.jku.se.smarthome.service.real.room.JdbcRoomService;
import at.jku.se.smarthome.service.real.schedule.JdbcScheduleService;

/**
 * Resolves shared service implementations used across the application.
 */
public final class ServiceRegistry {

    private static ScheduleService scheduleService;
    private static RoomService roomService;
    private static LogService logService;
    private static UserService userService;

    private ServiceRegistry() {
    }

    /**
     * Returns the active schedule service instance.
     *
     * @return lazily initialized schedule service
     */
    public static synchronized ScheduleService getScheduleService() {
        if (scheduleService == null) {
            scheduleService = JdbcScheduleService.getInstance();
        }
        return scheduleService;
    }

    /**
     * Returns the active room service instance.
     *
     * @return lazily initialized room service
     */
    public static synchronized RoomService getRoomService() {
        if (roomService == null) {
            roomService = JdbcRoomService.getInstance();
        }
        return roomService;
    }

    /**
     * Returns the active log service instance.
     *
     * @return lazily initialized log service
     */
    public static synchronized LogService getLogService() {
        if (logService == null) {
            logService = JdbcLogService.getInstance();
        }
        return logService;
    }

    /**
     * Overrides the log service for tests or alternate runtime wiring.
     *
     * @param testLogService replacement log service instance
     */
    public static synchronized void setLogServiceForTesting(LogService testLogService) {
        logService = testLogService;
    }

    /**
     * Overrides the schedule service for tests or alternate runtime wiring.
     *
     * @param testScheduleService replacement schedule service instance
     */
    public static synchronized void setScheduleServiceForTesting(ScheduleService testScheduleService) {
        scheduleService = testScheduleService;
    }

    /**
     * Overrides the room service for tests or alternate runtime wiring.
     * <p>
     * Use this to inject a mock or test-specific RoomService implementation so
     * that UI/controllers pick up the replacement via {@link #getRoomService()}.
     *
     * @param testRoomService replacement room service instance
     */
    public static synchronized void setRoomServiceForTesting(RoomService testRoomService) {
        roomService = testRoomService;
    }

    /**
     * Returns the active user service instance.
     *
     * @return lazily initialized user service
     */
    public static synchronized UserService getUserService() {
        if (userService == null) {
            userService = JdbcUserService.getInstance();
        }
        return userService;
    }

    /**
     * Overrides the user service for tests or alternate runtime wiring.
     *
     * @param testUserService replacement user service instance
     */
    public static synchronized void setUserServiceForTesting(UserService testUserService) {
        userService = testUserService;
    }

    /**
     * Clears the cached schedule service so it is re-created on next access.
     */
    public static synchronized void resetForTesting() {
        scheduleService = null;
    }

    /**
     * Clears the cached room service so it is re-created on next access.
     *
     * This is intended for test lifecycle management where the singleton
     * service instance must be reset between test cases.
     */
    public static synchronized void resetRoomServiceForTesting() {
        roomService = null;
    }
}