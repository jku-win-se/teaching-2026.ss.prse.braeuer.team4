# Milestone 3 Presentation Guide
## SmartHome Orchestrator — Team 4, JKU PRSE 2026

---

# ENGLISH VERSION

---

## Overview

This guide tells you exactly what to say and what to click for each part of the presentation. You have three HTML files to use as visual aids:

| File | Use for |
|------|---------|
| `docs/architecture-deep-dive.html` | Architecture section |
| `docs/rule-engine-flow.html` | Rule engine deep-dive |
| `docs/milestone3-presentation.html` | PMD findings + test coverage |

Suggested total time: **20–25 minutes**

---

## Part 1 — Architecture (8–10 minutes)

**File:** `architecture-deep-dive.html`

---

### Tab 1: Layers (~2 minutes)

**What to click:** Open the page and let the layer stack be visible. Do not click anything yet — walk through the layers top to bottom.

**What to say:**

> "Our application follows a strict layered architecture with six layers. The most important rule is: each layer only talks to the layer directly below it. The View never touches the database. The Controller never knows if it's talking to a test mock or a real database."

Walk through each layer:

- **Layer 1 — FXML (View):** "These are XML files. They describe what the screen looks like — buttons, tables, labels. There is no Java logic here. We have 15 FXML files in total, one per screen."

- **Layer 2 — Controllers:** "Each FXML is paired one-to-one with a Controller class. The Controller handles button clicks and calls services. It does not contain business logic — it just coordinates between the UI and the service layer."

- **Layer 3 — Service Interfaces:** "This is the most important layer for testability. Every service is defined as a Java interface. Controllers only import the interface — never the concrete class. This means you can swap the implementation without changing a single line in any controller."

- **Layer 4 — ServiceRegistry:** "This is our dependency injection point. Instead of Spring or any framework, we wrote a simple static factory. When a controller asks for a RuleService, the registry returns the correct implementation — JDBC in production, Mock in tests."

- **Layer 5 — Two implementations:** "Every service has two implementations. Mock stores everything in memory — perfect for fast unit tests. JDBC stores everything in our Aiven cloud PostgreSQL database — this is what runs in production."

- **Layer 6 — Database:** "Remote PostgreSQL hosted on Aiven cloud. Only the JDBC layer ever touches it. The rest of the application is completely unaware of the database structure."

- **Model layer:** "The domain model — plain Java objects like Device, Rule, Room, User. No framework annotations. They travel between all layers."

---

### Tab 2: View & Controllers (~1.5 minutes)

**What to click:** Switch to Tab 2.

**What to say:**

> "Let me show the concrete pairing. On the left: every FXML file. On the right: its controller. Look at DevicesController for example — it calls `roomService.updateDeviceState()`, `updateDeviceBrightness()`, `updateDeviceTemperature()`. It never calls the database directly. It logs every action via LogService."

> "The key design insight: the Controller does not know or care whether RoomService is backed by PostgreSQL or an in-memory list. This is the whole point of the interface layer."

---

### Tab 3: ServiceRegistry (~2 minutes)

**What to click:** Switch to Tab 3. Point at the central orange box and the surrounding service cards.

**What to say:**

> "ServiceRegistry is a single static class. Every controller in the whole application calls one of these `get` methods. That's all they do — ask the registry for a service."

**Point at the Holder pattern code comparison:**

> "This is where PMD caught a real concurrency bug for us. Our original code used the double-checked locking pattern — which is a well-known broken concurrency anti-pattern in Java. The `volatile` keyword alone is not enough to make it correct. PMD flagged it, we fixed it using the Initialization-on-Demand Holder pattern. The JVM class loader guarantees that the inner class `Holder` is loaded exactly once, thread-safely, on first access. Zero explicit synchronization needed."

**Point at the test injection diagram:**

> "For tests: before each test we call `ServiceRegistry.setRuleServiceForTesting(mock)`. The controller under test picks it up via the registry. After the test: `resetForTesting()`. This means our 603 test methods never connect to the real database."

---

### Tab 4: Mock vs JDBC (~1.5 minutes)

