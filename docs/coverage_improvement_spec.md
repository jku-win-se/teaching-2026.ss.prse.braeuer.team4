# Code Coverage Improvement — Implementation Specification

**Target branch**: `feature/coverage-improvement` (branch off `main`)  
**Goal**: Raise line coverage from ~45% to ≥70% (the enforced JaCoCo threshold in `pom.xml`).  
**Do not touch**: UI controllers (`at.jku.se.smarthome.controller`), production logic, PMD suppressions already in place.

---

## Background

JaCoCo is configured in `pom.xml` with:
```xml
<counter>LINE</counter>
<value>COVEREDRATIO</value>
<minimum>0.70</minimum>
```

Current measured coverage: **~45% lines**. The gap has three root causes addressed below in priority order.

Coverage data source: `target/site/jacoco/jacoco.csv` and `target/site/jacoco/index.html` — run `mvn verify` to regenerate.

---

## Step 1 — Fix JaCoCo instrumentation (fake zeroes)

**File**: `pom.xml`

Three classes show 0% coverage despite their tests passing and recording 0 failures. The build log contains a JaCoCo ASM `ClassReader` exception during instrumentation — this is a known JaCoCo 0.8.12 compatibility bug with newer Java bytecode.

Affected classes (all in `at.jku.se.smarthome.service.mock`):
- `MockEnergyService` — 13 tests pass, 0% recorded
- `MockNotificationService` — 7 tests pass, 0% recorded
- `MockVacationModeService` — 12 tests pass, 0% recorded

**Fix**: Upgrade the JaCoCo Maven plugin version from `0.8.12` to `0.8.13` (or latest stable).

Find this in `pom.xml`:
```xml
<artifactId>jacoco-maven-plugin</artifactId>
<version>0.8.12</version>
```
Change `0.8.12` → `0.8.13`.

After this change, run `mvn verify` and confirm those three classes no longer show 0%. Estimated gain: **~400 lines**.

---

## Step 2 — Fix the failing UI smoke test

**File**: `src/test/java/.../LoginViewSmokeTest.java`  
**Failing test**: `LoginViewSmokeTest.emailFieldAcceptsInput` (line ~83)  
**Error**: `NoSuchElement` — the test can't find the email input field

This test errors out in CI, which corrupts coverage collection for the entire test JVM run. Fixing it unlocks controller coverage already exercised by the other passing smoke tests.

**How to fix**: Look at how other passing smoke tests locate their input fields. The likely cause is a scene/stage initialization race condition or a changed `fx:id`. Verify the `fx:id` of the email field in the login FXML matches what the test queries for. If the field lookup uses a CSS selector or `#id`, confirm it matches the current FXML.

Do not rewrite the test or skip it with `@Ignore`. Fix the lookup so it passes.

After fixing, run `mvn verify` and confirm `LoginViewSmokeTest` shows 0 errors. Estimated gain: **~300–500 lines** from controllers whose coverage was already being exercised but not recorded.

---

## Step 3 — Expand `MockSmartHomeService` tests

**Existing test file**: `src/test/java/at/jku/se/smarthome/service/TestMockSmartHomeService.java`  
**Production class**: `src/main/java/at/jku/se/smarthome/service/mock/MockSmartHomeService.java`  
**Current coverage**: 22% (~170 lines missed)

The existing test file only covers basic instantiation. The following public methods have no test coverage:

- `toggleDevice(String deviceId)` — returns false for unknown id, true and flips `isOn` for known id
- `setBrightness(String deviceId, int brightness)` — clamps to 0–100, updates device
- `setTemperature(String deviceId, double temperature)` — updates sensor value
- `openBlind(String deviceId)` / `closeBlind(String deviceId)` — set `isOn` accordingly
- `injectSensorValue(String deviceId, double value)` — updates `currentValue`
- `getDeviceById(String deviceId)` — returns null for unknown, correct device for known
- `authenticate(String email, String password)` — returns true for seeded credentials
- `getCurrentUser()` — returns email after authenticate
- `logout()` — clears current user

