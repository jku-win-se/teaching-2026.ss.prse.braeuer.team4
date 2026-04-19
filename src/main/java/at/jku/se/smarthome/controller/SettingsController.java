package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.api.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

/**
 * Controller for the settings view.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.UnusedPrivateMethod"})
public class SettingsController {

    
    /** Label displaying current user email address. */
    @FXML
    private Label emailLabel;
    
    /** Password field for entering new password. */
    @FXML
    private PasswordField newPasswordField;
    
    /** Password field for confirming new password. */
    @FXML
    private PasswordField confirmPasswordField;
    
    /** User service for profile management. */
    private final UserService userService = ServiceRegistry.getUserService();
    
    @FXML
    private void initialize() {
        String currentEmail = userService.getCurrentUserEmail();
        emailLabel.setText(currentEmail != null ? currentEmail : "user@example.com");
    }
    
    @FXML
    @SuppressWarnings("PMD.OnlyOneReturn")
    private void handleUpdateProfile() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (newPassword.isEmpty()) {
            showSuccess("No changes made");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }
        
        showSuccess("Profile updated successfully");
        newPasswordField.clear();
        confirmPasswordField.clear();
    }
    
    @FXML
    private void handleExportData() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Data");
        alert.setHeaderText("Export Successful");
        alert.setContentText("Your data has been exported as JSON file.");
        alert.showAndWait();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
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
}
