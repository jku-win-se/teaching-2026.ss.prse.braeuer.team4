# Code Review: FR-10, FR-11, FR-12 Implementation

**Datum:** 2026-05-04
**Reviewer:** Claude (self-review der eigenen Änderungen)
**Scope:** Alle Dateien, die im Zuge der JDBC-Implementierung für FR-10/11/12 geändert oder neu erstellt wurden

---

## 1. Produktionscode

### 1.1 `JdbcRuleService.java` — NEU

#### Was gut ist
- **Korrektes Singleton-Pattern** mit `INSTANCE_LOCK` und DCL (Double-Checked-Locking via Holder wäre besser, aber so ist es auch thread-safe)
- **ObservableList-Mirror** hält UI und DB synchron — das ist die etablierte Projekt-Convention
- **Lazy Service-Lookups** in `executeEnabledRule()` — verhindert `ExceptionInInitializerError` bei statischer Initialisierung
- **Programmatisches Seeding** via `seedDemoRules()` statt SQL-`ON CONFLICT` — portable Lösung für H2 + PostgreSQL
- **PMD-Suppressions** sind dokumentiert und begründet (`GodClass`, `AvoidDeeplyNestedIfStmts`)

#### Was problematisch ist

**🔴 `nextRuleId()` ist nicht thread-safe und nicht restart-sicher**
```java
private String nextRuleId() {
    return String.format("rule-%03d", rules.size() + 1);
}
```
- Problem 1: `rules.size() + 1` funktioniert nur, solange keine Lücken entstehen. Wenn rule-002 gelöscht wird, gibt es doppelte IDs.
- Problem 2: Bei einem App-Restart könnten bereits IDs in der DB existieren, die größer sind als `rules.size()`.
- **Empfohlung:** UUIDs verwenden (`UUID.randomUUID().toString()`) oder eine DB-Sequence nutzen.

**🟡 `findRule()` sucht nur im Mirror, nicht in der DB**
```java
private Rule findRule(String ruleId) {
    return rules.stream()...findFirst().orElse(null);
}
```
- Der Mirror hat max. 200 Einträge (bzw. alle, aber das ist nicht garantiert). Wenn die DB mehr hat, findet `findRule()` eine existierende Regel nicht.
- **Empfohlung:** Bei Nicht-Finden im Mirror ein `SELECT` auf die DB machen.

**🟡 `executeRule()` erzeugt bei jedem Aufruf neue `RuleEvaluator`-Instanzen**
```java
if (new RuleEvaluator().evaluate(rule, sourceDevice)) { ... }
```
- `RuleEvaluator` hat keinen Zustand — es könnte ein `private static final` Singleton sein.

**🟡 `hasConflicts()` ist ein Stub**
```java
public boolean hasConflicts(String ruleId) { return false; }
```
- Die Methode wird vom Interface verlangt, aber sie tut nie etwas. Entweder implementieren oder aus dem Interface entfernen.

**🟡 Keine Transaktion bei `seedDemoRules()`**
- Wenn das INSERT der 2. Regel fehlschlägt, ist die 1. Regel bereits in der DB, aber der Mirror bleibt leer. Das führt zu Inkonsistenz.

---

### 1.2 `JdbcNotificationService.java` — NEU

#### Was gut ist
- **Idempotente `markAsRead()`** — mehrfaches Aufrufen ist kein Problem (`Math.max(0, ...)`)
- **Batch-Update in `markAllAsRead()`** — ein einziges `UPDATE` statt Loop
- **`MAX_LOADED_ENTRIES = 200`** — verhindert Memory-Leak bei sehr vielen Notifications
- **Keine Seed-Notifications** — korrekte Entscheidung, da Notifications echte Ereignisse sind

#### Was problematisch ist

**🔴 `markAsRead()` identifiziert Notifications via `(timestamp, message)` — nicht eindeutig!**
```sql
UPDATE notifications SET read_flag = TRUE
WHERE timestamp = ? AND message = ? AND read_flag = FALSE
```
- Wenn zwei Notifications zur gleichen Sekunde mit der gleichen Nachricht erstellt werden, werden beide als gelesen markiert.
- **Empfohlung:** Den `id` (BIGSERIAL PK) in `NotificationEntry` hinzufügen und danach identifizieren. Oder einen zusammengesetzten Unique-Index auf `(timestamp, message, type)`.

