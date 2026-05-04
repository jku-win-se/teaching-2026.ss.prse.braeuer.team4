package at.jku.se.smarthome.controller;

import java.io.IOException;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import at.jku.se.smarthome.model.NotificationEntry;
import at.jku.se.smarthome.model.NotificationType;
import at.jku.se.smarthome.service.api.NotificationService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.api.UserService;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;

/**
 * Main controller for the application shell.
 * 
 * Handles navigation between different views (Devices, Rooms, Rules, Energy, Settings)
 * and manages the user session (login/logout).
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods", "PMD.TooManyFields", "PMD.UnusedPrivateMethod", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public class MainController {


    /** Logger instance for application logging. */
    private static final Logger LOGGER = LogManager.getLogger(MainController.class);
    
    /** Label displaying current user information. */
    @FXML
    private Label userLabel;
    
    /** Button to logout the current user. */
    @FXML
    private Button logoutButton;
    
    /** Stack pane container for dynamic content area. */
    @FXML
    private StackPane contentArea;

    /** VBox container for toast notifications. */
    @FXML
    private VBox toastContainer;
    
    /** Button to navigate to rooms view. */
    @FXML
    private Button roomsBtn;

    /** Button to navigate to devices view. */
    @FXML
    private Button devicesBtn;

    /** Button to navigate to schedules view. */
    @FXML
    private Button schedulesBtn;
    
    /** Button to navigate to automation/rules view. */
    @FXML
    private Button automationBtn;

    /** Button to navigate to scenes view. */
    @FXML
    private Button scenesBtn;
    
    /** Button to navigate to energy view. */
    @FXML
    private Button energyBtn;

    /** Button to navigate to activity log view. */
    @FXML
    private Button activityLogBtn;

    /** Button to navigate to users management view. */
    @FXML
    private Button usersBtn;

    /** Button to navigate to vacation mode view. */
    @FXML
    private Button vacationModeBtn;

    /** Button to navigate to simulation view. */
    @FXML
    private Button simulationBtn;

    /** Button to navigate to IoT settings view. */
    @FXML
    private Button iotSettingsBtn;
    
    /** Button to navigate to settings view. */
    @FXML
    private Button settingsBtn;

    /** Button for notification bell in header. */
    @FXML
    private Button bellButton;

    /** Badge overlay for unread notification count. */
    @FXML
    private Label unreadBadge;
    
    /** Notification service instance. */
    private final NotificationService notificationService = ServiceRegistry.getNotificationService();
    /** User service instance. */
    private final UserService userService = ServiceRegistry.getUserService();
    /** Callback for main controller events. */
    private MainCallback mainCallback;
    /** Count of observed notifications for tracking new ones. */
    private int observedNotificationCount;
    
    /**
     * Callback interface for main controller events.
     */
    public interface MainCallback {
        /** Invoked when user initiates logout. */
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
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void initialize() {
        initializeToastNotifications();
        initializeBellBadge();
        refreshSessionState();
    }

    /**
     * Refreshes the session state including user label and role-based access control.
     */
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
     * Binds the unread badge to the notification service's unread count.
     */
    private void initializeBellBadge() {
        if (unreadBadge == null) {
            return;
        }
        unreadBadge.textProperty().bind(Bindings.convert(notificationService.unreadCountProperty()));
        unreadBadge.visibleProperty().bind(Bindings.greaterThan(notificationService.unreadCountProperty(), 0));
    }

    /**
     * Handles clicks on the notification bell.
     */
    @FXML
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void handleBellClick() {
        Popup popup = buildNotificationPopup();
        Bounds bounds = bellButton.localToScreen(bellButton.getBoundsInLocal());
        // Show popup below the bell button
        popup.show(bellButton.getScene().getWindow(),
                bounds.getMinX() - 280, // Offset to align right side roughly
                bounds.getMaxY() + 5);
    }

    /**
     * Constructs the notification list popup.
     *
     * @return the configured popup
     */
    private Popup buildNotificationPopup() {
        VBox container = new VBox(10);
        container.getStyleClass().add("notification-popup");
        container.setStyle("-fx-background-color: white; -fx-padding: 10; "
                + "-fx-border-color: #bdc3c7; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0); "
                + "-fx-min-width: 320; -fx-max-width: 320;");

        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label("Notifications");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Button markAllBtn = new Button("Mark all read");
        markAllBtn.setStyle("-fx-font-size: 11;");
        markAllBtn.setOnAction(e -> notificationService.markAllAsRead());
        header.getChildren().addAll(title, spacer, markAllBtn);

        ListView<NotificationEntry> listView = new ListView<>(notificationService.getNotifications());
        listView.setPrefHeight(350);
        listView.setCellFactory(lv -> new NotificationCell(notificationService));
        listView.setStyle("-fx-background-insets: 0; -fx-padding: 0;");

        container.getChildren().addAll(header, listView);

        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(container);
        return popup;
    }

    /**
     * Custom ListCell for displaying NotificationEntry in the popup list.
     */
    private static class NotificationCell extends ListCell<NotificationEntry> {
        /** The notification service used to mark entries as read. */
        private final NotificationService service;

        /**
         * Creates a new notification cell.
         *
         * @param service notification service
         */
        /* default */ NotificationCell(NotificationService service) {
            super();
            this.service = service;
        }

        @Override
        protected void updateItem(NotificationEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty || entry == null) {
                setGraphic(null);
                setText(null);
            } else {
                renderCell(entry);
            }
        }

        /**
         * Renders the cell content for a valid notification entry.
         *
         * @param entry the notification entry to render
         */
        private void renderCell(NotificationEntry entry) {
            HBox root = new HBox(10);
            root.setAlignment(javafx.geometry.Pos.TOP_LEFT);
            root.setPadding(new javafx.geometry.Insets(5));

            Label typeIcon = new Label(getTypeIcon(entry.getType()));
            typeIcon.setStyle("-fx-font-weight: bold; -fx-min-width: 20;");

            VBox content = new VBox(2);
            Label msg = new Label(entry.getMessage());
            msg.setWrapText(true);
            msg.setMaxWidth(220);
            msg.setStyle(entry.isRead() ? "-fx-text-fill: #7f8c8d;" : "-fx-font-weight: bold;");

            Label time = new Label(entry.getTimestamp());
            time.setStyle("-fx-font-size: 10; -fx-text-fill: #95a5a6;");
            content.getChildren().addAll(msg, time);

            root.getChildren().addAll(typeIcon, content);

            if (entry.isRead()) {
                root.setStyle("-fx-background-color: transparent;");
            } else {
                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                Button readBtn = new Button("✓");
                readBtn.setStyle("-fx-font-size: 10; -fx-padding: 2 5;");
                readBtn.setOnAction(e -> service.markAsRead(entry));
                root.getChildren().addAll(spacer, readBtn);
                root.setStyle("-fx-background-color: #f0f7fd;");
            }

            setGraphic(root);
        }

        private String getTypeIcon(NotificationType type) {
            return switch (type) {
                case SUCCESS -> "✅";
                case FAILURE -> "❌";
                case WARNING -> "⚠️";
                case INFO -> "ℹ️";
            };
        }
    }
    
    /**
     * Updates the user label with the current logged-in user.
     */
    private void updateUserLabel() {
        String email = userService.getCurrentUserEmail();
        String displayName = email != null ? email : "Guest";
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
        boolean hasAccess = userService.canManageSystem();
        if (!hasAccess) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Access Restricted");
            alert.setHeaderText("Owner access required");
            alert.setContentText(featureName + " is only available to the Owner role. Members can control devices but cannot manage the system.");
            alert.showAndWait();
        }
        return hasAccess;
    }
    
    /**
     * Shows the Rooms view.
     */
    @FXML
    @SuppressWarnings("PMD.UnusedPrivateMethod")
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
    @SuppressWarnings("PMD.UnusedPrivateMethod")
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
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void showEnergy() {
        loadView("energy-view.fxml");
    }
    
    /**
     * Shows the Settings view.
     */
    @FXML
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void showSettings() {
        loadView("settings-view.fxml");
    }
    
    /**
     * Shows the Activity Log view.
     */
    @FXML
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void showActivityLog() {
        loadView("activity-log-view.fxml");
    }
    
    /**
     * Shows the Schedules view.
     */
    @FXML
    @SuppressWarnings("PMD.UnusedPrivateMethod")
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
    @SuppressWarnings("PMD.UnusedPrivateMethod")
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
    @SuppressWarnings("PMD.UnusedPrivateMethod")
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
    @SuppressWarnings("PMD.UnusedPrivateMethod")
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
    @SuppressWarnings("PMD.UnusedPrivateMethod")
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
    @SuppressWarnings("PMD.UnusedPrivateMethod")
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
            URL resource = getClass().getResource(resourcePath);
            if (resource == null) {
                showError("View resource not found: " + fxmlFileName, new IllegalArgumentException(resourcePath));
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            showError("Failed to load view: " + fxmlFileName, e);
        }
    }
    
    /**
     * Shows an error alert to the user.
     * 
     * @param message the error message
     * @param exception the exception that caused the error
     */
    private void showError(String message, Exception exception) {
        LOGGER.error(message, exception);
        
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
        boolean isScene = message.contains("scene '") || message.contains("scene \"");
        return isScene
            ? entry.getType() == NotificationType.FAILURE ? "Scene Failed" : "Scene Executed"
            : switch (entry.getType()) {
                case SUCCESS -> "Rule Executed";
                case FAILURE -> "Rule Failed";
                default -> "Notification";
            };
    }
}
