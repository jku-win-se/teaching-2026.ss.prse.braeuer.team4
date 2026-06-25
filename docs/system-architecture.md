# Systemdokumentation — SmartHome Orchestrator

Diese Dokumentation beschreibt die Anwendung aus **Entwicklersicht**: Architektur,
wichtige Designentscheidungen, Erweiterungspunkte sowie Build- und Qualitätssicherung.
Die Bedienung aus Anwendersicht ist im [Benutzerhandbuch](./user-handbook.md) beschrieben.

## Inhaltsverzeichnis

1. [Überblick](#1-überblick)
2. [Architektur im Überblick](#2-architektur-im-überblick)
3. [Schichten und wichtige Klassen](#3-schichten-und-wichtige-klassen)
4. [Wichtige Designentscheidungen](#4-wichtige-designentscheidungen)
5. [Persistenz und Konfiguration](#5-persistenz-und-konfiguration)
6. [Erweiterungspunkte](#6-erweiterungspunkte)
7. [Build und Qualitätssicherung](#7-build-und-qualitätssicherung)
8. [Testfälle und Testabdeckung](#8-testfälle-und-testabdeckung)
9. [Weiterführende Diagramme](#9-weiterführende-diagramme)

---

## 1. Überblick

Der **SmartHome Orchestrator** ist eine JavaFX-Desktop-Anwendung zur Verwaltung eines
simulierten Smart Homes. Benutzer organisieren Geräte in Räumen, steuern sie manuell,
automatisieren sie über Regeln und Zeitpläne, überwachen den Energieverbrauch und
verwalten Benutzer mit unterschiedlichen Rollen (Owner/Member).

Kernfunktionsbereiche:

- **Identität & Zugriff**: Registrierung, Login, Rollen (Owner/Member), Einladungen
- **Struktur**: Räume und virtuelle Geräte anlegen, umbenennen, löschen
- **Gerätesteuerung**: Schalter, Dimmer, Thermostate, Jalousien, Sensoren
- **Automatisierung**: Regeln (IF-Trigger-THEN-Action), Zeitpläne, Szenen, Vacation Mode
- **Monitoring**: Activity-Log, Energie-Dashboard, In-App-Benachrichtigungen
- **Erweiterung**: optionale MQTT-Integration für physische Geräte, Tagessimulation

Die Anwendung ist als **monolithische, mehrschichtige Desktop-Applikation** umgesetzt.
Persistente Daten liegen in einer **PostgreSQL**-Datenbank; für Tests und Demos existiert
zu jedem Dienst eine **In-Memory-Mock-Implementierung**.

## 2. Architektur im Überblick

Die Anwendung folgt einer klassischen Drei-Schichten-Architektur (MVC-orientiert):

```
┌──────────────────────────────────────────────────────────────┐
│  Presentation Layer (JavaFX)                                 │
│  FXML-Views  ◄──►  *Controller  ──►  ViewNavigator           │
└───────────────────────────┬──────────────────────────────────┘
                            │ löst Dienste auf über
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  Service Layer                                               │
│  ServiceRegistry (Service Locator / DI)                      │
│  service.api.*  (Interfaces / abstrakte Verträge)            │
│      ├── service.mock.*   (In-Memory, Tests/Demo)            │
│      └── service.real.*   (JDBC, Produktion)                 │
│  service.rule.*  (RuleEvaluator, ConflictDetection, ...)     │
└───────────────────────────┬──────────────────────────────────┘
                            │ liest / schreibt
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  Domain Model (model.*)  +  Persistenz                       │
│  Device, Room, User, Rule, Scene, Schedule, LogEntry, ...    │
│  PostgreSQL  ◄──  DatabaseConfig  (.env / Properties)        │
└──────────────────────────────────────────────────────────────┘
```

Einstiegspunkt der Anwendung ist `at.jku.se.smarthome.SmartHomeApp` (JavaFX `Application`).
Eine vollständige Komponentenübersicht als PlantUML-Diagramm findet sich unter
[`docs/uml/01-architecture-overview.puml`](./uml/01-architecture-overview.puml).

## 3. Schichten und wichtige Klassen

### Presentation Layer — `controller/` + `resources/.../view/`

15 FXML-Views mit zugehörigen Controllern. Die Navigation läuft über den
`ViewNavigator`, die App-Shell hält `MainController`.

| Controller | Aufgabe |
|---|---|
| `LoginController`, `RegisterController` | Authentifizierung |
| `MainController`, `ViewNavigator` | App-Shell, Navigation zwischen Views |
| `RoomsController`, `DevicesController` | Räume & Geräte verwalten/steuern |
| `RulesController`, `SchedulesController` | Regeln & Zeitpläne |
| `ScenesController`, `VacationModeController` | Szenen & Vacation Mode |
| `SimulationController` | Tagessimulation |
| `EnergyController`, `ActivityLogController` | Energie-Dashboard & Log |
| `UsersController`, `SettingsController` | Benutzerverwaltung & Einstellungen |
| `IoTSettingsController` | MQTT-Integration |

Controller enthalten keine Persistenzlogik — sie holen sich Dienste ausschließlich
über die `ServiceRegistry`.

### Service Layer — `service/`

- **`service.api`** — Verträge (Interfaces bzw. abstrakte Klassen) je Domäne:
  `UserService`, `RoomService`, `RuleService`, `SceneService`, `ScheduleService`,
  `LogService`, `NotificationService`, `EnergyService`, `IoTIntegrationService`
  sowie der zentrale `ServiceRegistry`.
- **`service.mock`** — In-Memory-Implementierungen (`Mock*Service`) für Tests und
  Demobetrieb ohne Datenbank.
- **`service.real`** — produktive, JDBC-gestützte Implementierungen (`Jdbc*Service`),
  je Domäne in einem Unterpaket (`auth`, `room`, `rule`, `scene`, `schedule`,
  `energy`, `log`, `notification`, `mqtt`).
- **`service.rule`** — Regel-Querschnittslogik: `RuleEvaluator`, `RuleValidator`,
  `ConflictDetectionService`, `WeekdaySpec`.

Siehe [`docs/uml/03-service-layer.puml`](./uml/03-service-layer.puml).

### Domain Model — `model/`

Reine Datentypen ohne Persistenz- oder UI-Abhängigkeit: `Device`, `IntegrationDevice`,
`Room`, `User`, `Rule`, `Scene`, `Schedule`, `LogEntry`, `NotificationEntry`,
`NotificationType`, `SchedulingConflict`, `SimulationDeviceState`, `VacationModeConfig`.
Siehe [`docs/uml/02-domain-model.puml`](./uml/02-domain-model.puml).

### Konfiguration — `config/`

`DatabaseConfig` und `DatabaseSettings` lesen die Verbindungsdaten; `DeviceEnergyConstants`
hält die nominalen Leistungswerte je Gerätetyp für die Energieberechnung.

## 4. Wichtige Designentscheidungen

**Mehrschichtige Monolith-Architektur.** Für eine Einzelplatz-Desktop-Anwendung mit
überschaubarem Team ist ein Monolith mit klaren Schichtgrenzen einfacher zu bauen, zu
testen und zu deployen als ein verteiltes System. Die Schichtgrenzen halten den Code
trotzdem modular und erweiterbar.

**Service-Interface + Doppelimplementierung (Mock & JDBC).** Jede Domäne ist über ein
Interface in `service.api` von ihrer Implementierung entkoppelt. Dadurch:

- laufen Unit-Tests schnell und ohne Datenbank gegen die `Mock*`-Variante,
- ist die Persistenz austauschbar, ohne Controller anzufassen,
- kann die App auch ohne DB demonstriert werden.

**`ServiceRegistry` als Service Locator / DI-Einstiegspunkt.** Controller kennen keine
konkreten Implementierungen, sondern fragen Dienste zentral über die `ServiceRegistry`
ab. Die Registry nutzt das **Initialization-on-Demand-Holder-Pattern** für thread-sichere,
lazy initialisierte Singletons und gibt standardmäßig die `Jdbc*`-Dienste zurück. Für Tests
können über `setXxxServiceForTesting(...)` Mock-Implementierungen injiziert und über
`resetForTesting()` zurückgesetzt werden.

**Graceful Degradation bei optionalen Integrationen.** Die MQTT-Integration (FR-18) ist
opt-in. Ist kein Broker erreichbar, startet die App normal weiter und zeigt
Integrationsgeräte als OFFLINE; es gibt keine UI-blockierenden Retry-Schleifen.

**Regel-Engine als Hintergrunddienst.** Zeitpläne und Regeln werden über einen
`ScheduledExecutor` ausgewertet. Auslösung kann zeit-, schwellwert- oder ereignisbasiert
erfolgen (`RuleEvaluator`); `ConflictDetectionService` warnt vor widersprüchlichen
Aktionen auf demselben Gerät (FR-15). Ablauf siehe
[`docs/uml/05-sequence-rule-execution.puml`](./uml/05-sequence-rule-execution.puml).

**Sicherheit.** Passwörter werden mit BCrypt gehasht; Login-Versuche werden gedrosselt
(Throttling) gegen Brute-Force.

## 5. Persistenz und Konfiguration

- **Datenbank**: PostgreSQL. Die Schema-Initialisierung erfolgt über die SQL-Skripte
  unter `src/main/resources/db/` (je Domäne ein `init-*.sql`: rooms, rules, schedules,
  scenes, energy, activity-log, notifications, auth).
- **Verbindung**: `DatabaseConfig` liest die Zugangsdaten aus Umgebungsvariablen bzw.
  einer `.env`-Datei (Vorlage: `.env.example`). Ohne gültige Konfiguration kann mit den
  Mock-Diensten gearbeitet werden.
- **Aktivitätsdaten**: Alle manuellen und automatisierten Zustandsänderungen werden in
  `activity_log` protokolliert (Zeitstempel, Gerät, Actor) und bilden die Datenbasis für
  Energie-Berechnung und Activity-Log.
- **Persistenzumfang**: Die meisten Dienste (Auth, Räume, Geräte/Log, Zeitpläne, Regeln,
  Benachrichtigungen, Energie, Szenen) sind JDBC-gestützt und über die Datenbank persistent.
  Die Funktionen **MQTT-Integration (FR-18)**, **Tagessimulation (FR-19)** und **Vacation
  Mode (FR-21)** laufen produktiv über In-Memory-Dienste; ihr Zustand wird daher nicht über
  Programmläufe hinweg persistiert.

## 6. Erweiterungspunkte

- **Neue Domäne / neuer Dienst**: Interface in `service.api` definieren, je eine
  `Mock*`- und `Jdbc*`-Implementierung bereitstellen und in der `ServiceRegistry`
  registrieren.
- **Neuer Gerätetyp**: Modell in `model/` ergänzen, Leistungswert in
  `DeviceEnergyConstants` hinterlegen, ggf. UI in `DevicesController` erweitern.
- **Neuer Regel-Trigger/Action**: `RuleEvaluator` bzw. die Regel-Auswertung in
  `service.rule` erweitern.
- **Neue View**: FXML unter `resources/.../view/` anlegen, Controller ergänzen und im
  `ViewNavigator` verdrahten.

## 7. Build und Qualitätssicherung

| Befehl | Beschreibung |
|---|---|
| `mvn javafx:run` | Anwendung starten |
| `mvn clean test` | Unit-Tests + JaCoCo-Coverage-Check |
| `mvn clean package` | JAR bauen |
| `mvn pmd:pmd pmd:cpd` | PMD/CPD statische Analyse (`ruleset.xml`) |

- **Build-Tool**: Maven (Java 21, JavaFX)
- **Tests**: JUnit
- **Statische Analyse**: PMD/CPD mit projektspezifischem `ruleset.xml`, Checkstyle
- **CI**: GitHub Actions (`.github/workflows/Continuous Integration.yaml`) — kompiliert,
  testet inkl. Coverage-Check, führt PMD aus und baut das JAR bei jedem Push/PR auf `main`.
- **JavaDoc**: für wichtige Klassen unter
  [`docs/javadoc/index.html`](https://jku-win-se.github.io/teaching-2026.ss.prse.braeuer.team4/javadoc/index.html)
  veröffentlicht (GitHub Pages).

## 8. Testfälle und Testabdeckung

Die Tests liegen unter `src/test/java/at/jku/se/smarthome/` und decken vor allem die
Service-Schicht und die Domänenlogik ab (Mock-gestützt, ohne laufende Datenbank):

- **Auth**: Registrierung, Login, Throttling, Rollen
- **Rooms/Devices**: CRUD, Validierung
- **Rules/Schedules**: Auswertung, Konflikterkennung, Hintergrundausführung
- **Energy**: Verbrauchsaggregation Gerät → Raum → Haushalt
- **Log/Notification**: Protokollierung, Benachrichtigungen, CSV-Export

JavaFX-Einstiegspunkt (`SmartHomeApp`) und FXML-Controller sind von der
Coverage-Messung ausgenommen, da sie eine laufende Anzeige benötigen. Die aktuelle
Instruction-Coverage (ohne UI-Klassen) ist im README-Badge ausgewiesen und wird im
JaCoCo-Report unter `target/site/jacoco/index.html` nach jedem Testlauf erzeugt.

## 9. Weiterführende Diagramme

Alle PlantUML-Quellen liegen unter [`docs/uml/`](./uml/):

| Diagramm | Datei |
|---|---|
| Architekturüberblick | [`01-architecture-overview.puml`](./uml/01-architecture-overview.puml) |
| Domänenmodell | [`02-domain-model.puml`](./uml/02-domain-model.puml) |
| Service-Schicht | [`03-service-layer.puml`](./uml/03-service-layer.puml) |
| Sequenz: Gerätesteuerung | [`04-sequence-device-control.puml`](./uml/04-sequence-device-control.puml) |
| Sequenz: Regelausführung | [`05-sequence-rule-execution.puml`](./uml/05-sequence-rule-execution.puml) |
| MVC-Verdrahtung | [`06-mvc-wiring.puml`](./uml/06-mvc-wiring.puml) |

Ergänzende Domänenmodelle (Mermaid) liegen als `.mmd`-Dateien unter [`docs/`](./).