**What to click:** Switch to Tab 4.

**What to say:**

> "Every service exists in two flavors. Mock implementations store data in plain Java lists and Maps. They are reset between tests with `resetForTesting()`. They run in microseconds. JDBC implementations write to PostgreSQL. They mirror the database in an in-memory `ObservableList` so that JavaFX UI bindings update instantly without re-querying the database after every operation."

**Scroll to the table:**

> "We have nine service pairs. UserService, RuleService, RoomService — all the way to IoTIntegrationService. Every single one has both a mock and a JDBC implementation."

---

### Tab 5: Data Flow (~2 minutes)

**What to click:** Switch to Tab 5. Select the "Toggle Device" scenario, then click ▶ Next Step repeatedly.

**What to say as you step through:**

1. "The user clicks the toggle in the FXML view."
2. "DevicesController's change listener fires."
3. "It calls `roomService.updateDeviceState()` — note: there is no separate SmartHomeService, the RoomService handles both structure and device state."
4. "The ServiceRegistry returns JdbcRoomService."
5. "JdbcRoomService updates the in-memory ObservableList AND persists to the database with an UPDATE statement."
6. "The PostgreSQL database is updated."
7. "DevicesController logs the action via LogService — this also writes to PostgreSQL."
8. "The JavaFX UI updates automatically because it is bound to the ObservableList — no manual refresh needed."

> "Try the 'Rule Fires' scenario too — it shows the edge detection we implemented to fix the notification spam bug."

---

### Tabs 6–8: Quick reference

**Tab 6 — Domain Model:** "Nine plain Java classes. No Spring, no annotations. If you want to know what data a Rule carries, it's all here: id, name, triggerType, sourceDevice, condition, action, targetDevice, enabled, status."

**Tab 7 — Database:** "The actual table structure. Each service owns its tables. Important: devices has a foreign key to rooms with CASCADE DELETE — deleting a room deletes all its devices automatically. The energy service uses three tables: energy_daily, energy_weekly, and device_power_config."

**Tab 8 — Startup:** "Walk through the startup sequence. The purple step is the fix we added this session — moving the heavy JDBC initialization to after `show()` via `Platform.runLater()`, so the login window appears immediately. Before this fix, the window would not appear for several seconds while the app connected to Aiven."

---

## Part 2 — PMD Findings (5–6 minutes)

**File:** `milestone3-presentation.html` → PMD Findings tab

---

### Opening

**What to say:**

> "We activated PMD static analysis early in Milestone 3. It found 12 issues across the codebase. Four of them were real bugs — not style issues, not naming conventions — actual defects that could cause incorrect behavior or crashes."

**Point at the four colored summary badges at the top:**
> "4 bugs, 3 design issues, 4 style issues, 1 judgment call."

---

### Finding 1 — DoubleCheckedLocking (click to expand)

**What to say:**

> "This is the most important one. PMD's `DoubleCheckedLocking` rule caught a broken concurrency pattern in our ServiceRegistry. The code looked correct at first glance — it had `volatile`, it had `synchronized` — but double-checked locking is fundamentally unreliable in Java. We replaced it with the Holder pattern, which the JVM guarantees is thread-safe."

> "This is a great example of why static analysis is valuable: the bug was invisible to human code review."

---

### Finding 3 — AvoidCatchingNPE (click to expand)

**What to say:**

> "Here we were catching `NullPointerException` as control flow — to handle the case where an FXML resource file was not found. The problem: catching `NPE` catches *any* NullPointerException in the entire try block, not just the one you intended. If `loader.load()` threw an NPE for a completely different reason, you'd get the wrong error message. The fix is a simple explicit null check before the loader is created."

---

### Finding 4 — NullAssignment → domain logic bug (click to expand)

**What to say:**

> "This one is subtle. PMD flagged a `null` assignment in our session expiry logic. When a session expired, the code called `logout()` — which set `currentUserRole = null`. But the rest of the code expected the role to be `'Guest'` for unauthenticated users, not null. This was causing NullPointerExceptions downstream. PMD's warning led us to find a domain logic bug."

---

