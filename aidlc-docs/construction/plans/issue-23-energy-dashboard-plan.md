# Implementation Plan: Issue #23 - Energy Consumption Dashboard

**Issue**: FR-14 — Energy consumption dashboard  
**Related User Stories**: US-25, US-26  
**Milestone**: 2  
**Current Date**: May 4, 2026

---

## Overview

This document outlines the implementation plan for the energy consumption dashboard feature. The implementation must:

1. Calculate energy consumption based on device on-time from activity logs
2. Aggregate consumption at three levels: device, room, household
3. Support two time scales: daily and weekly
4. Implement a JDBC service layer following existing patterns
5. Provide reactive updates (≤ 2 seconds) when device state changes
6. Format all values in Wh/kWh with at most 2 decimals

---

## Acceptance Criteria Analysis

### 1. Estimation Model
- **Source of Truth**: Activity logs (LogEntry stream)
- **Formula**: Consumption (Wh) = nominal power (W) × on-time (hours)
- **Constants**: Centralized nominal power values by device type
- **Key Requirement**: No state duplication; derive on-time from LogEntry

### 2. Dashboard View
Three panels required:
- **By Device**: List all devices with daily/weekly Wh
- **By Room**: List all rooms with daily/weekly Wh (sum of contained devices)
- **Whole Household**: Single card with daily/weekly Wh totals
- **Format**: Display in Wh or kWh with ≤ 2 decimals

### 3. Time Scale
- **Daily**: Current calendar day (00:00 → now)
- **Weekly**: Current ISO week (Mon 00:00 → now)
- **Navigation**: User can switch between today/yesterday/this week/last week

### 4. Empty/Zero Data
- Devices with no activity must show 0 Wh (not hidden/error)

### 5. Reactive Update
- When device state change is logged: refresh relevant aggregate within ≤ 2s
- No manual reload required

---

## Architecture & Design Patterns

### Existing Patterns to Follow

1. **Service Layer Architecture**
   - Interface in: `at.jku.se.smarthome.service.api.*.java`
   - Mock implementation in: `at.jku.se.smarthome.service.mock.Mock*.java`
   - JDBC implementation in: `at.jku.se.smarthome.service.real.*/*.java`

2. **JDBC Service Pattern** (from JdbcLogService)
   - Use JDBC with PreparedStatement
   - DatabaseConfig & DatabaseSettings for connection management
   - Singleton pattern with getInstance() and resetForTesting()
   - Schema initialization with SQL scripts in classpath
   - Observable collections for UI binding

3. **Service Interface Pattern** (from LogService)
   - Clear, well-documented methods
   - Observable return types for UI binding
   - No JavaFX dependencies in interface (only in domain models)

### Service Layer Design

```
EnergyService (interface)
├── MockEnergyService (mock implementation)
└── JdbcEnergyService (JDBC implementation)
```

---

## Implementation Stages

### STAGE 1: Service Interface & Data Models

#### 1.1 Create EnergyService Interface
**File**: `src/main/java/at/jku/se/smarthome/service/api/EnergyService.java`

Methods needed:
- `getDailyByDevice(LocalDate date)` → Map<String, Double> (device → Wh)
- `getWeeklyByDevice(int isoWeekOfYear, int year)` → Map<String, Double> (device → Wh)
- `getDailyByRoom(LocalDate date)` → Map<String, Double> (room → Wh)
- `getWeeklyByRoom(int isoWeekOfYear, int year)` → Map<String, Double> (room → Wh)
- `getHouseholdDaily(LocalDate date)` → double (total Wh)
- `getHouseholdWeekly(int isoWeekOfYear, int year)` → double (total Wh)
- `getDeviceNominalPower(String deviceType)` → int (watts)
- `getAllDeviceTypes()` → Set<String> (available device types)

#### 1.2 Create DeviceEnergyConstants Class
**File**: `src/main/java/at/jku/se/smarthome/config/DeviceEnergyConstants.java`

Purpose: Single source of truth for nominal power values by device type

Content:
```java
public final class DeviceEnergyConstants {
    // Constants for each device type
    public static final int SWITCH_POWER_W = 10;           // 10W baseline
    public static final int DIMMER_POWER_W = 12;           // 12W baseline
    public static final int THERMOSTAT_POWER_W = 50;       // 50W for HVAC
    public static final int SENSOR_POWER_W = 2;            // 2W low-power
    public static final int BLIND_POWER_W = 20;            // 20W for motor
    public static final int LIGHT_POWER_W = 15;            // 15W for light bulb
    public static final int COFFEE_MACHINE_POWER_W = 1500; // 1500W active
    
    private static final Map<String, Integer> POWER_MAP = ...;
    public static int getPowerWatts(String deviceType) { ... }
}
```

---

### STAGE 2: Mock Service Implementation Update

#### 2.1 Refactor MockEnergyService
**File**: `src/main/java/at/jku/se/smarthome/service/mock/MockEnergyService.java`

