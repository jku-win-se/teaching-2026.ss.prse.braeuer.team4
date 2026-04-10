package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.service.mock.MockRoomService;
import at.jku.se.smarthome.service.mock.MockUserService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

/**
 * Controller for the rooms view.
 */
public class RoomsController {
    
    @FXML
    private TableView<Room> roomsTable;
    
    @FXML
    private TableColumn<Room, String> nameColumn;
    
    @FXML
    private TableColumn<Room, Integer> deviceCountColumn;
    
    @FXML
    private TableView<Device> devicesTable;
    
    @FXML
    private TableColumn<Device, String> deviceNameColumn;
    
    @FXML
    private TableColumn<Device, String> deviceTypeColumn;

    @FXML
    private Button addRoomBtn;
    
    @FXML
    private Button addDeviceBtn;
    
    @FXML
    private Label selectedRoomLabel;
    
    private final MockRoomService roomService = MockRoomService.getInstance();
    private final MockUserService userService = MockUserService.getInstance();
    private Room selectedRoom;
    
    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        deviceCountColumn.setCellValueFactory(new PropertyValueFactory<>("deviceCount"));
        
        // Create and configure the Actions column for rooms
        TableColumn<Room, Void> roomActionsColumn = new TableColumn<>("Actions");
        roomActionsColumn.setPrefWidth(200);
        roomActionsColumn.setCellFactory(param -> new RoomActionButtonCell());
        roomsTable.getColumns().add(roomActionsColumn);
        
        // Setup device table columns
        deviceNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        deviceTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        
        // Create and configure the Actions column for devices
        TableColumn<Device, Void> deviceActionsColumn = new TableColumn<>("Actions");
        deviceActionsColumn.setPrefWidth(200);
        deviceActionsColumn.setCellFactory(param -> new DeviceActionButtonCell());
        devicesTable.getColumns().add(deviceActionsColumn);
        
        // Set room table selection listener
        roomsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedRoom = newVal;
                selectedRoomLabel.setText("Selected Room: " + newVal.getName());
                devicesTable.setItems(newVal.getDevices());
                addDeviceBtn.setDisable(false);
            } else {
                selectedRoom = null;
                selectedRoomLabel.setText("No room selected");
                devicesTable.setItems(null);
                addDeviceBtn.setDisable(true);
            }
        });
        
        roomsTable.setItems(roomService.getRooms());
        addDeviceBtn.setDisable(true);

        if (!userService.canManageSystem()) {
            addRoomBtn.setDisable(true);
            addDeviceBtn.setDisable(true);
        }
    }
    
    @FXML
    private void handleAddRoom() {
        if (!userService.canManageSystem()) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Room");
        dialog.setHeaderText("Create a new room");
        dialog.setContentText("Room name:");
        
        dialog.showAndWait().ifPresent(roomName -> {
            if (!roomName.isEmpty()) {
                roomService.addRoom(roomName);
            }
        });
    }
    
    @FXML
    private void handleAddDevice() {
        if (!userService.canManageSystem() || selectedRoom == null) return;
        
        // First dialog for device name
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Add Device");
        nameDialog.setHeaderText("Enter device name for " + selectedRoom.getName());
        nameDialog.setContentText("Device name:");
        
        nameDialog.showAndWait().ifPresent(deviceName -> {
            if (!deviceName.isEmpty()) {
                // Second dialog for device type using ChoiceDialog
                javafx.scene.control.ChoiceDialog<String> typeDialog = 
                    new javafx.scene.control.ChoiceDialog<>("Switch", 
                        "Switch", "Dimmer", "Thermostat", "Sensor", "Cover/Blind");
                typeDialog.setTitle("Select Device Type");
                typeDialog.setHeaderText("Choose device type");
                typeDialog.setContentText("Device type:");
                
                typeDialog.showAndWait().ifPresent(deviceType -> {
                    roomService.addDeviceToRoom(selectedRoom.getId(), deviceName, deviceType);
                    devicesTable.refresh();
                });
            }
        });
    }
    
    /**
     * Inner class for room action buttons.
     */
    private class RoomActionButtonCell extends TableCell<Room, Void> {
        private final Button editBtn = new Button("Rename");
        private final Button deleteBtn = new Button("Delete");
        private final HBox container = new HBox(10);
        
        public RoomActionButtonCell() {
            editBtn.setStyle("-fx-padding: 5; -fx-font-size: 11;");
            editBtn.setOnAction(e -> handleEditRoom(getTableView().getItems().get(getIndex())));
            
            deleteBtn.setStyle("-fx-padding: 5; -fx-font-size: 11; -fx-text-fill: #e74c3c;");
            deleteBtn.setOnAction(e -> handleDeleteRoom(getTableView().getItems().get(getIndex())));
            
            container.setAlignment(Pos.CENTER_LEFT);
            container.getChildren().addAll(editBtn, deleteBtn);
        }
        
        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                boolean canManage = userService.canManageSystem();
                editBtn.setDisable(!canManage);
                deleteBtn.setDisable(!canManage);
                setGraphic(container);
            }
        }
    }
    
    /**
     * Inner class for device action buttons.
     */
    private class DeviceActionButtonCell extends TableCell<Device, Void> {
        private final Button editBtn = new Button("Rename");
        private final Button deleteBtn = new Button("Delete");
        private final HBox container = new HBox(10);
        
        public DeviceActionButtonCell() {
            editBtn.setStyle("-fx-padding: 5; -fx-font-size: 11;");
            editBtn.setOnAction(e -> {
                int index = getIndex();
                if (index >= 0 && selectedRoom != null) {
                    Device device = selectedRoom.getDevices().get(index);
                    handleRenameDevice(device);
                }
            });
            
            deleteBtn.setStyle("-fx-padding: 5; -fx-font-size: 11; -fx-text-fill: #e74c3c;");
            deleteBtn.setOnAction(e -> {
                int index = getIndex();
                if (index >= 0 && selectedRoom != null) {
                    Device device = selectedRoom.getDevices().get(index);
                    handleDeleteDevice(device);
                }
            });
            
            container.setAlignment(Pos.CENTER_LEFT);
            container.getChildren().addAll(editBtn, deleteBtn);
        }
        
        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                boolean canManage = userService.canManageSystem();
                editBtn.setDisable(!canManage);
                deleteBtn.setDisable(!canManage);
                setGraphic(container);
            }
        }
    }
    
    /**
     * Handles editing a room name.
     */
    private void handleEditRoom(Room room) {
        if (!userService.canManageSystem()) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog(room.getName());
        dialog.setTitle("Edit Room");
        dialog.setHeaderText("Edit room name");
        dialog.setContentText("Room name:");
        
        dialog.showAndWait().ifPresent(newName -> {
            if (!newName.isEmpty() && !newName.equals(room.getName())) {
                roomService.updateRoomName(room.getId(), newName);
                roomsTable.refresh();
            }
        });
    }
    
    /**
     * Handles deleting a room.
     */
    private void handleDeleteRoom(Room room) {
        if (!userService.canManageSystem()) {
            return;
        }
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Room");
        confirmDialog.setHeaderText("Delete room: " + room.getName());
        confirmDialog.setContentText("Are you sure you want to delete this room? This action cannot be undone.");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                roomService.deleteRoom(room.getId());
            }
        });
    }
    
    /**
     * Handles renaming a device.
     */
    private void handleRenameDevice(Device device) {
        if (!userService.canManageSystem()) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog(device.getName());
        dialog.setTitle("Rename Device");
        dialog.setHeaderText("Edit device name");
        dialog.setContentText("Device name:");
        
        dialog.showAndWait().ifPresent(newName -> {
            if (!newName.isEmpty() && !newName.equals(device.getName())) {
                roomService.renameDevice(selectedRoom.getId(), device.getId(), newName);
                devicesTable.refresh();
            }
        });
    }
    
    /**
     * Handles deleting a device.
     */
    private void handleDeleteDevice(Device device) {
        if (!userService.canManageSystem()) {
            return;
        }
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Device");
        confirmDialog.setHeaderText("Delete device: " + device.getName());
        confirmDialog.setContentText("Are you sure you want to delete this device?");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                roomService.removeDeviceFromRoom(selectedRoom.getId(), device.getId());
            }
        });
    }
}
