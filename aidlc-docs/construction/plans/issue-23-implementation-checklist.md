# Issue #23 Implementation Checklist

**Issue**: FR-14 — Energy consumption dashboard  
**User Stories**: US-25, US-26  
**Date Created**: May 4, 2026  
**Status**: Planning Phase

---

## PHASE 1: Foundation (Service Interface & Data Models)

### Task 1.1: Create EnergyService Interface
- [ ] Create file: `src/main/java/at/jku/se/smarthome/service/api/EnergyService.java`
- [ ] Define methods:
  - [ ] `getDailyByDevice(LocalDate date) → Map<String, Double>`
  - [ ] `getWeeklyByDevice(int isoWeekOfYear, int year) → Map<String, Double>`
  - [ ] `getDailyByRoom(LocalDate date) → Map<String, Double>`
  - [ ] `getWeeklyByRoom(int isoWeekOfYear, int year) → Map<String, Double>`
  - [ ] `getHouseholdDaily(LocalDate date) → double`
  - [ ] `getHouseholdWeekly(int isoWeekOfYear, int year) → double`
  - [ ] `getDeviceNominalPower(String deviceType) → int`
  - [ ] `getAllDeviceTypes() → Set<String>`
- [ ] Add comprehensive JavaDoc comments
- [ ] Add PMD suppressions where needed
- [ ] Compile and verify no errors

### Task 1.2: Create DeviceEnergyConstants Class
- [ ] Create file: `src/main/java/at/jku/se/smarthome/config/DeviceEnergyConstants.java`
- [ ] Add constants for each device type:
  - [ ] SWITCH_POWER_W = 10W
  - [ ] DIMMER_POWER_W = 12W
  - [ ] THERMOSTAT_POWER_W = 50W
  - [ ] SENSOR_POWER_W = 2W
  - [ ] BLIND_POWER_W = 20W
  - [ ] LIGHT_POWER_W = 15W
  - [ ] COFFEE_MACHINE_POWER_W = 1500W
- [ ] Implement `getPowerWatts(String deviceType)` method
- [ ] Add unit tests for edge cases (unknown device type, null)
- [ ] Compile and verify

### Task 1.3: Update MockEnergyService Implementation
- [ ] Open: `src/main/java/at/jku/se/smarthome/service/mock/MockEnergyService.java`
- [ ] Add `implements EnergyService` to class declaration
- [ ] Implement all interface methods:
  - [ ] `getDailyByDevice()` - return mock data
  - [ ] `getWeeklyByDevice()` - return mock data
  - [ ] `getDailyByRoom()` - return mock data
  - [ ] `getWeeklyByRoom()` - return mock data
  - [ ] `getHouseholdDaily()` - return mock total
  - [ ] `getHouseholdWeekly()` - return mock total
  - [ ] `getDeviceNominalPower()` - return from constants
  - [ ] `getAllDeviceTypes()` - return set of types
- [ ] Keep existing `getSnapshot()` and helper methods
- [ ] Update existing helper methods to use DeviceEnergyConstants
- [ ] Run tests: `mvn -Dtest=TestMockEnergyService test`
- [ ] Verify PMD compliance: `mvn pmd:check`

---

## PHASE 2: JDBC Energy Service Implementation

### Task 2.1: Create JdbcEnergyService Class
- [ ] Create directory: `src/main/java/at/jku/se/smarthome/service/real/energy/`
- [ ] Create file: `JdbcEnergyService.java`
- [ ] Add class structure:
  - [ ] Package declaration
  - [ ] Imports (JDBC, LocalDate/LocalDateTime, Collections)
  - [ ] Class declaration: `public final class JdbcEnergyService implements EnergyService`
  - [ ] Constants:
    - [ ] INIT_SCRIPT_PATH = "/db/init-energy.sql"
    - [ ] MAX_DAYS_HISTORY = 365
  - [ ] Static fields:
    - [ ] private static JdbcEnergyService instance
    - [ ] private static final Object INSTANCE_LOCK = new Object()
  - [ ] Instance fields:
    - [ ] private final Map<String, Double> dailyCache
    - [ ] private final AtomicLong lastCacheTime
  - [ ] Constructor (private, loads initial data)
  - [ ] getInstance() static method (synchronized singleton)
  - [ ] resetForTesting() static method