**What to add**: For each method above, add at least one happy-path test and one sad-path test (unknown deviceId, etc.). Use `MockSmartHomeService.resetForTesting()` in `@Before`. No external dependencies needed.

Class-level suppression already present: `@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods"})` — add it if not already on the test class.

Estimated gain: **~120 lines**.

---

## Step 4 — Expand `MockIoTIntegrationService` tests

**Existing test file**: `src/test/java/at/jku/se/smarthome/service/TestMockIoTIntegrationService.java`  
**Production class**: `src/main/java/at/jku/se/smarthome/service/mock/MockIoTIntegrationService.java`  
**Current coverage**: 14% (~190 lines missed)

Existing tests cover `connect()` and `saveConfiguration()` for the disabled path. Uncovered:

- `disconnect()` — clears connection state and device list
- `refreshDevices()` when connected — should return a non-empty list
- `refreshDevices()` when not connected — should fail or return empty
- `getDevices()` — returns observable list
- `getLastSyncTime()` — null before connect, non-null after
- `getProtocolName()` / `getBrokerAddress()` / `getPort()` — getter coverage
- `saveConfiguration()` enabled path — re-connects with new config

Use `MockIoTIntegrationService.resetForTesting()` in `@Before`. No ServiceRegistry wiring needed (IoT service does not call LogService).

Estimated gain: **~130 lines**.

---

## Step 5 — Expand `JdbcUserRegistrationStore` tests

**Existing test file**: `src/test/java/at/jku/se/smarthome/service/TestJdbcUserRegistrationStore.java`  
**Production class**: `src/main/java/at/jku/se/smarthome/service/real/auth/JdbcUserRegistrationStore.java`  
**Current coverage**: 56% (~155 lines missed)

Test infrastructure already uses an in-memory H2 database — keep using the same pattern:
```java
jdbcUrl = "jdbc:h2:mem:auth_" + System.nanoTime() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
System.setProperty("smarthome.db.url", jdbcUrl);
System.setProperty("smarthome.db.user", "sa");
System.setProperty("smarthome.db.password", "");
```

Uncovered paths to test:

- `findAllUsers()` — empty store returns empty list; store with 2 users returns list of 2
- `findAllUsers()` maps all fields correctly (email, username, passwordHash, role, status)
- `emailExists()` — false when store is empty
- Missing DB config (`smarthome.db.url` not set) → `save()` throws `StoreConfigurationException`
- Missing DB config → `findByEmail()` throws `StoreConfigurationException`
- Missing DB config → `emailExists()` throws `StoreConfigurationException`

For the missing-config tests: use a separate `@Test` that clears the system property before calling and restores it in `@After`.

Estimated gain: **~90 lines**.

---

## Step 6 — Add `JdbcScheduleService` tests

**No existing test file** for the JDBC implementation.  
**Production class**: `src/main/java/at/jku/se/smarthome/service/real/schedule/JdbcScheduleService.java`  
**Current coverage**: 45% (~160 lines missed, counting both JDBC and mock schedule services together)

**New file**: `src/test/java/at/jku/se/smarthome/service/TestJdbcScheduleService.java`

Follow the exact same H2 pattern used by `TestJdbcRoomService.java` and `TestJdbcRuleService.java` — they are the closest analogues. The schedule DB table is initialized by the SQL init scripts already on the classpath.

Setup:
```java
@Before
public void setUp() {
    String jdbcUrl = "jdbc:h2:mem:sched_" + System.nanoTime() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    System.setProperty("smarthome.db.url", jdbcUrl);
    System.setProperty("smarthome.db.user", "sa");
    System.setProperty("smarthome.db.password", "");
    service = new JdbcScheduleService();
}

@After
public void tearDown() {
    System.clearProperty("smarthome.db.url");
    System.clearProperty("smarthome.db.user");
    System.clearProperty("smarthome.db.password");
}
```

