package at.jku.se.smarthome.controller;

import java.io.IOException;
import java.util.Objects;

import at.jku.se.smarthome.model.NotificationEntry;
import at.jku.se.smarthome.service.MockNotificationService;
import at.jku.se.smarthome.service.MockSmartHomeService;
import at.jku.se.smarthome.service.MockUserService;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Main controller for the application shell.
 * 
 * Handles navigation between different views (Devices, Rooms, Rules, Energy, Settings)
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
    private VBox toastContainer;
    
    @FXML
    private Button roomsBtn;

    @FXML
    private Button devicesBtn;

    @FXML
    private Button schedulesBtn;
    
    @FXML
    private Button automationBtn;

    @FXML
    private Button scenesBtn;
    
    @FXML
    private Button energyBtn;

    @FXML
    private Button activityLogBtn;

    @FXML
    private Button usersBtn;

    @FXML
    private Button vacationModeBtn;

    @FXML
    private Button simulationBtn;

    @FXML
    private Button iotSettingsBtn;
    
    @FXML
    private Button settingsBtn;
    
    private final MockSmartHomeService service = MockSmartHomeService.getInstance();
    private final MockNotificationService notificationService = MockNotificationService.getInstance();
    private final MockUserService userService = MockUserService.getInstance();
    private MainCallback mainCallback;
    private int observedNotificationCount;
    
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
        initializeToastNotifications();
        refreshSessionState();
    }

    public void refreshSessionState() {
        updateUserLabel();
        applyRoleAccess();
        if (contentArea.getChildren().isEmpty()) {
            showDevices();
        }
    }

    private void initializeToastNotifications() {
        observedNotificationCount = notificationService.getNotifications().size();
        notificationService.getNotifications().addListener((javafx.collections.ListChangeListener<NotificationEntry>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (NotificationEntry entry : change.getAddedSubList()) {
                        if (observedNotificationCount > 0) {
                            showToast(entry);
                        }
                    }
                    observedNotificationCount += change.getAddedSize();
                }
                if (change.wasRemoved()) {
                    observedNotificationCount = Math.max(0, observedNotificationCount - change.getRemovedSize());
                }
            }
        });
    }
    
    /**
     * Updates the user label with the current logged-in user.
     */
    private void updateUserLabel() {
        String email = userService.getCurrentUserEmail();
        String displayName = email != null ? email : service.getCurrentUser();
        userLabel.setText("Welcome, " + displayName + " (" + userService.getCurrentUserRole() + ")");
    }

    private void applyRoleAccess() {
        boolean owner = userService.isOwner();
        setOwnerOnly(roomsBtn, owner);
        setOwnerOnly(schedulesBtn, owner);
        setOwnerOnly(automationBtn, owner);
        setOwnerOnly(scenesBtn, owner);
        setOwnerOnly(usersBtn, owner);
        setOwnerOnly(vacationModeBtn, owner);
        setOwnerOnly(simulationBtn, owner);
        setOwnerOnly(iotSettingsBtn, owner);
    }

    private void setOwnerOnly(Button button, boolean owner) {
        if (button == null) {
            return;
        }
        button.setManaged(owner);
        button.setVisible(owner);
    }

    private boolean requireOwnerAccess(String featureName) {
        if (userService.canManageSystem()) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Access Restricted");
        alert.setHeaderText("Owner access required");
        alert.setContentText(featureName + " is only available to the Owner role. Members can control devices but cannot manage the system.");
        alert.showAndWait();
        return false;
    }
    
    /**
     * Shows the Rooms view.
     */
    @FXML
    private void showRooms() {
        if (!requireOwnerAccess("Room management")) {
            return;
        }
        loadView("rooms-view.fxml");
    }
    
    /**
     * Shows the Device Control view.
     */
    @FXML
    private void showDevices() {
        loadView("devices-control-view.fxml");
    }
    
    /**
     * Shows the Rules view.
     */
    @FXML
    private void showAutomation() {
        if (!requireOwnerAccess("Rules")) {
            return;
        }
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
     * Shows the Activity Log view.
     */
    @FXML
    private void showActivityLog() {
        loadView("activity-log-view.fxml");
    }
    
    /**
     * Shows the Schedules view.
     */
    @FXML
    private void showSchedules() {
        if (!requireOwnerAccess("Schedules")) {
            return;
        }
        loadView("schedules-view.fxml");
    }
    
    /**
     * Shows the Scenes view.
     */
    @FXML
    private void showScenes() {
        if (!requireOwnerAccess("Scenes")) {
            return;
        }
        loadView("scenes-view.fxml");
    }
    
    /**
     * Shows the Users view.
     */
    @FXML
    private void showUsers() {
        if (!requireOwnerAccess("User management")) {
            return;
        }
        loadView("users-view.fxml");
    }
    
    /**
     * Shows the Vacation Mode view.
     */
    @FXML
    private void showVacationMode() {
        if (!requireOwnerAccess("Vacation mode configuration")) {
            return;
        }
        loadView("vacation-mode-view.fxml");
    }
    
    /**
     * Shows the Simulation view.
     */
    @FXML
    private void showSimulation() {
        if (!requireOwnerAccess("Simulation tools")) {
            return;
        }
        loadView("simulation-view.fxml");
    }
    
    /**
     * Shows the IoT Settings view.
     */
    @FXML
    private void showIoTSettings() {
        if (!requireOwnerAccess("IoT integration settings")) {
            return;
        }
        loadView("iot-settings-view.fxml");
    }
    
    /**
     * Handles the logout button action.
     * Clears the session and returns to login screen.
     */
    @FXML
    private void handleLogout() {
        service.logout();
        userService.logout();
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

    private void showToast(NotificationEntry entry) {
        if (toastContainer == null) {
            return;
        }

        VBox toast = new VBox(4);
        toast.getStyleClass().add("toast");
        toast.getStyleClass().add("toast-" + entry.getType());
        toast.setMouseTransparent(true);
        toast.setOpacity(0);
        toast.setMaxWidth(320);

        Label titleLabel = new Label(resolveToastTitle(entry));
        titleLabel.getStyleClass().add("toast-title");

        Label messageLabel = new Label(entry.getMessage());
        messageLabel.getStyleClass().add("toast-message");
        messageLabel.setWrapText(true);

        Label timeLabel = new Label(entry.getTimestamp());
        timeLabel.getStyleClass().add("toast-time");

        toast.getChildren().addAll(titleLabel, messageLabel, timeLabel);
        toastContainer.getChildren().add(0, toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition hold = new PauseTransition(Duration.seconds(3));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> toastContainer.getChildren().remove(toast));

        fadeIn.play();
        hold.setOnFinished(event -> fadeOut.play());
        hold.play();
    }

    private String resolveToastTitle(NotificationEntry entry) {
        String message = entry.getMessage() == null ? "" : entry.getMessage().toLowerCase();

        if (message.contains("scene '") || message.contains("scene \"")) {
            return switch (entry.getType()) {
                case "error" -> "Scene Failed";
                default -> "Scene Executed";
            };
        }

        return switch (entry.getType()) {
            case "success" -> "Rule Executed";
            case "error" -> "Rule Failed";
            default -> "Notification";
        };
    }
}
