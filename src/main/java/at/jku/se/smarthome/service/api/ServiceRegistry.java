package at.jku.se.smarthome.service.api;

import at.jku.se.smarthome.service.real.schedule.JdbcScheduleService;
import at.jku.se.smarthome.service.real.room.JdbcRoomService;

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

    public static synchronized void setRoomServiceForTesting(RoomService testRoomService) {
        roomService = testRoomService;
    }

    /**
     * Clears the cached schedule service so it is re-created on next access.
     */
    public static synchronized void resetForTesting() {
        scheduleService = null;
    }

    public static synchronized void resetRoomServiceForTesting() {
        roomService = null;
    }
}