package at.jku.se.smarthome.service;

import java.time.LocalDate;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.Schedule;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.mock.MockLogService;
import at.jku.se.smarthome.service.mock.MockNotificationService;
import at.jku.se.smarthome.service.mock.MockRoomService;
import at.jku.se.smarthome.service.mock.MockScheduleService;
import at.jku.se.smarthome.service.mock.MockVacationModeService;

public class TestMockVacationModeService {

    /** Vacation mode service under test. */
    private MockVacationModeService service;
    /** Mock schedule service. */
    private MockScheduleService scheduleService;
    /** Mock log service. */
    private MockLogService logService;
    /** Mock notification service. */
    private MockNotificationService notificationService;

    /**
     * Sets up test fixtures before each test.
     */
    @Before
    public void setUp() {
        MockRoomService.resetForTesting();
        MockLogService.resetForTesting();
        MockNotificationService.resetForTesting();
        MockScheduleService.resetForTesting();
        MockVacationModeService.resetForTesting();

        scheduleService = MockScheduleService.getInstance();
        ServiceRegistry.setScheduleServiceForTesting(scheduleService);
        logService = MockLogService.getInstance();
        notificationService = MockNotificationService.getInstance();
        service = MockVacationModeService.getInstance();
    }

    /**
     * Tears down test fixtures after each test.
     */
    @After
    public void tearDown() {
        ServiceRegistry.setScheduleServiceForTesting(scheduleService);
    }

    /**
     * Test: activate vacation mode sets status and override summaries.
     */
    @Test
    public void activateVacationModeSetsStatusAndOverrideSummaries() {
        Schedule selectedSchedule = scheduleService.getScheduleById("sched-001");
        LocalDate start = LocalDate.of(2026, 4, 20);
        LocalDate end = LocalDate.of(2026, 4, 25);

        service.activateVacationMode(start, end, selectedSchedule, "Owner");

        assertTrue(service.isEnabled());
        assertTrue(service.isActiveOn(LocalDate.of(2026, 4, 22)));
        assertEquals(selectedSchedule.getId(), service.getConfiguration().getScheduleId());
        assertNotNull(service.getSelectedSchedule());
        assertTrue(service.getStatusSummary().contains("Active from 2026-04-20 to 2026-04-25 using 'Morning Lights'"));
        assertTrue(service.getOverrideSummary().contains("Morning Lights"));
        assertTrue(notificationService.getNotifications().get(0).getMessage().contains("Vacation mode enabled"));
        assertTrue(logService.getLogs().get(0).getAction().contains("Vacation mode enabled"));
    }

    /**
     * Test: deactivate and clear reset configuration.
     */
    @Test
    public void deactivateAndClearResetConfiguration() {
        Schedule selectedSchedule = scheduleService.getScheduleById("sched-001");
        service.activateVacationMode(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 25), selectedSchedule, "Owner");

        service.deactivateVacationMode("Owner");
        assertFalse(service.isEnabled());
        assertEquals("Vacation mode is currently disabled.", service.getStatusSummary());

        service.activateVacationMode(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 25), selectedSchedule, "Owner");
        service.clearIfUsingSchedule(selectedSchedule.getId(), "Removed selected schedule");
        assertFalse(service.isEnabled());
        assertEquals("warning", notificationService.getNotifications().get(0).getType());
    }
}