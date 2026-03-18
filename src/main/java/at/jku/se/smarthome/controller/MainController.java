package at.jku.se.smarthome.controller;

import java.io.IOException;
import java.util.Objects;

import at.jku.se.smarthome.service.MockSmartHomeService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Main controller for the application shell.
 * 
 * Handles navigation between different views (Dashboard, Rooms, Automation, Energy, Settings)
 * and manages the user session (login/logout).
 */
public class MainController {
    
    @FXML
    private Label userLabel;
    
    @FXML
    private Button logoutButton;
    
    @FXML
    private StackPane contentArea;
    
    @FXML
    private Button dashboardBtn;
    
    @FXML
    private Button roomsBtn;
    
    @FXML
    private Button automationBtn;
    
    @FXML
    private Button energyBtn;
    
    @FXML
    private Button settingsBtn;
    
    private final MockSmartHomeService service = MockSmartHomeService.getInstance();
    private MainCallback mainCallback;
    
    /**
     * Callback interface for main controller events.
     */
    public interface MainCallback {
        void onLogout();
    }
    
    /**
     * Sets the callback to be invoked on logout.
     * 
     * @param callback the callback function
     */
    public void setMainCallback(MainCallback callback) {
        this.mainCallback = callback;
    }
    
    /**
     * Initializes the controller after FXML has been loaded.
     */
    @FXML
    private void initialize() {
        updateUserLabel();
        showDashboard();
    }
    
    /**
     * Updates the user label with the current logged-in user.
     */
    private void updateUserLabel() {
        userLabel.setText("Welcome, " + service.getCurrentUser());
    }
    
    /**
     * Shows the Dashboard view.
     */
    @FXML
    private void showDashboard() {
        loadView("dashboard-view.fxml");
    }
    
    /**
     * Shows the Rooms view.
     */
    @FXML
    private void showRooms() {
        loadView("rooms-view.fxml");
    }
    
    /**
     * Shows the Automation view.
     */
    @FXML
    private void showAutomation() {
        loadView("automation-view.fxml");
    }
    
    /**
     * Shows the Energy view.
     */
    @FXML
    private void showEnergy() {
        loadView("energy-view.fxml");
    }
    
    /**
     * Shows the Settings view.
     */
    @FXML
    private void showSettings() {
        loadView("settings-view.fxml");
    }
    
    /**
     * Handles the logout button action.
     * Clears the session and returns to login screen.
     */
    @FXML
    private void handleLogout() {
        service.logout();
        if (mainCallback != null) {
            mainCallback.onLogout();
        }
    }
    
    /**
     * Loads a view FXML file and displays it in the content area.
     * 
     * @param fxmlFileName the filename of the FXML file to load
     */
    private void loadView(String fxmlFileName) {
        try {
            String resourcePath = "/at/jku/se/smarthome/view/" + fxmlFileName;
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource(resourcePath))
            );
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            showError("Failed to load view: " + fxmlFileName, e);
        } catch (NullPointerException e) {
            showError("View resource not found: " + fxmlFileName, e);
        }
    }
    
    /**
     * Shows an error alert to the user.
     * 
     * @param message the error message
     * @param exception the exception that caused the error
     */
    private void showError(String message, Exception exception) {
        System.err.println(message);
        exception.printStackTrace();
        
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
