# Projekt-Audit: JDBC-Persistenz für FR-10, FR-11, FR-12

**Datum:** 2026-05-04
**Scope:** Gesamte Codebase, Fokus auf Datenbank-Schicht
**Status nach Audit:** Funktional, aber mit kritischen Lücken

---

## 1. Was wurde implementiert?

### 1.1 Neue JDBC-Services

| Service | Datei | Zweck |
|---------|-------|-------|
| **JdbcRuleService** | `service/real/rule/JdbcRuleService.java` | Speichert Automatisierungsregeln (FR-10/11) |
| **JdbcNotificationService** | `service/real/notification/JdbcNotificationService.java` | Speichert In-App-Notifications (FR-12) |

### 1.2 SQL-Schemas

| Datei | Tabelle(n) |
|-------|-----------|
| `db/init-rules.sql` | `rules` (mit Index auf `enabled`) |
| `db/init-notifications.sql` | `notifications` (mit Index auf `read_flag`) |

### 1.3 Registry-Wiring

`ServiceRegistry` liefert jetzt für ALLE Services JDBC-Implementierungen als Default:

```java
// Vorher (Mock)
RuleService INSTANCE = MockRuleService.getInstance();

// Nachher (JDBC)
RuleService INSTANCE = JdbcRuleService.getInstance();
```

**Betroffene Services:**
- `getScheduleService()` → `JdbcScheduleService`
- `getRoomService()` → `JdbcRoomService`
- `getLogService()` → `JdbcLogService`
- `getUserService()` → `JdbcUserService`
- `getRuleService()` → `JdbcRuleService` (neu)
- `getNotificationService()` → `JdbcNotificationService` (neu)

### 1.4 Tests

| Test-Klasse | Was wird getestet |
|-------------|-------------------|
| `TestJdbcRuleService` | CRUD + Toggle + Persistenz-Restart + Validation |
| `TestJdbcNotificationService` | Add/MarkRead/MarkAllRead + Unread-Count + Persistenz |

Beide nutzen **isolierte H2 In-Memory-Datenbanken** pro Test.

---

## 2. Wie funktioniert die Architektur?

### 2.1 Das Singleton + Observable-Mirror Pattern

Jeder JDBC-Service ist ein **Singleton**, der eine **ObservableList** als Mirror zur Datenbank hält:

```
┌─────────────────────┐
│   JavaFX UI         │
│  (bindet an List)   │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│  ObservableList     │  ◄── In-Memory Mirror
│  (FXCollections)    │      (neueste 200 Einträge)
└──────────┬──────────┘
           │ add/delete/update
┌──────────▼──────────┐
│   JDBC-Service      │  ◄── Singleton
│   (Singleton)       │
└──────────┬──────────┘
           │ INSERT/UPDATE/DELETE
┌──────────▼──────────┐
│   Datenbank         │  ◄── H2 (Tests) oder PostgreSQL (Prod)
│   (H2 / PostgreSQL) │
└─────────────────────┘
```

**Warum der Mirror?**
- JavaFX-UI bindet sich an `ObservableList` (live Updates)
- Ohne Mirror müsste die UI nach jeder Änderung die DB neu abfragen
- Der Mirror wird bei Konstruktor-Aufruf einmalig aus der DB geladen (`refreshXxx()`)

### 2.2 Schema-Initialisierung (ensureSchema)

Jeder Service prüft beim ersten DB-Zugriff, ob die Tabelle existiert:

```java
private void ensureSchema(Connection connection) {
    if (!schemaReady.get()) {
        synchronized (this) {
            if (!schemaReady.get()) {
                // Führt init-xxxx.sql aus
                // Setzt schemaReady = true
            }
        }
    }
}
```

**Vorteil:** Kein externer DB-Migration-Tool (Flyway/Liquibase) nötig.
**Nachteil:** Jeder Service lädt sein eigenes SQL-Script – bei 6 Services werden 6 separate Scripts gepflegt.

### 2.3 Test-Isolation via H2

Jeder Test bekommt seine eigene Datenbank:

```java
String jdbcUrl = "jdbc:h2:mem:rules_" + System.nanoTime()
    + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
System.setProperty("smarthome.db.url", jdbcUrl);
```

