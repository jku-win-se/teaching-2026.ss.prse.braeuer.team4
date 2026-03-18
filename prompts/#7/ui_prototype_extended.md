# Role
Act as a Senior JavaFX Developer and UI/UX Specialist.

# Context
We are continuing development of the "SmartHome Orchestrator" (JavaFX, Java 17+). 
The basic shell and dashboard exist, but many views still show "Coming Soon". 
I need you to implement ALL remaining views to make the prototype fully functional 
with mock data for demonstration purposes.

# Tech Stack
- JavaFX 17+
- FXML for all Views
- CSS for consistent styling
- MVC Pattern
- ObservableLists for real-time updates (FR-07)
- Mock Service Layer (no real backend yet)

# Task
Implement the following views with full UI components and mock logic:

---

## 1. Authentication Views (FR-01, FR-02)

### `register-view.fxml`
- Fields: Email, Password, Confirm Password, Username
- Validation: Email format, password match
- Button: Register → redirects to login
- Mock: Store in `MockUserService`

### `login-view.fxml` (Enhancement)
- Fields: Email, Password
- Button: Login → loads main shell
- Link: "No account? Register"
- Mock: Accept any valid email format

---

## 2. Room Management View (FR-03, FR-05)

### `rooms-view.fxml`
- `TableView<Room>` with columns: Name, Device Count, Actions
- Buttons per row: Edit Name, Delete
- "Add Room" button with Dialog (TextField for name)
- Role check: Hide Edit/Delete for "Member" role (FR-13)

### `room-detail-view.fxml`
- Shows all devices in selected room
- "Add Device" button → opens device creation dialog
- Device cards with type-specific controls (FR-06)

---

## 3. Device Management View (FR-04, FR-05, FR-06)

### `device-dialog.fxml` (Modal)
- Fields: Name (TextField), Type (ComboBox: Switch, Dimmer, Thermostat, Sensor, Shutter), Room (ComboBox)
- Button: Create/Update
- Validation: Name required, Type required

### Device Control Components (Reusable)
- `switch-control.fxml`: ToggleButton (On/Off)
- `dimmer-control.fxml`: Slider (0-100%) + Label
- `thermostat-control.fxml`: NumberField + Stepper (+/-)
- `shutter-control.fxml`: Buttons (Open/Close/Stop) + Progress indicator
- `sensor-control.fxml`: Read-only Label + "Simulate Value" Button (FR-06)

---

## 4. Activity Log View (FR-08, FR-16)

### `activity-log-view.fxml`
- `TableView<LogEntry>` with columns: Timestamp, Device, Action, Actor (User/Rule)
- Filter: Date range picker, Device dropdown
- Button: "Export CSV" → mock file download (FR-16)
- Auto-refresh every 5 seconds (FR-07)

---

## 5. Schedule/Time Plans View (FR-09)

### `schedules-view.fxml`
- `TableView<Schedule>` with columns: Name, Device, Action, Time, Recurrence (Daily/Weekly)
- Button: "Add Schedule" → opens dialog
- Toggle: Active/Inactive per schedule
- Conflict warning label if overlapping schedules detected (FR-15)

### `schedule-dialog.fxml`
- Fields: Name, Device (ComboBox), Action (ComboBox), Time (Picker), Recurrence (ComboBox)
- Button: Save

---

## 6. Rule Engine View (FR-10, FR-11, FR-12, FR-15)

### `rules-view.fxml`
- `TableView<Rule>` with columns: Name, Trigger Type, Condition, Action, Status
- Button: "Add Rule" → opens rule editor
- Toggle: Enable/Disable rule
- Notification badge for rule execution alerts (FR-12)

### `rule-editor-dialog.fxml`
- Section 1 (WHEN): Trigger Type (ComboBox: Time, Sensor Threshold, Device State Change)
- Section 2 (THEN): Action (ComboBox: Turn On, Turn Off, Set Value)
- Section 3: Target Device (ComboBox)
- Conflict detection: Show warning if rule conflicts with existing rules (FR-15)
- Button: Save Rule

---

## 7. Energy Dashboard View (FR-14, FR-16)

### `energy-view.fxml`
- `PieChart`: Consumption by Room
- `BarChart`: Consumption by Device (Top 5)
- `LineChart`: Consumption over Time (Day/Week toggle)
- Summary Cards: Total kWh, Estimated Cost, CO2 Savings
- Button: "Export CSV" (FR-16)
- Mock data generator for realistic values

---

## 8. Scenes View (FR-17)

### `scenes-view.fxml`
- Grid of Scene Cards (e.g., "Movie Night", "Away", "Morning")
- Each card: Scene Name, Icon, "Activate" Button
- Button: "Create New Scene"
- Scene includes multiple device states