Changes:
- Make it implement the new `EnergyService` interface
- Keep existing `EnergySnapshot` record and methods
- Add new interface methods for daily/weekly/room/household queries
- Update existing methods to use interface signatures

Purpose: Maintain compatibility with existing EnergyController while conforming to the interface

---

### STAGE 3: JDBC Energy Service Implementation

#### 3.1 Create JdbcEnergyService
**File**: `src/main/java/at/jku/se/smarthome/service/real/energy/JdbcEnergyService.java`

Key implementation details:
- **Aggregation Strategy**: Query activity logs, group by device/time, calculate on-time, multiply by nominal power
- **Time Range Calculations**:
  - Daily: LocalDate.now() (00:00:00 to 23:59:59)
  - Weekly: YearMonth.now().atDay(1) for week start, accounting for ISO week boundaries
- **Database Queries**:
  - For device daily: Join activity_log with room/device info, aggregate on-times per device
  - For room daily: Sum all devices in that room
  - For household daily: Sum all rooms
- **Caching**: Consider in-memory cache for today's aggregations (refresh on LogEntry writes)
- **Reactive Updates**: 
  - Integrate with LogService to listen for new log entries
  - Trigger dashboard refresh via observable collections or event listeners

**SQL Query Pattern**:
```sql
-- Example: Device daily consumption
SELECT 
    device,
    COALESCE(SUM(EXTRACT(EPOCH FROM (end_time - start_time))/3600), 0) as hours_on
FROM activity_log
WHERE DATE(timestamp) = ?
  AND action = 'ON' OR action = 'TURNED_ON'
GROUP BY device
```

**Challenge**: Activity logs only record state changes, not continuous on-time. Solution:
- For each device, find consecutive ON logs
- Calculate on-time between consecutive state-change log entries
- Handle edge cases: device ON at start of period, ON at end of period

#### 3.2 Create Database Schema for Energy Data (Optional)
**File**: `src/main/resources/db/init-energy.sql`

Purpose: Optional pre-calculated energy data for performance optimization

Content (if needed):
```sql
CREATE TABLE IF NOT EXISTS energy_daily (
    date DATE NOT NULL,
    device_id UUID NOT NULL,
    room_id UUID NOT NULL,
    consumption_wh INTEGER NOT NULL,
    PRIMARY KEY (date, device_id),
    FOREIGN KEY (room_id) REFERENCES rooms(id)
);

CREATE TABLE IF NOT EXISTS energy_weekly (
    year INTEGER NOT NULL,
    iso_week INTEGER NOT NULL,
    device_id UUID NOT NULL,
    room_id UUID NOT NULL,
    consumption_wh INTEGER NOT NULL,
    PRIMARY KEY (year, iso_week, device_id)
);
```

Note: Implementation may use on-the-fly calculations instead of storing pre-computed values.

---

### STAGE 4: Controller & UI Updates

#### 4.1 Update EnergyController
**File**: `src/main/java/at/jku/se/smarthome/controller/EnergyController.java`

Changes:
- Inject `EnergyService` interface instead of directly using MockEnergyService
- Update initialization to get service from ServiceRegistry or DI container
- Add support for time navigation: today/yesterday/this week/last week
- Implement reactive update mechanism (listener on LogService)
- Handle refresh schedule (≤ 2s max latency)

#### 4.2 Update EnergySnapshot Record (if needed)
Modify to work with calculated values instead of hard-coded mock data

---

### STAGE 5: Testing Strategy

#### 5.1 Unit Tests
**File**: `src/test/java/at/jku/se/smarthome/service/JdbcEnergyServiceTest.java`

Test scenarios:
1. **Known On-Time → Expected Wh**
   - Create activity logs with known ON/OFF pairs
   - Verify calculated consumption matches formula: power × hours
   
2. **Room Sum = Device Sum**
   - Verify sum of device consumption equals room total
   
3. **Household Sum = Room Sum**
   - Verify sum of room consumption equals household total
   
4. **Zero-Activity Case**
   - Device with no logs should return 0 Wh (not error)
   - Should appear in results with 0 value
   
5. **Time Boundary Cases**
   - Device ON at start of period (no prior OFF)
   - Device ON at end of period (no OFF after)
   - Device toggled multiple times within period
   
6. **Multiple Rooms & Devices**
   - Verify correct grouping by room
   - Verify isolation between rooms

#### 5.2 Integration Tests
- Create complete activity log scenario
- Calculate energy consumption
- Verify dashboard displays correct values
- Test time navigation (today/yesterday/week/last week)

#### 5.3 Manual Smoke Test
Per acceptance criteria:
- Toggle a device ON for known interval (e.g., 1 hour)
- Verify displayed Wh matches formula (power × 1 hour)
- Verify within rounding tolerance

#### 5.4 Reactive Update Test
- Log a device state change
- Verify dashboard updates within 2 seconds without manual reload
- Check all affected aggregates (device, room, household)

