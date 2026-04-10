package at.jku.se.smarthome.service.api;

import at.jku.se.smarthome.service.real.schedule.JdbcScheduleService;

/**
 * Resolves shared service implementations used across the application.
 */
public final class ServiceRegistry {

    private static volatile ScheduleService scheduleService;

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
     * Overrides the schedule service for tests or alternate runtime wiring.
     *
     * @param testScheduleService replacement schedule service instance
     */
    public static synchronized void setScheduleServiceForTesting(ScheduleService testScheduleService) {
        scheduleService = testScheduleService;
    }

    /**
     * Clears the cached schedule service so it is re-created on next access.
     */
    public static synchronized void resetForTesting() {
        scheduleService = null;
    }
}