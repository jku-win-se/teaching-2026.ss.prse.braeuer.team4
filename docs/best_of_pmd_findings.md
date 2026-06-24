# Best-of: PMD Findings in This Project

Real issues caught by PMD static analysis during the development of the Smart Home application. Each finding shows the rule, what was wrong, and how it was fixed — with commit references.

---

## 1. Concurrency Bug — `DoubleCheckedLocking`

**Rule:** `DoubleCheckedLocking` (multithreading)
**Commit:** `79b4a08`

### What PMD found
`ServiceRegistry` used the double-checked locking pattern with `volatile` fields:

```java
// BROKEN — double-checked locking is broken in Java
private static volatile ScheduleService scheduleService;

public static ScheduleService getScheduleService() {
    if (scheduleService == null) {
        synchronized (ServiceRegistry.class) {
            if (scheduleService == null) {
                scheduleService = JdbcScheduleService.getInstance();
            }
        }
    }
    return scheduleService;
}
```

### Why it matters
Double-checked locking is a well-known broken pattern. The `volatile` keyword prevents some reorderings, but the pattern remains error-prone and hard to reason about. If anyone removes the `volatile` (easy to miss), the code silently breaks under concurrent access.

### The fix
Replaced with the **initialization-on-demand holder pattern** — thread-safe lazy init without any explicit synchronization:

```java
private static ScheduleService testScheduleServiceOverride;

public static ScheduleService getScheduleService() {
    if (testScheduleServiceOverride != null) {
        return testScheduleServiceOverride;
    }
    return ScheduleServiceHolder.INSTANCE;
}

private static final class ScheduleServiceHolder {
    private static final ScheduleService INSTANCE = JdbcScheduleService.getInstance();
}
```

**Takeaway:** PMD caught a real concurrency anti-pattern that could cause subtle race conditions.

---

## 2. Debug Code Left in Production — `SystemPrintln`

**Rule:** `SystemPrintln` / `AvoidPrintStackTrace` (best practices)
**Commit:** `79b4a08`

### What PMD found
```java
private void handleExportCSV() {
    String csv = logService.exportToCSV();
    // In a real application, this would save to a file or copy to clipboard
    System.out.println("Export CSV:\n" + csv);  // <-- debug leftover!
}
```

### Why it matters
`System.out.println` in production code is a debug leftover. In a JavaFX app it silently dumps data to stdout — users never see it, but it clutters logs and may leak information.

### The fix
Removed the `System.out.println` call entirely and cleaned up the unused variable.

**Takeaway:** PMD acts as a safety net catching leftover debug statements before they ship.

---

## 3. NullPointerException as Control Flow — `AvoidCatchingNPE`

**Rule:** `AvoidCatchingNPE` (error prone)
**Commit:** `34fcf81`

### What PMD found
```java
private void loadView(String fxmlFileName) {
    try {
        FXMLLoader loader = new FXMLLoader(
            Objects.requireNonNull(getClass().getResource(resourcePath))  // may throw NPE
        );
        Node view = loader.load();
        contentArea.getChildren().setAll(view);
    } catch (IOException e) {
        showError("Failed to load view: " + fxmlFileName, e);
    } catch (NullPointerException e) {           // <-- catching NPE for control flow!
        showError("View resource not found: " + fxmlFileName, e);
    }
}
```

### Why it matters
Using `NullPointerException` for control flow is an anti-pattern: it's slow, it catches *any* NPE in the block (not just the one you intended), and it's hard to read. If `loader.load()` threw an NPE, you'd get the wrong error message.

### The fix
```java
URL resource = getClass().getResource(resourcePath);
if (resource == null) {
    showError("View resource not found: " + fxmlFileName, ...);
    return;
}
FXMLLoader loader = new FXMLLoader(resource);
```

**Takeaway:** Explicit null checks are always better than catching NPE.

---

## 4. Session Expiry Logic Bug — `NullAssignment`

**Rule:** `NullAssignment` (error prone)
**Commit:** `25d7f54`

### What PMD found
```java
// In MockUserService — session expiry check:
if (System.currentTimeMillis() >= currentSessionExpiresAt) {
    logout();  // <-- this sets currentUserRole = null!
}
```

The `logout()` method sets `currentUserRole = null`, but after session expiry the user should become "Guest" — not have a null role. The rest of the code expected a non-null role string.

### The fix
Instead of calling `logout()`, inline the session-expiry logic with the correct default:

