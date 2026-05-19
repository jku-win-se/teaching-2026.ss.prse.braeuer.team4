package at.jku.se.smarthome.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Stores the active vacation mode configuration for the mock prototype.
 */
@SuppressWarnings("PMD.DataClass")
public class VacationModeConfig {

    /** Whether vacation mode is currently enabled. */
    private boolean enabled;
    /** Start date of vacation mode. */
    private LocalDate startDate;
    /** End date of vacation mode. */
    private LocalDate endDate;
    /** Start time of vacation mode. */
    private LocalTime startTime;
    /** End time of vacation mode. */
    private LocalTime endTime;
    /** ID of the schedule to apply during vacation. */
    private String scheduleId;

    /**
     * Creates a vacation mode configuration.
     *
     * @param enabled whether vacation mode is enabled
     * @param startDate configured start date
     * @param endDate configured end date
     * @param scheduleId selected schedule identifier
     */
    public VacationModeConfig(boolean enabled, LocalDate startDate, LocalDate endDate, String scheduleId) {
        this(enabled, startDate, endDate, LocalTime.MIN, LocalTime.of(23, 59), scheduleId);
    }

    /**
     * Creates a vacation mode configuration with date and time range.
     *
     * @param enabled whether vacation mode is enabled
     * @param startDate configured start date
     * @param endDate configured end date
     * @param startTime configured start time
     * @param endTime configured end time
     * @param scheduleId selected schedule identifier
     */
    public VacationModeConfig(
            boolean enabled,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            String scheduleId
    ) {
        this.enabled = enabled;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.scheduleId = scheduleId;
    }

    /**
     * Returns whether vacation mode is enabled.
     *
     * @return true when enabled, otherwise false
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Updates whether vacation mode is enabled.
     *
     * @param enabled updated enabled state
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the configured start date.
     *
     * @return configured start date
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Updates the configured start date.
     *
     * @param startDate updated start date
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Returns the configured end date.
     *
     * @return configured end date
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Updates the configured end date.
     *
     * @param endDate updated end date
     */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * Returns the configured start time.
     *
     * @return configured start time
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * Updates the configured start time.
     *
     * @param startTime updated start time
     */
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    /**
     * Returns the configured end time.
     *
     * @return configured end time
     */
    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * Updates the configured end time.
     *
     * @param endTime updated end time
     */
    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    /**
     * Returns the selected schedule identifier.
     *
     * @return selected schedule identifier
     */
    public String getScheduleId() {
        return scheduleId;
    }

    /**
     * Updates the selected schedule identifier.
     *
     * @param scheduleId updated schedule identifier
     */
    public void setScheduleId(String scheduleId) {
        this.scheduleId = scheduleId;
    }

    /**
     * Indicates whether the configuration contains the required schedule and date range.
     *
     * @return true when the configuration is complete, otherwise false
     */
    public boolean isConfigured() {
        return startDate != null
                && endDate != null
                && startTime != null
                && endTime != null
                && scheduleId != null
                && !scheduleId.isBlank();
    }
}