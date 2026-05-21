package at.jku.se.smarthome.model;

/**
 * Represents a scheduling conflict between two rules or schedules.
 * A conflict exists when two enabled automations target the same device
 * with incompatible target values that overlap in time.
 */
public final class SchedulingConflict {

    /** Unique conflict identifier. */
    private final String id;
    
    /** The candidate rule or schedule being validated. */
    private final String candidateId;
    
    /** The existing rule or schedule that conflicts with the candidate. */
    private final String conflictingId;
    
    /** Human-readable name of the candidate automation. */
    private final String candidateName;
    
    /** Human-readable name of the conflicting automation. */
    private final String conflictingName;
    
    /** The target device identifier. */
    private final String deviceId;
    
    /** Human-readable device name. */
    private final String deviceName;
    
    /** The target value of the candidate (e.g., "ON", "OFF", "75%", "22.5°C"). */
    private final String candidateValue;
    
    /** The target value of the conflicting automation. */
    private final String conflictingValue;
    
    /** The trigger time or time window of the candidate. */
    private final String candidateTime;
    
    /** The trigger time or time window of the conflicting automation. */
    private final String conflictingTime;
    
    /** Human-readable description of the conflict. */
    private final String description;

    /**
     * Creates a scheduling conflict record.
     *
     * @param id unique conflict identifier
     * @param candidateId ID of the candidate rule/schedule
     * @param conflictingId ID of the conflicting rule/schedule
     * @param candidateName name of the candidate
     * @param conflictingName name of the conflicting automation
     * @param deviceId target device ID
     * @param deviceName device display name
     * @param candidateValue target value of the candidate
     * @param conflictingValue target value of the conflicting automation
     * @param candidateTime trigger time of the candidate
     * @param conflictingTime trigger time of the conflicting automation
     * @param description human-readable conflict description
     */
    public SchedulingConflict(String id, String candidateId, String conflictingId,
                              String candidateName, String conflictingName,
                              String deviceId, String deviceName,
                              String candidateValue, String conflictingValue,
                              String candidateTime, String conflictingTime,
                              String description) {
        this.id = id;
        this.candidateId = candidateId;
        this.conflictingId = conflictingId;
        this.candidateName = candidateName;
        this.conflictingName = conflictingName;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.candidateValue = candidateValue;
        this.conflictingValue = conflictingValue;
        this.candidateTime = candidateTime;
        this.conflictingTime = conflictingTime;
        this.description = description;
    }

    /**
     * Gets the conflict ID.
     *
     * @return conflict ID
     */
    public String getId() { return id; }

    /**
     * Gets the candidate automation ID.
     *
     * @return candidate ID
     */
    public String getCandidateId() { return candidateId; }

    /**
     * Gets the conflicting automation ID.
     *
     * @return conflicting automation ID
     */
    public String getConflictingId() { return conflictingId; }

    /**
     * Gets the candidate automation name.
     *
     * @return candidate name
     */
    public String getCandidateName() { return candidateName; }

    /**
     * Gets the conflicting automation name.
     *
     * @return conflicting name
     */
    public String getConflictingName() { return conflictingName; }

    /**
     * Gets the target device ID.
     *
     * @return device ID
     */
    public String getDeviceId() { return deviceId; }

    /**
     * Gets the device display name.
     *
     * @return device name
     */
    public String getDeviceName() { return deviceName; }

    /**
     * Gets the candidate's target value.
     *
     * @return target value
     */
    public String getCandidateValue() { return candidateValue; }

    /**
     * Gets the conflicting automation's target value.
     *
     * @return conflicting target value
     */
    public String getConflictingValue() { return conflictingValue; }

    /**
     * Gets the candidate's trigger time or time window.
     *
     * @return trigger time
     */
    public String getCandidateTime() { return candidateTime; }

    /**
     * Gets the conflicting automation's trigger time or time window.
     *
     * @return conflicting trigger time
     */
    public String getConflictingTime() { return conflictingTime; }

    /**
     * Gets the human-readable conflict description.
     *
     * @return description
     */
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return "SchedulingConflict{" +
                "id='" + id + '\'' +
                ", device='" + deviceName + '\'' +
                ", candidate='" + candidateName + '\'' +
                ", conflicting='" + conflictingName + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
