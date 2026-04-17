package at.jku.se.smarthome.service.api;
import at.jku.se.smarthome.service.real.auth.JdbcUserService;
import at.jku.se.smarthome.service.real.log.JdbcLogService;
import at.jku.se.smarthome.service.real.room.JdbcRoomService;
import at.jku.se.smarthome.service.real.schedule.JdbcScheduleService;

/**
 * Resolves shared service implementations used across the application.
 */
public final class ServiceRegistry {

    /** Cached schedule service singleton reference. */
    private static ScheduleService scheduleService;
    /** Cached room service singleton reference. */
    private static RoomService roomService;
    /** Cached log service singleton reference. */
    private static LogService logService;
    /** Cached user service singleton reference. */
    private static UserService userService;

    private ServiceRegistry() {
    }

    /**
     * Returns the active schedule service instance.
     *
     * @return lazily initialized schedule service
     */
    public static ScheduleService getScheduleService() {
        synchronized (ServiceRegistry.class) {
            if (scheduleService == null) {
                scheduleService = JdbcScheduleService.getInstance();
            }
            return scheduleService;
        }
    }

    /**
     * Returns the active room service instance.
     *
     * @return lazily initialized room service
     */
    public static RoomService getRoomService() {
        synchronized (ServiceRegistry.class) {
            if (roomService == null) {
                roomService = JdbcRoomService.getInstance();
            }
            return roomService;
        }
    }

    /**
     * Returns the active log service instance.
     *
     * @return lazily initialized log service
     */
    public static LogService getLogService() {
        synchronized (ServiceRegistry.class) {
            if (logService == null) {
                logService = JdbcLogService.getInstance();
            }
            return logService;
        }
    }

    /**
     * Overrides the log service for tests or alternate runtime wiring.
     *
     * @param testLogService replacement log service instance
     */
    public static void setLogServiceForTesting(LogService testLogService) {
        synchronized (ServiceRegistry.class) {
            logService = testLogService;
        }
    }

    /**
     * Overrides the schedule service for tests or alternate runtime wiring.
     *
     * @param testScheduleService replacement schedule service instance
     */
    public static void setScheduleServiceForTesting(ScheduleService testScheduleService) {
        synchronized (ServiceRegistry.class) {
            scheduleService = testScheduleService;
        }
    }

    /**
     * Overrides the room service for tests or alternate runtime wiring.
     * <p>
     * Use this to inject a mock or test-specific RoomService implementation so
     * that UI/controllers pick up the replacement via {@link #getRoomService()}.
     *
     * @param testRoomService replacement room service instance
     */
    public static void setRoomServiceForTesting(RoomService testRoomService) {
        synchronized (ServiceRegistry.class) {
            roomService = testRoomService;
        }
    }

    /**
     * Returns the active user service instance.
     *
     * @return lazily initialized user service
     */
    public static UserService getUserService() {
        synchronized (ServiceRegistry.class) {
            if (userService == null) {
                userService = JdbcUserService.getInstance();
            }
            return userService;
        }
    }

    /**
     * Overrides the user service for tests or alternate runtime wiring.
     *
     * @param testUserService replacement user service instance
     */
    public static void setUserServiceForTesting(UserService testUserService) {
        synchronized (ServiceRegistry.class) {
            userService = testUserService;
        }
    }

    /**
     * Clears the cached schedule service so it is re-created on next access.
     */
    public static void resetForTesting() {
        synchronized (ServiceRegistry.class) {
            scheduleService = JdbcScheduleService.getInstance();
        }
    }

    /**
     * Clears the cached room service so it is re-created on next access.
     *
     * This is intended for test lifecycle management where the singleton
     * service instance must be reset between test cases.
     */
    public static void resetRoomServiceForTesting() {
        synchronized (ServiceRegistry.class) {
            roomService = JdbcRoomService.getInstance();
        }
    }
}