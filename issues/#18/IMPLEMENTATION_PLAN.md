# FR-09 Persistent Schedule Implementation Plan

Date: 2026-04-10
Status: Implemented
Scope: replace the current mock-only FR-09 schedule implementation with a JDBC-backed persistent schedule implementation

## Implementation Outcome
The planned persistence-first direction is now implemented.

Completed in this slice:
- moved prototype services into `service/mock`
- moved JDBC auth classes into `service/real/auth`
- introduced the controller-facing `service/api/ScheduleService`
- added `service/api/ServiceRegistry` to wire the real schedule service by default
- added `service/real/schedule/JdbcScheduleService`
- added `src/main/resources/db/init-schedules.sql`
- extended `Schedule` with `deviceId`
- rewired `SchedulesController`, `VacationModeController`, and `SmartHomeApp` to the service contract
- preserved mock test compatibility where needed during the package split

Not completed in this slice:
- a dedicated JDBC repository class separate from `JdbcScheduleService`
- repository-level persistence tests
- full persistence for rooms, devices, or vacation mode configuration

## Goal
Implement FR-09 as a real persistent feature backed by PostgreSQL, while clearly separating mock services from real services in the codebase so the project no longer mixes prototype logic and production-style JDBC logic in the same package.

## Why A New Plan Is Needed
The current FR-09 work improved `MockScheduleService`, but it still keeps schedule definitions in memory and does not persist them across restarts. That does not satisfy the persistence expectation for a real implementation.

This plan supersedes the mock-first FR-09 approach as the implementation target.

## Required Architectural Direction

### 1. Separate Mock And Real Services Into Different Packages
The service layer should be reorganized so the package structure itself communicates which code is prototype-only and which code is the JDBC-backed implementation.

Recommended package split:
- `src/main/java/at/jku/se/smarthome/service/mock/`
- `src/main/java/at/jku/se/smarthome/service/real/`
- `src/main/java/at/jku/se/smarthome/service/api/`

Recommended usage:
- `service/mock`: all current `Mock*Service` classes that remain in-memory prototypes
- `service/real`: JDBC-backed or otherwise persistent implementations
- `service/api`: interfaces or controller-facing contracts used by both mock and real implementations

Minimum FR-09 package moves and additions:
- Move `MockScheduleService` to `service/mock/MockScheduleService`
- Move `MockVacationModeService` to `service/mock/MockVacationModeService`
- Move `MockRoomService`, `MockLogService`, and `MockNotificationService` only if they are directly referenced from schedule flows and need package consistency
- Move `JdbcUserRegistrationStore` out of the mixed root `service` package into `service/real/auth/JdbcUserRegistrationStore`
- Move `UserRegistrationStore` into either `service/api` or `service/real/auth` depending on whether controllers will ever see it directly
- Add a controller-facing `ScheduleService` contract in `service/api`
- Add a real JDBC-backed implementation such as `service/real/schedule/JdbcScheduleService`

This refactor should happen before or together with the persistent schedule work, not afterward.

### 2. Controllers Must Depend On Contracts, Not Mock Classes
`SchedulesController` and any scheduler startup code should no longer hardcode `MockScheduleService.getInstance()`.

Recommended direction:
- Introduce a `ScheduleService` interface in `service/api`
- Introduce a small service locator or application wiring class that chooses the real implementation for FR-09
- Keep JavaFX controllers mostly unchanged by injecting the controller-facing service contract rather than calling mock singletons directly

## Scope Boundary
This plan focuses on making schedules persistent.

In scope:
- schedule CRUD persistence
- active/inactive state persistence
- persistent recurring execution source of truth
- schedule startup loading from PostgreSQL
- schedule execution against current device state in the running JavaFX app
- clear package separation between mock and real services

Out of scope for this slice:
- full device persistence
- full room persistence
- conflict detection logic for FR-15
- rule persistence for FR-10/FR-11
- persistent vacation mode configuration unless needed to keep FR-21 working with the new schedule IDs

