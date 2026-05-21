# Systemdokumentation

## Überblick

Kurze Beschreibung von Zweck und Umfang des Systems.

## Architektur

- UI: `SimpleCalculator`
- Domänenlogik: Operationen in `operators/`
- Erzeugung: `OperationFactory`

### Energy Consumption Service (Issue #23)

**Purpose**: Calculate household energy consumption from device activity logs

**Architecture**:
- **Interface**: `EnergyService` - Service contract with 8 query methods
- **Implementations**:
  - `MockEnergyService` - In-memory demo data
  - `JdbcEnergyService` - PostgreSQL-backed real implementation
- **Registry**: `ServiceRegistry.getEnergyService()` - DI entry point
- **Data Source**: `activity_log` table with device ON/OFF state changes
- **Aggregation Levels**: Device → Room → Household
- **Time Scales**: Daily (24h) and Weekly (ISO 8601)
- **Caching**: 30s TTL (daily), 5min TTL (weekly)

**Calculation Formula**:
- Wh = Nominal Power (W) × On-Time (hours)
- Room Consumption = Sum of Device Consumption
- Household Consumption = Sum of Room Consumption

**UI Integration** (EnergyController):
- Time Navigation: Today, Yesterday, This Week, Last Week buttons
- Dashboard Display: Pie chart (rooms), bar chart (devices), line chart (timeline)
- Reactive Updates: LogService listener framework for automatic refresh (≤2s)

### MQTT IoT Integration (FR-18)

**Purpose**: Optional integration layer for connecting physical MQTT-enabled devices alongside virtual devices

**Architecture**:
- **Interface**: `IoTIntegrationService` - Decouples app from MQTT implementation
- **Implementations**:
  - `MockIoTIntegrationService` - In-memory mock for testing without broker
  - `MqttIntegrationService` - Real MQTT client using Eclipse Paho library
- **Configuration**: Broker address, port, optional username/password
- **Controllers**: `IoTSettingsController` - Settings UI and device management
- **Enable/Disable**: Opt-in toggle; all FR-01..FR-17 features work without MQTT

**MQTT Topic Convention**:
- **State Topic**: `smarthome/<deviceId>/state` - Device publishes state here
- **Command Topic**: `smarthome/<deviceId>/cmd` - App publishes commands here
- **QoS**: Level 1 (At Least Once) for reliable delivery
- **Subscriptions**: Wildcard pattern `smarthome/+/state` for all devices

**Graceful Degradation**:
- If broker unreachable: App starts normally, integration devices show OFFLINE
- No UI-blocking retry loops
- All state changes logged to activity_log with actor="MQTT"

**UI Features**:
- Settings tab: Configure broker, test connection, discover devices
- Device table: Lists discovered devices with status badges
- Bidirectional control: UI commands publish to device topics; device updates reflect in UI (≤2s)
- Permission checking: Only Owner can configure broker or pair devices

## Wichtige Designentscheidungen

Dokumentiert technische Entscheidungen und deren Begründung.

## Erweiterungspunkte

- Neue Operatoren implementieren
- Factory erweitern
- Zusätzliche Tests ergänzen

## Build und Qualität

- Build-Tool: Maven
- Tests: JUnit
- Statische Analyse: PMD

## Testfallbeschreibung und Testabdeckung

- Beschreibung der wichtigsten Testfälle
- Aktuelle Testabdeckung: 80 % (ohne UI-Klassen)





