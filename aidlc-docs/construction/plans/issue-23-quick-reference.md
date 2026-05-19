# Issue #23 - Implementation Summary & Quick Reference

**Issue**: FR-14 — Energy consumption dashboard  
**User Stories**: US-25, US-26  
**Status**: ✓ Planning Complete - Ready for Implementation  
**Date**: May 4, 2026

---

## Quick Summary

Issue #23 requires implementing an **Energy Consumption Dashboard** that calculates and displays estimated power consumption for devices, rooms, and the entire household. The implementation must follow the **existing JDBC service patterns** and source all energy data from the **activity log**.

### Key Requirements
- ✓ Calculate consumption using formula: **Wh = nominal power (W) × on-time (hours)**
- ✓ Aggregate at three levels: **device → room → household**
- ✓ Support two time scales: **daily (00:00→now) and weekly (Mon 00:00→now)**
- ✓ Display in **Wh/kWh with ≤2 decimals**
- ✓ **Reactive updates** within **2 seconds** (no manual reload)
- ✓ Follow **existing JdbcLogService patterns**

---

## What to Build

### 1. **EnergyService Interface** (New)
**Location**: `src/main/java/at/jku/se/smarthome/service/api/EnergyService.java`

Methods to implement:
```java
// Device-level consumption
Map<String, Double> getDailyByDevice(LocalDate date);
Map<String, Double> getWeeklyByDevice(int isoWeekOfYear, int year);

// Room-level consumption
Map<String, Double> getDailyByRoom(LocalDate date);
Map<String, Double> getWeeklyByRoom(int isoWeekOfYear, int year);

// Household-level consumption
double getHouseholdDaily(LocalDate date);
double getHouseholdWeekly(int isoWeekOfYear, int year);

// Device type information
int getDeviceNominalPower(String deviceType);
Set<String> getAllDeviceTypes();
```

---

### 2. **DeviceEnergyConstants Class** (New)
**Location**: `src/main/java/at/jku/se/smarthome/config/DeviceEnergyConstants.java`

Define nominal power for each device type:
```
SWITCH         → 10W
DIMMER         → 12W
THERMOSTAT     → 50W
SENSOR         → 2W
BLIND          → 20W
LIGHT          → 15W
COFFEE_MACHINE → 1500W
```

Provide: `getPowerWatts(String deviceType)` method

---

### 3. **MockEnergyService Update** (Refactor)
**Location**: `src/main/java/at/jku/se/smarthome/service/mock/MockEnergyService.java`

Changes:
- Implement the new `EnergyService` interface
- Keep existing `getSnapshot()` and helper methods
- Use `DeviceEnergyConstants` instead of hard-coded values
- Maintain backward compatibility with existing UI

---

### 4. **JdbcEnergyService Implementation** (New)
**Location**: `src/main/java/at/jku/se/smarthome/service/real/energy/JdbcEnergyService.java`

Core functionality:
- Query activity_log for device state changes
- Calculate on-time for each device in period
- Multiply by nominal power: `Wh = W × hours`
- Aggregate by room (sum devices in room)
- Sum all rooms for household total
- Cache daily/weekly results for performance
- Subscribe to LogService for reactive updates

Key implementation pattern (from JdbcLogService):
```java
public final class JdbcEnergyService implements EnergyService {
    private static JdbcEnergyService instance;
    private static final Object INSTANCE_LOCK = new Object();
    
    public static JdbcEnergyService getInstance() {
        synchronized(INSTANCE_LOCK) {
            if (instance == null) instance = new JdbcEnergyService();
            return instance;
        }
    }
    
    // ... implementation methods
}
```

---

### 5. **Database Schema** (Optional)
**Location**: `src/main/resources/db/init-energy.sql`

Optional pre-calculated energy tables if performance requires caching:
```sql
CREATE TABLE IF NOT EXISTS energy_daily (
    date DATE NOT NULL,
    device_id UUID NOT NULL,
    consumption_wh INTEGER NOT NULL,
    PRIMARY KEY (date, device_id)
);
```

---

### 6. **EnergyController Updates** (Refactor)
**Location**: `src/main/java/at/jku/se/smarthome/controller/EnergyController.java`

Changes:
- Use `EnergyService` interface instead of `MockEnergyService`
- Add time navigation (Today, Yesterday, This Week, Last Week)
- Implement auto-refresh listener (≤2s)
- Query service with selected date/week
- Display all devices with 0 Wh if no activity

---