## Important Constraint: Devices Are Still Mock-Backed
The current device and room catalog is still mock/in-memory. Because of that, the real schedule implementation cannot rely on a persisted device table yet.

The safest short-term persistence model is:
- store `device_id` in the schedules table
- resolve `device_id` against the current in-memory room/device catalog at runtime

Why `device_id` instead of `device_name`:
- device names can change in the UI
- IDs are more stable for execution and lookup
- it avoids brittle schedule execution after a rename

Known limitation to document:
- schedules targeting dynamically created mock devices may not remain valid across restarts until device persistence also exists

## Recommended Database Design

### New SQL Script Strategy
Keep the current plain JDBC approach used for auth and add one checked-in schedule schema script under `src/main/resources/db/`.

Recommended new script:
- `src/main/resources/db/init-schedules.sql`

Recommended table:

`scheduled_actions`
- `id` UUID or stable text primary key
- `name` varchar not null
- `device_id` varchar not null
- `action` varchar not null
- `time_pattern` varchar not null
- `recurrence` varchar not null
- `active` boolean not null default true
- `created_at` timestamp not null default current_timestamp
- `updated_at` timestamp not null default current_timestamp
- optional `last_triggered_at` timestamp null

Recommended indexes:
- index on `active`
- index on `device_id`

Recommended constraints:
- check recurrence in (`Daily`, `Weekdays`, `Weekends`, `Weekly`)
- check `name` is non-empty via application validation, not only SQL

## Recommended Java Structure

### Contracts
Add in `service/api`:
- `ScheduleService`
- optional `ScheduleRepository` only if that abstraction improves testability

`ScheduleService` should cover:
- `ObservableList<Schedule> getSchedules()`
- `Schedule getScheduleById(String id)`
- `Schedule addSchedule(...)`
- `boolean updateSchedule(...)`
- `boolean toggleSchedule(String id)`
- `boolean deleteSchedule(String id)`
- `boolean executeSchedule(String id)`
- `void startRecurringExecution()`
- `void stopRecurringExecution()`
- `boolean hasConflicts(String id)`

### Real Implementation
Add in `service/real/schedule`:
- `JdbcScheduleService`
- `JdbcScheduleRepository` or a store class similar to the auth implementation
- optional internal `PersistedSchedule` record

Responsibilities:
- load schedules from PostgreSQL
- persist CRUD changes immediately
- maintain an `ObservableList<Schedule>` for JavaFX table binding
- drive recurring execution from persisted active schedules
- update `last_triggered_at` when a schedule runs successfully

### Mock Implementation
Keep in `service/mock`:
- `MockScheduleService`

Role after this change:
- fallback/testing prototype only
- no longer the controller default for the real application path

## Scheduler Execution Model
The easiest pragmatic model remains an in-process scheduler inside the JavaFX desktop application, but the schedule definitions themselves come from PostgreSQL.

Recommended runtime flow:
1. App startup creates the real `ScheduleService`
2. Service loads persisted schedules into an observable list
3. A background scheduler checks due schedules every 15 seconds
4. Due schedules are resolved against current in-memory devices by `device_id`
5. Successful executions update logs and `last_triggered_at`
6. CRUD operations update both the database and the observable list

This preserves the current UI responsiveness while making schedule data persistent.

## UI And Model Changes

### Schedule Model
The current `Schedule` model stores a device display string only.

Recommended update:
- add `deviceId`
- keep `device` or rename it to `deviceName` for display

This allows:
- persistent storage by stable identifier
- existing JavaFX table display with human-readable names

### SchedulesController
Required controller changes:
- stop directly binding to `MockScheduleService`
- consume `ScheduleService` instead
- save selected `deviceId` and display name together
- preserve current add/edit/delete UX with minimal visual change

## Interaction With Vacation Mode
`MockVacationModeService` currently references schedule IDs directly.

Recommended first step:
- keep vacation mode mock-backed for now
- make it depend on the `ScheduleService` contract instead of `MockScheduleService`
- if it stores only schedule IDs in memory, it can still work with the real schedule implementation

Do not expand this FR-09 slice to persist all vacation mode configuration unless it becomes necessary for integration stability.