### Task 2.2: Implement Daily/Weekly Query Methods
- [ ] Implement `getDailyByDevice(LocalDate date)`:
  - [ ] Query activity_log for given date
  - [ ] Group by device name
  - [ ] Calculate on-time between consecutive state changes
  - [ ] Multiply by nominal power from constants
  - [ ] Return Map with device names and Wh values
  - [ ] Handle devices with no activity (return 0)
  
- [ ] Implement `getWeeklyByDevice(int isoWeekOfYear, int year)`:
  - [ ] Calculate week start and end dates
  - [ ] Query activity_log for week range
  - [ ] Group by device name
  - [ ] Calculate on-time for full week
  - [ ] Return aggregated Wh values

- [ ] Implement `getDailyByRoom(LocalDate date)`:
  - [ ] Get daily consumption by device
  - [ ] Join with room-device relationships
  - [ ] Group by room
  - [ ] Sum device consumption per room
  - [ ] Return Map<room, wh>

- [ ] Implement `getWeeklyByRoom(int isoWeekOfYear, int year)`:
  - [ ] Get weekly consumption by device
  - [ ] Group by room via device mappings
  - [ ] Sum consumption per room
  - [ ] Return Map<room, wh>

- [ ] Implement `getHouseholdDaily(LocalDate date)`:
  - [ ] Sum all daily device consumption
  - [ ] Return total Wh

- [ ] Implement `getHouseholdWeekly(int isoWeekOfYear, int year)`:
  - [ ] Sum all weekly device consumption
  - [ ] Return total Wh

- [ ] Implement `getDeviceNominalPower(String deviceType)`:
  - [ ] Delegate to DeviceEnergyConstants.getPowerWatts()

- [ ] Implement `getAllDeviceTypes()`:
  - [ ] Return set of all known device types from constants

### Task 2.3: Implement Helper Methods
- [ ] Implement `openConnection()`:
  - [ ] Load DatabaseConfig
  - [ ] Get connection from DatabaseSettings
  - [ ] Return Connection

- [ ] Implement `ensureSchema()`:
  - [ ] Check if schema initialized
  - [ ] Load init-energy.sql
  - [ ] Execute schema creation
  - [ ] Set initialized flag

- [ ] Implement `calculateDeviceOnTimeForDate()`:
  - [ ] Query activity_log for device on given date
  - [ ] Calculate on-time from consecutive ON/OFF pairs
  - [ ] Handle edge cases:
    - [ ] ON at start without prior OFF
    - [ ] ON at end without following OFF
    - [ ] No state changes (device already ON or OFF)
  - [ ] Return total on-time in hours

- [ ] Implement `loadInitScript()`:
  - [ ] Read SQL from classpath resource
  - [ ] Return as String

### Task 2.4: Add Caching & Performance
- [ ] Implement in-memory cache for today's data
- [ ] Add cache invalidation on activity log updates
- [ ] Add cache TTL (5-10 minutes for historical data)
- [ ] Log cache hits/misses (DEBUG level)

### Task 2.5: Add Reactive Update Support
- [ ] Subscribe to LogService changes (if available)
- [ ] Listen for new LogEntry additions
- [ ] Invalidate affected cache entries
- [ ] Trigger UI refresh notification
- [ ] Ensure updates complete within 2 seconds

---

## PHASE 3: Database Schema

### Task 3.1: Create Database Schema File
- [ ] Create file: `src/main/resources/db/init-energy.sql`
- [ ] Add schema (if pre-calculating):
  - [ ] energy_daily table (optional)
  - [ ] energy_weekly table (optional)
  - [ ] Indexes for performance
- [ ] Add comments explaining structure
- [ ] Test script parses without errors

