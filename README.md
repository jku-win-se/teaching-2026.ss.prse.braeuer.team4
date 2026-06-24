# SmartHome Orchestrator

[![Continuous Integration](https://github.com/jku-win-se/teaching-2026.ss.prse.braeuer.team4/actions/workflows/Continuous%20Integration.yaml/badge.svg)](https://github.com/jku-win-se/teaching-2026.ss.prse.braeuer.team4/actions/workflows/Continuous%20Integration.yaml)
[![Coverage](https://img.shields.io/badge/coverage-77%25-yellow)](https://github.com/jku-win-se/teaching-2026.ss.prse.braeuer.team4/actions/workflows/Continuous%20Integration.yaml)

## Überblick über das Projekt

Der **SmartHome Orchestrator** ist eine JavaFX-Desktop-Anwendung zur Verwaltung eines
(simulierten) Smart Homes. Benutzer organisieren virtuelle Geräte in Räumen, steuern sie
manuell, automatisieren sie über Regeln, Zeitpläne und Szenen, überwachen den
Energieverbrauch und verwalten Benutzer mit unterschiedlichen Rollen.

Wesentliche Funktionen:

- **Identität & Rollen** – Registrierung, Login, Owner-/Member-Berechtigungen, Einladungen
- **Struktur** – Räume und virtuelle Geräte anlegen, umbenennen, löschen
- **Steuerung** – Schalter, Dimmer, Thermostate, Jalousien und Sensoren bedienen
- **Automatisierung** – Regeln (IF-Trigger-THEN-Action), Zeitpläne, Szenen, Vacation Mode
- **Monitoring** – Activity-Log, Energie-Dashboard, In-App-Benachrichtigungen
- **Erweiterung** – optionale MQTT-Integration für physische Geräte, Tagessimulation

Technisch handelt es sich um einen mehrschichtigen Monolithen (JavaFX-UI → Service-Schicht
→ PostgreSQL), wobei zu jedem Dienst eine In-Memory-Mock-Variante für Tests und Demobetrieb
existiert. Details siehe [Systemarchitektur](./docs/system-architecture.md).

## Umgesetzte Anforderungen

Die funktionalen Anforderungen FR-01 bis FR-21 sind umgesetzt. Die folgende Tabelle gibt
Anforderung, Status und Verantwortlichkeit wieder. Die vollständige Rückverfolgbarkeit zu
den User Stories ist in der [User-Stories-Dokumentation](./docs/user-stories.md) abgebildet.

> **Hinweis:** Die Spalte *Verantwortlich* ist ein Vorschlag auf Basis der Git-Historie
> (`†` = aus Commit-Historie abgeleitet) und vom Team zu bestätigen. Die Spalte *Stunden*
> ist vom Team zu ergänzen.

| FR | Anforderung | Status | Verantwortlich † | Stunden |
|----|-------------|:------:|------------------|:-------:|
| FR-01 | Registrierung mit eindeutiger E-Mail/Passwort | ✅ | Manuel Gruber | _TODO_ |
| FR-02 | Login / Logout | ✅ | Manuel Gruber | _TODO_ |
| FR-03 | Räume anlegen / umbenennen / löschen | ✅ | Manuel Gruber | _TODO_ |
| FR-04 | Virtuelle Geräte hinzufügen (Typ + Name) | ✅ | Xin Li | _TODO_ |
| FR-05 | Geräte umbenennen / entfernen | ✅ | Xin Li | _TODO_ |
| FR-06 | Geräte manuell steuern (Schalter/Dimmer/Thermostat/Jalousie/Sensor) | ✅ | Xin Li | _TODO_ |
| FR-07 | Echtzeit-Status der Geräte in der UI | ✅ | Xin Li | _TODO_ |
| FR-08 | Activity-Log (Zeitstempel, Gerät, Actor) | ✅ | Manuel Gruber | _TODO_ |
| FR-09 | Wiederkehrende Zeitpläne | ✅ | Manuel Gruber | _TODO_ |
| FR-10 | Regeln (IF-Trigger-THEN-Action) | ✅ | Xin Li | _TODO_ |
| FR-11 | Zeit-, Schwellwert- und ereignisbasierte Trigger | ✅ | Xin Li | _TODO_ |
| FR-12 | In-App-Benachrichtigungen bei Regelausführung/-fehler | ✅ | Xin Li | _TODO_ |
| FR-13 | Rollen & Berechtigungen (Owner/Member) | ✅ | Manuel Gruber | _TODO_ |
| FR-14 | Energie-Dashboard (Gerät/Raum/Haushalt, Tag/Woche) | ✅ | Manuel Gruber | _TODO_ |
| FR-15 | Erkennung von Zeitplan-/Regelkonflikten | ✅ | Xin Li | _TODO_ |
| FR-16 | CSV-Export von Activity-Log / Energieverbrauch | ✅ | Manuel Gruber | _TODO_ |
| FR-17 | Szenen (benannte Geräte-Zustände, 1-Klick-Aktivierung) | ✅ | Xin Li | _TODO_ |
| FR-18 | MQTT-Integration für physische Geräte (optional) | ✅ | Manuel Gruber | _TODO_ |
| FR-19 | Tagessimulation mit beschleunigtem Replay | ✅ | Manuel Gruber | _TODO_ |
| FR-20 | Member per E-Mail einladen / Zugriff entziehen | ✅ | Manuel Gruber | _TODO_ |
| FR-21 | Vacation Mode (Zeitplan für Datumsbereich) | ✅ | Manuel Gruber | _TODO_ |

*Nicht umgesetzte Anforderungen: keine.*

## Überblick über die Applikation aus Benutzersicht

➡️ **[Benutzerhandbuch](./docs/user-handbook.md)** — Installation & Start, Funktionsüberblick,
Bedienung anhand von Szenarien (z. B. „Wie erstelle ich eine Regel?"), bekannte Einschränkungen
und FAQ.

## Überblick über die Applikation aus Entwicklersicht

➡️ **[Systemarchitektur-Dokumentation](./docs/system-architecture.md)** — Schichten und
wichtige Klassen, zentrale Designentscheidungen, Erweiterungspunkte, Build- und
Qualitätssicherung sowie Testfälle und Testabdeckung.

## JavaDoc für wichtige Klassen, Interfaces und Methoden

➡️ **[JavaDoc (GitHub Pages)](https://jku-win-se.github.io/teaching-2026.ss.prse.braeuer.team4/javadoc/index.html)**

## Weitere Dokumentation

| Thema | Dokument |
|---|---|
| User Stories & FR-Rückverfolgbarkeit | [docs/user-stories.md](./docs/user-stories.md) |
| Architektur-/Sequenzdiagramme (PlantUML) | [docs/uml/](./docs/uml/) |
| Domänenmodelle (Mermaid) | [docs/current-domain-model.mmd](./docs/current-domain-model.mmd), [docs/current_ui_domai_model.mmd](./docs/current_ui_domai_model.mmd) |
| Branching-Strategie des Teams | [docs/group_4_branching_strategy.md](./docs/group_4_branching_strategy.md) |
| Lessons Learned (JDBC Rules & Notifications) | [docs/lessons_learned_feature_jdbc_rules_notifications.md](./docs/lessons_learned_feature_jdbc_rules_notifications.md) |

### Interne Arbeitsdokumente & Präsentationen

| Thema | Dokument |
|---|---|
| Architektur (interaktive Deep-Dive-Ansicht) | [docs/architecture-deep-dive.html](./docs/architecture-deep-dive.html) |
| Regel-Engine – Ablaufvisualisierung | [docs/rule-engine-flow.html](./docs/rule-engine-flow.html) |
| Milestone-3-Präsentation | [docs/milestone3-presentation.html](./docs/milestone3-presentation.html) |
| Präsentationsleitfaden | [docs/presentation-guide.md](./docs/presentation-guide.md) |
| Code-Review FR-10/11/12 | [docs/code_review_fr10_11_12.md](./docs/code_review_fr10_11_12.md) |
| Projekt-Audit FR-10/11/12 | [docs/project_audit_fr10_11_12.md](./docs/project_audit_fr10_11_12.md) |
| Rule-Fixes – Implementierungsspezifikation | [docs/rule_fixes_implementation_spec.md](./docs/rule_fixes_implementation_spec.md) |
| Coverage-Verbesserung – Spezifikation | [docs/coverage_improvement_spec.md](./docs/coverage_improvement_spec.md) |
| PMD-Findings (Best-of) | [docs/best_of_pmd_findings.md](./docs/best_of_pmd_findings.md) |

## Schnellstart

```bash
# Voraussetzungen: Java 21, Maven 3.9+
mvn clean package      # bauen und testen
mvn javafx:run         # Anwendung starten
```

Konfiguration der Datenbankanbindung über eine `.env`-Datei (Vorlage: `.env.example`).
Ohne DB-Konfiguration kann mit den Mock-Diensten gearbeitet werden. Ausführliche
Anleitung im [Benutzerhandbuch](./docs/user-handbook.md#2-systemanforderungen-und-start).
