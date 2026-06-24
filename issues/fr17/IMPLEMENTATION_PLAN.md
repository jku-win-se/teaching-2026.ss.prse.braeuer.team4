# FR-17: Persistent Scene Management — Implementation Plan

**Date:** 2026-05-16  
**Status:** Ready for implementation  
**Scope:** Lift the existing mock-only scene implementation to a JDBC-backed persistent
service, following the same `api / mock / real` pattern already established for rules,
schedules, and notifications.

---

## User Stories

| ID | Requirement |
|---|---|
| US-29 | As a user, I want to create named scenes with predefined device states, so that I can prepare recurring smart home situations such as "Movie Night" or "Away". |
| US-30 | As a user, I want to activate a scene with a single action, so that multiple devices change to the desired states at once. |

---

## Current State

| Artefact | Location | Problem |
|---|---|---|
| `Scene` model | `model/Scene.java` | Fine as-is. Stores `deviceStates` as `ObservableList<String>`. |
| `MockSceneService` | `service/mock/MockSceneService.java` | In-memory only. No interface. Hard-wires `MockRoomService` and `MockLogService` directly instead of going through `ServiceRegistry`. |
| `ScenesController` | `controller/ScenesController.java` | Imports and instantiates `MockSceneService` directly — not via `ServiceRegistry`. |
| `SceneService` interface | **does not exist** | Controllers cannot be switched to a real implementation. |
| `JdbcSceneService` | **does not exist** | No persistence. |
| `init-scenes.sql` | **does not exist** | No database schema. |
| `ServiceRegistry` | `service/api/ServiceRegistry.java` | Has no `getSceneService()` method. |

---

## Required Changes — Step by Step

### Step 1 — Create `SceneService` interface

**File:** `src/main/java/at/jku/se/smarthome/service/api/SceneService.java`

Extract the public contract from `MockSceneService`. The interface must declare:

```java
ObservableList<Scene> getScenes();

Scene addScene(String name, String description);
Scene addScene(String name, String description, List<String> deviceStates);

boolean updateScene(String sceneId, String name, String description);
boolean updateScene(String sceneId, String name, String description, List<String> deviceStates);

boolean deleteScene(String sceneId);

boolean activateScene(String sceneId);
```

---

### Step 2 — Make `MockSceneService` implement `SceneService`

**File:** `src/main/java/at/jku/se/smarthome/service/mock/MockSceneService.java`

Two changes:
1. Add `implements SceneService` to the class declaration.
2. Replace the two hard-wired field references:
   - `private final MockRoomService roomService = MockRoomService.getInstance();`
     → `private final RoomService roomService = ServiceRegistry.getRoomService();`
   - `private final MockLogService logService = MockLogService.getInstance();`
     → `private final LogService logService = ServiceRegistry.getLogService();`

This makes the mock safe for use in tests that inject service overrides and removes
the mock↔mock coupling.

---

### Step 3 — Create the SQL schema

**File:** `src/main/resources/db/init-scenes.sql`

Two tables — scenes and their device state rows (one-to-many):

```sql
CREATE TABLE IF NOT EXISTS scenes (
    id          VARCHAR(64)  PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS scene_device_states (
    id          SERIAL       PRIMARY KEY,
    scene_id    VARCHAR(64)  NOT NULL REFERENCES scenes(id) ON DELETE CASCADE,
    device_state VARCHAR(512) NOT NULL,
    sort_order  INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_scene_device_states_scene_id
    ON scene_device_states (scene_id);
```

`ON DELETE CASCADE` means deleting a scene automatically removes all its device-state rows —
no manual cleanup needed in the service.

---

### Step 4 — Create `JdbcSceneService`

**File:** `src/main/java/at/jku/se/smarthome/service/real/scene/JdbcSceneService.java`

Follows the exact same pattern as `JdbcRuleService`. Key points:

**Constructor and singleton:**
```java
private static final String INIT_SCRIPT_PATH = "/db/init-scenes.sql";
private static final Object INSTANCE_LOCK = new Object();
private static JdbcSceneService instance;

private final ObservableList<Scene> scenes = FXCollections.observableArrayList();

private JdbcSceneService() {
    super("scene", INIT_SCRIPT_PATH);
    refreshScenes();
    seedDemoScenes();
}
```

**`refreshScenes()`** — load all scenes, then for each load its ordered device states:
```
SELECT id, name, description FROM scenes ORDER BY created_at, id
```
For each scene id:
```
SELECT device_state FROM scene_device_states
WHERE scene_id = ? ORDER BY sort_order, id
```

**`addScene()`** — validate name is non-blank, insert into `scenes`, then insert each
device state into `scene_device_states` with `sort_order = index`. Add the new `Scene`
object to the in-memory list. Return the created scene.

