package at.jku.se.smarthome.model;

import java.time.LocalDate;

/**
 * Stores the active vacation mode configuration for the mock prototype.
 */
public class VacationModeConfig {

    private boolean enabled;
    private LocalDate startDate;
    private LocalDate endDate;
    private String scheduleId;

    public VacationModeConfig(boolean enabled, LocalDate startDate, LocalDate endDate, String scheduleId) {
        this.enabled = enabled;
        this.startDate = startDate;
        this.endDate = endDate;
        this.scheduleId = scheduleId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(String scheduleId) {
        this.scheduleId = scheduleId;
    }

    public boolean isConfigured() {
        return startDate != null && endDate != null && scheduleId != null && !scheduleId.isBlank();
    }
}