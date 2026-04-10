# SmartHome Orchestrator - Chat Worklog

## Purpose
This document records how the SmartHome prototype was developed iteratively during the chat. It complements `INITIAL_IMPLEMENTATION_SUMMARY.md`, which only reflects the initial prototype state.

## Working Style Used In This Chat
- Implement features directly in the JavaFX prototype instead of stopping at design discussion.
- Keep the app mock-based and requirement-focused.
- Validate changes frequently with Maven compile checks.
- Fix runtime/FXML issues immediately when they block feature work.
- Prefer small, focused updates on top of the existing structure.
- Keep Owner vs Member restrictions enforced both in navigation and inside controllers.

## Chronological Development Log

### 1. Initial Prototype
- Implemented the baseline SmartHome prototype from `ui_prototype.md`.
- Created the JavaFX shell, login flow, dashboard, mock device model, mock service layer, and initial styling.

### 2. Extended UI Implementation
- Implemented the remaining screens from `ui_prototype_extended.md`.
- Added models such as `User`, `Room`, `Rule`, `Schedule`, `Scene`, `LogEntry`, and `NotificationEntry`.
- Added mock services and controllers for the new screens.

### 3. Login and Registration
- Completed the login and register flow.
- Wired `SmartHomeApp.java`, `LoginController`, `RegisterController`, `login-view.fxml`, and `register-view.fxml` together.

### 4. FXML and Runtime Stabilization
- Fixed loading failures in `energy-view.fxml` and `simulation-view.fxml`.
- Removed unsupported or malformed FXML attributes and simplified problematic parts of the view definitions.

### 5. Rooms and Device Management
- Added room edit and delete support.
- Added device CRUD support inside rooms.
- Fixed a duplicated `Actions` column regression in the rooms table.

### 6. Device Control and Live State Display
- Implemented dedicated device-control functionality for switches, dimmers, thermostats, sensors, and covers/blinds.
- Moved manual device control into a dedicated `devices-control-view.fxml` to avoid crowding the rooms view.
- Bound device state to JavaFX properties for live updates.

### 7. Activity Logging
- Implemented activity logging for manual device changes.
- Extended log entries to include device, room, action, and actor.
- Added activity log viewing and CSV export support.

### 8. Schedules
- Added schedule create, edit, and delete support.
- Added device-aware action options so valid actions depend on the selected device type.
- Excluded sensors from schedulable device targets.

### 9. Rules
- Fixed the missing add-rule dialog.
- Expanded rule support to cover source device, target device, trigger type, condition, action, and execution state.
- Added notifications and logging for rule execution.
- Added global toast popups in the shell for important events.

### 10. Role-Based Access
- Implemented Owner and Member roles.
- Restricted management views and actions for Members.
- Fixed a bug where the Owner did not see all views after login because the shell role state had not been refreshed correctly.

### 11. Settings Simplification
- Removed dark mode and multi-language options because they were out of scope for the requirements.
- Simplified the settings view and controller accordingly.

### 12. Window Startup Behavior
- Changed startup behavior so the main window opens maximized.

### 13. Energy Dashboard
- Implemented FR-14 with day/week switching.
- Added household total, per-room, per-device, and timeline visualizations.
- Fixed a chart refresh issue where the room chart disappeared after switching aggregation.
- Moved CSV export to the top of the energy view.

### 14. Scenes
- Implemented scene create, edit, delete, and activation.
- Added structured device-state selection instead of free-text scene actions.
- Improved the scene editor with duplicate-device prevention, richer labels, compact summaries, and automatic dialog resizing.
- Fixed the device selection bug in the scene editor.
- Corrected notifications so scene execution is labeled as a scene event instead of a rule event.

### 15. Login UX
- Added Enter-to-login behavior in the password field.

### 16. IoT Integration Mock
- Implemented FR-18 as a mock MQTT integration flow.
- Added mock connection settings, connect/disconnect behavior, connection test, and mock device discovery.

### 17. Full-Day Simulation Mock
- Implemented FR-19 as an isolated replay simulation.
- Added start time, initial conditions, active rule selection, accelerated playback, replay log, and simulated device-state output.
- Kept the simulation isolated from live device state.

### 18. User Management Mock
- Implemented FR-20 for inviting Members by email and revoking access.
- Changed revocation from deletion to status-based access control.
- Added restore access and status filtering.

### 19. Vacation Mode Mock
- Implemented FR-21 in mock form.
- Added persisted vacation-mode state with selected schedule, start date, end date, and enabled state.
- Added validation, visible override status, activity logging, and notification integration.
- Added schedule-view banner text showing when vacation mode overrides normal schedules.

### 20. Terminology Alignment
- Renamed visible UI wording from `Automation` to `Rules` to match the requirement language.
- Kept internal method names and file names stable to avoid unnecessary code churn.

### 21. UI Polish Fixes
- Fixed a device-control styling issue where large switch buttons could lose visible `ON` text after repeated toggling.

## Validation Pattern Used Throughout
- Frequent validation with:

```bash
mvn -q -DskipTests compile
```

- Periodic full compile checks with:

```bash
mvn clean compile -DskipTests
```

- Runtime checks using:

```bash
mvn javafx:run
```

## Key Architectural Decisions Reinforced During The Chat
- Keep everything mock-based and in-memory.
- Use singleton services as the state source for each feature area.
- Use JavaFX properties for UI reactivity.
- Prefer dedicated screens for complex functions instead of overloading a single view.
- Treat `MockUserService` as the role/authentication source of truth.

## Remaining Scope Boundaries
- No real backend persistence.
- No real MQTT or physical IoT integration.
- No background automation engine executing real schedules over time.
- No real database or multi-user synchronization.