## Migration Sequence

### Phase 1: Package Refactor
1. Create `service/api`, `service/mock`, and `service/real` packages
2. Move current mock schedule-related services under `service/mock`
3. Move current auth JDBC classes under `service/real/auth`
4. Update imports across controllers and services

### Phase 2: Schedule Persistence Foundation
1. Add `init-schedules.sql`
2. Add a JDBC schedule repository/store
3. Add real schedule persistence types and mapping code
4. Load schema on first connection similarly to the auth path

### Phase 3: Controller-Facing Real Service
1. Introduce `ScheduleService`
2. Implement `JdbcScheduleService`
3. Keep an observable in-memory cache synchronized with PostgreSQL
4. Wire the controller to the real implementation by default

### Phase 4: Recurring Execution Against Persistent Data
1. Move the recurring scheduler logic from the mock implementation into the real schedule service
2. Poll persisted active schedules
3. Execute actions against the current device catalog
4. Update `last_triggered_at` after success

### Phase 5: Integration Cleanup
1. Update vacation mode to depend on the `ScheduleService` interface
2. Update any test setup still assuming `MockScheduleService` is the default
3. Ensure app startup and shutdown manage the real scheduler lifecycle

### Phase 6: Validation
1. Compile the project
2. Run focused schedule tests
3. Add repository tests for CRUD persistence
4. Add integration-style tests for service reload from database

## Files Likely To Change

### Existing files likely to move or change
- `src/main/java/at/jku/se/smarthome/service/MockScheduleService.java`
- `src/main/java/at/jku/se/smarthome/service/MockVacationModeService.java`
- `src/main/java/at/jku/se/smarthome/service/MockRoomService.java`
- `src/main/java/at/jku/se/smarthome/service/MockLogService.java`
- `src/main/java/at/jku/se/smarthome/service/MockNotificationService.java`
- `src/main/java/at/jku/se/smarthome/service/MockUserService.java`
- `src/main/java/at/jku/se/smarthome/service/JdbcUserRegistrationStore.java`
- `src/main/java/at/jku/se/smarthome/service/UserRegistrationStore.java`
- `src/main/java/at/jku/se/smarthome/controller/SchedulesController.java`
- `src/main/java/at/jku/se/smarthome/controller/VacationModeController.java`
- `src/main/java/at/jku/se/smarthome/SmartHomeApp.java`
- `src/main/java/at/jku/se/smarthome/model/Schedule.java`

### New files likely to be added
- `src/main/java/at/jku/se/smarthome/service/api/ScheduleService.java`
- `src/main/java/at/jku/se/smarthome/service/real/schedule/JdbcScheduleService.java`
- `src/main/java/at/jku/se/smarthome/service/real/schedule/JdbcScheduleRepository.java`
- `src/main/resources/db/init-schedules.sql`
- focused repository and service tests for persistent schedules

## Verification Criteria
1. Schedules created in the UI are stored in PostgreSQL and survive application restart.
2. Edited schedules persist their updated values.
3. Deleted schedules are removed from PostgreSQL.
4. The schedules table in the UI is populated from persisted records rather than hardcoded seed data.
5. Active schedules execute automatically based on persisted `time_pattern` and `recurrence`.
6. Schedule execution still logs activity entries correctly.
7. Vacation mode continues to work against persisted schedule IDs.
8. Controllers no longer import `MockScheduleService` directly in the real application path.
9. Mock services and real services are physically separated into different packages/folders.

## Security And Quality Notes
- Use prepared statements only.
- Keep database settings out of source control and reuse the existing database config flow.
- Continue using generic user-facing error messages for database failures.
- Do not run JDBC operations on the JavaFX UI thread when loading or mutating schedules.
- Keep the scheduler thread daemonized and stop it on application shutdown.

## Recommended First Execution Step
Start by refactoring the service package layout so the codebase cleanly distinguishes `mock` and `real` services. Then implement `JdbcScheduleService` behind a `ScheduleService` contract instead of further extending `MockScheduleService`.