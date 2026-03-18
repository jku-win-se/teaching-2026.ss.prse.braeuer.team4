package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.service.MockSmartHomeService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for the login view.
 * 
 * Handles user authentication using the mock service.
 */
public class LoginController {
    
    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Label errorLabel;
    
    private final MockSmartHomeService service = MockSmartHomeService.getInstance();
    private LoginCallback loginCallback;
    
    /**
     * Callback interface for login success.
     */
    public interface LoginCallback {
        void onLoginSuccess();
    }
    
    /**
     * Sets the callback to be invoked on successful login.
     * 
     * @param callback the callback function
     */
    public void setLoginCallback(LoginCallback callback) {
        this.loginCallback = callback;
    }
    
    /**
     * Handles the login button action.
     * Validates credentials and attempts to authenticate.
     */
    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();
        
        if (email.isEmpty() || password.isEmpty()) {
            showError("Email and password cannot be empty");
            return;
        }
        
        if (service.authenticate(email, password)) {
            errorLabel.setText("");
            if (loginCallback != null) {
                loginCallback.onLoginSuccess();
            }
        } else {
            showError("Authentication failed. Please try again.");
        }
    }
    
    /**
     * Shows an error message to the user.
     * 
     * @param message the error message
     */
    private void showError(String message) {
        errorLabel.setText(message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Login Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Clears the form fields.
     */
    public void clearFields() {
        emailField.clear();
        passwordField.clear();
        errorLabel.setText("");
    }
}