### Finding 11 — UnitTestContainsTooManyAsserts (click to expand)

**What to say:**

> "This is about test design. We had a single test method checking 7 things at once. The PMD rule `UnitTestContainsTooManyAsserts` flagged it. When that test failed, you couldn't tell which of the 7 assertions broke. We split it into focused single-responsibility tests. Now when a test fails, the name tells you exactly what broke."

---

### Finding 12 — Judgment call (click to expand)

**What to say:**

> "Not every PMD finding requires a code change. We use `snake_case` for test method names — which is a widely accepted convention for readability. PMD expects camelCase. Rather than rename all test methods, we added `@SuppressWarnings('PMD.MethodNamingConventions')` to the test classes with a justification. Knowing *when not to fix* a PMD finding is part of using the tool correctly."

---

## Part 3 — Test Coverage (4–5 minutes)

**File:** `milestone3-presentation.html` → Test Coverage tab

---

### Opening numbers

**What to say:**

> "At the end of Milestone 3 we have 603 test methods across 48 test classes. The JaCoCo threshold we configured is 75% line coverage on the service layer — we are at 77%."

**Important caveat to mention:**

> "Our JaCoCo configuration deliberately excludes Controllers, FXML views, Config classes, and the Model — because these either cannot run headlessly without a JavaFX runtime, or they are data carriers with no branching logic. The 77% measures the service layer only, which is where the business logic lives."

---

### Coverage journey

**Point at the journey diagram:**

> "We started Milestone 3 at around 45% overall. After activating PMD and fixing violations, we added tests alongside the fixes. After implementing the JDBC services, we wrote exhaustive test suites — the coverage jumped to 65%, then 77% after the dedicated coverage sprint."

> "The key technique: for every JDBC service, we wrote three types of tests. Happy path — the normal case. Boundary cases — empty inputs, null inputs, values at the edges. Error cases — what happens when the database is not configured, when a record is not found."

---

### Test pyramid

**Point at the pyramid:**

> "Our test pyramid looks like this: a very thin top with one UI smoke test — this runs the JavaFX login screen and verifies it loads. A small middle layer of Controller tests that can run headlessly. And a wide base of 603 service tests — these are fast, isolated, and independent of the UI."

---

### Notable test cases to mention

> "A few tests worth calling out:"

- **`TestJdbcUserService`** — "Tests the login throttling: after 3 failed attempts, the account is locked for 2 seconds, then 5, then 15. Tests that session expiry correctly resets the role to 'Guest'."

- **`ConflictDetectionServiceTest`** — "773 lines of tests covering every device type combination. Switch ON vs OFF is a conflict. Two rules setting the same brightness are not a conflict. Two thermostat rules within 0.5°C of each other are not a conflict."

- **`TestMqttIntegrationService`** — "Tests graceful degradation: if the broker is unreachable at startup, the app continues normally. Integration devices show OFFLINE. No retry loop blocks the UI thread."

---

## Closing statement

**What to say:**

> "To summarize Milestone 3: we delivered 10 new functional requirements, fixed 12 PMD violations including 4 real bugs, reached 77% service-layer test coverage with 603 tests, and today we also fixed two runtime bugs — the startup freeze and the rule notification spam. The architecture is designed so that any service can be swapped between mock and JDBC without touching a single controller."

---
---

# DEUTSCHE VERSION

---

## Überblick

Dieser Leitfaden sagt dir genau, was du sagen und was du klicken sollst. Du hast drei HTML-Dateien als visuelle Hilfsmittel:

| Datei | Verwendung |
|-------|-----------|
| `docs/architecture-deep-dive.html` | Architektur-Abschnitt |
| `docs/rule-engine-flow.html` | Rule Engine im Detail |
| `docs/milestone3-presentation.html` | PMD-Findings + Testabdeckung |

Empfohlene Gesamtdauer: **20–25 Minuten**

---

## Teil 1 — Architektur (8–10 Minuten)

**Datei:** `architecture-deep-dive.html`

---

### Tab 1: Schichten (~2 Minuten)

**Was klicken:** Die Seite öffnen und den Layer-Stack sichtbar lassen. Noch nichts anklicken — die Schichten von oben nach unten durchgehen.

