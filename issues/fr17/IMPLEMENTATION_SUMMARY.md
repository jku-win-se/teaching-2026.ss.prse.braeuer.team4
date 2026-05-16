# FR-17: Persistent Scene Management — Implementation Summary

**Date:** 2026-05-16  
**Issue:** FR-17

## Overview

The goal of this implementation was to transition the application's "Scene" feature from an ephemeral, mock-only state into a fully persistent, JDBC-backed service. This was achieved by introducing a standard service interface and implementing a SQL-backed version, mirroring the architecture established by `JdbcRuleService` and `JdbcScheduleService`.

## 1. Database Schema (`init-scenes.sql`)

A new initialization script was added to the resources (`src/main/resources/db/init-scenes.sql`) containing two tables:
*   `scenes`: Stores the high-level scene metadata (`id`, `name`, `description`, timestamps).
*   `scene_device_states`: Stores the individual device states associated with a scene (`scene_id`, `device_state`, `sort_order`).
*   **Relationship**: A one-to-many relationship was established with an `ON DELETE CASCADE` constraint on `scene_id` to ensure that deleting a scene automatically purges its associated device states.

## 2. Service Layer

### `SceneService` Interface
Extracted the public contract from the original `MockSceneService` into `src/main/java/at/jku/se/smarthome/service/api/SceneService.java`. This guarantees that UI controllers interact with an abstraction rather than a concrete implementation.

### `MockSceneService` Refactoring
Updated `src/main/java/at/jku/se/smarthome/service/mock/MockSceneService.java` to:
*   Implement the new `SceneService` interface.
*   Decouple from concrete mock classes (`MockRoomService`, `MockLogService`) by fetching dependencies dynamically through the `ServiceRegistry`.

### `JdbcSceneService` Implementation
Created `src/main/java/at/jku/se/smarthome/service/real/scene/JdbcSceneService.java` which:
*   Persists scene creations, updates, and deletions to the SQLite database.
*   Maintains an in-memory `ObservableList<Scene>` mirror so JavaFX UI components update reactively without requiring continuous database polling.
*   Implements `activateScene` logic, parsing the stored "DeviceName: State" strings, interacting with the `RoomService` to apply the state, and utilizing the `LogService` and `NotificationService` to report the outcome.
*   Automatically seeds three demo scenes ("Movie Night", "Away", "Morning") if the database is empty on startup.

## 3. Application Wiring

### `ServiceRegistry`
Added a `SceneServiceHolder` and `getSceneService()` method to `src/main/java/at/jku/se/smarthome/service/api/ServiceRegistry.java`. This registers the `JdbcSceneService` as the default singleton while allowing tests to inject mock overrides via `setSceneServiceForTesting()`.

### `ScenesController`
Updated `src/main/java/at/jku/se/smarthome/controller/ScenesController.java` to import `SceneService` and fetch its instance via `ServiceRegistry.getSceneService()`, successfully swapping out the mock for the real database-backed implementation without needing to alter any UI logic.

## 4. Testing and Code Quality

*   **`TestJdbcSceneService.java`**: Added comprehensive unit tests for the JDBC implementation, verifying that creations, updates, cascades on deletion, and activations behave correctly and survive service reloads (verifying actual persistence).
*   **Test Maintenance**: Refactored `TestMockSceneService.java` setup and teardown methods to correctly register and deregister `ServiceRegistry` overrides, ensuring test isolation.
*   **Code Quality**: Addressed PMD static analysis warnings, specifically renaming test methods in `TestJdbcSceneService` to adhere to JUnit 4 `[a-z][a-zA-Z0-9]*` naming conventions, and resolving missing dependency bindings for testing logs. All tests and static checks (`mvn clean verify`) now pass successfully.
