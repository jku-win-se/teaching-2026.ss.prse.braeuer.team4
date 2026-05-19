# Issue #23 Architecture Diagram

## Service Layer Integration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           EnergyController (UI)                             │
│  - refreshDashboard()                                                       │
│  - handleDayToggle()                                                        │
│  - handleWeekToggle()                                                       │
│  - handleTimeNavigation() [TODAY | YESTERDAY | THIS_WEEK | LAST_WEEK]      │
└────────────────────┬────────────────────────────────────────────────────────┘
                     │
                     │ uses (interface)
                     ▼
        ┌────────────────────────────┐
        │   EnergyService (API)      │◄──── NEW INTERFACE
        │   ─────────────────────    │
        │ + getDailyByDevice()       │
        │ + getWeeklyByDevice()      │
        │ + getDailyByRoom()         │
        │ + getWeeklyByRoom()        │
        │ + getHouseholdDaily()      │
        │ + getHouseholdWeekly()     │
        │ + getDeviceNominalPower()  │
        │ + getAllDeviceTypes()      │
        └────────┬────────────────────┘
                 │
        ┌────────┴──────────┐
        │                   │ implements
        ▼                   ▼
┌──────────────────┐  ┌──────────────────────────────────────┐
│ MockEnergyService│  │  JdbcEnergyService (REFACTORED)     │◄── NEW IMPL
│ [EXISTING]       │  │  ──────────────────────────────────  │
│                  │  │ - Queries activity_log               │
│ • Hard-coded     │  │ - Calculates on-time per device      │
│   mock data      │  │ - Aggregates by room                 │
│ • For testing    │  │ - Sums household totals              │
└──────────────────┘  │ - Caches daily aggregations          │
                      │ - Reactive update on log changes     │
                      │                                      │
                      │ Dependencies:                        │
                      │ • DatabaseConfig                    │
                      │ • DeviceEnergyConstants             │
                      │ • LogService (for updates)          │
                      │ • RoomService (for room→device)     │
                      └──────┬─────────────────────────────────┘
                             │
                ┌────────────┼────────────┐
                │            │            │
                ▼            ▼            ▼
         ┌────────────┐ ┌──────────┐ ┌─────────────┐
         │  Database  │ │ Activity │ │ Device Info │
         │  Config    │ │   Log    │ │ & Rooms     │
         └────────────┘ │ (JDBC)   │ │             │
                        └──────────┘ └─────────────┘
```

## Data Flow: Energy Calculation

```
Activity Log Entry
    (device "ON", timestamp)
            │
            ▼
    ┌───────────────────────┐
    │ Find consecutive      │
    │ ON/OFF pairs for      │
    │ given time period     │
    └───────────┬───────────┘
                │
                ▼
    ┌───────────────────────┐
    │ Calculate on-time     │
    │ in hours for each     │
    │ device                │
    └───────────┬───────────┘
                │
                ▼
    ┌───────────────────────────────────┐
    │ Get nominal power from            │
    │ DeviceEnergyConstants             │
    │ Consumption = Power × On-Time     │
    └───────────┬───────────────────────┘
                │
                ▼
    ┌───────────────────────────────────┐
    │ Aggregate by:                     │
    │ 1. By Device (raw calculation)    │
    │ 2. By Room (sum of devices)       │
    │ 3. Household (sum of rooms)       │
    └───────────┬───────────────────────┘
                │
                ▼
    ┌───────────────────────────────────┐
    │ Format & Return to Controller     │
    │ • Wh/kWh formatting (2 decimals)  │
    │ • Cache for performance           │
    │ • Observable for UI binding       │
    └───────────────────────────────────┘
```

## Time Period Handling

```
Daily View
┌─────────────────────────────────┐
│ Today:     2026-05-04 (TODAY)   │
│ Yesterday: 2026-05-03           │
├─────────────────────────────────┤
│ Time Range: 00:00 to 23:59:59   │
│ Query:  date(timestamp) = ?     │
└─────────────────────────────────┘

Weekly View
┌─────────────────────────────────┐
│ This Week: ISO Week 18/2026     │
│ Last Week: ISO Week 17/2026     │
├─────────────────────────────────┤
│ Mon-Sun: ISO week boundaries    │
│ Query: week_of_year(timestamp)  │
└─────────────────────────────────┘
```

## Reactive Update Mechanism

```
Activity Log Modified
    (LogEntry added)
         │
         ▼
  ┌─────────────────┐
  │ LogService      │
  │ fires update    │
  │ event/listener  │
  └────────┬────────┘
           │
           ▼
  ┌─────────────────────────┐
  │ JdbcEnergyService       │
  │ invalidates cache for:  │
  │ • today                 │
  │ • this week             │
  └────────┬────────────────┘
           │
           ▼
  ┌─────────────────────────┐
  │ EnergyController        │
  │ receives notification   │
  │ calls refreshDashboard()│
  └────────┬────────────────┘
           │
           ▼
  ┌─────────────────────────┐
  │ UI Updates (within 2s)  │
  │ • device values         │
  │ • room values           │
  │ • household total       │
  └─────────────────────────┘
```

## File Structure

```
src/main/java/at/jku/se/smarthome/
├── config/
│   └── DeviceEnergyConstants.java          [NEW]
│       Constants for nominal power by device type
│
├── service/
│   ├── api/
│   │   └── EnergyService.java              [NEW]
│   │       Service interface for energy calculations
│   │
│   ├── mock/
│   │   └── MockEnergyService.java          [UPDATED]
│   │       Now implements EnergyService
│   │
│   └── real/
│       ├── log/
│       │   └── JdbcLogService.java         [EXISTING]
│       │
│       ├── room/
│       │   └── JdbcRoomService.java        [EXISTING]
│       │
│       └── energy/                         [NEW]
│           └── JdbcEnergyService.java      [NEW]
│               JDBC-backed energy service
│
└── controller/
    └── EnergyController.java               [UPDATED]
        Updated to use EnergyService interface