### Task 3.2: Verify Schema Initialization
- [ ] Test that schema initializes on first JdbcEnergyService use
- [ ] Verify tables/indexes created correctly
- [ ] Test with test database
- [ ] Clean up and reset for next test run

---

## PHASE 4: Unit Testing

### Task 4.1: Create Test File Structure
- [ ] Create file: `src/test/java/at/jku/se/smarthome/service/JdbcEnergyServiceTest.java`
- [ ] Add test class with:
  - [ ] @BeforeClass: Initialize test database
  - [ ] @AfterClass: Clean up test data
  - [ ] @Before: Reset service instance
  - [ ] @After: Clean activity logs

### Task 4.2: Test Scenario 1 - Known On-Time → Expected Wh
- [ ] Create test case: `testKnownOnTimeCalculation()`
- [ ] Setup:
  - [ ] Log device OFF at 00:00
  - [ ] Log device ON at 08:00
  - [ ] Log device OFF at 17:00 (9 hours)
- [ ] Calculate: Thermostat 50W × 9h = 450Wh
- [ ] Assert: getDailyByDevice returns 450 Wh for device
- [ ] Test multiple devices with different on-times
- [ ] Verify within rounding tolerance (±1 Wh)

### Task 4.3: Test Scenario 2 - Room Sum = Device Sum
- [ ] Create test case: `testRoomSumEqualsDeviceSum()`
- [ ] Setup:
  - [ ] Create 3 devices in Living Room
  - [ ] Log on-times for each device
  - [ ] Create 2 devices in Bedroom
  - [ ] Log on-times for each device
- [ ] Calculate: Sum device Wh for each room
- [ ] Assert: Room total = sum of room's devices
- [ ] Repeat for multiple rooms

### Task 4.4: Test Scenario 3 - Household Sum = Room Sum
- [ ] Create test case: `testHouseholdSumEqualsRoomSum()`
- [ ] Setup: Create multiple rooms with multiple devices
- [ ] Calculate:
  - [ ] All rooms' consumption for date
  - [ ] Household consumption for same date
- [ ] Assert: Household total = sum of all rooms

### Task 4.5: Test Scenario 4 - Zero-Activity Case
- [ ] Create test case: `testZeroActivityReturnsZero()`
- [ ] Setup:
  - [ ] Add device to room
  - [ ] Don't log any activity for that device
- [ ] Assert: getDailyByDevice returns 0 Wh (not null)
- [ ] Assert: Device appears in results with 0 value
- [ ] Assert: Room includes device with 0 value

### Task 4.6: Test Scenario 5 - Time Boundary Cases
- [ ] Create test case: `testDeviceOnAtStartOfPeriod()`
  - [ ] Device already ON before period start
  - [ ] Calculate on-time correctly until OFF or period end
  - [ ] Assert correct Wh calculation

- [ ] Create test case: `testDeviceOnAtEndOfPeriod()`
  - [ ] Device turns ON during period
  - [ ] No OFF logged before period ends
  - [ ] Assert calculates on-time to period end

- [ ] Create test case: `testMultipleTogglesInPeriod()`
  - [ ] Device toggled ON/OFF multiple times
  - [ ] Assert total on-time sums all ON periods
  - [ ] Assert correct Wh value

### Task 4.7: Test Scenario 6 - Multiple Rooms & Devices
- [ ] Create test case: `testMultipleRoomsIsolation()`
- [ ] Setup:
  - [ ] Living Room: 3 devices with specific on-times
  - [ ] Bedroom: 2 devices with specific on-times
  - [ ] Kitchen: 1 device with specific on-time
- [ ] Assert: Each room's total correct
- [ ] Assert: Rooms don't bleed consumption into each other
- [ ] Assert: Household = sum of all rooms

### Task 4.8: Run All Tests
- [ ] Execute: `mvn -Dtest=JdbcEnergyServiceTest test`
- [ ] Verify all tests pass
- [ ] Check code coverage (target > 80%)
- [ ] Verify PMD compliance

---

## PHASE 5: Controller & UI Integration

