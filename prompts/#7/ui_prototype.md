# Role
Act as a Senior JavaFX Architect and Frontend Developer.

# Context
We are building the "SmartHome Orchestrator" for a University Software Engineering project (JavaFX, Java 17+). 
I need you to implement the **UI Prototype Structure** based on specific functional requirements (FR-01 to FR-17). 
The backend logic is not fully ready, so you must implement a **Mock Service Layer** to simulate device states and user authentication.

# Tech Stack
- JavaFX 17+
- FXML for Views (Separation of Concerns)
- CSS for Styling (Modern, Clean Look)
- MVC Pattern (Model-View-Controller)
- ControlsFX (optional, otherwise standard JavaFX controls)
- Chart API for Energy Dashboard

# Task
Create the core UI structure and dummy logic for the following views:

1. **Login View (`login-view.fxml`)**
   - Fields: Email, Password.
   - Button: Login.
   - Logic: Mock authentication (accept any non-empty input).

2. **Main Shell (`main-shell.fxml`)**
   - Layout: `BorderPane`.
   - Sidebar: Navigation Buttons (Dashboard, Rooms, Automation, Energy, Settings).
   - Header: User Profile Label, Logout Button.
   - Content Area: `StackPane` to swap views dynamically.

3. **Dashboard View (`dashboard-view.fxml`)**
   - GridPane of "Favorite Device Cards" (Mock data: 1 Light, 1 Thermostat).
   - Simple `PieChart` or `BarChart` placeholder for Energy Consumption (FR-14).
   - Notification Tray ( VBox for alerts).

4. **Room/Device Control Component**
   - Create a reusable custom control or FXML snippet for device types (FR-04/06):
     - **Switch:** ToggleButton.
     - **Dimmer:** Slider + Label (%).
     - **Thermostat:** TextField + Buttons (+/-).
   - Ensure these controls update a mock state observable.

# Requirements & Constraints
- **Mocking:** Create a `MockSmartHomeService` class that provides `ObservableList<Device>` and methods like `toggleDevice`, `setTemperature`. Do not connect to a real database yet.
- **Styling:** Provide a `styles.css` with variables for colors (Primary: #2c3e50, Accent: #3498db, Success: #27ae60).
- **Navigation:** Implement a `ViewManager` or similar logic in the MainController to switch FXMLs in the Content Area.
- **Code Quality:** Follow Java naming conventions. Add Javadoc for public methods (NFR-06).
- **Error Handling:** Show a simple `Alert` if a mock action fails (NFR-05).

# Output Needed
Please generate the code for:
1. `Device.java` (Model: id, name, type, state, room)
2. `MockSmartHomeService.java` (Singleton with dummy data)
3. `MainController.java` (Navigation logic)
4. `dashboard-view.fxml` & `dashboard-controller.java`
5. `styles.css` (Basic styling for cards and buttons)

# Note
Keep the code modular. I will extend the Automation and Energy views in subsequent steps. Focus on making the Device Controls interactive in the Dashboard first.