- `DB_CLOSE_DELAY=-1`: Datenbank bleibt bis JVM-Ende bestehen
- `MODE=PostgreSQL`: H2 versteht PostgreSQL-Syntax (z. B. `BIGSERIAL`)
- `DATABASE_TO_LOWER=TRUE`: Tabellennamen werden lowercase (PostgreSQL-Standard)

---

## 3. Kritische Lücken (Müssen dringend gefixt werden)

### 🔴 Lücke 1: JdbcRoomService hat KEINE Seed-Daten

**Problem:**
- `MockRoomService` erstellt 5 Räume + 8 Geräte beim Start
- `JdbcRoomService` lädt nur aus der DB – bei leerer DB = **keine Räume, keine Geräte**

**Warum ist das kritisch?**
- Regeln referenzieren Geräte wie "Main Light", "Motion Sensor"
- Schedules referenzieren Geräte
- Szenen referenzieren Geräte
- Wenn die DB leer ist, funktioniert die gesamte App nicht

**Lösung:** `JdbcRoomService` braucht ein `seedDemoRooms()`-Methode (analog zu `JdbcRuleService`):

```java
private void seedDemoRooms() {
    if (!rooms.isEmpty()) return;
    // Füge Living Room, Bedroom, Kitchen, Bathroom, Hallway ein
    // Füge die 8 Geräte ein (Main Light, Dimmer Light, Bed Light, ...)
}
```

### 🔴 Lücke 2: Kein Connection Pooling

**Problem:** Jede SQL-Operation öffnet eine neue Verbindung:

```java
// Das passiert in JEDER Methode:
try (Connection connection = openConnection()) {
    // ... SQL ...
}
// Verbindung wird hier sofort geschlossen
```

**Auswirkungen:**
- Performance: ~50-100ms Overhead pro Verbindung (TCP-Handshake + Auth)
- Bei 6 Services × N Operationen = Hunderte Verbindungen pro Sekunde möglich
- PostgreSQL hat ein Limit an gleichzeitigen Verbindungen (default 100)

**Lösung:** HikariCP (schnellster Java-Connection-Pool) einbauen:

```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>6.2.1</version>
</dependency>
```

Und dann statt `DriverManager.getConnection()`:

```java
private static final HikariDataSource DATA_SOURCE;
static {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(settings.jdbcUrl());
    config.setUsername(settings.username());
    config.setPassword(settings.password());
    config.setMaximumPoolSize(10);
    DATA_SOURCE = new HikariDataSource(config);
}

private Connection openConnection() throws SQLException {
    return DATA_SOURCE.getConnection(); // Wiederverwendet Verbindungen
}
```

### 🔴 Lücke 3: Keine Transaktionen

**Problem:** Multi-Table-Operationen sind nicht atomar.

Beispiel in `JdbcRoomService.deleteRoom()`:
```java
try (Connection connection = openConnection()) {
    // DELETE FROM devices WHERE room_id = ?   <-- eigene Connection
}
try (Connection connection = openConnection()) {
    // DELETE FROM rooms WHERE id = ?          <-- andere Connection!
}
```

**Auswirkung:** Wenn das Geräte-Delete klappt, aber das Room-Delete fehlschlägt, hast du verwaiste Geräte-Einträge ohne Raum.

**Lösung:** Beide Deletes in EINER Transaktion:

```java
try (Connection connection = openConnection()) {
    connection.setAutoCommit(false);
    try {
        // DELETE devices ...
        // DELETE rooms ...
        connection.commit();
    } catch (SQLException e) {
        connection.rollback();
        throw e;
    }
}
```

### 🟡 Lücke 4: Inkonsistente Schema-Namen

**Problem:** Die SQL-Schemas nutzen unterschiedliche Datentypen:

| Tabelle | PK-Type | Timestamp-Type | String-Type |
|---------|---------|----------------|-------------|
| `activity_log` | `BIGSERIAL` | `TEXT` | `TEXT` |
| `rules` | `VARCHAR(64)` | `TIMESTAMP WITH TIME ZONE` | `VARCHAR(255)` |
| `notifications` | `BIGSERIAL` | `VARCHAR(32)` + separate `created_at` | `VARCHAR(1024)` |
| `rooms` | `TEXT` | `TIMESTAMP` | `TEXT` |
| `devices` | `TEXT` | `TIMESTAMP` | `TEXT` |
| `scheduled_actions` | `VARCHAR(64)` | `TIMESTAMP WITH TIME ZONE` | `VARCHAR(255)` |