### Task 5.1: Update EnergyController
- [ ] Open: `src/main/java/at/jku/se/smarthome/controller/EnergyController.java`
- [ ] Modify service injection:
  - [ ] Change: `MockEnergyService energyService = MockEnergyService.getInstance()`
  - [ ] To: Dependency injection via ServiceRegistry or constructor
  - [ ] OR: Use ServiceRegistry lookup pattern
- [ ] Update refresh mechanism:
  - [ ] Listen for LogService changes
  - [ ] Implement auto-refresh timer (≤ 2s)
  - [ ] Trigger updateDashboard() on state changes

### Task 5.2: Add Time Navigation
- [ ] Add buttons/controls for: Today, Yesterday, This Week, Last Week
- [ ] Implement `handleTodayClick()` - use LocalDate.now()
- [ ] Implement `handleYesterdayClick()` - use LocalDate.now().minusDays(1)
- [ ] Implement `handleThisWeekClick()` - use WeekFields.ISO.weekOfYear()
- [ ] Implement `handleLastWeekClick()` - use weekOfYear - 1
- [ ] Update refresh to use selected date instead of current date
- [ ] Display selected time period in UI

### Task 5.3: Update Dashboard Display
- [ ] Update loadSummaryData():
  - [ ] Call EnergyService methods instead of mock
  - [ ] Pass selected date/week
  - [ ] Format values using energy formatter
  
- [ ] Update loadDeviceChart():
  - [ ] Query EnergyService.getDailyByDevice() or getWeeklyByDevice()
  - [ ] Display all devices with values
  - [ ] Show 0 Wh for devices with no activity

- [ ] Update loadRoomChart():
  - [ ] Query EnergyService.getDailyByRoom() or getWeeklyByRoom()
  - [ ] Show all rooms with values
  - [ ] Ensure sum validation on UI

### Task 5.4: Verify Controller Compiles
- [ ] Run: `mvn clean compile -DskipTests`
- [ ] Fix any compilation errors
- [ ] Verify PMD compliance

---

## PHASE 6: Testing & Validation

### Task 6.1: Integration Test
- [ ] Create test: `src/test/java/at/jku/se/smarthome/integration/EnergyDashboardIntegrationTest.java`
- [ ] Setup complete activity log scenario
- [ ] Test end-to-end flow:
  - [ ] Log device state changes
  - [ ] Query energy service
  - [ ] Verify calculated values
  - [ ] Display in controller
  - [ ] Verify dashboard shows correct values

### Task 6.2: Manual Smoke Test (AC#5 - Known Interval)
- [ ] Clear all activity logs
- [ ] Turn ON a device (e.g., Thermostat)
- [ ] Wait exactly 60 minutes
- [ ] Turn OFF device
- [ ] Query dashboard for that device
- [ ] Verify displayed Wh = nominal_power_w × 1 hour
  - [ ] Thermostat: 50W × 1h = 50 Wh = 0.05 kWh
- [ ] Check formatting (should show "50.00 Wh" or "0.05 kWh")
- [ ] Tolerance: ±1 Wh acceptable

### Task 6.3: Reactive Update Test (AC#5 - Refresh)
- [ ] Setup initial dashboard view
- [ ] Toggle a device ON
- [ ] Wait up to 2 seconds
- [ ] Verify dashboard updates without manual reload
- [ ] Check:
  - [ ] Device consumption updated
  - [ ] Room total updated
  - [ ] Household total updated
- [ ] Repeat for OFF toggle
- [ ] Verify all aggregates refresh

### Task 6.4: Run Full Build & Verify
- [ ] Execute: `mvn verify`
- [ ] Verify:
  - [ ] All tests pass
  - [ ] Coverage meets threshold (if set)
  - [ ] PMD clean (no violations)
  - [ ] Checkstyle compliant
  - [ ] Package builds successfully

---

## PHASE 7: Code Quality & Documentation

### Task 7.1: Code Review Preparation
- [ ] Review EnergyService interface for clarity
- [ ] Review JdbcEnergyService for:
  - [ ] PMD violations (suppressed where justified)
  - [ ] Code duplication
  - [ ] Performance issues
  - [ ] Security (SQL injection via PreparedStatement)
  - [ ] Resource management (connection closure)