---

### STAGE 6: Data Validation & Formatting

#### 6.1 Energy Value Formatting
Rules:
- Display in Wh for values < 1000
- Display in kWh for values ≥ 1000
- Maximum 2 decimal places
- Example: "1234.56 Wh" or "1.23 kWh"

Implementation:
```java
public static String formatEnergy(double wh) {
    if (wh >= 1000) {
        return String.format("%.2f kWh", wh / 1000.0);
    }
    return String.format("%.2f Wh", wh);
}
```

#### 6.2 Edge Case Handling
- Null devices/rooms → skip or include with 0 value
- Missing nominal power → use sensible default or error log
- Future timestamps → return empty/0

---

## Implementation Sequence

### Phase 1: Foundation (High Priority)
- [ ] Create EnergyService interface
- [ ] Create DeviceEnergyConstants class
- [ ] Update MockEnergyService to implement interface
- [ ] Create JdbcEnergyService with core queries

### Phase 2: Database & Persistence (High Priority)
- [ ] Create init-energy.sql schema (if using pre-calc)
- [ ] Integrate JdbcEnergyService with DatabaseConfig
- [ ] Test JDBC queries with real activity log data

### Phase 3: UI Integration (High Priority)
- [ ] Update EnergyController to use EnergyService interface
- [ ] Implement time navigation (today/yesterday/week/last week)
- [ ] Add reactive update mechanism

### Phase 4: Testing & Validation (High Priority)
- [ ] Unit tests for consumption calculations
- [ ] Integration tests for full workflow
- [ ] Manual smoke test per AC#5
- [ ] Reactive update test (≤ 2s)

### Phase 5: Code Quality & Documentation (Medium Priority)
- [ ] Code review & cleanup
- [ ] Update docs/user-stories.md for US-25, US-26
- [ ] Update javadoc for new service
- [ ] Verify mvn verify green

---

## Key Risks & Mitigation

| Risk | Mitigation |
|------|-----------|
| Activity log doesn't capture initial state | Assume devices start OFF, only count ON→OFF pairs |
| Performance: calculating aggregates on each query | Cache daily/weekly results, invalidate on new log entries |
| Time zone handling for daily/weekly boundaries | Use system timezone consistently, document assumptions |
| Device deleted but has historical logs | Filter by current rooms/devices, handle gracefully |
| Reactive update lag > 2s | Pre-calculate during off-peak, use polling fallback |

---

## Definition of Done Checklist

- [ ] mvn verify green (tests + PMD + coverage)
- [ ] Unit tests cover all acceptance criteria scenarios
- [ ] Manual smoke test passes (known on-time → expected Wh)
- [ ] Dashboard refreshes after state change without manual reload
- [ ] Code reviewed and PR merged
- [ ] docs/user-stories.md updated for US-25, US-26

---

## Technical Notes

1. **Activity Log Analysis**:
   - Review LogEntry schema to understand ON/OFF action naming
   - Verify timestamp format and accuracy
   - Handle case sensitivity and variations

2. **Room-Device Relationships**:
   - Use RoomService to get room membership
   - Handle devices in multiple rooms (if allowed)
   - Handle devices not in any room

3. **Performance Optimization**:
   - Consider materialized views for daily/weekly aggregates
   - Implement TTL cache for time-window queries
   - Use connection pooling in DatabaseConfig

4. **Reactive Updates**:
   - Subscribe to LogService changes via Observable listeners
   - Update cached aggregates when new LogEntry added
   - Publish changes via ObservableList to UI

---

## File Structure Summary

```
src/main/java/at/jku/se/smarthome/
├── config/
│   └── DeviceEnergyConstants.java         [NEW]
├── service/
│   ├── api/
│   │   └── EnergyService.java             [NEW]
│   ├── mock/
│   │   └── MockEnergyService.java         [MODIFIED - implement interface]
│   └── real/
│       └── energy/
│           └── JdbcEnergyService.java     [NEW]
└── controller/
    └── EnergyController.java              [MODIFIED - use interface]

src/main/resources/db/
└── init-energy.sql                        [NEW - optional]

src/test/java/at/jku/se/smarthome/service/
└── JdbcEnergyServiceTest.java            [NEW]
```

---

## Estimated Effort

- Interface & Models: 2-3 hours
- JDBC Implementation: 4-5 hours
- Testing: 3-4 hours
- UI Integration & Refinement: 2-3 hours
- Documentation & Code Review: 1-2 hours

**Total**: ~12-17 hours of development time

---

## References

- **Issue**: #23 (FR-14 — Energy consumption dashboard)
- **User Stories**: US-25, US-26
- **Related Issues**: FR-18 (Physical device integration, out of scope)
- **Existing Patterns**: JdbcLogService, LogService interface
- **Database Config**: at.jku.se.smarthome.config.DatabaseConfig
