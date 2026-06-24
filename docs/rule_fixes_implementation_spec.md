# Rule Fixes — Implementation Specification

Branch with Manuel's partial fix already merged upstream:
`origin/feature/20-fix-rules-not-executable-from-ui`

---

## Bug 1 — Manual UI execution checks condition and blocks action

### What breaks
Clicking "Run" in the rules table calls `handleRunRule(rule)` in
`RulesController` (line 172–174). That calls
`ruleService.executeRule(rule.getId())`, which routes to
`JdbcRuleService.executeEnabledRule(rule)` (line 222–253). That always calls
`new RuleEvaluator().evaluate(rule, sourceDevice)`. If the condition is not
currently satisfied (e.g. wrong time of day, device in wrong state), execution
is silently blocked and a "condition not met" notification is emitted.

### Fix — already implemented by Manuel in `feature/20-fix-rules-not-executable-from-ui`
The fix is already in that branch. Do not re-implement; just merge/rebase on top.

Changes in that branch:
- `RuleService.java` — new method signature added to interface:
  ```java
  boolean executeRule(String ruleId, boolean isManual);
  ```
- `JdbcRuleService.java` — `executeRule(String)` now delegates to
  `executeRule(ruleId, false)`; the new overload passes `isManual` into
  `executeEnabledRule(rule, isManual)`, which uses
  `boolean conditionMet = isManual || new RuleEvaluator().evaluate(rule, sourceDevice);`
- `MockRuleService.java` — same two-method pattern applied
- `RulesController.java:174` — changed to `ruleService.executeRule(rule.getId(), true)`

---

## Bug 2 — Invalid condition produces no error message

### What breaks
`RulesController.handleAddRule()` (lines 116–129):
```java
result.ifPresent(input -> ruleService.addRule(
        input.name(), input.triggerType(), input.sourceDevice(),
        input.condition(), input.action(), input.targetDevice()
));
```
The return value of `addRule()` is discarded. When the condition is malformed,
`JdbcRuleService.addRule()` (lines 84–109) calls `RuleValidator.validate()`
and, if invalid, returns `null` with **no notification and no UI feedback**.
The dialog closes, the rule never appears in the table, and the user has no
idea what went wrong.

`RuleValidator.validate()` already produces a human-readable `reason` string
(e.g. `"malformed time expression: foo"`, `"source device not found: X"`).

### Fix

**Option A (recommended) — show an alert in the controller**

In `RulesController.handleAddRule()`, change the `ifPresent` block:
```java
result.ifPresent(input -> {
    Rule created = ruleService.addRule(
            input.name(), input.triggerType(), input.sourceDevice(),
            input.condition(), input.action(), input.targetDevice()
    );
    if (created == null) {
        showRuleValidationError(input.triggerType(), input.condition(),
                input.sourceDevice());
    }
});
```

Add a helper that runs the validator and shows the reason:
```java
private void showRuleValidationError(String triggerType, String condition,
                                     String sourceDevice) {
    RuleValidator.Result result =
            RuleValidator.validate(triggerType, condition, sourceDevice, roomService);
    String detail = result.reason() != null ? result.reason() : "Invalid rule configuration.";
    javafx.scene.control.Alert alert =
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
    alert.setTitle("Rule not saved");
    alert.setHeaderText("The rule could not be created");
    alert.setContentText(detail);
    alert.showAndWait();
}
```

Apply the same pattern to `handleEditRule()` — `ruleService.updateRule()` returns
`false` when validation fails; show the same alert when it returns `false`.

**Files to change:**
- `src/main/java/at/jku/se/smarthome/controller/RulesController.java`
  - `handleAddRule()` — check null return, call `showRuleValidationError`
  - `handleEditRule()` — check false return, call `showRuleValidationError`
  - add private helper `showRuleValidationError(...)`

No changes needed in service layer for this bug.

---

## Bug 3 & 4 — Time-based and Device-State-based background rules never fire

### What breaks
`JdbcRuleService` has **no background polling loop**. The `RuleService`
interface has no `startRecurringExecution()` / `stopRecurringExecution()`
methods. `SmartHomeApp.start()` starts the schedule background loop
(`ServiceRegistry.getScheduleService().startRecurringExecution()` at line 66)
but never starts any equivalent for rules.

As a result, `"Time"` and `"Device State"` (and `"Sensor Threshold"`) rules
with automatic/background triggering never evaluate — they only run when
the user clicks "Run" manually.

