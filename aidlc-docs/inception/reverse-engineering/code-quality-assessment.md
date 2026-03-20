# Code Quality Assessment

## Test Coverage
- **Overall**: Fair for the calculator module, poor for the smart-home module
- **Unit Tests**: Present for calculator controller, factory, and arithmetic operators
- **Integration Tests**: None detected
- **Coverage Tooling**: JaCoCo is configured and uploaded in CI, but current coverage is concentrated in the calculator package

## Code Quality Indicators
- **Linting and Static Analysis**: PMD is configured in Maven and executed in GitHub Actions
- **Code Style**: Mostly consistent JavaDoc use and naming conventions across packages
- **Documentation**: Fair to good. The repository already includes user, system-architecture, and JavaDoc outputs, but detailed design rationale for the smart-home module remains thin
- **Build Automation**: Good. CI compiles, tests, runs PMD, packages, and uploads artifacts

## Technical Debt
- Mock services hold mutable singleton state, which simplifies demos but complicates repeatable tests and future persistence integration
- Smart-home controllers mix UI code, business decisions, and service orchestration, which increases controller size and coupling
- The smart-home module has little automated test coverage compared with the calculator module
- SmartHomeApp uses `printStackTrace()` during login-scene reload failure handling instead of project logging
- Logging framework configuration exists, but many runtime events are recorded only in in-memory activity logs rather than centralized structured logs

## Patterns and Anti-patterns

### Good Patterns
- Calculator logic is decoupled from UI through CalculatorController
- Calculator operations use a factory plus interchangeable operator implementations
- JavaFX properties and observable collections support reactive UI updates
- CI automation is present and covers build, test, static analysis, and packaging

### Anti-patterns
- Hardcoded mock credentials and plain-text password comparisons in MockUserService
- Role checks are implemented in controllers, increasing duplication and risking inconsistent enforcement
- Business logic is embedded directly in several UI controllers instead of a dedicated service layer per feature
- The repository mixes two different application scopes, smart-home orchestration and calculator teaching code, in one Maven module

## Security Findings
- **SECURITY-01**: N/A. No database, object store, cache, or comparable persistence layer was detected.
- **SECURITY-02**: N/A. No load balancer, API gateway, or CDN components were detected.
- **SECURITY-03**: Non-compliant. Log4j is configured, but the application predominantly uses in-memory activity logging and direct UI alerts instead of centralized structured application logging.
- **SECURITY-04**: N/A. No HTML-serving web endpoints were detected.
- **SECURITY-05**: Non-compliant. Input validation is ad hoc and UI-driven; there is no systematic validation layer for feature inputs or configuration values.
- **SECURITY-06**: N/A. No IAM-style infrastructure policies were detected.
- **SECURITY-07**: N/A. No network infrastructure configuration was detected.
- **SECURITY-08**: N/A for server endpoints, partially relevant in UI role checks. Authorization exists in controllers, but enforcement is not centralized.
- **SECURITY-09**: Non-compliant. MockUserService contains default credentials and SmartHomeApp exposes stack traces through `printStackTrace()`.
- **SECURITY-10**: Non-compliant. Exact dependency versions are declared in Maven, but no dependency vulnerability scanning step was detected in the build workflow.
- **SECURITY-11**: Non-compliant. Security-critical concerns such as authentication and authorization are not isolated into dedicated modules with layered controls.
- **SECURITY-12**: Non-compliant. Passwords are stored and compared in plain text, there is no brute-force protection, and admin-grade MFA support is absent.

## Summary

The project is in a good state for classroom demonstration and iterative feature work, but the smart-home module is not production-ready. The largest gaps are security posture, controller/service separation, and automated test coverage outside the calculator package.