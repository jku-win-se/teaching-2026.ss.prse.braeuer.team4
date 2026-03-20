# API Documentation

## REST APIs

No REST, GraphQL, gRPC, or WebSocket endpoints were detected in this repository. The application is a desktop JavaFX client with internal controller-to-service interactions.

## Internal APIs

### SmartHomeApp
- **Methods**:
  - `start(Stage primaryStage)`
  - `main(String[] args)`
- **Purpose**: Bootstraps login, registration, and main-shell scenes.

### MainController
- **Methods**:
  - `setMainCallback(MainCallback callback)`
  - `refreshSessionState()`
  - Multiple FXML event handlers such as `showDevices()`, `showRooms()`, `showEnergy()`, and `handleLogout()`
- **Purpose**: Provides application-shell navigation and owner-only UI gating.

### LoginController
- **Methods**:
  - `setLoginCallback(LoginCallback callback)`
  - `clearFields()`
  - FXML actions `handleLogin()` and `handleRegisterClick()`
- **Purpose**: Validates login form input and delegates authentication to MockUserService.

### MockUserService
- **Methods**:
  - `register(String email, String username, String password, String confirmPassword)`
  - `login(String email, String password)`
  - `getCurrentUserEmail()`
  - `getCurrentUserRole()`
  - `getCurrentUser()`
  - `isOwner()`
  - `canManageSystem()`
  - `getUsers()`
  - `inviteUser(String email, String role)`
  - `revokeUser(String email)`
  - `restoreUser(String email)`
  - `logout()`
- **Purpose**: Exposes authentication, authorization state, and user administration.

### MockSmartHomeService
- **Methods**:
  - `getDevices()`
  - `getDeviceById(String deviceId)`
  - `toggleDevice(String deviceId)`
  - `setBrightness(String deviceId, int brightness)`
  - `setTemperature(String deviceId, double temperature)`
  - `authenticate(String email, String password)`
  - `getCurrentUser()`
  - `logout()`
- **Purpose**: Exposes base device-control behavior and a legacy authentication path.

### MockIoTIntegrationService
- **Methods**:
  - `getConfiguration()`
  - `saveConfiguration(boolean enabled, String broker, int port, String username, String password)`
  - `testConnection(String broker, String portValue)`
  - `connect()`
  - `disconnect()`
  - `refreshDevices()`
  - `getDiscoveredDevices()`
- **Purpose**: Stores mock MQTT settings and simulates device discovery.

### MockLogService
- **Methods**:
  - `getLogs()`
  - `addLogEntry(String device, String room, String action, String actor)`
  - `getLogsByRoom(String room)`
  - `getLogsByDevice(String device)`
  - `getLogsByActor(String actor)`
  - `getUniqueDevices()`
  - `exportToCSV()`
- **Purpose**: Records activity entries and supports filtered retrieval and export.

### CalculatorController
- **Methods**:
  - `applyAction(String displayText, CalcAction action)`
  - `appendDigit(String displayText, int digit)`
  - `clear()`
- **Purpose**: Encapsulates calculator behavior independently of the UI.

## Data Models

### Device
- **Fields**: `id`, `name`, `type`, `room`, `state`, `brightness`, `temperature`
- **Relationships**: Belongs to a room and is manipulated by multiple controllers and services.
- **Validation**: Service layer enforces some brightness and temperature bounds.

### User
- **Fields**: Email, username, password, role, status.
- **Relationships**: Stored by MockUserService and referenced by login, registration, and user-management flows.
- **Validation**: Duplicate email and password confirmation checks exist, but passwords are stored in plain text.

### Room
- **Fields**: Room identity plus associated devices.
- **Relationships**: Used by DevicesController and MockRoomService for filtering and organization.

### Rule, Scene, and Schedule
- **Fields**: Automation-specific configuration for conditions, desired device states, and timing.
- **Relationships**: Managed by dedicated controllers and services.

### LogEntry and NotificationEntry
- **Fields**: Timestamped user-facing event metadata.
- **Relationships**: Produced by service and controller actions and displayed in activity or notification views.

### IntegrationDevice and VacationModeConfig
- **Fields**: External integration metadata and vacation-mode settings.
- **Relationships**: Used by IoT and vacation-mode screens.