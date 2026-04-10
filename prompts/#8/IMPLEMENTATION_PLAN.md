# SmartHome Auth Persistence Plan

Date: 2026-04-10
Status: Refined after team decision

## Goal
Implement FR-01 and FR-02 by replacing the current in-memory authentication flow with a PostgreSQL-backed authentication module, while keeping the rest of the SmartHome mock services in place for now.

## Recommendation Summary
- Recommended persistence approach: plain JDBC with PostgreSQL
- Recommended migration/setup approach: one checked-in SQL schema script for auth
- Recommended connection handling: simple `DriverManager`-based connections first, no pool unless performance proves it necessary
- Recommended password hashing: bcrypt
- Recommended local secret handling: one ignored local properties file or environment variables, with a committed example file

## Why JDBC Is The Easiest Option Now
Given the updated project constraints, plain JDBC is the shortest implementation path because:
1. The current application already uses JavaFX controllers plus singleton services rather than a larger framework.
2. Only authentication needs persistence right now, not the full smart-home domain.
3. Adding Hibernate would introduce entities, ORM configuration, lifecycle concerns, and extra dependencies that do not buy much for a single `users` table.
4. The existing `MockUserService` API can be kept almost unchanged and reimplemented against SQL.
5. JDBC keeps the refactor localized and easier for a student team to understand, debug, and demo.

## Decision Boundary
For this university project, direct PostgreSQL access from the JavaFX client is acceptable as a documented tradeoff.

This remains unsuitable for a distributed production desktop application because database credentials in a client can be extracted and access control is weaker than with a backend.

If the project later needs a safer architecture, the upgrade path is:

JavaFX client -> HTTPS REST API -> PostgreSQL

That future architecture should be treated as a separate scope increase, not part of this first implementation.

## Scope For First Iteration
Limit the first migration to authentication only:
1. User registration
2. User login
3. User logout
4. Current authenticated user session state
5. Role lookup needed by the existing UI
6. Optional reuse of the same table for owner/member administration if time permits

Leave all other SmartHome mock services unchanged for now.

## Existing Code To Reuse
Keep the current JavaFX flow and replace only the underlying auth implementation.

Relevant existing files:
- `src/main/java/at/jku/se/smarthome/SmartHomeApp.java`
- `src/main/java/at/jku/se/smarthome/controller/LoginController.java`
- `src/main/java/at/jku/se/smarthome/controller/RegisterController.java`
- `src/main/java/at/jku/se/smarthome/controller/MainController.java`
- `src/main/java/at/jku/se/smarthome/service/MockUserService.java`
- `src/main/java/at/jku/se/smarthome/model/User.java`

The UI flow already exists and should be preserved. The easiest implementation is to keep the controllers almost unchanged and swap the backing service from in-memory storage to JDBC.

## Recommended Minimal Architecture

### Keep
1. Current JavaFX controllers and scene flow.
2. Current `User` model as the UI-facing model for table views and bindings.
3. Singleton-style service access if that is already the project convention.

### Add
1. A small database configuration helper.
2. A small JDBC repository or DAO for user queries.
3. A JDBC-backed auth service that preserves the current service API as much as possible.
4. A SQL schema script for creating the auth table.

### Avoid For Now
1. Hibernate or Jakarta Persistence.
2. Flyway or Liquibase unless the team later wants stricter migration tracking.
3. HikariCP unless repeated connection overhead becomes a measurable problem.
4. Persisting every existing mock domain service.

## Why Not Put SQL Directly In Controllers
Direct database access from the JavaFX application is acceptable.

Direct SQL inside `LoginController` and `RegisterController` is still the wrong tradeoff because it would spread persistence code into UI classes and make testing harder.

The lowest-friction clean boundary is:

Controller -> Auth/User Service -> JDBC Repository -> PostgreSQL

This keeps the project simple without creating ORM complexity.

## Implementation Phases

### Phase 1: Lock Scope
1. Keep the migration limited to auth and user-role lookup.
2. Treat direct client-to-database access as a documented course-project exception.
3. Leave all non-auth mock services untouched.

### Phase 2: Add Minimal Dependencies And Config
1. Add dependencies in `pom.xml` for:
   - PostgreSQL JDBC driver
   - bcrypt library
2. Add a lightweight config helper to read database host, port, database name, username, password, and SSL mode.
3. Store developer-local database settings in either:
   - an ignored local properties file, or
   - environment variables
4. Commit only an example config file, never real credentials.
5. Fail fast with a clear startup message if required DB settings are missing.

### Phase 3: Create The Auth Schema
1. Add a checked-in SQL script for the auth table setup.
2. Start with a single `users` table.
3. At minimum include:
   - `id`
   - `email` with a unique constraint
   - `username`
   - `password_hash`
   - `role`
   - `status`
   - `created_at`
   - `updated_at`
   - optional `last_login_at`
