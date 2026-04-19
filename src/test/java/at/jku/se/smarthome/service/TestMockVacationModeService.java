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

/**
 * Unit tests for MockVacationModeService.
 */
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
     * Test: activate vacation mode enables service.
     */
    @Test
    public void activateVacationModeEnablesService() {
        Schedule selectedSchedule = scheduleService.getScheduleById("sched-001");
        LocalDate start = LocalDate.of(2026, 4, 20);
        LocalDate end = LocalDate.of(2026, 4, 25);
        service.activateVacationMode(start, end, selectedSchedule, "Owner");
        assertTrue(service.isEnabled());
    }

    /**
     * Test: activate vacation mode is active on date.
     */
    @Test
    public void activateVacationModeIsActiveOnDate() {
        Schedule selectedSchedule = scheduleService.getScheduleById("sched-001");
        LocalDate start = LocalDate.of(2026, 4, 20);
        LocalDate end = LocalDate.of(2026, 4, 25);
        service.activateVacationMode(start, end, selectedSchedule, "Owner");
        assertTrue(service.isActiveOn(LocalDate.of(2026, 4, 22)));
    }

    /**
     * Test: activate vacation mode sets schedule ID.
     */
    @Test
    public void activateVacationModeSetsScheduleId() {
        Schedule selectedSchedule = scheduleService.getScheduleById("sched-001");
        LocalDate start = LocalDate.of(2026, 4, 20);
        LocalDate end = LocalDate.of(2026, 4, 25);
        service.activateVacationMode(start, end, selectedSchedule, "Owner");
        assertEquals(selectedSchedule.getId(), service.getConfiguration().getScheduleId());
    }

    /**
     * Test: activate vacation mode sets selected schedule.
     */
    @Test
    public void activateVacationModeSetsSelectedSchedule() {
        Schedule selectedSchedule = scheduleService.getScheduleById("sched-001");
        LocalDate start = LocalDate.of(2026, 4, 20);
        LocalDate end = LocalDate.of(2026, 4, 25);
        service.activateVacationMode(start, end, selectedSchedule, "Owner");
        assertNotNull(service.getSelectedSchedule());
    }

    /**
     * Test: activate vacation mode sets status summary.
     */
    @Test
    public void activateVacationModeSetsStatusSummary() {
        Schedule selectedSchedule = scheduleService.getScheduleById("sched-001");
        LocalDate start = LocalDate.of(2026, 4, 20);
        LocalDate end = LocalDate.of(2026, 4, 25);
        service.activateVacationMode(start, end, selectedSchedule, "Owner");
        assertTrue(service.getStatusSummary().contains("Active from 2026-04-20 to 2026-04-25 using 'Morning Lights'"));
    }

    /**
     * Test: activate vacation mode sets override summary.
     */
    @Test
    public void activateVacationModeSetsOverrideSummary() {
        Schedule selectedSchedule = scheduleService.getScheduleById("sched-001");
        LocalDate start = LocalDate.of(2026, 4, 20);
        LocalDate end = LocalDate.of(2026, 4, 25);
        service.activateVacationMode(start, end, selectedSchedule, "Owner");
        assertTrue(service.getOverrideSummary().contains("Morning Lights"));
    }

    /**
     * Test: activate vacation mode sends notification.
     */
    @Test
    public void activateVacationModeSendsNotification() {
        Schedule selectedSchedule = scheduleService.getScheduleById("sched-001");
        LocalDate start = LocalDate.of(2026, 4, 20);
        LocalDate end = LocalDate.of(2026, 4, 25);
        service.activateVacationMode(start, end, selectedSchedule, "Owner");
        assertTrue(notificationService.getNotifications().get(0).getMessage().contains("Vacation mode enabled"));
    }

    /**
     * Test: activate vacation mode logs action.
     */
    @Test
    public void activateVacationModeLogsAction() {
        Schedule selectedSchedule = scheduleService.getScheduleById("sched-001");
        LocalDate start = LocalDate.of(2026, 4, 20);
        LocalDate end = LocalDate.of(2026, 4, 25);
        service.activateVacationMode(start, end, selectedSchedule, "Owner");
        assertTrue(logService.getLogs().get(0).getAction().contains("Vacation mode enabled"));
    }

    /**
     * Test: deactivate vacation mode disables service.
     */
    @Test
    public void deactivateVacationModeDisablesService() {
        Schedule selectedSchedule = scheduleService.getScheduleById("sched-001");
        service.activateVacationMode(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 25), selectedSchedule, "Owner");
        service.deactivateVacationMode("Owner");
        assertFalse(service.isEnabled());
    }

    /**
     * Test: deactivate vacation mode resets status summary.
     */
    @Test
    public void deactivateVacationModeResetsStatusSummary() {
        Schedule selectedSchedule = scheduleService.getScheduleById("sched-001");
        service.activateVacationMode(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 25), selectedSchedule, "Owner");
        service.deactivateVacationMode("Owner");
        assertEquals("Vacation mode is currently disabled.", service.getStatusSummary());
    }

    /**
     * Test: reactivate and clear resets status summary.
     */
    @Test
    public void reactivateAndClearResetsStatusSummary() {
        Schedule selectedSchedule = scheduleService.getScheduleById("sched-001");
        service.activateVacationMode(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 25), selectedSchedule, "Owner");
        service.clearIfUsingSchedule(selectedSchedule.getId(), "Removed selected schedule");
        assertFalse(service.isEnabled());
    }

    /**
     * Test: clear if using schedule sends warning notification.
     */
    @Test
    public void clearIfUsingScheduleSendsWarningNotification() {
        Schedule selectedSchedule = scheduleService.getScheduleById("sched-001");
        service.activateVacationMode(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 25), selectedSchedule, "Owner");
        service.clearIfUsingSchedule(selectedSchedule.getId(), "Removed selected schedule");
        assertEquals("warning", notificationService.getNotifications().get(0).getType());
    }
}