**Warum das schlecht ist:**
- `BIGSERIAL` funktioniert nur in PostgreSQL/H2, nicht in MySQL
- Manche Tabellen haben `created_at` + `updated_at`, andere gar keine Zeitstempel
- `TEXT` vs `VARCHAR` ist semantisch gleich in PostgreSQL, aber uneinheitlich

**Empfohlene Vereinheitlichung:**

```sql
-- Für ALLE Tabellen:
id VARCHAR(64) PRIMARY KEY,     -- oder BIGSERIAL für Auto-IDs
...
created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
```

### 🟡 Lücke 5: JdbcRoomService nutzt noch MockLogService

**Problem:**
```java
// In JdbcRoomService.java:46
private final MockLogService logService = MockLogService.getInstance();
```

Das sollte eigentlich `ServiceRegistry.getLogService()` sein, damit es auch den JDBC-LogService nutzt.

### 🟡 Lücke 6: Keine Tests für JdbcRoomService, JdbcScheduleService, JdbcUserService

**Vorhandene JDBC-Tests:**
- `TestJdbcRuleService` (7 Tests)
- `TestJdbcNotificationService` (6 Tests)
- `TestJdbcScheduleService` (12 Tests)
- `TestJdbcRoomService` (?? – existiert, aber wie viele?)
- `TestJdbcUserRegistrationStore` (??)

**Fehlend:** Integration-Tests, die die Services kombiniert testen (z. B. Rule ausführen → Log-Eintrag prüfen → Notification prüfen).

---

## 4. Wo kannst du noch optimieren?

### 4.1 Kurzfristig (1-2 Stunden)

| Optimierung | Aufwand | Impact |
|-------------|---------|--------|
| **Seed-Daten in JdbcRoomService** | 30 min | 🔴 App funktioniert mit leerer DB |
| **JdbcRoomService: MockLogService → ServiceRegistry** | 5 min | 🟡 Konsistenz |
| **SQL-Schema-Review (einheitliche Datentypen)** | 1h | 🟡 Wartbarkeit |

### 4.2 Mittelfristig (1 Tag)

| Optimierung | Aufwand | Impact |
|-------------|---------|--------|
| **HikariCP Connection Pool** | 2-3h | 🔴 Performance + Stabilität |
| **Transaktionen für Multi-Table-Ops** | 2h | 🔴 Datenintegrität |
| **Consolidate init-*.sql in ein Script** | 1h | 🟡 Wartbarkeit |

### 4.3 Langfristig (Optional)

| Optimierung | Aufwand | Impact |
|-------------|---------|--------|
| **Flyway oder Liquibase** für Migrations | 4h | 🟡 Professionelles DB-Management |
| **Repository-Pattern extrahieren** | 4h | 🟡 Testbarkeit + Clean Architecture |
| **Caching-Layer für Reads** | 3h | 🟢 Performance |

---

## 5. Die gute Nachricht

Das Grundgerüst ist **solide**:

1. **ServiceRegistry + Test-Overrides funktioniert** – Mocks und JDBC koexistieren perfekt
2. **H2-Test-Pattern ist etabliert** – jeder neue Service kann dem gleichen Muster folgen
3. **Schema-Initialisierung ist automatisiert** – kein manuelles `CREATE TABLE` nötig
4. **PMD-konform** – der Code folgt den Projekt-Standards
5. **CI ist grün** (nach den letzten Fixes)

---

## 6. Empfohlene nächste Schritte

1. **SOFORT:** `JdbcRoomService.seedDemoRooms()` implementieren
2. **Diese Woche:** HikariCP einbauen
3. **Bei Gelegenheit:** `init-*.sql` konsolidieren und Transaktionen hinzufügen

Falls du Hilfe bei einem dieser Schritte brauchst, sag einfach Bescheid!
