# Issue #21 — Lessons Learned

## What went wrong

When implementing FR-12 (In-app notifications), we encountered CI build failures related to TestFX UI tests and PMD static analysis.

1. **Headless UI Test Failures**: We attempted to skip UI tests in the GitHub Actions (headless) environment by placing `Assume.assumeTrue(System.getenv("CI") == null)` inside a `@Before` method. However, the JavaFX toolkit initialization (triggered by the `TestFX` `ApplicationTest` runner invoking `start(Stage)`) happens *before* JUnit 4 executes the `@Before` lifecycle methods. This caused the CI to crash with `java.lang.UnsupportedOperationException: Unable to open DISPLAY` before the test could be gracefully skipped.
2. **Overzealous PMD Rules**: After moving the assumption to a `@BeforeClass` method, PMD flagged `LoginViewSmokeTest` with `UnitTestShouldUseBeforeAnnotation` because the method was named `setUp()`. PMD enforces that any method named `setUp()` must be annotated with `@Before`, even if we intentionally changed its lifecycle to `@BeforeClass` to solve the headless crash.

## How we fixed it

1. **Static Class-Level Assumptions**: We moved the CI assumption to a static `@BeforeClass` method. This ensures the condition is evaluated by JUnit *before* the TestFX runner instantiates the test class and attempts to initialize the JavaFX toolkit.
2. **Avoiding PMD Naming Traps**: We renamed the static setup method from `setUp()` to `skipIfHeadless()`. This bypasses the PMD rule that rigidly associates the `setUp` method name with the `@Before` annotation.
3. **PMD UI Test Suppressions**: We added class-level `@SuppressWarnings` for PMD rules (e.g., `PMD.MethodNamingConventions`, `PMD.TooManyStaticImports`, `PMD.UnitTestContainsTooManyAsserts`) to all UI test classes, acknowledging that UI tests inherently require different stylistic conventions than production code or standard unit tests.