**`updateScene()`** — update the `scenes` row, then `DELETE FROM scene_device_states
WHERE scene_id = ?`, then re-insert the new device states. Update the in-memory object.

**`deleteScene()`** — `DELETE FROM scenes WHERE id = ?` (cascade handles device states).
Remove from in-memory list.

**`activateScene()`** — same logic as `MockSceneService.activateScene()`: iterate
device states, parse "DeviceName: state" format, apply to device via
`ServiceRegistry.getRoomService()`, log via `ServiceRegistry.getLogService()`,
emit notification via `ServiceRegistry.getNotificationService()`.

**`seedDemoScenes()`** — called in constructor only when the `scenes` table is empty.
Insert the same three demo scenes that `MockSceneService.initializeMockScenes()` uses:
- `"Movie Night"` with 3 device states
- `"Away"` with 5 device states
- `"Morning"` with 4 device states

**`resetForTesting()`** — same pattern as `JdbcRuleService.resetForTesting()`.

---

### Step 5 — Register in `ServiceRegistry`

**File:** `src/main/java/at/jku/se/smarthome/service/api/ServiceRegistry.java`

Add a holder, getter, and test override — mirror the existing `RuleServiceHolder` block:

```java
private static SceneService testSceneServiceOverride;

public static SceneService getSceneService() {
    return testSceneServiceOverride != null
            ? testSceneServiceOverride : SceneServiceHolder.INSTANCE;
}

private static final class SceneServiceHolder {
    private static final SceneService INSTANCE = JdbcSceneService.getInstance();
}

public static void setSceneServiceForTesting(SceneService svc) {
    synchronized (OVERRIDE_LOCK) {
        testSceneServiceOverride = svc;
    }
}
```

Also add `testSceneServiceOverride = null;` to `resetForTesting()`.

---

### Step 6 — Rewire `ScenesController`

**File:** `src/main/java/at/jku/se/smarthome/controller/ScenesController.java`

Change line 54:
```java
// Before
private final MockSceneService sceneService = MockSceneService.getInstance();

// After
private final SceneService sceneService = ServiceRegistry.getSceneService();
```

Remove the `import at.jku.se.smarthome.service.mock.MockSceneService;` line.
Add `import at.jku.se.smarthome.service.api.SceneService;`.

No other controller logic changes — all method signatures in the interface match
what the controller already calls.

---

### Step 7 — Add `TestJdbcSceneService`

**File:** `src/test/java/at/jku/se/smarthome/service/TestJdbcSceneService.java`

Follow the pattern of `TestJdbcRuleService`. Cover:

1. `addScene` persists and appears in `getScenes()`
2. `addScene` with device states — device states are stored and retrieved correctly
3. `updateScene` changes name, description, and device states
4. `deleteScene` removes the scene and its device states
5. `activateScene` returns true for a known scene (use a mock room service that
   returns a test device so the action can be applied without a real DB room)
6. `activateScene` returns false for an unknown scene id
7. After restart simulation (`resetForTesting()` + re-get instance), previously added
   scenes are still present (verifies actual persistence round-trip)

Test setup pattern — same as `TestJdbcRuleService`:
- Use `ServiceRegistry.setRoomServiceForTesting(...)` and
  `ServiceRegistry.setNotificationServiceForTesting(...)` with mock instances
- Call `JdbcSceneService.resetForTesting()` in `@Before`

---

## Files Changed Summary

| File | Change type |
|---|---|
| `service/api/SceneService.java` | **new** |
| `service/mock/MockSceneService.java` | add `implements SceneService`; swap `MockRoomService`/`MockLogService` fields to use `ServiceRegistry` |
| `service/real/scene/JdbcSceneService.java` | **new** |
| `src/main/resources/db/init-scenes.sql` | **new** |
| `service/api/ServiceRegistry.java` | add `getSceneService()`, holder, test override, reset |
| `controller/ScenesController.java` | swap field type and import |
| `test/.../TestJdbcSceneService.java` | **new** |

---

## Implementation Order

Follow this order to keep the build green at every step:

1. `init-scenes.sql` (no Java dependency)
2. `SceneService` interface (nothing depends on it yet)
3. `MockSceneService` implements `SceneService` + `ServiceRegistry` fields fix (compile check)
4. `JdbcSceneService` (depends on interface + schema)
5. `ServiceRegistry` wiring (depends on `JdbcSceneService`)
6. `ScenesController` rewire (depends on `ServiceRegistry`)
7. `TestJdbcSceneService` (depends on all of the above)
8. `mvn clean verify` — confirm build green and no PMD/Checkstyle violations

---

## What Is Explicitly Out of Scope

- No changes to the `Scene` model class — it is sufficient as-is
- No changes to `SmartHomeApp` — scenes have no background execution loop
- No changes to existing `TestMockSceneService` — it stays as a mock-layer test