**Was sagen:**

> „Unsere Anwendung folgt einer strikten Schichtenarchitektur mit sechs Ebenen. Die wichtigste Regel: Jede Schicht kommuniziert nur mit der direkt darunter liegenden. Das View berührt niemals die Datenbank. Der Controller weiß nicht, ob er mit einem Test-Mock oder einer echten Datenbank spricht."

Jede Schicht erklären:

- **Schicht 1 — FXML (View):** „Das sind XML-Dateien. Sie beschreiben, wie der Screen aussieht — Buttons, Tabellen, Labels. Keine Java-Logik hier. Wir haben 15 FXML-Dateien, eine pro Screen."

- **Schicht 2 — Controller:** „Jede FXML-Datei ist 1-zu-1 mit einer Controller-Klasse gekoppelt. Der Controller verarbeitet Klicks und ruft Services auf. Er enthält keine Business-Logik — er koordiniert nur zwischen UI und Service-Schicht."

- **Schicht 3 — Service-Interfaces:** „Das ist die wichtigste Schicht für Testbarkeit. Jeder Service ist als Java-Interface definiert. Controller importieren nur das Interface — nie die konkrete Klasse. Das bedeutet: du kannst die Implementierung austauschen, ohne eine einzige Zeile in einem Controller zu ändern."

- **Schicht 4 — ServiceRegistry:** „Das ist unser Dependency-Injection-Punkt. Statt Spring oder einem anderen Framework haben wir eine einfache statische Factory geschrieben. Wenn ein Controller einen RuleService will, gibt die Registry die richtige Implementierung zurück — JDBC in Production, Mock in Tests."

- **Schicht 5 — Zwei Implementierungen:** „Jeder Service hat zwei Implementierungen. Mock speichert alles im Arbeitsspeicher — ideal für schnelle Unit-Tests. JDBC speichert alles in unserer Aiven-Cloud-PostgreSQL-Datenbank — das läuft in Production."

- **Schicht 6 — Datenbank:** „Remote PostgreSQL auf Aiven Cloud gehostet. Nur die JDBC-Schicht greift darauf zu. Der Rest der Anwendung weiß nichts über die Datenbankstruktur."

- **Model-Schicht:** „Das Domänenmodell — einfache Java-Objekte wie Device, Rule, Room, User. Keine Framework-Annotationen. Sie werden zwischen allen Schichten weitergegeben."

---

### Tab 2: View & Controller (~1,5 Minuten)

**Was klicken:** Zu Tab 2 wechseln.

**Was sagen:**

> „Jetzt zeige ich die konkreten Paarungen. Links jede FXML-Datei, rechts ihr Controller. Schau auf den DevicesController — er ruft `roomService.updateDeviceState()`, `updateDeviceBrightness()`, `updateDeviceTemperature()` auf. Er greift nie direkt auf die Datenbank zu. Jede Aktion wird über den LogService protokolliert."

> „Der wichtigste Designgedanke: Der Controller weiß nicht, ob der RoomService durch PostgreSQL oder eine In-Memory-Liste gesichert ist. Genau das ist der Sinn der Interface-Schicht."

---

### Tab 3: ServiceRegistry (~2 Minuten)

**Was klicken:** Zu Tab 3 wechseln. Auf die zentrale orange Box und die Service-Karten zeigen.

**Was sagen:**

> „ServiceRegistry ist eine einzelne statische Klasse. Jeder Controller in der gesamten Anwendung ruft eine dieser `get`-Methoden auf. Das ist alles — sie fragen die Registry nach einem Service."

**Auf den Code-Vergleich zeigen:**

> „Hier hat uns PMD einen echten Concurrency-Bug gezeigt. Unser ursprünglicher Code verwendete das Double-Checked-Locking-Pattern — ein bekanntes, kaputtes Concurrency-Anti-Pattern in Java. Das `volatile`-Schlüsselwort allein reicht nicht aus. PMD hat es gefunden, wir haben es mit dem Initialization-on-Demand-Holder-Pattern behoben. Der JVM-Class-Loader garantiert, dass die innere Klasse `Holder` genau einmal, thread-sicher, beim ersten Zugriff geladen wird. Kein explizites Synchronisieren nötig."