### Task 7.2: Add JavaDoc Comments
- [ ] Add class-level JavaDoc to:
  - [ ] EnergyService interface
  - [ ] JdbcEnergyService
  - [ ] DeviceEnergyConstants
- [ ] Add method-level JavaDoc for all public methods
- [ ] Include examples in complex method docs
- [ ] Add @param, @return, @throws tags

### Task 7.3: Update User Story Documentation
- [ ] Open: `docs/user-stories.md`
- [ ] Mark US-25 as implemented:
  - [ ] Status: ✓ Implemented
  - [ ] Completion date: [date]
  - [ ] Implementation reference: Issue #23, feature branch
  
- [ ] Mark US-26 as implemented:
  - [ ] Status: ✓ Implemented
  - [ ] Completion date: [date]
  - [ ] Implementation reference: Issue #23, feature branch

### Task 7.4: Update Architecture Documentation
- [ ] Update `docs/system-architecture.md`:
  - [ ] Add EnergyService layer description
  - [ ] Document calculation method (on-time from logs)
  - [ ] Document aggregation strategy
  - [ ] Add JdbcEnergyService integration point

### Task 7.5: Update README if Needed
- [ ] Check if energy dashboard feature should be documented
- [ ] Add any user-facing usage notes

---

## PHASE 8: Final Verification (Definition of Done)

### Task 8.1: Build & Test Green
- [ ] Run: `mvn verify`
- [ ] Verify:
  - [ ] ✓ Build SUCCESS
  - [ ] ✓ All unit tests passed
  - [ ] ✓ All integration tests passed
  - [ ] ✓ No PMD violations
  - [ ] ✓ Code coverage acceptable (>80% for new code)

### Task 8.2: Test Coverage Verification
- [ ] Known on-time → expected Wh: ✓ PASS
- [ ] Room sum = device sum: ✓ PASS
- [ ] Household sum = room sum: ✓ PASS
- [ ] Zero-activity case: ✓ PASS
- [ ] All aggregation levels tested: ✓ PASS

### Task 8.3: Manual Smoke Test Verification
- [ ] Device ON for known interval: ✓ PASS
- [ ] Displayed Wh matches formula (50W × 1h = 50Wh): ✓ PASS
- [ ] Within rounding tolerance (±1 Wh): ✓ PASS
- [ ] Formatting correct (Wh/kWh with 2 decimals): ✓ PASS

### Task 8.4: Reactive Update Verification
- [ ] Dashboard refreshes after state change: ✓ PASS
- [ ] Refresh within 2 seconds: ✓ PASS
- [ ] No manual reload required: ✓ PASS
- [ ] All aggregates update correctly: ✓ PASS

### Task 8.5: Code Review & Merge
- [ ] Create pull request with description
- [ ] Address reviewer comments
- [ ] Resolve all conflicts
- [ ] Get approval: ✓ APPROVED
- [ ] Merge to main branch

### Task 8.6: Documentation Update
- [ ] ✓ User stories (US-25, US-26) marked as implemented
- [ ] ✓ Architecture docs updated
- [ ] ✓ Code reviewed and commented
- [ ] ✓ README updated (if applicable)

---

## Notes & Issues During Implementation

(To be filled during implementation)

### Implementation Notes
- 

### Blockers Encountered
- 

### Design Decisions Made
- 

### Performance Optimizations Applied
- 

### Lessons Learned
- 

---

## Sign-Off

**Implementation Started**: [Date]  
**Implementation Completed**: [Date]  
**Tested By**: [Name]  
**Code Reviewed By**: [Name]  
**Merged By**: [Name]  
**Merge Date**: [Date]

---

## Related Files
- Implementation Plan: `aidlc-docs/construction/plans/issue-23-energy-dashboard-plan.md`
- Issue: #23 (FR-14)
- User Stories: US-25, US-26
- Related Issues: FR-18 (out of scope)
