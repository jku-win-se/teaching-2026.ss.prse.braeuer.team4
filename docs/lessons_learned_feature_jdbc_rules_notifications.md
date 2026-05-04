# Lessons Learned: JDBC Rules & Notifications (FR-10/11/12)

**Datum:** 2026-05-04  
**Branch:** `feature/jdbc-rules-notifications`  
**Ziel:** Mock-Implementierungen für `RuleService` und `NotificationService` durch JDBC-Varianten ersetzen, die Daten in einer echten Datenbank persistieren.

---

## 1. Ausgangslage & Was wir gebaut haben

### 1.1 Neue Dateien

| Datei | Zweck |
|-------|-------|
| `src/main/java/.../real/rule/JdbcRuleService.java` | JDBC-Implementierung von `RuleService` (FR-10/11) |
| `src/main/java/.../real/notification/JdbcNotificationService.java` | JDBC-Implementierung von `NotificationService` (FR-12) |
| `src/main/resources/db/init-rules.sql` | DDL für `rules`-Tabelle |
| `src/main/resources/db/init-notifications.sql` | DDL für `notifications`-Tabelle |
| `src/test/java/.../TestJdbcRuleService.java` | Unit-Tests für JdbcRuleService |
| `src/test/java/.../TestJdbcNotificationService.java` | Unit-Tests für JdbcNotificationService |

### 1.2 Geaenderte Dateien

| Datei | Warum geaendert |
|-------|-----------------|
| `ServiceRegistry.java` | `getRuleService()` und `getNotificationService()` liefern jetzt JDBC-Defaults |
| `FR10RuleEngineTest.java` | Test muss jetzt auch `RuleService` ueberschreiben, weil Default = JDBC ist |

---

## 2. Architektur-Entscheidung: Registry Pattern mit Test-Overrides

### Das Problem
Wenn wir einfach `MockRuleService` durch `JdbcRuleService` ersetzen, laufen alle bestehenden Unit-Tests kaputt, weil sie keine Datenbank haben.

### Die Loesung: `ServiceRegistry.setXxxForTesting()`

```java
// Produktionscode: bekommt JDBC-Implementierung
RuleService svc = ServiceRegistry.getRuleService(); // -> JdbcRuleService

// Testcode: kann Mock injizieren
ServiceRegistry.setRuleServiceForTesting(mockRuleService);
```

**Warum das gut ist:**
- Produktion bekommt automatisch die Persistenz-Schicht (JDBC)
- Tests bleiben schnell und isoliert (Mock)
- Keine Code-Duplikation: Mock-Klassen bleiben erhalten und werden weiter genutzt

### Wichtige Erkenntnis
> **Nie die Mocks loeschen!** Sie sind kein "Legacy-Code", sondern aktiver Teil der Test-Strategie. Die Mocks werden in ~60 Unit-Tests weiter verwendet.

---

## 3. Problem 1: PostgreSQL-Syntax in H2-Testdatenbank

### Fehler
```
org.h2.jdbc.JdbcSQLSyntaxErrorException: Syntax error in SQL statement ... ON CONFLICT (id) DO NOTHING
```

### Ursache
Das `init-rules.sql` enthielt Seed-Daten mit PostgreSQL-spezifischer Syntax:
```sql
INSERT INTO rules (...) VALUES (...)
ON CONFLICT (id) DO NOTHING;  -- <-- PostgreSQL-only!
```

Die Tests nutzen H2 (in-memory), und H2 unterstuetzt `ON CONFLICT` nicht.

### Loesung
**Zwei Schritte:**

1. **Seed-Daten aus SQL entfernen** – das Script enthaelt nur noch DDL (CREATE TABLE, CREATE INDEX)
2. **Programmatisches Seeding im Java-Code** – im Konstruktor von `JdbcRuleService`:

```java
private void seedDemoRules() {
    if (!rules.isEmpty()) return;  // Nur wenn Tabelle leer ist

    List<Rule> seed = List.of(
        new Rule("rule-001", "Morning Routine", ...),
        new Rule("rule-002", "Motion Welcome", ...),
        new Rule("rule-003", "Heat Boost", ...)
    );
    // INSERT INTO rules ... fuer jede Regel
    rules.setAll(seed);
}
```