### 7. **Unit Tests** (New)
**Location**: `src/test/java/at/jku/se/smarthome/service/JdbcEnergyServiceTest.java`

Test cases required by Acceptance Criteria:
- ✓ Known on-time → Expected Wh (e.g., 50W × 1h = 50Wh)
- ✓ Room sum = Device sum (aggregation accuracy)
- ✓ Household sum = Room sum (hierarchical consistency)
- ✓ Zero-activity case (devices with no logs show 0 Wh)
- ✓ Time boundary cases (ON at period start/end)
- ✓ Multiple room/device isolation (no data leakage)

---

## Implementation Phases

```
PHASE 1: Foundation (2-3 hours)
├── EnergyService interface
├── DeviceEnergyConstants
└── MockEnergyService update → implements interface

PHASE 2: JDBC Service (4-5 hours)
├── JdbcEnergyService class
├── Daily/weekly query methods
├── On-time calculation logic
└── Aggregation by room/household

PHASE 3: Database (0.5-1 hour)
├── init-energy.sql schema (optional)
└── Schema initialization

PHASE 4: Testing (3-4 hours)
├── Unit tests for all scenarios
├── Integration tests
└── Manual smoke test (AC#5)

PHASE 5: UI Integration (2-3 hours)
├── EnergyController refactoring
├── Time navigation
└── Reactive update mechanism

PHASE 6: Verification (1-2 hours)
├── Build & test (mvn verify)
├── Code review
└── Documentation updates
```

**Total Estimated Effort: 12-17 hours**

---

## Critical Success Factors

### 1. **Energy Calculation Formula**
```
Consumption (Wh) = Nominal Power (W) × On-Time (hours)

Example:
  Device: Thermostat (50W)
  Activity: ON 08:00 → OFF 17:00 (9 hours)
  Consumption: 50W × 9h = 450 Wh = 0.45 kWh
```

### 2. **Time Period Definitions**
```
Daily:   Current calendar day (00:00:00 to 23:59:59)
         Query: WHERE DATE(timestamp) = ?

Weekly:  Current ISO week (Monday 00:00 to current time)
         Query: WHERE YEAR_ISO = ? AND WEEK_ISO = ?
```

### 3. **Aggregation Hierarchy**
```
Household Total
    ↑
    └─ Room A (sum of devices in Room A)
    │  ├─ Device 1: 450 Wh
    │  ├─ Device 2: 120 Wh
    │  └─ Device 3: 0 Wh ← includes zero devices
    │
    └─ Room B (sum of devices in Room B)
       ├─ Device 4: 800 Wh
       └─ Device 5: 0 Wh
```

### 4. **Zero-Activity Handling**
Devices with no activity must:
- ✓ Appear in results
- ✓ Show 0 Wh (not null/hidden)
- ✓ Not cause errors
- ✓ Be included in room/household sums

### 5. **Reactive Updates**
When device state changes:
- ✓ LogService logs new LogEntry
- ✓ JdbcEnergyService invalidates cache
- ✓ EnergyController receives notification
- ✓ Dashboard refreshes automatically
- ✓ All within 2 seconds

---

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Query activity_log on-the-fly | Source of truth, no state duplication |
| Cache daily/weekly results | Performance for repeated queries |
| Invalidate on new log entry | Reactive updates within 2s |
| Use nominal power by type | Consistent, testable, documented |
| Implement as service interface | Pluggable (mock ↔ JDBC) |
| Follow JdbcLogService pattern | Consistency, proven pattern |
| Store in database (optional) | Future performance optimization |

---

## Testing Checklist

Before marking done, verify:

### Unit Tests
- [ ] Test 1: Known on-time → Expected Wh
- [ ] Test 2: Room sum = Device sum
- [ ] Test 3: Household sum = Room sum
- [ ] Test 4: Zero-activity case
- [ ] Test 5: Time boundary cases
- [ ] Test 6: Multiple room/device isolation

### Manual Tests
- [ ] Turn device ON for exactly 60 minutes
- [ ] Verify displayed value = nominal power × 1 hour
- [ ] Example: Thermostat 50W × 1h = 50.00 Wh ✓
- [ ] Toggle device, verify refresh within 2 seconds
- [ ] No manual page reload needed

### Build Tests
- [ ] `mvn verify` passes (tests + PMD + coverage)
- [ ] All unit tests green
- [ ] Code coverage > 80% for new code
- [ ] No PMD violations
- [ ] Checkstyle compliant