Compare with the working schedule background loop in `JdbcScheduleService`
(lines 254–282): a `ScheduledExecutorService` (daemon thread, 15-second
interval) calls `processDueSchedules()` → `executeSchedule()` for each
schedule whose time matches.

### Fix

#### Step 1 — Extend `RuleService` interface

File: `src/main/java/at/jku/se/smarthome/service/api/RuleService.java`

Add two methods:
```java
/**
 * Starts background polling that evaluates all enabled rules on a fixed
 * interval and executes them when their condition is met.
 * Must be idempotent — calling it a second time is a no-op.
 */
void startRecurringExecution();

/**
 * Stops background polling started by {@link #startRecurringExecution()}.
 * Must be idempotent — safe to call even if not started.
 */
void stopRecurringExecution();
```

#### Step 2 — Implement in `JdbcRuleService`

File: `src/main/java/at/jku/se/smarthome/service/real/rule/JdbcRuleService.java`

Add a field:
```java
private ScheduledExecutorService ruleScheduler;
```

Add imports (same as `JdbcScheduleService` uses):
```java
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
```

Implement `startRecurringExecution()` — mirror the schedule pattern exactly:
```java
@Override
public void startRecurringExecution() {
    synchronized (INSTANCE_LOCK) {
        if (ruleScheduler != null && !ruleScheduler.isShutdown()) {
            return;
        }
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "jdbc-rule-dispatcher");
            thread.setDaemon(true);
            return thread;
        };
        ruleScheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
        ruleScheduler.scheduleAtFixedRate(
                () -> Platform.runLater(this::processDueRules),
                0, 30, TimeUnit.SECONDS);
    }
}
```

Implement `stopRecurringExecution()`:
```java
@Override
@SuppressWarnings("PMD.NullAssignment")
public void stopRecurringExecution() {
    synchronized (INSTANCE_LOCK) {
        if (ruleScheduler != null) {
            ruleScheduler.shutdownNow();
            ruleScheduler = null;
        }
    }
}
```

Implement `processDueRules()` — called on the JavaFX thread every 30 seconds:
```java
private void processDueRules() {
    for (Rule rule : rules) {
        if (rule.isEnabled()) {
            executeEnabledRule(rule, false);
        }
    }
}
```

This reuses the existing `executeEnabledRule(rule, isManual)` method (which
will exist after Bug 1's fix is merged). Passing `false` means the condition
IS evaluated — background execution only fires when the condition is truly met.

Also update `resetForTesting()` to shut down the scheduler:
```java
public static void resetForTesting() {
    synchronized (INSTANCE_LOCK) {
        if (instance != null) {
            instance.stopRecurringExecution();
        }
        instance = null;
    }
}
```

#### Step 3 — Add no-op stubs in `MockRuleService`

File: `src/main/java/at/jku/se/smarthome/service/mock/MockRuleService.java`

```java
@Override
public void startRecurringExecution() {
    // no-op for mock — rules execute on explicit call only
}

@Override
public void stopRecurringExecution() {
    // no-op for mock
}
```

#### Step 4 — Wire up in `SmartHomeApp`

File: `src/main/java/at/jku/se/smarthome/SmartHomeApp.java`

In `start()`, after the schedule line (line 66), add:
```java
ServiceRegistry.getRuleService().startRecurringExecution();
```

In `stop()`, after the schedule stop, add:
```java
ServiceRegistry.getRuleService().stopRecurringExecution();
```

---

## Implementation Order

1. Merge `feature/20-fix-rules-not-executable-from-ui` into main first (Bug 1 is done).
2. Branch off the updated main.
3. Implement Bug 2 fix (controller only, no service changes).
4. Implement Bug 3/4 fix (`RuleService` interface → `JdbcRuleService` → `MockRuleService` → `SmartHomeApp`).
5. Confirm `resetForTesting()` shuts down the scheduler (prevents thread leaks in test runs).

## Files Changed Summary

| File | Bug |
|---|---|
| `controller/RulesController.java` | 2 |
| `service/api/RuleService.java` | 3/4 |
| `service/real/rule/JdbcRuleService.java` | 3/4 |
| `service/mock/MockRuleService.java` | 3/4 |
| `SmartHomeApp.java` | 3/4 |

Bug 1 files are already handled by Manuel's branch:
`controller/RulesController.java`, `service/api/RuleService.java`,
`service/real/rule/JdbcRuleService.java`, `service/mock/MockRuleService.java`
