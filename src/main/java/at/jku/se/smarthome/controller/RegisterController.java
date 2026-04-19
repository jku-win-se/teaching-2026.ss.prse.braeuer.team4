package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.api.UserService;
import at.jku.se.smarthome.service.api.UserService.RegistrationStatus;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for the register view.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.UnusedPrivateMethod"})
public class RegisterController {

    
    /** Text field for user email input. */
    @FXML
    private TextField emailField;
    
    /** Text field for username input. */
    @FXML
    private TextField usernameField;
    
    /** Password field for password input. */
    @FXML
    private PasswordField passwordField;
    
    /** Password field for password confirmation. */
    @FXML
    private PasswordField confirmPasswordField;
    
    /** Label for displaying error messages. */
    @FXML
    private Label errorLabel;
    
    /** User service for registration operations. */
    private final UserService userService = ServiceRegistry.getUserService();
    /** Callback for navigation to login view. */
    private RegisterCallback registerCallback;
    
    /**
     * Callback interface for navigation from register view.
     */
    public interface RegisterCallback {
        /** Invoked when user clicks button to return to login view. */
        void onBackToLogin();
    }
    
    public void setRegisterCallback(RegisterCallback callback) {
        this.registerCallback = callback;
    }
    
    @SuppressWarnings("PMD.ConfusingTernary")
    @FXML
    private void handleRegister() {
        String email = emailField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String validationError = null;

        if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            validationError = "All fields are required";
        } else if (!email.contains("@")) {
            validationError = "Please enter a valid email";
        } else if (!password.equals(confirmPassword)) {
            validationError = "Passwords do not match";
        }

        if (validationError != null) {
            showError(validationError);
        } else {
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