4. Seed at least one owner account for demos if needed.
5. Use SQL types and constraints that are simple and explicit rather than generic ORM-driven defaults.

### Phase 4: Add JDBC Persistence Classes
1. Create a `DatabaseConfig` or similar helper.
2. Create a `UserRepository` or `JdbcUserRepository` with methods for:
   - finding a user by e-mail
   - inserting a new user
   - updating last login timestamp
   - listing users if admin management is included
   - changing user status if revoke/restore is included
3. Use only prepared statements.
4. Map rows into a simple internal record or directly into a service-layer representation.

### Phase 5: Replace Mock Authentication With A JDBC Service
1. Keep the public API close to `MockUserService` so controller changes stay small.
2. Replace in-memory login and registration with repository-backed logic.
3. Keep current session state in memory inside the service after successful login.
4. Preserve existing role checks such as owner/member behavior.
5. Decide whether the existing class should be adapted in place or replaced by a new `JdbcUserService` behind the same controller-facing contract.

### Phase 6: Apply Minimum Security Baseline
1. Hash all passwords with bcrypt before insert.
2. Never store or compare plaintext passwords.
3. Keep login failure messages generic.
4. Normalize e-mail addresses before lookup and insert.
5. Use prepared statements for all SQL to prevent injection.
6. Keep credentials out of source control.
7. If the shared PostgreSQL instance supports it, require SSL/TLS in the JDBC URL.

This keeps the course-project approach simple while still covering the most important security basics.

### Phase 7: Keep The JavaFX UI Responsive
1. Do not perform JDBC calls on the JavaFX application thread.
2. Use `Task` or a small executor-backed helper for login and registration.
3. Update UI state back on the JavaFX thread after the database operation completes.

### Phase 8: Testing And Validation
1. Add non-UI tests for auth logic where possible.
2. Cover at least:
   - duplicate e-mail rejection
   - successful registration
   - successful login
   - failed login
   - revoked or inactive user rejection if retained
   - logout/session clearing
3. Keep tests independent from the shared production/demo database.
4. If testcontainers is too heavy for the team, allow repository tests against a dedicated local test database or keep service tests mocked.

### Phase 9: Documentation And Handover
1. Document the required DB configuration values.
2. Document how to create the schema from the SQL script.
3. Document that this is a direct-client-to-database university-project tradeoff.
4. Document the future migration path to a backend service if the project grows.

## Suggested New Packages
Recommended new packages for the implementation:
- `src/main/java/at/jku/se/smarthome/config`
- `src/main/java/at/jku/se/smarthome/persistence`
- `src/main/java/at/jku/se/smarthome/service`
- `src/main/resources/db`

## Files Likely To Change
- `pom.xml`
- `src/main/java/at/jku/se/smarthome/SmartHomeApp.java`
- `src/main/java/at/jku/se/smarthome/controller/LoginController.java`
- `src/main/java/at/jku/se/smarthome/controller/RegisterController.java`
- `src/main/java/at/jku/se/smarthome/controller/MainController.java`
- `src/main/java/at/jku/se/smarthome/service/MockUserService.java` or a replacement service class
- `src/main/java/at/jku/se/smarthome/model/User.java`
- `src/main/resources/db/init-auth.sql`
- `.gitignore`
- example local configuration file

## Verification Criteria
1. The project compiles after adding the JDBC auth stack.
2. Registration rejects duplicate e-mail addresses.
3. Stored passwords are bcrypt hashes only.
4. Login succeeds for valid credentials.
5. Login fails generically for invalid credentials.
6. Logout clears session state and returns the app to the login scene.
7. Missing local configuration fails fast with a clear error message.
8. Login and registration remain responsive during database access.
9. SQL is executed through prepared statements only.

## Final Recommendation
For this project, the easiest and most pragmatic next step is:
1. Keep direct PostgreSQL access from the JavaFX app.
2. Use plain JDBC instead of Hibernate.
3. Keep a thin service/repository boundary so controllers stay simple.
4. Add only the PostgreSQL driver, bcrypt, and minimal config support.
5. Use one checked-in SQL setup script instead of a full ORM or migration framework.

## What Changed Compared To The Previous Plan
1. Replaced Hibernate with plain JDBC.
2. Removed the need for JPA entities.
3. Removed the need for Flyway in the first iteration.
4. Removed the need for HikariCP in the first iteration.
5. Reduced the implementation to the smallest change that still keeps the code maintainable.

## Open Team Decisions
Before implementation starts, the team should still confirm:
1. Whether the JDBC-backed service should replace `MockUserService` directly or sit behind a new interface
2. Whether user administration features should immediately use the same table or stay mock-based for the first auth-only iteration
3. Whether local DB config should use environment variables only or an ignored local properties file
4. Whether a simple SQL setup script is enough or whether the team still wants a migration tool later
