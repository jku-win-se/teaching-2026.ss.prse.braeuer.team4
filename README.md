# SmartHome Orchestrator

[![Continuous Integration](https://github.com/jku-win-se/teaching-2026.ss.prse.braeuer.team4/actions/workflows/Continuous%20Integration.yaml/badge.svg)](https://github.com/jku-win-se/teaching-2026.ss.prse.braeuer.team4/actions/workflows/Continuous%20Integration.yaml)
[![Coverage](https://img.shields.io/badge/coverage-77%25-yellow)](https://github.com/jku-win-se/teaching-2026.ss.prse.braeuer.team4/actions/workflows/Continuous%20Integration.yaml)

> **„Personalisierte Rules. Orchestration. Automation."**

![SmartHome Orchestrator – Device Control](./pic/home-orchestrator-showcase.jpg)

## Überblick über das Projekt

Der **SmartHome Orchestrator** ist eine JavaFX-Desktop-Anwendung zur Verwaltung eines
(simulierten) Smart Homes — **ein System für alle Geräte** statt einer App pro Hersteller.
Benutzer organisieren virtuelle Geräte in Räumen, steuern sie manuell, automatisieren sie
über Regeln, Zeitpläne und Szenen, überwachen den Energieverbrauch und verwalten Benutzer
mit unterschiedlichen Rollen.

Was den Orchestrator ausmacht:

- **Herstellerunabhängig** – ein System für alle Geräte
- **Konfliktfrei** – Geräte spielen zusammen, ohne sich zu widersprechen
- **Energie-Transparenz** – Verbrauch pro Gerät, Raum und Haushalt
- **Eigene Automatisierung** – persönliche Regeln, Szenen und Urlaubsmodus
- **Zuverlässig & testbar** – läuft von selbst, im Hintergrund

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

**Team 4:** Gruber Manuel, Li Xinyue, Möseneder Simon — Praktikum Software Engineering, SS 2026.

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

## Schnellstart

```bash
# Voraussetzungen: Java 21, Maven 3.9+
mvn clean package      # bauen und testen
mvn javafx:run         # Anwendung starten
```

Konfiguration der Datenbankanbindung über eine `.env`-Datei (Vorlage: `.env.example`).
Ohne DB-Konfiguration kann mit den Mock-Diensten gearbeitet werden. Ausführliche
Anleitung im [Benutzerhandbuch](./docs/user-handbook.md#2-systemanforderungen-und-start).