**Auf das Test-Injection-Diagramm zeigen:**

> „Für Tests: Vor jedem Test rufen wir `ServiceRegistry.setRuleServiceForTesting(mock)` auf. Der Controller unter Test holt sich den Mock über die Registry. Nach dem Test: `resetForTesting()`. Das bedeutet: unsere 603 Test-Methoden verbinden sich nie mit der echten Datenbank."

---

### Tab 4: Mock vs. JDBC (~1,5 Minuten)

**Was klicken:** Zu Tab 4 wechseln.

**Was sagen:**

> „Jeder Service existiert in zwei Varianten. Mock-Implementierungen speichern Daten in einfachen Java-Listen und Maps. Sie werden zwischen Tests mit `resetForTesting()` zurückgesetzt. Sie laufen in Mikrosekunden. JDBC-Implementierungen schreiben nach PostgreSQL. Sie spiegeln die Datenbank in einer In-Memory-`ObservableList`, damit JavaFX-UI-Bindings sofort aktualisieren, ohne nach jeder Operation erneut die Datenbank abzufragen."

**Zur Tabelle scrollen:**

> „Wir haben neun Service-Paare. UserService, RuleService, RoomService — bis zum IoTIntegrationService. Jeder einzelne hat sowohl eine Mock- als auch eine JDBC-Implementierung."

---

### Tab 5: Datenfluss (~2 Minuten)

**Was klicken:** Zu Tab 5 wechseln. Das Szenario „Toggle Device" auswählen, dann schrittweise auf ▶ Next Step klicken.

**Was sagen beim Durchgehen:**

1. „Der User klickt den Toggle in der FXML-View."
2. „Der Change-Listener in DevicesController feuert."
3. „Er ruft `roomService.updateDeviceState()` auf — kein separater SmartHomeService, der RoomService übernimmt sowohl Struktur als auch Gerätestatus."
4. „Die ServiceRegistry gibt JdbcRoomService zurück."
5. „JdbcRoomService aktualisiert die In-Memory-ObservableList UND persistiert in die Datenbank mit einem UPDATE-Statement."
6. „Die PostgreSQL-Datenbank wird aktualisiert."
7. „DevicesController protokolliert die Aktion via LogService — schreibt ebenfalls nach PostgreSQL."
8. „Das JavaFX-UI aktualisiert sich automatisch, weil es an die ObservableList gebunden ist — kein manuelles Refresh nötig."

> „Das Szenario 'Rule Fires' zeigt auch die Edge-Detection, die wir implementiert haben, um den Notification-Spam-Bug zu beheben."

---

### Tabs 6–8: Kurzreferenz

**Tab 6 — Domänenmodell:** „Neun einfache Java-Klassen. Kein Spring, keine Annotationen. Wenn man wissen will, welche Daten eine Rule trägt: id, name, triggerType, sourceDevice, condition, action, targetDevice, enabled, status — alles hier."

**Tab 7 — Datenbank:** „Die tatsächliche Tabellenstruktur. Jeder Service besitzt seine Tabellen. Wichtig: devices hat einen Foreign Key zu rooms mit CASCADE DELETE — beim Löschen eines Raums werden alle seine Geräte automatisch gelöscht. Der Energy-Service nutzt drei Tabellen: energy_daily, energy_weekly und device_power_config."

**Tab 8 — Startup:** „Die Startup-Sequenz. Der violette Schritt ist der Fix, den wir in dieser Session hinzugefügt haben — das Verschieben der schweren JDBC-Initialisierung nach `show()` via `Platform.runLater()`, damit das Login-Fenster sofort erscheint. Vorher erschien das Fenster für mehrere Sekunden nicht, während die App sich mit Aiven verbunden hat."

---

## Teil 2 — PMD-Findings (5–6 Minuten)

**Datei:** `milestone3-presentation.html` → PMD Findings Tab

---

### Einstieg

**Was sagen:**

