package at.jku.se.smarthome.service.api;
import at.jku.se.smarthome.service.real.auth.JdbcUserService;
import at.jku.se.smarthome.service.real.log.JdbcLogService;
import at.jku.se.smarthome.service.real.room.JdbcRoomService;
import at.jku.se.smarthome.service.real.schedule.JdbcScheduleService;

/**
 * Resolves shared service implementations used across the application.
 * Uses the initialization-on-demand holder pattern for thread-safe lazy initialization.
 */
public final class ServiceRegistry {

    private static LogService testLogServiceOverride;
    private static ScheduleService testScheduleServiceOverride;
    private static RoomService testRoomServiceOverride;
    private static UserService testUserServiceOverride;

    private ServiceRegistry() {
    }

    /**
     * Returns the active schedule service instance.
     *
     * @return lazily initialized schedule service
     */
    public static ScheduleService getScheduleService() {
        if (testScheduleServiceOverride != null) {
            return testScheduleServiceOverride;
        }
        return ScheduleServiceHolder.INSTANCE;
    }

    private static final class ScheduleServiceHolder {
        private static final ScheduleService INSTANCE = JdbcScheduleService.getInstance();
    }

    /**
     * Returns the active room service instance.
     *
     * @return lazily initialized room service
     */
    public static RoomService getRoomService() {
        if (testRoomServiceOverride != null) {
            return testRoomServiceOverride;
        }
        return RoomServiceHolder.INSTANCE;
    }

    private static final class RoomServiceHolder {
        private static final RoomService INSTANCE = JdbcRoomService.getInstance();
    }

    /**
     * Returns the active log service instance.
     *
     * @return lazily initialized log service (or test override if set)
     */
    public static LogService getLogService() {
        if (testLogServiceOverride != null) {
            return testLogServiceOverride;
        }
        return LogServiceHolder.INSTANCE;
    }

    private static final class LogServiceHolder {
        private static final LogService INSTANCE = JdbcLogService.getInstance();
    }

    /**
     * Overrides the log service for tests or alternate runtime wiring.
     *
     * @param testLogService replacement log service instance
     */
    public static synchronized void setLogServiceForTesting(LogService testLogService) {
        testLogServiceOverride = testLogService;
    }

    /**
     * Overrides the schedule service for tests or alternate runtime wiring.
     *
     * @param testScheduleService replacement schedule service instance
     */
    public static synchronized void setScheduleServiceForTesting(ScheduleService testScheduleService) {
        testScheduleServiceOverride = testScheduleService;
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
        testRoomServiceOverride = testRoomService;
    }

    /**
     * Returns the active user service instance.
     *
     * @return lazily initialized user service
     */
    public static UserService getUserService() {
        if (testUserServiceOverride != null) {
            return testUserServiceOverride;
        }
        return UserServiceHolder.INSTANCE;
    }

    private static final class UserServiceHolder {
        private static final UserService INSTANCE = JdbcUserService.getInstance();
    }

    /**
     * Overrides the user service for tests or alternate runtime wiring.
     *
     * @param testUserService replacement user service instance
     */
    public static synchronized void setUserServiceForTesting(UserService testUserService) {
        testUserServiceOverride = testUserService;
    }

    /**
     * Clears the cached schedule service so it is re-created on next access.
     */
    public static synchronized void resetForTesting() {
        testScheduleServiceOverride = null;
    }

    /**
     * Clears the cached room service so it is re-created on next access.
     *
     * This is intended for test lifecycle management where the singleton
     * service instance must be reset between test cases.
     */
    public static synchronized void resetRoomServiceForTesting() {
        testRoomServiceOverride = null;
    }
}