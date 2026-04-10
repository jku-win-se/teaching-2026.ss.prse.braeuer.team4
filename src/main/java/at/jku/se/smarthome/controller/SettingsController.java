package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.service.mock.MockUserService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

/**
 * Controller for the settings view.
 */
public class SettingsController {
    
    @FXML
    private Label emailLabel;
    
    @FXML
    private PasswordField newPasswordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    private final MockUserService userService = MockUserService.getInstance();
    
    @FXML
    private void initialize() {
        String currentEmail = userService.getCurrentUserEmail();
        emailLabel.setText(currentEmail != null ? currentEmail : "user@example.com");
    }
    
    @FXML
    private void handleUpdateProfile() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (!newPassword.isEmpty()) {
            if (!newPassword.equals(confirmPassword)) {
                showError("Passwords do not match");
                return;
            }
            
            showSuccess("Profile updated successfully");
            newPasswordField.clear();
            confirmPasswordField.clear();
        } else {
            showSuccess("No changes made");
        }
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