> „Wir haben PMD Static Analysis früh in Milestone 3 aktiviert. Es hat 12 Probleme im Code gefunden. Vier davon waren echte Bugs — keine Style-Probleme, keine Naming-Konventionen — tatsächliche Defekte, die zu falschem Verhalten oder Crashes führen könnten."

**Auf die vier farbigen Badges zeigen:**
> „4 Bugs, 3 Design-Probleme, 4 Style-Probleme, 1 Ermessensentscheidung."

---

### Finding 1 — DoubleCheckedLocking (zum Aufklappen klicken)

**Was sagen:**

> „Das ist das wichtigste. PMDs `DoubleCheckedLocking`-Regel hat ein kaputtes Concurrency-Pattern in unserem ServiceRegistry gefunden. Der Code sah auf den ersten Blick korrekt aus — er hatte `volatile`, er hatte `synchronized` — aber Double-Checked Locking ist in Java grundsätzlich unzuverlässig. Wir haben es durch das Holder-Pattern ersetzt, das der JVM garantiert thread-sicher ist."

> „Das ist ein gutes Beispiel dafür, warum statische Analyse wertvoll ist: Der Bug war beim manuellen Code-Review unsichtbar."

---

### Finding 3 — AvoidCatchingNPE (zum Aufklappen klicken)

**Was sagen:**

> „Hier haben wir `NullPointerException` als Control-Flow verwendet — um den Fall zu behandeln, dass eine FXML-Ressource nicht gefunden wurde. Das Problem: NPE abzufangen, fängt *jede* NullPointerException im gesamten Try-Block ab, nicht nur die beabsichtigte. Wenn `loader.load()` aus einem ganz anderen Grund eine NPE geworfen hätte, hätte man die falsche Fehlermeldung bekommen. Die Lösung: einfache explizite Null-Prüfung vor dem Erstellen des Loaders."

---

### Finding 4 — NullAssignment → Domain-Logic-Bug (zum Aufklappen klicken)

**Was sagen:**

> „Das ist subtil. PMD hat eine `null`-Zuweisung in unserer Session-Expiry-Logik markiert. Beim Ablauf einer Session rief der Code `logout()` auf — was `currentUserRole = null` setzte. Aber der Rest des Codes erwartete, dass die Rolle für nicht-authentifizierte Nutzer `'Guest'` ist, nicht null. Das führte zu NullPointerExceptions weiter unten. PMDs Warnung hat uns auf einen Domain-Logic-Bug aufmerksam gemacht."

---

### Finding 11 — UnitTestContainsTooManyAsserts (zum Aufklappen klicken)

**Was sagen:**

> „Das betrifft Test-Design. Wir hatten eine Test-Methode, die 7 Dinge auf einmal prüft. Wenn dieser Test fehlschlug, wusste man nicht, welche der 7 Assertions gebrochen war. Wir haben ihn in fokussierte Einzeltests aufgeteilt. Wenn jetzt ein Test fehlschlägt, sagt der Name genau, was kaputt ist."

---

### Finding 12 — Ermessensentscheidung (zum Aufklappen klicken)

**Was sagen:**

> „Nicht jedes PMD-Finding erfordert eine Code-Änderung. Wir verwenden `snake_case` für Test-Methodennamen — eine weit verbreitete und akzeptierte Konvention für Lesbarkeit. PMD erwartet camelCase. Statt alle Testmethoden umzubenennen, haben wir `@SuppressWarnings('PMD.MethodNamingConventions')` mit Begründung hinzugefügt. Zu wissen, *wann man ein PMD-Finding nicht behebt*, gehört zum richtigen Umgang mit dem Tool."

---

## Teil 3 — Testabdeckung (4–5 Minuten)

**Datei:** `milestone3-presentation.html` → Test Coverage Tab

---

### Einstiegszahlen

**Was sagen:**

> „Am Ende von Milestone 3 haben wir 603 Test-Methoden in 48 Test-Klassen. Der JaCoCo-Threshold, den wir konfiguriert haben, ist 75% Zeilenabdeckung für die Service-Schicht — wir liegen bei 77%."

**Wichtiger Vorbehalt:**