### Warum das besser ist
| Ansatz | Portabel? | Idempotent? | Klar? |
|--------|-----------|-------------|-------|
| `ON CONFLICT` im SQL | Nein (PostgreSQL-only) | Ja | Nein (DB-spezifisch) |
| Programmatisch im Java | Ja (ueberall) | Ja (check `isEmpty()`) | Ja (lesbarer Code) |

---

## 4. Problem 2: UI-Tests finden keine Regeln mehr

### Fehler
```
EmptyNodeQuery: there is no node in the scene-graph matching the query: ... "Motion → Light"
```

### Ursache
`FR10RuleEngineTest` hat nur `RoomService` ueberschrieben, aber nicht `RuleService`:
```java
ServiceRegistry.setRoomServiceForTesting(roomService);
// RuleService fehlt! -> ServiceRegistry liefert JdbcRuleService
```

Da `ServiceRegistry.getRuleService()` jetzt `JdbcRuleService` liefert, hat die JavaFX-UI die JDBC-Regeln geladen (3 Demo-Regeln aus dem Seed) – aber nicht die im Test erstellte "Motion → Light"-Regel.

### Loesung
Den `RuleService` ebenfalls auf Mock umleiten:
```java
MockRuleService ruleService = MockRuleService.getInstance();
ServiceRegistry.setRoomServiceForTesting(roomService);
ServiceRegistry.setRuleServiceForTesting(ruleService);  // <-- HINZUGEFUEGT
```

### Pattern fuer UI-Tests
Wenn ein UI-Test einen Service ueberschreibt, muessen **alle** Services ueberschrieben werden, die:
1. im `ServiceRegistry` registriert sind
2. von der UI oder ihren Controllern verwendet werden

---

## 5. Problem 3: PMD MethodNamingConventions in neuen Tests

### Fehler
```
PMD Failure: TestJdbcNotificationService:55
Rule:MethodNamingConventions
The JUnit 4 test method name 'freshDatabase_hasNoNotifications' doesn't match '[a-z][a-zA-Z0-9]*'
```

### Ursache
Unsere Test-Methoden nutzen `snake_case` (z. B. `addNotification_persistsAcrossRestart`), aber PMD erwartet `camelCase`.

### Loesung
`@SuppressWarnings` hinzugefuegt – genau wie in den bestehenden Mock-Tests:
```java
@SuppressWarnings({"PMD.MethodNamingConventions", ...})
public class TestJdbcRuleService {
```

**Wichtig:** Wenn ein existierender Test (z. B. `TestMockRuleService`) schon eine Suppression hat, sollten neue Tests im selben Paket den **gleichen** Stil verwenden – konsistent ist besser als perfekt.

---

## 6. Teststrategie fuer JDBC-Services

### H2 In-Memory Pattern
Jeder JDBC-Test bekommt seine eigene isolierte H2-Datenbank:
```java
@Before
public void setUp() {
    String jdbcUrl = "jdbc:h2:mem:rules_" + System.nanoTime()
        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    System.setProperty("smarthome.db.url", jdbcUrl);
    System.setProperty("smarthome.db.user", "sa");
    System.setProperty("smarthome.db.password", "");

    JdbcRuleService.resetForTesting();
    service = JdbcRuleService.getInstance();
}
```

### Was testen wir?
| Test-Typ | Beispiel |
|----------|----------|
| Persistenz | `addRule_persistsAcrossServiceRestart()` |
| Mirror-Synchronisation | `deleteRule_removesFromDatabaseAndMirror()` |
| Validation | `addRule_invalidValidation_returnsNullAndDoesNotPersist()` |
| Idempotenz | `markAsRead_alreadyReadEntry_isNoOp()` |

---

## 7. Zusammenfassung der Checkliste

Beim naechsten Mal, wenn wir einen Mock-Service durch JDBC ersetzen:

1. [ ] JDBC-Service erstellen (Singleton + `resetForTesting()`)
2. [ ] SQL-DDL Script erstellen (nur CREATE TABLE/INDEX, kein Seed!)
3. [ ] `ServiceRegistry` auf JDBC-Default umstellen
4. [ ] Seed-Daten programmatisch im Service-Konstruktor einfuegen
5. [ ] Unit-Tests fuer den JDBC-Service schreiben (H2 in-memory)
6. [ ] UI-Tests pruefen: alle relevanten Services muessen mit `setForTesting()` ueberschrieben werden
7. [ ] PMD-Check laufen lassen (`mvn pmd:check`)
8. [ ] Volle Test-Suite laufen lassen (`mvn test`)
