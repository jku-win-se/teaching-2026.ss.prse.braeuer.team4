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
public class RegisterController {
    
    /** Input field for account email. */
    @FXML
    private TextField emailField;
    
    /** Input field for display/user name. */
    @FXML
    private TextField usernameField;
    
    /** Input field for the chosen password. */
    @FXML
    private PasswordField passwordField;
    
    /** Input field for password confirmation. */
    @FXML
    private PasswordField confirmPasswordField;
    
    /** Inline error label shown above dialogs. */
    @FXML
    private Label errorLabel;

    /** Service used for registration. */
    private final UserService userService;
    /** Callback used to navigate back to login. */
    private RegisterCallback registerCallback;

    /**
     * Creates a register controller with the default user service.
     */
    public RegisterController() {
        this.userService = ServiceRegistry.getUserService();
    }

    /** Callback contract for leaving the register screen. */
    @FunctionalInterface
    public interface RegisterCallback {
        /**
         * Navigates back to the login view.
         */
        void onBackToLogin();
    }
    
    /**
     * Sets the callback used after successful registration or cancel action.
     *
     * @param callback callback implementation
     */
    public void setRegisterCallback(RegisterCallback callback) {
        this.registerCallback = callback;
    }
    
    /**
     * Validates form input and performs user registration.
     */
    @FXML
    public void handleRegister() {
        String email = emailField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        String validationError = validateInput(email, username, password, confirmPassword);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        RegistrationStatus registrationStatus = userService.registerUser(email, username, password, confirmPassword);
        processRegistrationStatus(registrationStatus);
    }

    private String validateInput(String email, String username, String password, String confirmPassword) {
        String validationError = null;
        if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            validationError = "All fields are required";
        }
        if (validationError == null && !email.contains("@")) {
            validationError = "Please enter a valid email";
        }
        if (validationError == null && !password.equals(confirmPassword)) {
            validationError = "Passwords do not match";
        }
        return validationError;
    }

    private void processRegistrationStatus(RegistrationStatus registrationStatus) {
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
    
    /**
     * Navigates back to the login screen.
     */
    @FXML
    public void handleBackToLogin() {
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
