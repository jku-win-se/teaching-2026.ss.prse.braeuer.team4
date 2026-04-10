package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.service.MockUserService;
import at.jku.se.smarthome.service.MockUserService.LoginStatus;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
    private Button loginButton;
    
    @FXML
    private Label errorLabel;
    
    private final MockUserService userService = MockUserService.getInstance();
    private LoginCallback loginCallback;
    
    /**
     * Callback interface for login and registration events.
     */
    public interface LoginCallback {
        void onLoginSuccess();
        void onRegisterClick();
    }
    
    /**
     * Sets the callback to be invoked on login or register.
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

        setLoginFormDisabled(true);

        Task<LoginStatus> loginTask = new Task<>() {
            @Override
            protected LoginStatus call() {
                return userService.authenticate(email, password);
            }
        };

        loginTask.setOnSucceeded(event -> {
            setLoginFormDisabled(false);
            handleLoginResult(email, loginTask.getValue());
        });

        loginTask.setOnFailed(event -> {
            setLoginFormDisabled(false);
            showError("Authentication temporarily unavailable. Please try again.");
        });

        Thread loginThread = new Thread(loginTask, "smarthome-login-task");
        loginThread.setDaemon(true);
        loginThread.start();
    }
    
    /**
     * Handles register link click.
     */
    @FXML
    private void handleRegisterClick() {
        if (loginCallback != null) {
            loginCallback.onRegisterClick();
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

    private void handleLoginResult(String email, LoginStatus status) {
        switch (status) {
            case SUCCESS -> {
                errorLabel.setText("");
                if (loginCallback != null) {
                    loginCallback.onLoginSuccess();
                }
            }
            case THROTTLED -> showError("Too many login attempts. Please wait "
                    + userService.getRemainingThrottleSeconds(email)
                    + " seconds and try again.");
            case DATABASE_NOT_CONFIGURED -> showError("Authentication database is not configured. Check the local database settings.");
            case DATABASE_ERROR -> showError("Authentication temporarily unavailable. Please try again.");
            case INVALID_INPUT -> showError("Email and password cannot be empty");
            case ACCOUNT_INACTIVE, AUTHENTICATION_FAILED -> showError("Authentication failed. Please try again.");
            default -> showError("Authentication temporarily unavailable. Please try again.");
        }
    }

    private void setLoginFormDisabled(boolean disabled) {
        emailField.setDisable(disabled);
        passwordField.setDisable(disabled);
        if (loginButton != null) {
            loginButton.setDisable(disabled);
        }
    }
}