src/main/resources/db/
├── init-activity-log.sql    [EXISTING]
├── init-auth.sql             [EXISTING]
├── init-rooms.sql            [EXISTING]
├── init-schedules.sql        [EXISTING]
└── init-energy.sql           [NEW - optional]

src/test/java/at/jku/se/smarthome/service/
└── JdbcEnergyServiceTest.java       [NEW]
    Comprehensive unit tests
```

## Nominal Power Constants by Device Type

```
Device Type          Nominal Power    Purpose
──────────────────────────────────────────────────────
SWITCH               10W              Basic on/off
DIMMER               12W              Dimmable light
THERMOSTAT           50W              HVAC control
SENSOR               2W               Motion/temp detection
BLIND                20W              Motor-driven blinds
LIGHT                15W              LED/incandescent
COFFEE_MACHINE       1500W            High-power appliance
```

## Aggregation Examples

### Example 1: Daily by Device
```
Device: "Living Room Light" (LIGHT = 15W)
Activity Log:
  - 08:00 ON  (logged)
  - 12:00 OFF (logged)
  - 18:00 ON  (logged)
  - 22:00 OFF (logged)

On-time calculation:
  - 08:00→12:00 = 4 hours
  - 18:00→22:00 = 4 hours
  Total = 8 hours

Consumption:
  15W × 8h = 120Wh = 0.12kWh

Display: "120.00 Wh" or "0.12 kWh"
```

### Example 2: Daily by Room
```
Living Room devices:
  - Light (120 Wh as above)
  - Thermostat (50W × 24h = 1200 Wh)
  - Blind (20W × 0.5h = 10 Wh)

Room Total:
  120 + 1200 + 10 = 1330 Wh = 1.33 kWh

Display: "1.33 kWh"
```

### Example 3: Household Daily
```
All Rooms:
  - Living Room: 1330 Wh
  - Bedroom: 800 Wh
  - Kitchen: 2500 Wh
  - Hallway: 100 Wh
  - Bathroom: 200 Wh

Household Total:
  1330 + 800 + 2500 + 100 + 200 = 4930 Wh = 4.93 kWh

Display: "4.93 kWh"
```

## Performance Considerations

### Caching Strategy
```
┌─────────────────────────────────────────────┐
│ In-Memory Cache (JdbcEnergyService)        │
├─────────────────────────────────────────────┤
│ Key: "2026-05-04:daily"                    │
│ Value: Map<device, wh> + householdWh      │
│ TTL: Varies                                 │
│   - Today's data: TTL 30 seconds           │
│   - Historical: TTL 5 minutes              │
│   - Invalidated on LogEntry added         │
└─────────────────────────────────────────────┘
```

### Query Optimization
- Index on activity_log(timestamp, device, action)
- Indexed joins with room-device mappings
- Prepared statements for performance
- Connection pooling via DatabaseConfig

## Error Handling

```
JdbcEnergyService Query
         │
    ┌────┴────┐
    │          │
    ▼          ▼
Success      SQLException
    │              │
    │              ▼
    │         Log error
    │              │
    │              ▼
    │         Throw IllegalStateException
    │              │
    ▼              ▼
Return data    Caller handles
(cached)       or shows UI error
```

## Testing Strategy Overview

```
┌──────────────────────────────────────┐
│   Unit Tests (JdbcEnergyServiceTest) │
├──────────────────────────────────────┤
│ ✓ Known on-time → Expected Wh        │
│ ✓ Room sum = Device sum              │
│ ✓ Household = Room sum               │
│ ✓ Zero-activity case                 │
│ ✓ Time boundary cases                │
│ ✓ Multiple room/device isolation     │
│ ✓ Edge cases (nulls, missing data)   │
└──────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────┐
│   Integration Test                   │
├──────────────────────────────────────┤
│ • Full activity log scenario          │
│ • End-to-end calculation             │
│ • Controller display verification    │
└──────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────┐
│   Manual Smoke Test (AC#5)            │
├──────────────────────────────────────┤
│ • Turn device ON for known interval  │
│ • Verify Wh calculation matches      │
│ • Verify reactive update (≤2s)       │
│ • Verify no manual reload needed     │
└──────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────┐
│   Build Verification                 │
├──────────────────────────────────────┤
│ • mvn verify GREEN                   │
│ • Tests pass                         │
│ • PMD compliant                      │
│ • Coverage acceptable (>80%)         │
└──────────────────────────────────────┘
```

## Related Services Integration

### With LogService
- JdbcEnergyService reads from activity_log (created/managed by LogService)
- Subscribes to LogService updates for reactive refresh
- Uses LogEntry model from LogService

### With RoomService
- JdbcEnergyService queries device→room mappings
- Aggregates consumption by room grouping
- Handles devices not in any room gracefully

### With DeviceNominalPower
- JdbcEnergyService queries DeviceEnergyConstants
- Uses nominal power × on-time formula
- Falls back to sensible default if device type unknown

---

## References

- **Issue**: #23 (FR-14 — Energy consumption dashboard)
- **User Stories**: US-25, US-26
- **Existing Service Pattern**: JdbcLogService
- **Database Config**: DatabaseConfig, DatabaseSettings