```java
if (System.currentTimeMillis() >= currentSessionExpiresAt) {
    this.currentUserEmail = null;
    this.currentUsername = null;
    this.currentUserRole = "Guest";    // correct default, not null
    this.currentUserStatus = null;
    this.currentSessionExpiresAt = 0;
}
```

**Takeaway:** PMD flagged `NullAssignment` because null roles propagate and cause `NullPointerException` downstream. The fix revealed a domain logic bug.

---

## 5. Multiple Returns Obscuring Logic — `OnlyOneReturn`

**Rule:** `OnlyOneReturn` (code style)
**Commit:** `85e9a6b`, `2540116`

### What PMD found
Multiple early `return` statements in `ActivityLogController.matches()`:

```java
private boolean matches(LogEntry entry, ...) {
    if (filterByDevice && !selectedDevice.equals(entry.getDevice())) {
        return false;
    }
    // ... more returns
    return toIso == null || date.compareTo(toIso) <= 0;
}
```

### The fix
Restructured with named boolean variables and a single return:

```java
private boolean matches(LogEntry entry, ...) {
    final boolean deviceOk = !filterByDevice || selectedDevice.equals(entry.getDevice());
    final boolean fromOk = fromIso == null || date.compareTo(fromIso) >= 0;
    final boolean toOk = toIso == null || date.compareTo(toIso) <= 0;
    return deviceOk && fromOk && toOk;
}
```

**Takeaway:** Single-return forces you to name intermediate results, making the logic self-documenting.

---

## 6. Cryptic Variable Names — `ShortVariable`

**Rule:** `ShortVariable` (code style)
**Commit:** `85e9a6b`

### What PMD found
```java
String id = UUID.randomUUID().toString();          // "id" — what kind of ID?
String t = deviceType.trim().toLowerCase(...);      // "t" — type? time? temp?
String s = sql.trim();                              // "s" — string? sql? status?
Room r = input.room();                              // "r" — room? result?
Device d = roomService.addDeviceToRoom(...);        // "d" — device? data?
```

### The fix
```java
String deviceId = UUID.randomUUID().toString();
String normalizedInput = deviceType.trim().toLowerCase(...);
String sqlStatement = sql.trim();
Room selectedRoom = input.room();
Device createdDevice = roomService.addDeviceToRoom(...);
```

**Takeaway:** Short names save typing but cost reading time. PMD forces intention-revealing names.

---

## 7. Unused Variable — `UnusedLocalVariable`

**Rule:** `UnusedLocalVariable` (best practices)
**Commit:** `85e9a6b`

### What PMD found
```java
private void handleExportCSV() {
    String csv = logService.exportToCSV();  // csv is assigned but never read!
    // ...
}
```

### The fix
```java
private void handleExportCSV() {
    logService.exportToCSV();  // call only, no unused assignment
    // ...
}
```

**Takeaway:** Dead assignments are noise. They make readers wonder "was this supposed to be used?".

---

## 8. Inner Class Should Be Final — `ClassWithOnlyPrivateConstructorsShouldBeFinal`

**Rule:** `ClassWithOnlyPrivateConstructorsShouldBeFinal` (design)
**Commit:** `79b4a08`

### What PMD found
```java
private class RuleActionCell extends TableCell<Rule, Void> { ... }
private class ScheduleActionCell extends TableCell<Schedule, Void> { ... }
private class UserActionCell extends TableCell<User, Void> { ... }
```

All have only private (implicit) constructors, meaning no subclass can meaningfully extend them — but the `private` access alone doesn't prevent it at the JVM level.

### The fix
```java
private final class RuleActionCell extends TableCell<Rule, Void> { ... }
private final class ScheduleActionCell extends TableCell<Schedule, Void> { ... }
private final class UserActionCell extends TableCell<User, Void> { ... }
```

**Takeaway:** `final` communicates intent clearly: this class is not designed for inheritance.

---

## 9. StringBuilder With Wrong Capacity — `InsufficientStringBufferDeclaration`

**Rule:** `InsufficientStringBufferDeclaration` (performance)
**Commit:** `34fcf81`

### What PMD found
```java
StringBuilder csv = new StringBuilder();  // default capacity = 16 chars
csv.append("Timestamp,Device,Room,Action,Actor\n");
for (LogEntry log : logs) { ... }  // potentially hundreds of entries
```

The default capacity of 16 chars means the StringBuilder reallocates and copies its internal array many times as the CSV grows.

### The fix
```java
int initialCapacity = logs.size() * 96 + 64;  // ~96 chars per log entry + header
StringBuilder csv = new StringBuilder(initialCapacity);
```