**🟡 `addNotification()` speichert `timestamp` als `VARCHAR(32)` statt `TIMESTAMP`**
```java
String timestamp = LocalDateTime.now().format(formatter); // "HH:mm:ss"
```
- Das ist im Schema so definiert (`timestamp VARCHAR(32)`), aber es ist schlecht für Sortierung und Vergleich in SQL.
- **Empfohlung:** In der DB als `TIMESTAMP` speichern und nur für die UI formatieren.

**🟡 Keine `LIMIT` bei `markAllAsRead()`**
- Wenn 10.000 Notifications existieren, werden alle auf einmal aktualisiert. Das ist in Ordnung für eine kleine App, aber skaliert nicht.

---

### 1.3 `ServiceRegistry.java` — GEÄNDERT

#### Was gut ist
- **Konsistentes Pattern** für alle Services (Override + Holder)
- **Thread-sicheres `OVERRIDE_LOCK`** — verhindert Race Conditions bei parallelen Tests
- **Lazy Initialisierung** via statische Holder-Klassen — `JdbcXxxService.getInstance()` wird erst beim ersten Zugriff aufgerufen

#### Was problematisch ist

**🔴 `getNotificationService()` nutzt KEINEN Holder — inkonsistent!**
```java
public static NotificationService getNotificationService() {
    return testNotificationServiceOverride != null
            ? testNotificationServiceOverride
            : JdbcNotificationService.getInstance();  // <-- direkter Aufruf, kein Holder!
}
```
- Alle anderen Services nutzen einen Holder (`RoomServiceHolder.INSTANCE`, etc.)
- Der Kommentar sagt "so that resetForTesting() is visible", aber das gilt für alle Services gleich.
- **Empfohlung:** Auch hier einen `NotificationServiceHolder` einführen für Konsistenz.

**🟡 `resetForTesting()` resettet nur Schedule — inkonsistent benannt**
```java
public static void resetForTesting() {
    testScheduleServiceOverride = null;
}
```
- Die Methode heißt generisch `resetForTesting()`, resettet aber nur den Schedule-Service.
- Es gibt `resetRoomServiceForTesting()`, aber keine für Log, User, Rule, Notification.
- **Empfohlung:** Entweder alle Overrides in `resetForTesting()` auf null setzen, oder pro Service eine eigene Methode.

---

### 1.4 `JdbcScheduleService.java` — GEÄNDERT

#### Was gut ist
- **Lazy Service-Lookups** statt Feld-Initialisierung — verhindert CI-Failures
- **`resolveDevice()` ist jetzt eine eigene Methode** mit lokalem `RoomService` — saubere Trennung

#### Was problematisch ist

**🟡 `MockVacationModeService` wird direkt importiert statt über Registry**
```java
MockVacationModeService vacationModeService = MockVacationModeService.getInstance();
```
- Das ist ein direkter Verweis auf eine Mock-Klasse im Produktionscode.
- **Empfohlung:** Über `ServiceRegistry` oder ein Interface lösen.

---

## 2. SQL-Schemas

### 2.1 `init-rules.sql` — GEÄNDERT

#### Was gut ist
- **DDL-only** — kein DB-spezifisches `ON CONFLICT` mehr
- **Index auf `enabled`** — beschleunigt Abfragen aktiver Regeln
- **`IF NOT EXISTS`** — idempotent, kann mehrfach ausgeführt werden

#### Was problematisch ist

**🟡 Keine `updated_at` Automatisierung**
```sql
updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
```
- Die Spalte wird beim INSERT gesetzt, aber nie automatisch beim UPDATE aktualisiert.
- Der Java-Code macht das manuell (`updated_at = CURRENT_TIMESTAMP`), aber das ist fehleranfällig.
- **Empfohlung:** PostgreSQL-Trigger oder H2-Equivalent:
```sql
CREATE TRIGGER IF NOT EXISTS trg_rules_updated_at
BEFORE UPDATE ON rules
FOR EACH ROW
SET NEW.updated_at = CURRENT_TIMESTAMP;
```

---

### 2.2 `init-notifications.sql` — NEU

#### Was gut ist
- **`BIGSERIAL`** für Auto-IDs — einfach und funktioniert in H2 + PostgreSQL
- **CHECK-Constraint auf `notification_type`** — verhindert invalide Werte
- **Index auf `read_flag`** — beschleunigt `markAllAsRead()`

#### Was problematisch ist

**🟡 `timestamp VARCHAR(32)` statt `TIMESTAMP`**
- Wie bei `JdbcNotificationService` besprochen: Sortierung und Zeitvergleich sind in SQL schwierig mit Strings.
- **Empfohlung:** `TIMESTAMP` in der DB und Formatierung nur in der UI.

---

## 3. Testcode

### 3.1 `TestJdbcRuleService.java` — NEU

