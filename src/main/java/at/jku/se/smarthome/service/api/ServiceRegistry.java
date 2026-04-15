package at.jku.se.smarthome.service.api;

import at.jku.se.smarthome.service.real.room.JdbcRoomService;
import at.jku.se.smarthome.service.real.schedule.JdbcScheduleService;

/**
 * Resolves shared service implementations used across the application.
 */
public final class ServiceRegistry {

    private static volatile ScheduleService scheduleService;
    private static volatile RoomService roomService;

    private ServiceRegistry() {
    }

    /**
     * Returns the active schedule service instance.
     *
     * @return lazily initialized schedule service
     */
    public static ScheduleService getScheduleService() {
        if (scheduleService == null) {
            synchronized (ServiceRegistry.class) {
                if (scheduleService == null) {
                    scheduleService = JdbcScheduleService.getInstance();
                }
            }
        }
        return scheduleService;
    }

    /**
     * Returns the active room service instance.
     *
     * @return lazily initialized room service
     */
    public static RoomService getRoomService() {
        if (roomService == null) {
            synchronized (ServiceRegistry.class) {
                if (roomService == null) {
                    roomService = JdbcRoomService.getInstance();
                }
            }
        }
        return roomService;
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