> „Unsere JaCoCo-Konfiguration schließt bewusst Controller, FXML-Views, Config-Klassen und das Modell aus — weil diese entweder ohne JavaFX-Runtime nicht headless laufen können, oder weil sie reine Datenträger ohne Verzweigungslogik sind. Die 77% messen nur die Service-Schicht — genau dort, wo die Business-Logik sitzt."

---

### Coverage-Journey

**Auf das Journey-Diagramm zeigen:**

> „Wir haben Milestone 3 bei etwa 45% Gesamtabdeckung begonnen. Nach dem Aktivieren von PMD und dem Beheben von Violations haben wir Tests neben den Fixes geschrieben. Nach dem Implementieren der JDBC-Services haben wir umfangreiche Test-Suites geschrieben — die Abdeckung stieg auf 65%, dann auf 77% nach dem dedizierten Coverage-Sprint."

> „Die Schlüsseltechnik: Für jeden JDBC-Service haben wir drei Arten von Tests geschrieben. Happy Path — der normale Fall. Boundary-Cases — leere Eingaben, null-Eingaben, Grenzwerte. Error-Cases — was passiert, wenn die Datenbank nicht konfiguriert ist oder ein Datensatz nicht gefunden wird."

---

### Testpyramide

**Auf die Pyramide zeigen:**

> „Unsere Testpyramide sieht so aus: Eine sehr dünne Spitze mit einem UI-Smoke-Test — der startet den JavaFX-Login-Screen und prüft, ob er geladen wird. Eine kleine mittlere Schicht mit Controller-Tests, die headless laufen können. Und eine breite Basis mit 603 Service-Tests — schnell, isoliert und unabhängig von der UI."

---

### Bemerkenswerte Testfälle

> „Ein paar Tests, die es wert sind, erwähnt zu werden:"

- **`TestJdbcUserService`** — „Testet das Login-Throttling: nach 3 fehlgeschlagenen Versuchen wird der Account für 2 Sekunden gesperrt, dann 5, dann 15. Testet, dass Session-Expiry die Rolle korrekt auf 'Guest' zurücksetzt."

- **`ConflictDetectionServiceTest`** — „773 Zeilen Tests, die jede Gerättyp-Kombination abdecken. Switch ON vs. OFF ist ein Konflikt. Zwei Rules, die die gleiche Helligkeit setzen, sind kein Konflikt. Zwei Thermostat-Rules innerhalb von 0,5°C sind kein Konflikt."

- **`TestMqttIntegrationService`** — „Testet Graceful Degradation: Wenn der Broker beim Start nicht erreichbar ist, läuft die App normal weiter. Integrationsgeräte zeigen OFFLINE. Kein Retry-Loop blockiert den UI-Thread."

---

## Schlusswort

**Was sagen:**

> „Zusammenfassung von Milestone 3: Wir haben 10 neue Functional Requirements umgesetzt, 12 PMD-Violations behoben — davon 4 echte Bugs — 77% Service-Schicht-Testabdeckung mit 603 Tests erreicht, und heute haben wir auch zwei Runtime-Bugs behoben: den Startup-Freeze und den Rule-Notification-Spam. Die Architektur ist so konzipiert, dass jeder Service zwischen Mock und JDBC ausgetauscht werden kann, ohne eine einzige Controller-Zeile zu ändern."

---

## Quick Reference — Zahlen für die Präsentation

| Metrik | Wert |
|--------|------|
| Neue FRs in M3 | 10 |
| FXML-Dateien gesamt | 15 |
| Controller gesamt | 15 |
| Service-Interfaces | 9 |
| Mock-Implementierungen | 9 |
| JDBC-Implementierungen | 9 |
| Datenbanktabellen | 9+ |
| Test-Klassen | 48 |
| Test-Methoden (@Test) | 603 |
| Coverage (Service-Schicht) | 77% |
| Coverage-Threshold | 75% |
| PMD-Findings gesamt | 12 |
| Davon echte Bugs | 4 |
| Commits in M3 | 116 |
| Bugs heute behoben | 2 (startup freeze, notification spam) |