### Code Review
- [ ] No duplication with existing code
- [ ] Clear JavaDoc comments
- [ ] Proper error handling
- [ ] Resource management (connections closed)
- [ ] Security (PreparedStatement used)

---

## Common Pitfalls to Avoid

❌ **Don't**: Hard-code device on-times in EnergyService
✓ **Do**: Calculate from activity_log with formula

❌ **Don't**: Store only one nominal power value
✓ **Do**: Define per device type in DeviceEnergyConstants

❌ **Don't**: Hide devices with 0 Wh consumption
✓ **Do**: Show 0 Wh explicitly (AC#4)

❌ **Don't**: Query database on every UI refresh
✓ **Do**: Cache and invalidate on log changes

❌ **Don't**: Make reactive updates event-driven only
✓ **Do**: Guarantee update within 2 seconds (polling fallback)

❌ **Don't**: Duplicate state in both LogEntry and energy tables
✓ **Do**: Derive energy from LogEntry on-demand

---

## File Locations Summary

```
New Files to Create:
✓ service/api/EnergyService.java
✓ config/DeviceEnergyConstants.java
✓ service/real/energy/JdbcEnergyService.java
✓ resources/db/init-energy.sql (optional)
✓ test/JdbcEnergyServiceTest.java

Files to Modify:
✓ service/mock/MockEnergyService.java
✓ controller/EnergyController.java

Planning Documents:
✓ aidlc-docs/construction/plans/issue-23-energy-dashboard-plan.md
✓ aidlc-docs/construction/plans/issue-23-implementation-checklist.md
✓ aidlc-docs/construction/plans/issue-23-architecture.md
```

---

## Resources & References

### Existing Code to Study
- `JdbcLogService`: JDBC pattern template
- `LogService` interface: Service interface pattern
- `MockEnergyService`: Current mock implementation
- `EnergyController`: Current UI controller

### Documentation
- Acceptance Criteria: See issue #23
- User Stories: US-25, US-26 in docs/user-stories.md
- Architecture: docs/system-architecture.md
- UML: docs/uml/03-service-layer.puml

### Build & Test Commands
```bash
# Compile
mvn clean compile -DskipTests

# Run specific test
mvn -Dtest=JdbcEnergyServiceTest test

# Full verification
mvn verify

# PMD check
mvn pmd:check
```

---

## Next Steps

1. **Review this plan** with team
2. **Create feature branch**: `feature/issue-23-energy-dashboard`
3. **Implement Phase 1**: Service interface & constants
4. **Implement Phase 2**: JDBC service
5. **Write unit tests**: All 6 test scenarios
6. **Update EnergyController**: Integrate with service
7. **Manual smoke test**: Verify AC#5 requirements
8. **Code review & merge**: PR with full CI pass

---

## Questions & Clarifications

### Q: What if a device is ON at the start of the day?
**A**: The activity_log might not have a prior OFF event. Handle by:
- Check if device was already ON before period start
- Include on-time from period start to first OFF logged
- If never OFF'd, count to period end

### Q: How to handle multiple rooms for one device?
**A**: Based on code structure, devices belong to single room. If multi-room needed:
- Document in comments
- Query room-device relationship via RoomService
- Sum consumption across all rooms containing device

### Q: What if device type not in constants?
**A**: Handle gracefully:
- Log warning/debug message
- Use sensible default (e.g., 10W for unknown)
- Or throw exception and show UI error

### Q: How often to refresh cache?
**A**: Strategy:
- Today's data: 30-second TTL
- Historical (< 1 week): 5-minute TTL
- Older data: 1-hour TTL
- Always invalidate on new LogEntry

### Q: What about devices deleted from system?
**A**: Approach:
- Keep historical consumption data
- Filter by current devices when generating reports
- Show deleted devices with warning in historical view

---

## Sign-Off Required

- [ ] Product Owner approves plan
- [ ] Tech Lead reviews architecture
- [ ] Team confirms effort estimates
- [ ] QA reviews test checklist
- [ ] Ready to start implementation

---

## Document Tracking

| Document | Location | Status |
|----------|----------|--------|
| Implementation Plan | issue-23-energy-dashboard-plan.md | ✓ Complete |
| Implementation Checklist | issue-23-implementation-checklist.md | ✓ Complete |
| Architecture Diagram | issue-23-architecture.md | ✓ Complete |
| This Summary | issue-23-quick-reference.md | ✓ Complete |

---

*Plan created: May 4, 2026*  
*Ready for: Implementation Phase*  
*Contact: [Implementation Lead]*
