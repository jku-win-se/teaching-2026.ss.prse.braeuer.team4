# PRSE Teaching Template (Calculator)

This repository is a minimal Java/JavaFX template used in the PRSE course.
It demonstrates a clean, testable codebase featuring a graphical calculator UI, a separated business logic layer, an operation factory, and an enforced CI pipeline.

## Prerequisites

- Java 21 (JDK)
- Maven 3.9+

## Project Structure

```
src/
├── main/
│   ├── java/at/jku/se/calculator/
│   │   ├── SimpleCalculator.java          # JavaFX Application entry point
│   │   ├── CalcAction.java                # Enum of supported operations (ADD, SUB, MULT, DIV, POW)
│   │   ├── CalculatorController.java      # Business logic (state, evaluation, input handling)
│   │   ├── CalculatorFXController.java    # FXML event controller (UI ↔ CalculatorController)
│   │   ├── factory/
│   │   │   ├── ICalculationOperation.java # Interface for all operations
│   │   │   └── OperationFactory.java      # Maps CalcAction → ICalculationOperation
│   │   └── operators/
│   │       ├── AddOperation.java          # Implemented: addition
│   │       └── NullOperation.java         # Null-object for unimplemented operations
│   └── resources/at/jku/se/calculator/
│       ├── calculator.fxml                # JavaFX UI layout
│       └── log4j2.xml                     # Logging configuration
└── test/
    └── java/at/jku/se/calculator/
        ├── TestCalculatorController.java  # Business logic tests
        ├── factory/TestOperationFactory.java
        └── operators/
            ├── TestAddOperation.java
            └── TestNullOperation.java
docs/
├── benutzerdoku.md                        # User documentation
└── systemdoku.md                          # Technical/system documentation
```

## Common Commands

| Command | Description |
|---|---|
| `mvn javafx:run` | Compile and launch the JavaFX UI |
| `mvn clean test` | Run all unit tests + JaCoCo coverage check (≥ 80%) |
| `mvn clean package` | Build the JAR (skipping tests) |
| `mvn pmd:pmd pmd:cpd` | Run PMD static analysis |

## Test Coverage

JaCoCo is configured to enforce a minimum of **80% instruction coverage** on every `mvn test` run. The JavaFX entry point (`SimpleCalculator`) and FXML controller (`CalculatorFXController`) are excluded from coverage measurement as they require a live display to run.

Coverage reports are generated at `target/site/jacoco/index.html` after each test run.

## CI Pipeline

The GitHub Actions workflow (`.github/workflows/Continuous Integration.yaml`) runs on every push and pull request to `main`:

1. **Compile** — fail fast on compilation errors
2. **Test & coverage check** — runs all unit tests and enforces ≥ 80% coverage
3. **PMD static analysis** — runs PMD and CPD using the project's `ruleset.xml`
4. **Package** — builds the JAR artifact

Test results, JaCoCo reports, PMD reports, and the JAR are all uploaded as workflow artifacts.

## Notes for Students

- Add new operations in `operators/` implementing `ICalculationOperation`.
- Register them in `OperationFactory` by adding a case to the `CalcAction` switch.
- Keep test coverage above 80% — the build will fail otherwise.
- Document user-visible features in `docs/benutzerdoku.md`.
- Document architecture and design decisions in `docs/systemdoku.md`.