### `scene-editor-dialog.fxml`
- Fields: Scene Name, Description
- Device State Selector: Multi-select devices with target states
- Button: Save Scene

---

## 9. User Management View (FR-13, FR-20)

### `users-view.fxml` (Owner only)
- `TableView<User>` with columns: Email, Role, Status, Actions
- Button: "Invite Member" → Dialog with Email + Role selection
- Action per row: Revoke Access (FR-20)
- Role badge: Owner (Gold), Member (Silver)
- Hide this view entirely for "Member" role (FR-13)

### `invite-dialog.fxml`
- Fields: Email, Role (ComboBox: Owner, Member)
- Button: Send Invite (mock)

---

## 10. Vacation Mode View (FR-21)

### `vacation-mode-view.fxml`
- Toggle Switch: Vacation Mode On/Off
- Date Pickers: Start Date, End Date
- Schedule Selector: ComboBox (select which schedule to apply)
- Info Label: "Normal schedules will be overridden during this period"
- Preview: List of affected devices and actions
- Button: Activate Vacation Mode

---

## 11. Simulation View (FR-19)

### `simulation-view.fxml`
- Configuration Section:
  - Start Time (Picker)
  - Simulation Speed (ComboBox: 1x, 10x, 100x)
  - Initial Sensor Values (Input fields)
  - Active Rules (Multi-select CheckBoxes)
- Control Section:
  - Button: "Start Simulation"
  - Button: "Pause"
  - Button: "Reset"
- Visualization Section:
  - Timeline slider showing progress through simulated day
  - Live device state updates (read-only during simulation)
  - Log output area showing triggered rules
- Warning: "Simulation does not affect live system"

---

## 12. IoT Integration Settings (FR-18)

### `iot-settings-view.fxml`
- Toggle: Enable IoT Integration (MQTT)
- Fields: Broker URL, Port, Username, Password (masked)
- Connection Status Indicator (Green/Red)
- Button: "Test Connection" (mock)
- Button: "Save Settings"
- Info Label: "Optional - Virtual devices work without this"

---

## 13. Notifications & Alerts (FR-12, FR-15)

### `notification-component.fxml` (Reusable)
- Toast-style popup for rule execution notifications
- Color coding: Success (Green), Warning (Orange), Error (Red)
- Auto-dismiss after 5 seconds
- Notification center icon in header with badge count

### `conflict-warning-dialog.fxml`
- Shows when scheduling/rule conflicts detected (FR-15)
- Lists conflicting rules/schedules
- Options: "Override", "Cancel", "Edit Conflicting Item"

---

## 14. Settings View

### `settings-view.fxml`
- Profile Section: Change Password, Update Email
- Appearance: Light/Dark Mode Toggle
- Language: ComboBox (DE/EN)
- Data Section: "Export All Data" Button
- About Section: Version info, JKU Linz SE Practicum SS 2026

---

# Mock Service Requirements

Create/Extend these mock services:

1. **`MockUserService.java`**
   - Methods: register(), login(), getCurrentUser(), getRole(), inviteUser(), revokeUser()
   - Store users in-memory with password hashing simulation (NFR-02)

2. **`MockDeviceService.java`**
   - Methods: getDevices(), addDevice(), updateDevice(), deleteDevice(), controlDevice()
   - ObservableList for real-time UI updates (FR-07)

3. **`MockRuleService.java`**
   - Methods: getRules(), addRule(), updateRule(), deleteRule(), evaluateRules()
   - Conflict detection logic (FR-15)

4. **`MockEnergyService.java`**
   - Methods: getConsumptionByDevice(), getConsumptionByRoom(), getConsumptionOverTime()
   - Generate realistic mock data

5. **`MockLogService.java`**
   - Methods: getLogs(), addLogEntry(), exportLogsCSV()
   - Auto-generate entries on device actions (FR-08)

6. **`MockSimulationService.java`**
   - Methods: startSimulation(), pauseSimulation(), getSimulationStatus()
   - Time-acceleration logic (FR-19)

---

# Styling Requirements (styles.css)

- **Color Palette:**
  - Primary: #2c3e50 (Dark Blue-Grey)
  - Accent: #3498db (Blue)
  - Success: #27ae60 (Green)
  - Warning: #f39c12 (Orange)
  - Error: #e74c3c (Red)
  - Background: #ecf0f1 (Light Grey)

- **Components:**
  - Cards: White background, subtle shadow, rounded corners
  - Buttons: Primary style for main actions, secondary for cancel
  - Tables: Alternating row colors, hover effects
  - Navigation: Active state highlighting

- **Responsive:**
  - Minimum window size: 1024x768
  - Controls should not overflow on resize

---

# Navigation Structure

Update `MainController.java` to handle all views:
