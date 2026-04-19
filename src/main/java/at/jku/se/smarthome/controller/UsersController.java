package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.User;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.api.UserService;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

/**
 * Controller for the user management view.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.UnusedPrivateMethod"})
public class UsersController {

    
    /** Table view for displaying all users. */
    @FXML
    private TableView<User> usersTable;
    
    /** Column displaying user email. */
    @FXML
    private TableColumn<User, String> emailColumn;
    
    /** Column displaying user role. */
    @FXML
    private TableColumn<User, String> roleColumn;
    
    /** Column displaying user account status. */
    @FXML
    private TableColumn<User, String> statusColumn;

    /** Column displaying action buttons. */
    @FXML
    private TableColumn<User, Void> actionsColumn;

    /** Button to invite new users. */
    @FXML
    private Button inviteBtn;

    /** ComboBox for filtering users by status. */
    @FXML
    private ComboBox<String> statusFilterCombo;
    
    /** User service for user management. */
    private final UserService userService = ServiceRegistry.getUserService();
    /** Filtered view of users for applying status filters. */
    private FilteredList<User> filteredUsers;
    
    @FXML
    private void initialize() {
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        actionsColumn.setCellFactory(column -> new UserActionCell());

        filteredUsers = new FilteredList<>(userService.getUsers(), user -> true);
        usersTable.setItems(filteredUsers);

        statusFilterCombo.getItems().addAll("All Statuses", "Active", "Pending", "Revoked", "Inactive");
        statusFilterCombo.setValue("All Statuses");
        statusFilterCombo.setOnAction(event -> applyStatusFilter());

        inviteBtn.setDisable(!userService.isOwner());
    }
    
    @FXML
    private void handleInviteMember() {
        if (!userService.isOwner()) {
            return;
        }
        TextInputDialog emailDialog = new TextInputDialog();
        emailDialog.setTitle("Invite Member");
        emailDialog.setHeaderText("Send an invitation");
        emailDialog.setContentText("Email address:");
        
        emailDialog.showAndWait().ifPresent(email -> {
            if (!email.contains("@")) {
                showAlert(Alert.AlertType.WARNING, "Invalid E-mail", "Please enter a valid member e-mail address.");
                return;
            }

            boolean invited = userService.inviteUser(email.trim(), "Member");
            if (invited) {
                showAlert(Alert.AlertType.INFORMATION, "Invitation Sent", "The member invitation was added to the mock user list.");
            } else {
                showAlert(Alert.AlertType.WARNING, "Invitation Failed", "A user with this e-mail already exists.");
            }
        });
    }

    private void handleRevokeAccess(User user) {
        if (!userService.isOwner()) {
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Revoke Access");
        confirmation.setHeaderText("Revoke access for " + user.getEmail());
        confirmation.setContentText("This member will no longer be able to access the system.");

        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                boolean revoked = userService.revokeUser(user.getEmail());
                if (revoked) {
                    applyStatusFilter();
                    usersTable.refresh();
                    showAlert(Alert.AlertType.INFORMATION, "Access Revoked", "Member access has been revoked in the mock system.");
                } else {
                    showAlert(Alert.AlertType.WARNING, "Revoke Failed", "Owner accounts cannot be revoked.");
                }
            }
        });
    }

    private void handleRestoreAccess(User user) {
        if (!userService.isOwner()) {
            return;
        }

        boolean restored = userService.restoreUser(user.getEmail());
        if (restored) {
            applyStatusFilter();
            usersTable.refresh();
            showAlert(Alert.AlertType.INFORMATION, "Access Restored", "Member access has been restored in the mock system.");
        } else {
            showAlert(Alert.AlertType.WARNING, "Restore Failed", "This account cannot be restored.");
        }
    }

    private void applyStatusFilter() {
        String selectedStatus = statusFilterCombo.getValue();
        filteredUsers.setPredicate(user -> selectedStatus == null
                || "All Statuses".equals(selectedStatus)
                || selectedStatus.equalsIgnoreCase(user.getStatus()));
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Inner class rendering action buttons for users table. */
    private final class UserActionCell extends TableCell<User, Void> {
        /** Button for revoking user access. */
        private final Button revokeButton = new Button("Revoke Access");
        /** Button for restoring user access. */
        private final Button restoreButton = new Button("Restore Access");
        /** Container for action buttons. */
        private final HBox container = new HBox(8, revokeButton, restoreButton);

        private UserActionCell() {
            super();
            container.setAlignment(Pos.CENTER_LEFT);
            revokeButton.setStyle("-fx-text-fill: #e74c3c;");
            restoreButton.setStyle("-fx-text-fill: #27ae60;");
            revokeButton.setOnAction(event -> handleRevokeAccess(getTableView().getItems().get(getIndex())));
            restoreButton.setOnAction(event -> handleRestoreAccess(getTableView().getItems().get(getIndex())));
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
                return;
            }

            User user = getTableView().getItems().get(getIndex());
            boolean isOwnerAccount = "Owner".equalsIgnoreCase(user.getRole());
            boolean alreadyRevoked = "Revoked".equalsIgnoreCase(user.getStatus());
            boolean isPending = "Pending".equalsIgnoreCase(user.getStatus());
            boolean isCurrentUser = user.getEmail().equals(userService.getCurrentUserEmail());

            revokeButton.setDisable(!userService.isOwner() || isOwnerAccount || alreadyRevoked || isCurrentUser);
            revokeButton.setText(isPending ? "Cancel Invite" : "Revoke Access");
            restoreButton.setDisable(!userService.isOwner() || isOwnerAccount || (!alreadyRevoked && !isPending));
            setGraphic(container);
        }
    }
}
