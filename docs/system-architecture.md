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





