package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.service.mock.MockUserService;
import at.jku.se.smarthome.service.mock.MockUserService.RegistrationStatus;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for the register view.
 */
public class RegisterController {
    
    @FXML
    private TextField emailField;
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private Label errorLabel;
    
    private final MockUserService userService = MockUserService.getInstance();
    private RegisterCallback registerCallback;
    
    public interface RegisterCallback {
        void onBackToLogin();
    }
    
    public void setRegisterCallback(RegisterCallback callback) {
        this.registerCallback = callback;
    }
    
    @FXML
    private void handleRegister() {
        String email = emailField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showError("All fields are required");
            return;
        }
        
        if (!email.contains("@")) {
            showError("Please enter a valid email");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        RegistrationStatus registrationStatus = userService.registerUser(email, username, password, confirmPassword);
        if (registrationStatus == RegistrationStatus.SUCCESS) {
            errorLabel.setText("");
            showSuccess("Registration successful! Redirecting to login...");
            if (registerCallback != null) {
                registerCallback.onBackToLogin();
            }
        } else {
            showError(mapErrorMessage(registrationStatus));
        }
    }
    
    @FXML
    private void handleBackToLogin() {
        if (registerCallback != null) {
            registerCallback.onBackToLogin();
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Registration Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String mapErrorMessage(RegistrationStatus status) {
        return switch (status) {
            case INVALID_INPUT -> "All fields are required";
            case PASSWORD_MISMATCH -> "Passwords do not match";
            case DUPLICATE_EMAIL -> "Registration failed. Email may already exist.";
            case DATABASE_NOT_CONFIGURED -> "Registration database is not configured. Add a local .env file or SMARTHOME_DB_* environment variables.";
            case DATABASE_ERROR -> "Registration failed because the database could not be reached or initialized.";
            case SUCCESS -> "";
        };
    }
}
