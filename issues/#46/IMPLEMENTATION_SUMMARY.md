# FR-46: Update Schedules to Use Real Services and JdbcLogService

## Issue Description
Schedules were implemented before the Device Service was finished. Schedules must now use the real Room/Device Service instead of mock classes. Also ensure that for the execution of the Schedules the logs are correctly written with JdbcLogService.

## Current State Analysis
- `JdbcScheduleService` already uses `ServiceRegistry.getRoomService()` which provides `JdbcRoomService` (real implementation)
- `JdbcScheduleService` was using `MockLogService.getInstance()` for logging
- Schedule execution calls `logService.addLogEntry()` to record actions

## Required Changes
1. Update `JdbcScheduleService` to use `ServiceRegistry.getLogService()` instead of `MockLogService.getInstance()`
2. Ensure `ServiceRegistry.getLogService()` returns `JdbcLogService` (already configured)
3. Verify that schedule execution properly logs to the database

## Implementation Details
- Changed import from `MockLogService` to `LogService`
- Updated field initialization: `private final LogService logService = ServiceRegistry.getLogService();`
- Kept `MockNotificationService` as no real notification service exists yet
- Maintained all other functionality unchanged

## Verification
- Project compiles successfully
- `TestJdbcScheduleService` passes (3 tests)
- Schedule execution will now persist logs to the database via `JdbcLogService`

## Files Modified
- `src/main/java/at/jku/se/smarthome/service/real/schedule/JdbcScheduleService.java`

## Dependencies
- Relies on existing `ServiceRegistry.getLogService()` returning `JdbcLogService`
- No new dependencies added