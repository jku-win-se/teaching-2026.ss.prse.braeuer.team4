# FR-05 / FR-06 / FR-08 DB Integration — Implementation Summary

Date: 2026-04-12
Status: Implemented
Scope: Persist device state changes and activity log entries to PostgreSQL

## Goal

Connect the manual device control (FR-06) and activity log (FR-08) features —
previously wired only to in-memory mock services — to the shared Aiven PostgreSQL
database, so that device states and log entries survive application restarts.
Also complete the device management DB integration (FR-05) by ensuring rename
and delete are properly routed through the already-wired `JdbcRoomService`.

## What Existed Before This Slice

- `JdbcRoomService` was already implemented and wired via `ServiceRegistry`,
  covering room CRUD, device add/remove/rename (FR-05).
- `DevicesController` used `ServiceRegistry.getRoomService()` for room/device
  management but still called `MockSmartHomeService` for state changes (toggle,
  brightness, temperature, blinds). `MockSmartHomeService` holds its own
  in-memory device list with hard-coded IDs (`dev-001` etc.) that never match
  the UUID-based IDs from the database — so every state-change call silently
  failed at runtime.
- `ActivityLogController` and `DevicesController` both used `MockLogService`,
  which starts with five hard-coded fake log entries and writes nothing to the DB.

## Files Changed or Created

| File | Change |
|------|--------|
| `service/api/RoomService.java` | Added `updateDeviceState`, `updateDeviceBrightness`, `updateDeviceTemperature` |
| `service/api/LogService.java` | New interface for log operations |
| `service/api/ServiceRegistry.java` | Added `getLogService()` + `setLogServiceForTesting()` |
| `service/real/room/JdbcRoomService.java` | Implemented the three new state-update methods |
| `service/real/log/JdbcLogService.java` | New — JDBC-backed log service |
| `service/mock/MockRoomService.java` | Added in-memory implementations of the three state-update methods |
| `controller/DevicesController.java` | Replaced `MockSmartHomeService` calls with `roomService.update*()`; replaced `MockLogService` with `ServiceRegistry.getLogService()` |
| `controller/ActivityLogController.java` | Replaced `MockLogService` with `ServiceRegistry.getLogService()` |
| `resources/db/init-activity-log.sql` | New — creates `activity_log` table if not present |

## Implemented Behavior

- Every device state change (toggle on/off, brightness slider, thermostat
  +/− buttons, open/close blind, sensor inject) now calls
  `roomService.updateDeviceState/Brightness/Temperature(deviceId, value)`,
  which immediately issues an `UPDATE devices SET ... WHERE id = ?` to the DB
  and also updates the in-memory `Device` object so the JavaFX UI stays in sync.
- `JdbcLogService` creates the `activity_log` table on first connection via
  `init-activity-log.sql`. On startup it loads the last 200 entries (newest
  first) into the observable list so the activity log view is pre-populated
  from real persisted data. Each `addLogEntry()` call does an immediate
  `INSERT` and prepends the new entry to the in-memory list.
- `ServiceRegistry.getLogService()` returns the singleton `JdbcLogService`
  instance and is the single source of log access for all controllers.
- `MockRoomService` gained the same three state-update method signatures for
  test compatibility (in-memory only, no DB call).

## Implementation Notes

- `MockSmartHomeService` is no longer referenced by any controller. It remains
  in `service/mock` because some existing tests may still depend on it directly.
- The `activity_log` table uses `BIGSERIAL` as the primary key so ordering by
  `id DESC` gives insertion order without relying on the timestamp string format.
- `JdbcLogService` does not contain any mock seed data — the log view starts
  empty on a fresh database and fills from real user actions.

## Execution Checklist

- [x] Extend `RoomService` interface with device state update methods
- [x] Implement state updates in `JdbcRoomService` with DB persistence
- [x] Implement state updates in `MockRoomService` for test compatibility
- [x] Create `LogService` interface
- [x] Create `JdbcLogService` with schema init, load-on-startup, write-through
- [x] Add `ServiceRegistry.getLogService()` wired to `JdbcLogService`
- [x] Wire `DevicesController` to DB-backed room service for all state changes
- [x] Wire `DevicesController` and `ActivityLogController` to `ServiceRegistry` log
- [x] `mvn clean compile` passes with 0 errors

## What Is Still Required

- A `.env` file in the project root must be created by each developer locally:
  ```
  SMARTHOME_DATABASE_URL="postgres://avnadmin:<password>@<host>:<port>/defaultdb?sslmode=require"
  ```
  The file is gitignored and must not be committed. The Aiven service port can
  be found in the Aiven web console on the service overview page.
- `MockSmartHomeService` is now unused in production — it can be removed in a
  follow-up cleanup once dependent tests are migrated.
