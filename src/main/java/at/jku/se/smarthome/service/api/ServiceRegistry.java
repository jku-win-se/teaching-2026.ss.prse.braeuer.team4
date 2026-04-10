package at.jku.se.smarthome.service.api;

import at.jku.se.smarthome.service.real.schedule.JdbcScheduleService;

public final class ServiceRegistry {

    private static final ScheduleService SCHEDULE_SERVICE = JdbcScheduleService.getInstance();

    private ServiceRegistry() {
    }

    public static ScheduleService getScheduleService() {
        return SCHEDULE_SERVICE;
    }
}