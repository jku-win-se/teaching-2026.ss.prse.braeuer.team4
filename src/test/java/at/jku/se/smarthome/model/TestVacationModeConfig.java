package at.jku.se.smarthome.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDate;

import org.junit.Test;

@SuppressWarnings("PMD.AtLeastOneConstructor")
public class TestVacationModeConfig {

    @Test
    public void testConstructorSetsAllFields() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 15);
        VacationModeConfig config = new VacationModeConfig(true, start, end, "sched-1");

        assertTrue(config.isEnabled());
        assertEquals(start, config.getStartDate());
        assertEquals(end, config.getEndDate());
        assertEquals("sched-1", config.getScheduleId());
    }

    @Test
    public void testSetEnabled() {
        VacationModeConfig config = new VacationModeConfig(false, null, null, null);
        config.setEnabled(true);
        assertTrue(config.isEnabled());
    }

    @Test
    public void testSetStartDate() {
        VacationModeConfig config = new VacationModeConfig(false, null, null, null);
        LocalDate date = LocalDate.of(2026, 7, 1);
        config.setStartDate(date);
        assertEquals(date, config.getStartDate());
    }

    @Test
    public void testSetEndDate() {
        VacationModeConfig config = new VacationModeConfig(false, null, null, null);
        LocalDate date = LocalDate.of(2026, 7, 15);
        config.setEndDate(date);
        assertEquals(date, config.getEndDate());
    }

    @Test
    public void testSetScheduleId() {
        VacationModeConfig config = new VacationModeConfig(false, null, null, null);
        config.setScheduleId("new-sched");
        assertEquals("new-sched", config.getScheduleId());
    }

    @Test
    public void testIsConfiguredReturnsTrueWhenComplete() {
        VacationModeConfig config = new VacationModeConfig(
                true,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 15),
                "sched-1"
        );
        assertTrue(config.isConfigured());
    }

    @Test
    public void testIsConfiguredReturnsFalseWhenStartDateMissing() {
        VacationModeConfig config = new VacationModeConfig(
                true, null, LocalDate.of(2026, 6, 15), "sched-1");
        assertFalse(config.isConfigured());
    }

    @Test
    public void testIsConfiguredReturnsFalseWhenEndDateMissing() {
        VacationModeConfig config = new VacationModeConfig(
                true, LocalDate.of(2026, 6, 1), null, "sched-1");
        assertFalse(config.isConfigured());
    }

    @Test
    public void testIsConfiguredReturnsFalseWhenScheduleIdMissing() {
        VacationModeConfig config = new VacationModeConfig(
                true, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15), null);
        assertFalse(config.isConfigured());
    }

    @Test
    public void testIsConfiguredReturnsFalseWhenScheduleIdBlank() {
        VacationModeConfig config = new VacationModeConfig(
                true, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15), "   ");
        assertFalse(config.isConfigured());
    }

    @Test
    public void testConstructorAllowsNullFields() {
        VacationModeConfig config = new VacationModeConfig(false, null, null, null);
        assertNotNull(config);
        assertFalse(config.isEnabled());
    }
}