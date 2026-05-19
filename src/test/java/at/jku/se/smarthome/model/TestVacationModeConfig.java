package at.jku.se.smarthome.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.Test;

/**
 * Unit tests for {@link VacationModeConfig} model class.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods"})
public class TestVacationModeConfig {

    /**
     * Test: constructor sets all fields correctly.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testConstructorSetsAllFields() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 15);
        VacationModeConfig config = new VacationModeConfig(true, start, end, "sched-1");

        assertTrue(config.isEnabled());
        assertEquals(start, config.getStartDate());
        assertEquals(end, config.getEndDate());
        assertEquals(LocalTime.MIN, config.getStartTime());
        assertEquals(LocalTime.of(23, 59), config.getEndTime());
        assertEquals("sched-1", config.getScheduleId());
    }

    /**
     * Test: setEnabled toggles enabled state.
     */
    @Test
    public void testSetEnabled() {
        VacationModeConfig config = new VacationModeConfig(false, null, null, null);
        config.setEnabled(true);
        assertTrue(config.isEnabled());
    }

    /**
     * Test: setStartDate updates start date.
     */
    @Test
    public void testSetStartDate() {
        VacationModeConfig config = new VacationModeConfig(false, null, null, null);
        LocalDate date = LocalDate.of(2026, 7, 1);
        config.setStartDate(date);
        assertEquals(date, config.getStartDate());
    }

    /**
     * Test: setEndDate updates end date.
     */
    @Test
    public void testSetEndDate() {
        VacationModeConfig config = new VacationModeConfig(false, null, null, null);
        LocalDate date = LocalDate.of(2026, 7, 15);
        config.setEndDate(date);
        assertEquals(date, config.getEndDate());
    }

    /**
     * Test: setScheduleId updates schedule ID.
     */
    @Test
    public void testSetScheduleId() {
        VacationModeConfig config = new VacationModeConfig(false, null, null, null);
        config.setScheduleId("new-sched");
        assertEquals("new-sched", config.getScheduleId());
    }

    /**
     * Test: setStartTime updates start time.
     */
    @Test
    public void testSetStartTime() {
        VacationModeConfig config = new VacationModeConfig(false, null, null, null);
        LocalTime time = LocalTime.of(8, 0);
        config.setStartTime(time);
        assertEquals(time, config.getStartTime());
    }

    /**
     * Test: setEndTime updates end time.
     */
    @Test
    public void testSetEndTime() {
        VacationModeConfig config = new VacationModeConfig(false, null, null, null);
        LocalTime time = LocalTime.of(20, 0);
        config.setEndTime(time);
        assertEquals(time, config.getEndTime());
    }

    /**
     * Test: isConfigured returns true when all fields set.
     */
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

    /**
     * Test: isConfigured returns false when start date is null.
     */
    @Test
    public void testIsConfiguredReturnsFalseWhenStartDateMissing() {
        VacationModeConfig config = new VacationModeConfig(
                true, null, LocalDate.of(2026, 6, 15), "sched-1");
        assertFalse(config.isConfigured());
    }

    /**
     * Test: isConfigured returns false when end date is null.
     */
    @Test
    public void testIsConfiguredReturnsFalseWhenEndDateMissing() {
        VacationModeConfig config = new VacationModeConfig(
                true, LocalDate.of(2026, 6, 1), null, "sched-1");
        assertFalse(config.isConfigured());
    }

    /**
     * Test: isConfigured returns false when schedule ID is null.
     */
    @Test
    public void testIsConfiguredReturnsFalseWhenScheduleIdMissing() {
        VacationModeConfig config = new VacationModeConfig(
                true, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15), null);
        assertFalse(config.isConfigured());
    }

    /**
     * Test: isConfigured returns false when schedule ID is blank.
     */
    @Test
    public void testIsConfiguredReturnsFalseWhenScheduleIdBlank() {
        VacationModeConfig config = new VacationModeConfig(
                true, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15), "   ");
        assertFalse(config.isConfigured());
    }

    /**
     * Test: constructor allows null fields and sets enabled to false.
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    public void testConstructorAllowsNullFields() {
        VacationModeConfig config = new VacationModeConfig(false, null, null, null);
        assertNotNull(config);
        assertFalse(config.isEnabled());
    }
}