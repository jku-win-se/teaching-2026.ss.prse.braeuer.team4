# Component Inventory

## Application Packages
- `at.jku.se.smarthome` - JavaFX smart-home desktop application entry point
- `at.jku.se.smarthome.controller` - Screen-level controllers for all smart-home workflows
- `at.jku.se.smarthome.service` - In-memory service layer for application behavior
- `at.jku.se.calculator` - Calculator application and controller logic
- `at.jku.se.calculator.factory` - Calculator operation resolution abstractions
- `at.jku.se.calculator.operators` - Concrete arithmetic operator implementations

## Infrastructure Packages
- None detected in source packages

## Shared Packages
- `at.jku.se.smarthome.model` - Domain models shared across smart-home controllers and services

## Test Packages
- `at.jku.se.calculator` - Calculator controller tests
- `at.jku.se.calculator.factory` - OperationFactory tests
- `at.jku.se.calculator.operators` - Arithmetic operator tests

## Resource Components
- `src/main/resources/at/jku/se/smarthome/view` - 15 FXML views and shared stylesheet for the smart-home UI
- `src/main/resources/at/jku/se/calculator` - Calculator FXML resource
- `src/main/resources/log4j2.xml` - Log4j configuration

## Total Count
- **Total Packages**: 10
- **Application**: 6
- **Infrastructure**: 0
- **Shared**: 1
- **Test**: 3