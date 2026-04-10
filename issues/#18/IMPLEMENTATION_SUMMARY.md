# FR-09 Implementation Summary

Date: 2026-04-10
Status: Implemented
Scope: persistent recurring unconditional time-based schedules for device actions

## Goal
Provide a dedicated scheduling flow where users can define recurring, unconditional device actions by time pattern, store them persistently in PostgreSQL, and execute them automatically in the running application.

## What Exists In The Current Codebase
- A dedicated `Schedule` model now stores both `deviceId` and a display `device` name so schedules can persist against stable device identifiers.
- A controller-facing `ScheduleService` contract in `service/api` isolates JavaFX controllers from concrete mock versus real implementations.
- `ServiceRegistry` wires the real JDBC-backed schedule implementation as the application default.
- `JdbcScheduleService` bootstraps the schedule schema, loads persisted schedules into an observable list, persists CRUD changes immediately, and runs automatic due-schedule execution.
- The JavaFX schedules screen still provides the existing owner-facing add, edit, delete, and table-management flow.
- Vacation mode integration still works through schedule IDs and clears deleted schedules.
- The legacy mock schedule service remains available under `service/mock` and includes compatibility helpers so existing tests continue to run after the package split.

## Files Representing This Slice
- `src/main/java/at/jku/se/smarthome/model/Schedule.java`
- `src/main/java/at/jku/se/smarthome/service/api/ScheduleService.java`
- `src/main/java/at/jku/se/smarthome/service/api/ServiceRegistry.java`
- `src/main/java/at/jku/se/smarthome/service/real/schedule/JdbcScheduleService.java`
- `src/main/java/at/jku/se/smarthome/service/real/auth/JdbcUserRegistrationStore.java`
- `src/main/java/at/jku/se/smarthome/service/real/auth/UserRegistrationStore.java`
- `src/main/java/at/jku/se/smarthome/service/mock/MockScheduleService.java`
- `src/main/java/at/jku/se/smarthome/service/mock/MockVacationModeService.java`
- `src/main/java/at/jku/se/smarthome/controller/SchedulesController.java`
- `src/main/java/at/jku/se/smarthome/SmartHomeApp.java`
- `src/main/resources/db/init-schedules.sql`
- `src/main/resources/at/jku/se/smarthome/view/schedules-view.fxml`
- `src/test/java/at/jku/se/smarthome/service/TestMockScheduleService.java`

## Implemented Behavior
- Owners can create, edit, delete, and toggle recurring schedules through the schedules screen.
- Schedule definitions persist in PostgreSQL and are reloaded into the UI-facing observable list on service startup.
- The persisted schema stores schedule identity, name, `device_id`, `device_name`, action, time pattern, recurrence, active state, and last-trigger metadata.
- `executeSchedule(scheduleId)` resolves devices by stable `deviceId` first, falls back to display name when necessary, applies the action, and writes an activity-log entry.
- A background dispatcher in the real schedule service checks due schedules every 15 seconds and evaluates them minute-accurately.
- Time patterns are parsed from strings such as `07:00`, `07:00 AM`, and weekly variants such as `Fri 09:00 AM`.
- Recurrence handling supports `Daily`, `Weekdays`, `Weekends`, and `Weekly`.
- UI action labels remain aligned with the execution parser for switches, dimmers, thermostats, and covers/blinds.
- The application lifecycle now starts and stops recurring execution through the real schedule service instead of calling the mock singleton directly.
- The codebase now clearly separates prototype services in `service/mock` from real JDBC-backed services in `service/real`.

## Implementation Notes
- Devices and rooms are still mock-backed, so the persistent schedule service executes against the in-memory device catalog while using PostgreSQL as the source of truth for schedule definitions.
- Weekly schedules expect the time pattern to include a weekday token such as `Mon`, `Tue`, or `Friday`.
- The schedule UI still stores the time pattern as a string rather than a strongly typed recurrence object.
- Conflict detection remains outside this slice and belongs to FR-15 rather than FR-09.
- Vacation mode configuration itself is still mock-backed, but it now interacts through the shared schedule contract.

## Deferred Follow-Up
- Conflict detection still returns `false` in `MockScheduleService.hasConflicts(...)` and should be implemented later under FR-15.
- The schedule dialog does not yet perform strict input validation for malformed time-pattern strings before save.
- Devices and rooms are not yet persistent, so schedule targets depend on the current mock device catalog remaining consistent.
- There are no dedicated repository-level tests yet for direct database CRUD behavior in `JdbcScheduleService`.

## Execution Checklist
- [x] Introduce a dedicated schedule model for recurring time-based actions
- [x] Persist schedules in PostgreSQL through a real JDBC-backed schedule service
- [x] Add a controller-facing schedule contract and wire the real service by default
- [x] Separate prototype services and real services into distinct package folders
- [x] Keep the schedule-management JavaFX screen operational against the real service
- [x] Execute active schedules automatically based on persisted time and recurrence data
- [x] Preserve vacation-mode integration for schedule deletion and effective schedule lookup
- [x] Keep focused mock-service tests passing after the package split
- [ ] Implement real conflict detection for contradictory schedules under FR-15
- [ ] Add dedicated persistence-layer tests for the real schedule service

## Verification Snapshot
- `TestMockScheduleService` verifies unknown IDs, inactive schedules, missing devices, turn-on, turn-off, dimmer brightness changes, thermostat actions, recurring due-time execution, weekday filtering, weekly filtering, and logging behavior.
- `TestMockUserServiceLoginLogout` and `TestMockUserServiceRegistration` verify the auth path after moving JDBC auth classes into `service/real/auth`.
- Verified with `mvn clean compile -DskipTests`.
- Verified with `mvn "-Dtest=TestMockScheduleService,TestMockUserServiceLoginLogout,TestMockUserServiceRegistration" test`.
- The targeted Maven test run completed successfully. JaCoCo emitted JDK instrumentation warnings for newer runtime classes, but the test run still passed.

## Security And Extension Compliance Summary
- `security/baseline`: Enforced
- `SECURITY-03` Application-Level Logging: Compliant because successful automatic and manual schedule executions write structured activity-log entries through the shared log service.
- `SECURITY-11` Secure Design Principles: Compliant because recurring scheduling logic remains encapsulated in the schedule service and application lifecycle rather than being scattered across controllers.
- `SECURITY-01`, `SECURITY-02`, `SECURITY-04`, `SECURITY-05`, `SECURITY-06`, `SECURITY-07`, `SECURITY-08`, `SECURITY-09`, `SECURITY-10`, `SECURITY-12`, `SECURITY-13`: N/A for this slice because no new storage, network surface, authentication flow, or supply-chain change is introduced.

## Recommended Next Implementation Step
The next schedule-related step should be FR-15 conflict detection so overlapping contradictory schedules can be identified before execution, followed by persistence of the device and room catalog so schedule targets also survive restarts.