**Takeaway:** Pre-sizing avoids costly internal array growth. PMD catches this automatically.

---

## 10. Magic Numbers — `AvoidLiteralsInIfCondition`

**Rule:** `AvoidLiteralsInIfCondition` (error prone)
**Commit:** `85e9a6b`

### What PMD found
```java
if (device.getTemperature() > 5) { ... }
if (device.getTemperature() < 35) { ... }
```

What do 5 and 35 mean? A reader has to guess these are thermostat min/max.

### The fix
```java
private static final int THERMOSTAT_MIN_TEMPERATURE = 5;
private static final int THERMOSTAT_MAX_TEMPERATURE = 35;

if (device.getTemperature() > THERMOSTAT_MIN_TEMPERATURE) { ... }
if (device.getTemperature() < THERMOSTAT_MAX_TEMPERATURE) { ... }
```

**Takeaway:** Named constants replace guesswork with clarity and make future changes single-point.

---

## 11. Overly Complex Tests — `UnitTestContainsTooManyAsserts`

**Rule:** `UnitTestContainsTooManyAsserts` (best practices)
**Commits:** `25d7f54`, `30cc133`

### What PMD found
A single test method asserting 5+ things:

```java
@Test
public void addUpdateToggleDeleteSchedulePersistsChanges() throws Exception {
    Schedule schedule = service.addSchedule("Morning", ...);
    assertNotNull(schedule);
    assertEquals(1, service.getSchedules().size());
    assertTrue(service.updateSchedule(...));
    assertEquals("Morning Updated", ...);
    assertFalse(service.getScheduleById(...).isActive());
    // ... and more
}
```

### The fix
Split into focused single-responsibility tests:

```java
@Test public void addScheduleReturnsNonNull() { ... }
@Test public void addSchedulePersistedToCollection() { ... }
@Test public void updateScheduleChangesName() { ... }
@Test public void updateScheduleDeactivates() { ... }
@Test public void toggleScheduleEnablesInactive() { ... }
```

**Takeaway:** When a multi-assert test fails, you don't know *which* assertion broke. One assert per test = precise failure localization.

---

## 12. PMD Naming Rules vs Test Conventions — `MethodNamingConventions`

**Rule:** `MethodNamingConventions` (code style)
**Commits:** `2589426`, `e43862c`, documented in `lessons_learned_feature_jdbc_rules_notifications.md`

### What PMD found
Test methods using `snake_case` names:
```java
@Test
public void freshDatabase_hasNoNotifications() { ... }
```

PMD expects `camelCase`: `[a-z][a-zA-Z0-9]*`.

### The fix
Not a code fix — a judgment call. `snake_case` test names are a widespread and accepted convention for readability. The team added `@SuppressWarnings` to test classes:

```java
@SuppressWarnings("PMD.MethodNamingConventions")
public class TestJdbcRuleService { ... }
```

**Takeaway:** Not every PMD finding requires a code change. When a rule conflicts with an established team convention, suppression with justification is the right call.

---

## Summary Table

| # | PMD Rule | Category | Severity | Bug / Design / Style |
|---|----------|----------|----------|---------------------|
| 1 | `DoubleCheckedLocking` | Multithreading | High | **Bug** — broken concurrency pattern |
| 2 | `SystemPrintln` | Best Practices | Medium | **Bug** — debug leftover in production |
| 3 | `AvoidCatchingNPE` | Error Prone | High | **Bug** — NPE as control flow |
| 4 | `NullAssignment` | Error Prone | High | **Bug** — null role instead of "Guest" |
| 5 | `OnlyOneReturn` | Code Style | Low | **Design** — clearer logic flow |
| 6 | `ShortVariable` | Code Style | Low | **Style** — intention-revealing names |
| 7 | `UnusedLocalVariable` | Best Practices | Medium | **Style** — dead assignment |
| 8 | `ClassWithOnlyPrivate...Final` | Design | Low | **Design** — explicit final intent |
| 9 | `InsufficientStringBuffer` | Performance | Medium | **Design** — avoidable reallocations |
| 10 | `AvoidLiteralsInIfCondition` | Error Prone | Low | **Style** — named constants |
| 11 | `UnitTestTooManyAsserts` | Best Practices | Medium | **Design** — precise test failures |
| 12 | `MethodNamingConventions` | Code Style | Low | **Judgment** — suppress when justified |

### Key Insight

4 out of 12 findings were **real bugs** (concurrency, debug leak, NPE control flow, null role). The rest improved design clarity, readability, and maintainability. PMD paid for itself on finding #1 alone.