Tests to add:
- `addSchedule` with valid data → `getSchedules()` returns one entry
- `addSchedule` stores correct name and cron expression
- `deleteSchedule` removes the entry from `getSchedules()`
- `deleteSchedule` on unknown id → no error, list unchanged
- `updateSchedule` changes the name
- `getSchedules()` on empty store → returns empty list

Use `@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods"})` on the class.

Estimated gain: **~110 lines**.

---

## Step 7 — Expand `MockSimulationService` tests

**Existing test file**: `src/test/java/at/jku/se/smarthome/service/TestMockSimulationService.java`  
**Production class**: `src/main/java/at/jku/se/smarthome/service/mock/MockSimulationService.java`  
**Current coverage**: 28% (~145 lines missed)

`MockSimulationService` has pure logic methods with no JavaFX or DB dependencies:

- `buildPlan(SimulationConfiguration)` — takes start time string and duration, returns a `SimulationPlan` with a list of `SimulationEvent` objects. Test that the returned plan's event list is non-empty and covers the expected duration.
- `applyEvent(ObservableList<SimulationDeviceState>, SimulationEvent)` — applies a single event to a device state list. Create a minimal `ObservableList`, call `applyEvent`, assert state changed.
- `parseStartTime(String)` — returns a `LocalTime`. Test valid format (`"08:30"`), null input, blank input, and malformed input.

Use `MockSimulationService.resetForTesting()` in `@Before`.

Estimated gain: **~100 lines**.

---

## Step 8 — Expand `MockSceneService` tests

**Existing test file**: `src/test/java/at/jku/se/smarthome/service/TestMockSceneService.java`  
**Production class**: `src/main/java/at/jku/se/smarthome/service/mock/MockSceneService.java`  
**Current coverage**: 29% (~150 lines missed)

The existing tests cover add/delete. Uncovered paths:

- `executeScene(String sceneId)` — applies each device state in the scene to the room service; verify via `MockRoomService` that devices are toggled
- `executeScene` with unknown id — no error, returns false
- `updateScene(String sceneId, Scene updated)` — replaces the scene in the list
- `getSceneById(String sceneId)` — returns correct scene or null

The test class already wires `MockLogService` and `MockRoomService` via `ServiceRegistry` in `@Before`/`@After`. Continue using that same setup.

Estimated gain: **~100 lines**.

---

## Step 9 — Cover `VacationModeConfig` model class

**No existing tests** for this model class.  
**Production class**: `src/main/java/at/jku/se/smarthome/model/VacationModeConfig.java`  
**Current coverage**: 0%

Add tests to a new file: `src/test/java/at/jku/se/smarthome/model/TestVacationModeConfig.java`

Test every getter and setter. No dependencies — pure POJO. Follow the same style as existing model tests (`TestDevice.java`, etc.).

Estimated gain: **~25 lines**.

---

## PMD rules to keep in mind

These PMD rules are enforced and break the build if violated. Apply suppressions as needed:

- `PMD.AtLeastOneConstructor` → add `@SuppressWarnings("PMD.AtLeastOneConstructor")` on any test class without an explicit constructor
- `PMD.TooManyMethods` → add to class-level annotation when a test class exceeds ~10 methods
- `PMD.UnitTestContainsTooManyAsserts` → add to individual test methods with more than 1 assert, or split the test
- `PMD.UnitTestShouldIncludeAssert` → every `@Test` method must have at least one `assert*`

Reference pattern for class-level suppression:
```java
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods"})
public class TestFooBar {
```

---

## Verification

After completing all steps, run:
```
mvn verify
```

Expected result:
- 0 test failures
- 0 PMD violations
- JaCoCo line coverage ≥ 70% (build succeeds past the check goal)

If coverage is between 68–70% after all steps, check `target/site/jacoco/index.html` for any remaining zero-coverage classes that still have passing tests — that would indicate the JaCoCo instrumentation issue is not fully resolved (try `0.8.13` → `0.8.14` if available).