#### Was gut ist
- **Isolierte H2-Datenbank pro Test** via `System.nanoTime()` im URL
- **Persistenz-Restart-Tests** — verifiziert, dass Daten in der DB bleiben
- **Negative Tests** — `invalidValidation_returnsNullAndDoesNotPersist`
- **Direkte SQL-Abfragen** zur Verifizierung (nicht nur Mirror prüfen)

#### Was problematisch ist

**🟡 `assertEquals(null, rule)` — besser `assertNull()`**
```java
assertEquals(null, rule);  // funktioniert, aber assertNull(rule) ist lesbarer
```

**🟡 `tearDown()` wirft bei Fehler keine Exception**
```java
catch (SQLException exception) {
    throw new IllegalStateException(exception);
}
```
- Besser wäre `Assert.fail("...")` oder einfach `throw new RuntimeException(...)`.

**🟡 Test-Name `seededRules_loadFromInitScriptOnFirstStart` ist irreführend**
- Die Regeln kommen nicht aus dem SQL-Script (das wurde entfernt), sondern aus `seedDemoRules()`.
- **Empfohlung:** `seededRules_loadFromJavaSeedingOnFirstStart`

---

### 3.2 `TestJdbcNotificationService.java` — NEU

#### Was gut ist
- **Persistenz-Restart-Tests** für alle Operationen (add, markRead, markAllRead)
- **Idempotenz-Test** für `markAsRead()` — wichtiger Edge Case
- **`freshDatabase_hasNoNotifications()`** — verifiziert korrekten Startzustand

#### Was problematisch ist

**🟡 Kein Negativ-Test für ungültige NotificationType**
- Es gibt keinen Test, der prüft, was passiert, wenn ein ungültiger `NotificationType` übergeben wird.

---

### 3.3 `TestMockRuleService.java` — GEÄNDERT

#### Was gut ist
- **tearDown setzt beide Overrides zurück** (`NotificationService` + `RuleService`)
- **`setForTestingWorksForRuleService`** — verifiziert Registry-Pattern

#### Was problematisch ist

**🟡 Test-Name enthält immer noch `serviceRegistry_setForTestingWorksForRuleService`**
- Der alte Name war `serviceRegistry_returnsRuleService_andSetForTestingWorks`.
- Der neue Name ist besser, aber der Test prüft nicht, was `getRuleService()` zurückgibt — nur `setForTesting`.
- **Empfohlung:** Test in zwei aufteilen:
```java
@Test
public void serviceRegistry_defaultReturnsJdbcRuleService() { ... }

@Test
public void serviceRegistry_setForTestingOverridesToMock() { ... }
```

---

### 3.4 `TestMockNotificationService.java` — GEÄNDERT

#### Was gut ist
- **Expliziter Override** via `setNotificationServiceForTesting()` — verhindert JDBC-Default
- **tearDown** setzt Override zurück

#### Was problematisch ist
- **Keine** — die Änderungen sind minimal und korrekt.

---

### 3.5 `FR10RuleEngineTest.java` — GEÄNDERT

#### Was gut ist
- **Beide Services werden überschrieben** (`RoomService` + `RuleService`)
- **Saubere Test-Daten** — Motion Sensor ON, Main Light OFF

#### Was problematisch ist
- **Keine** — der Fix ist korrekt und minimal.

---

## 4. Zusammenfassung: Top 5 Issues

| # | Datei | Issue | Schwere |
|---|-------|-------|---------|
| 1 | `JdbcNotificationService.java` | `markAsRead()` identifiziert via nicht-eindeutigem `(timestamp, message)` | 🔴 |
| 2 | `JdbcRuleService.java` | `nextRuleId()` ist nicht restart-sicher und nicht thread-safe | 🔴 |
| 3 | `ServiceRegistry.java` | `getNotificationService()` nutzt keinen Holder — inkonsistent | 🟡 |
| 4 | `JdbcNotificationService.java` | `timestamp` als `VARCHAR` statt `TIMESTAMP` | 🟡 |
| 5 | `JdbcRuleService.java` | `hasConflicts()` ist ein Stub | 🟡 |

---

## 5. Empfohlene nächste Schritte

1. **NotificationEntry.id hinzufügen** und `markAsRead()` danach identifizieren
2. **`nextRuleId()` durch UUID ersetzen**
3. **`ServiceRegistry.getNotificationService()` auf Holder-Pattern umstellen**
4. **Schema: `timestamp` auf `TIMESTAMP` ändern**
5. **`hasConflicts()` implementieren oder aus Interface entfernen**
