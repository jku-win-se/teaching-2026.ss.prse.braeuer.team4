# FR-02 Implementation Summary

Date: 2026-04-10
Status: Implemented
Scope: FR-02 secure login and logout for registered users

## Goal
Replace the remaining in-memory-only login path with the same PostgreSQL-backed auth store already used for FR-01 registration, while keeping the existing JavaFX scene flow and service boundaries intact.

## Current Gap Before This Slice
- Registration persisted users to PostgreSQL.
- Login still authenticated only against the in-memory `users` list.
- Logout cleared local session fields, but the auth flow did not update persisted login metadata.
- The UI performed login synchronously, which would block once JDBC-backed authentication was introduced.

## Implemented Approach
- Reused `MockUserService` as the controller-facing auth service to avoid wider controller churn.
- Extended the JDBC auth store to support user lookup by e-mail and `last_login_at` updates.
- Added a dedicated login result model in the service so controllers can distinguish invalid credentials, throttling, configuration failures, and infrastructure failures.
- Added session hardening in the service with in-memory session expiration and explicit invalidation on logout.
- Added progressive login throttling after repeated failed attempts.
- Moved login execution off the JavaFX application thread with a background `Task`.

## Files Changed
- `src/main/java/at/jku/se/smarthome/service/UserRegistrationStore.java`
- `src/main/java/at/jku/se/smarthome/service/JdbcUserRegistrationStore.java`
- `src/main/java/at/jku/se/smarthome/service/MockUserService.java`
- `src/main/java/at/jku/se/smarthome/controller/LoginController.java`
- `src/test/java/at/jku/se/smarthome/service/TestMockUserServiceRegistration.java`
- `src/test/java/at/jku/se/smarthome/service/TestMockUserServiceLoginLogout.java`

## Execution Checklist
- [x] Document the FR-02 implementation slice in `prompts/#9/IMPLEMENTATION_SUMMARY.md`
- [x] Extend the auth persistence boundary with lookup and last-login update operations
- [x] Replace in-memory-only login with JDBC-backed credential verification
- [x] Keep invalid login feedback generic while surfacing throttling and system errors separately
- [x] Invalidate in-memory session state on logout and add session expiry handling
- [x] Keep login responsive by running authentication off the JavaFX UI thread
- [x] Add focused service-level tests for successful login, failed login, throttling, logout, and session expiry
- [x] Verify with compile and targeted tests

## Verification Targets
- Valid persisted credentials authenticate successfully.
- Invalid credentials fail with a generic user-facing message.
- Repeated failed attempts trigger temporary throttling.
- Successful login updates `last_login_at` in the auth table.
- Logout clears the authenticated session state.
- Expired sessions are invalidated on access.

## Security Notes
- SECURITY-05: Addressed with prepared statements only for all auth queries.
- SECURITY-09: Addressed by keeping invalid login responses generic in the UI.
- SECURITY-11: Addressed by keeping authentication logic inside the service and persistence boundary rather than controllers.
- SECURITY-12: Addressed for password hashing, session invalidation, session expiry, and brute-force throttling.

## Deferred Items
- MFA for owner accounts is still not implemented in this desktop prototype.
- Full persisted user-list synchronization for the user-management screen remains outside this FR-02 slice.
