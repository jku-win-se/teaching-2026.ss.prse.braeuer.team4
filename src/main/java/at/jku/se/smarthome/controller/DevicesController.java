package at.jku.se.smarthome.controller;

import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.service.api.LogService;
import at.jku.se.smarthome.service.api.UserService;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import at.jku.se.smarthome.service.api.RoomService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Controller for device control and state display.
 * Handles manual control of devices with type-specific UI.
 * Logs all state changes to activity log (FR-08).
 */
public class DevicesController {

    private static final String ALL_ROOMS_OPTION = "All Rooms";
    
    @FXML
    private VBox devicesContainer;
    
    @FXML
    private ComboBox<String> roomFilterCombo;
    
    @FXML
    private Button addDeviceBtn;
    
    private final RoomService roomService = ServiceRegistry.getRoomService();
    private final LogService logService = ServiceRegistry.getLogService();
    private final UserService userService = ServiceRegistry.getUserService();
    private final Map<Room, ChangeListener<String>> roomNameListeners = new IdentityHashMap<>();
    private final ListChangeListener<Room> roomListListener = change -> {
        while (change.next()) {
            if (change.wasRemoved()) {
                change.getRemoved().forEach(this::detachRoomNameListener);
            }
            if (change.wasAdded()) {
                change.getAddedSubList().forEach(this::attachRoomNameListener);
            }
        }
        refreshRoomFilterOptions();
        loadDevices();
    };
    private String selectedRoomFilterId = null;
    
    /**
     * Initializes the controller after FXML loading.
     * <p>
     * Populates the room filter, sets up UI handlers and loads the initial
     * device view. Disables the add button for users without management
     * permissions.
     */

    @FXML
    private void initialize() {
        configureAddDeviceVisibility();
        roomFilterCombo.setOnAction(e -> handleRoomFilterChange());
        observeRooms();
        refreshRoomFilterOptions();
        
        // Initial load of all devices
        loadDevices();
    }
    
    /**
     * Loads and displays devices based on filter.
     */
    void loadDevices() {
        devicesContainer.getChildren().clear();
        
        for (Room room : roomService.getRooms()) {
            // Check room filter
            if (selectedRoomFilterId != null && !room.getId().equals(selectedRoomFilterId)) {
                continue;
            }
            
            for (Device device : room.getDevices()) {
                VBox deviceCard = createDeviceCard(device, room);
                devicesContainer.getChildren().add(deviceCard);
            }
        }
    }
    
    /**
     * Creates a device control card based on device type.
     */
    VBox createDeviceCard(Device device, Room room) {
        VBox card = new VBox(10);
        card.setStyle("-fx-border-color: #bdc3c7; -fx-border-radius: 8; -fx-padding: 15; -fx-background-color: #ffffff;");
        
        // Header with device name, type, and room
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(device.getName());
        nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label typeLabel = new Label(device.getType());
        typeLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #7f8c8d; -fx-padding: 3 8; -fx-border-radius: 3; -fx-background-color: #ecf0f1;");
        
        Label roomLabel = new Label(room.getName());
        roomLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #95a5a6;");
        
        header.getChildren().addAll(nameLabel, typeLabel, roomLabel);
        card.getChildren().add(header);
        
        // Device-type specific controls
        switch (device.getType().toLowerCase()) {
            case "switch":
                createSwitchControls(card, device, room);
                break;
            case "dimmer":
                createDimmerControls(card, device, room);
                break;
            case "thermostat":
                createThermostatControls(card, device, room);
                break;
            case "sensor":
                createSensorControls(card, device, room);
                break;
            case "cover/blind":
                createCoverControls(card, device, room);
                break;
            default:
                break;
        }



        return card;
    }
    
    /**
     * Creates controls for Switch devices.
     */
    void createSwitchControls(VBox card, Device device, Room room) {
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        
        ToggleButton toggleBtn = new ToggleButton(device.getState() ? "ON" : "OFF");
        toggleBtn.setSelected(device.getState());
        toggleBtn.setPrefWidth(80);
        applyLargeSwitchButtonStyle(toggleBtn, device.getState());
        
        toggleBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            roomService.updateDeviceState(device.getId(), newVal);
            toggleBtn.setText(newVal ? "ON" : "OFF");
            applyLargeSwitchButtonStyle(toggleBtn, newVal);
            logService.addLogEntry(device.getName(), room.getName(),
                "Turned " + (newVal ? "ON" : "OFF"), "User");
        });
        
        Label stateLabel = new Label("State: " + (device.getState() ? "ON" : "OFF"));
        stateLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #34495e;");
        device.stateProperty().addListener((obs, oldVal, newVal) -> 
            stateLabel.setText("State: " + (newVal ? "ON" : "OFF")));
        
        controls.getChildren().addAll(toggleBtn, stateLabel);
        card.getChildren().add(controls);
    }

    /**
     * Applies the large visual style used for prominent switch toggle buttons.
     *
     * @param toggleButton the toggle button to style
     * @param enabled whether the style represents the enabled/on state
     */
    void applyLargeSwitchButtonStyle(ToggleButton toggleButton, boolean enabled) {
        toggleButton.setStyle(enabled
                ? "-fx-padding: 8 20; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-color: #27ae60; -fx-text-fill: #ffffff; -fx-border-color: #1e8449; -fx-border-radius: 4; -fx-background-radius: 4;"
                : "-fx-padding: 8 20; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50; -fx-border-color: #bdc3c7; -fx-border-radius: 4; -fx-background-radius: 4;");
    }
    
    /**
     * Creates controls for Dimmer devices.
     */
    void createDimmerControls(VBox card, Device device, Room room) {
        VBox controls = new VBox(8);
        final boolean[] syncingBrightnessFromModel = {false};
        final boolean[] syncingStateFromModel = {false};
        final int[] committedBrightness = {device.getBrightness()};
        final int[] lastNonZeroBrightness = {Math.max(1, device.getBrightness())};
        
        HBox sliderBox = new HBox(10);
        sliderBox.setAlignment(Pos.CENTER_LEFT);
        
        Slider brightnessSlider = new Slider(0, 100, device.getBrightness());
        brightnessSlider.setPrefWidth(300);
        brightnessSlider.setShowTickLabels(true);
        brightnessSlider.setShowTickMarks(true);
        brightnessSlider.setMajorTickUnit(10);
        
        Label brightnessLabel = new Label();
        brightnessLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #34495e; -fx-min-width: 100;");
        updateBrightnessLabel(brightnessLabel, device.getBrightness());
        
        brightnessSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateBrightnessLabel(brightnessLabel, newVal.intValue());
            if (!syncingBrightnessFromModel[0] && !brightnessSlider.isValueChanging()) {
                commitDimmerBrightnessChange(device, room, brightnessSlider, committedBrightness);
            }
        });

        brightnessSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging && !syncingBrightnessFromModel[0]) {
                commitDimmerBrightnessChange(device, room, brightnessSlider, committedBrightness);
            }
        });

        device.brightnessProperty().addListener((obs, oldVal, newVal) -> {
            int brightness = newVal.intValue();
            committedBrightness[0] = brightness;
            if (brightness > 0) {
                lastNonZeroBrightness[0] = brightness;
            }

            syncingBrightnessFromModel[0] = true;
            try {
                if ((int) Math.round(brightnessSlider.getValue()) != brightness) {
                    brightnessSlider.setValue(brightness);
                }
                updateBrightnessLabel(brightnessLabel, brightness);
            } finally {
                syncingBrightnessFromModel[0] = false;
            }
        });
        
        sliderBox.getChildren().addAll(brightnessSlider, brightnessLabel);
        
        HBox stateBox = new HBox(10);
        stateBox.setAlignment(Pos.CENTER_LEFT);
        
        ToggleButton toggleBtn = new ToggleButton(device.getState() ? "ON" : "OFF");
        toggleBtn.setSelected(device.getState());
        toggleBtn.setStyle("-fx-padding: 5 15; -fx-font-size: 11;");
        toggleBtn.setPrefWidth(60);
        
        toggleBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (syncingStateFromModel[0]) {
                return;
            }

            boolean updated = newVal
                    ? roomService.updateDeviceBrightness(device.getId(), Math.max(1, lastNonZeroBrightness[0]))
                    : roomService.updateDeviceBrightness(device.getId(), 0);

            if (updated) {
                logService.addLogEntry(device.getName(), room.getName(),
                    "Turned " + (newVal ? "ON" : "OFF"), "User");
            }
        });
        
        Label stateLabel = new Label();
        stateLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #34495e;");
        updateDimmerStateControls(toggleBtn, stateLabel, device.getState());
        device.stateProperty().addListener((obs, oldVal, newVal) -> {
            syncingStateFromModel[0] = true;
            try {
                updateDimmerStateControls(toggleBtn, stateLabel, newVal);
            } finally {
                syncingStateFromModel[0] = false;
            }
        });
        
        stateBox.getChildren().addAll(toggleBtn, stateLabel);
        
        controls.getChildren().addAll(sliderBox, stateBox);
        card.getChildren().add(controls);
    }
    
    /**
     * Creates controls for Thermostat devices.
     */
    void createThermostatControls(VBox card, Device device, Room room) {
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        
        Label tempLabel = new Label("Target Temperature:");
        tempLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #34495e;");
        
        Button minusBtn = new Button("−");
        minusBtn.setStyle("-fx-padding: 5 12; -fx-font-size: 14;");
        minusBtn.setPrefWidth(40);
        
        Label tempValueLabel = new Label(String.format("%.1f°C", device.getTemperature()));
        tempValueLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #e74c3c; -fx-min-width: 60; -fx-text-alignment: center;");
        
        Button plusBtn = new Button("+");
        plusBtn.setStyle("-fx-padding: 5 12; -fx-font-size: 14;");
        plusBtn.setPrefWidth(40);
        
        minusBtn.setOnAction(e -> {
            if (device.getTemperature() > 5) {
                roomService.updateDeviceTemperature(device.getId(), device.getTemperature() - 1);
                tempValueLabel.setText(String.format("%.1f°C", device.getTemperature()));
                logService.addLogEntry(device.getName(), room.getName(),
                    "Set temperature to " + String.format("%.1f°C", device.getTemperature()), "User");
            }
        });

        plusBtn.setOnAction(e -> {
            if (device.getTemperature() < 35) {
                roomService.updateDeviceTemperature(device.getId(), device.getTemperature() + 1);
                tempValueLabel.setText(String.format("%.1f°C", device.getTemperature()));
                logService.addLogEntry(device.getName(), room.getName(),
                    "Set temperature to " + String.format("%.1f°C", device.getTemperature()), "User");
            }
        });
        
        device.temperatureProperty().addListener((obs, oldVal, newVal) -> 
            tempValueLabel.setText(String.format("%.1f°C", newVal.doubleValue())));
        
        controls.getChildren().addAll(tempLabel, minusBtn, tempValueLabel, plusBtn);
        card.getChildren().add(controls);
    }
    
    /**
     * Creates controls for Sensor devices.
     */
    void createSensorControls(VBox card, Device device, Room room) {
        VBox controls = new VBox(8);
        
        HBox readingBox = new HBox(10);
        readingBox.setAlignment(Pos.CENTER_LEFT);
        
        Label readingLabel = new Label("Current Reading:");
        readingLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #34495e;");
        
        Label valueLabel = new Label(String.format("%.1f", device.getTemperature()));
        valueLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #3498db;");
        
        device.temperatureProperty().addListener((obs, oldVal, newVal) -> 
            valueLabel.setText(String.format("%.1f", newVal.doubleValue())));
        
        readingBox.getChildren().addAll(readingLabel, valueLabel);
        
        HBox injectBox = new HBox(10);
        injectBox.setAlignment(Pos.CENTER_LEFT);
        
        Label injectLabel = new Label("Inject Test Value:");
        injectLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #34495e;");
        
        TextField testValueField = new TextField();
        testValueField.setPromptText("Enter value (e.g., 25.5)");
        testValueField.setPrefWidth(150);
        testValueField.setStyle("-fx-padding: 5;");
        
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button injectBtn = new Button("Inject");
        injectBtn.setStyle("-fx-padding: 5 15; -fx-font-size: 11;");
        injectBtn.setOnAction(e -> {
            try {
                double value = Double.parseDouble(testValueField.getText());
                roomService.updateDeviceTemperature(device.getId(), value);
                testValueField.clear();
                errorLabel.setVisible(false);
                errorLabel.setManaged(false);
                logService.addLogEntry(device.getName(), room.getName(),
                    "Test value injected: " + value, "User");
            } catch (NumberFormatException ex) {
                errorLabel.setText("Invalid input — please enter a numeric value (e.g., 25.5)");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });

        injectBox.getChildren().addAll(injectLabel, testValueField, injectBtn);
        controls.getChildren().addAll(readingBox, injectBox, errorLabel);
        card.getChildren().add(controls);
    }
    
    /**
     * Creates controls for Cover/Blind devices.
     */
    void createCoverControls(VBox card, Device device, Room room) {
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        
        Label positionLabel = new Label("Cover Position:");
        positionLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #34495e;");
        
        Label statusLabel = new Label(device.getState() ? "OPEN" : "CLOSED");
        statusLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: " + 
            (device.getState() ? "#27ae60;" : "#e74c3c;"));
        
        device.stateProperty().addListener((obs, oldVal, newVal) -> {
            statusLabel.setText(newVal ? "OPEN" : "CLOSED");
            statusLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: " + 
                (newVal ? "#27ae60;" : "#e74c3c;"));
        });
        
        Button openBtn = new Button("Open");
        openBtn.setStyle("-fx-padding: 5 15; -fx-font-size: 11;");
        openBtn.setOnAction(e -> {
            if (!device.getState()) {
                roomService.updateDeviceState(device.getId(), true);
                logService.addLogEntry(device.getName(), room.getName(), "Opened", "User");
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-padding: 5 15; -fx-font-size: 11;");
        closeBtn.setOnAction(e -> {
            if (device.getState()) {
                roomService.updateDeviceState(device.getId(), false);
                logService.addLogEntry(device.getName(), room.getName(), "Closed", "User");
            }
        });
        
        controls.getChildren().addAll(positionLabel, statusLabel, openBtn, closeBtn);
        card.getChildren().add(controls);
    }
    
    /**
     * Opens a rename dialog and applies the new name if valid.
     */
    void handleRename(Device device, Room room, Label nameLabel) {
        if (!userService.canManageSystem()) return;

        TextInputDialog dialog = new TextInputDialog(device.getName());
        dialog.setTitle("Rename Device");
        dialog.setHeaderText(null);
        dialog.setContentText("New name:");
        dialog.showAndWait().ifPresent(newName -> {
            if (roomService.renameDevice(room.getId(), device.getId(), newName)) {
                nameLabel.setText(newName);
                logService.addLogEntry(device.getName(), room.getName(),
                        "Renamed to " + newName, userService.getCurrentUserEmail());
            }
        });
    }

    /**
     * Removes the device from its room and refreshes the device list.
     */
    void handleDelete(Device device, Room room) {
        if (!userService.canManageSystem()) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Device");
        confirmation.setHeaderText("Delete device: " + device.getName());
        confirmation.setContentText("This will remove the device from the room and delete its persisted record.");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (roomService.removeDeviceFromRoom(room.getId(), device.getId())) {
                    logService.addLogEntry(device.getName(), room.getName(), "Deleted", userService.getCurrentUserEmail());
                    loadDevices();
                }
            }
        });
    }

    /**
     * Handles the UI flow to add a new device.
     *
     * Presents a dialog to the user to select a room, enter a device name and
     * choose a device type. If the current user is not authorized
     * ({@code userService.canManageSystem()}), this method returns immediately
     * and no dialog is shown.
     * <p>
     * On successful creation the device is persisted via {@code RoomService}
     * and the UI is refreshed via {@link #loadDevices()}.
     *
     * @see at.jku.se.smarthome.service.api.RoomService#addDeviceToRoom(String, String, String)
     */
    @FXML
    void handleAddDevice() {
        if (!userService.canManageSystem()) return;

        Dialog<DeviceInput> dialog = new Dialog<>();
        dialog.setTitle("Add Device");
        dialog.setHeaderText("Create a new device and assign it to a room");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        ComboBox<Room> roomCombo = new ComboBox<>();
        roomCombo.setPrefWidth(240);
        for (Room r : roomService.getRooms()) roomCombo.getItems().add(r);
        roomCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Room room) { return room != null ? room.getName() : ""; }
            @Override
            public Room fromString(String string) { return null; }
        });

        TextField nameField = new TextField();
        nameField.setPromptText("Device name");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Switch", "Dimmer", "Thermostat", "Sensor", "Cover/Blind");
        typeCombo.setPrefWidth(160);

        grid.add(new Label("Room:"), 0, 0);
        grid.add(roomCombo, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Type:"), 0, 2);
        grid.add(typeCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.disableProperty().bind(
                nameField.textProperty().isEmpty()
                        .or(typeCombo.valueProperty().isNull())
                        .or(roomCombo.valueProperty().isNull())
        );

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                return new DeviceInput(roomCombo.getValue(), nameField.getText().trim(), typeCombo.getValue());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(input -> {
            Room r = input.room();
            try {
                Device d = roomService.addDeviceToRoom(r.getId(), input.name(), input.type());
                if (d != null) {
                    logService.addLogEntry(d.getName(), r.getName(), "Device created", userService.getCurrentUserEmail());
                    loadDevices();
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Add Device");
                    alert.setHeaderText("Could not create device");
                    alert.setContentText("Device could not be created. Please check input and try again.");
                    alert.showAndWait();
                }
            } catch (IllegalArgumentException | IllegalStateException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Add Device");
                alert.setHeaderText("Error creating device");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        });
    }

    private record DeviceInput(Room room, String name, String type) { }

    /**
     * Updates the action options shown for a selected device.
     * <p>
     * This method reads the currently selected {@code Device} from {@code deviceCombo}
     * and fills {@code actionCombo} with type-specific actions. If {@code preferredAction}
     * is non-null and present in the resulting list, it will be selected. Otherwise,
     * the first available action is selected when the list is not empty.
     * <p>
     * Typical callers: UI initialization and device selection change handlers.
     *
     * @param deviceCombo the ComboBox containing Device entries (selected device is used)
     * @param actionCombo the ComboBox to be populated with human-readable action labels
     * @param preferredAction optional preferred action to select if present
     */
    void updateActionOptions(ComboBox<Device> deviceCombo, ComboBox<String> actionCombo, String preferredAction) {
        actionCombo.getItems().clear();

        Device selectedDevice = deviceCombo.getValue();
        if (selectedDevice == null) {
            return;
        }

        switch (selectedDevice.getType().toLowerCase()) {
            case "switch" -> actionCombo.getItems().addAll("Turn On", "Turn Off");
            case "dimmer" -> actionCombo.getItems().addAll("Turn On", "Turn Off", "Set to 25%", "Set to 50%", "Set to 75%", "Set to 100%");
            case "thermostat" -> actionCombo.getItems().addAll("Set to 18°C", "Set to 20°C", "Set to 22°C", "Set to 24°C");
            case "cover/blind" -> actionCombo.getItems().addAll("Open", "Close");
            default -> { }
        }

        if (preferredAction != null && actionCombo.getItems().contains(preferredAction)) {
            actionCombo.setValue(preferredAction);
        } else if (!actionCombo.getItems().isEmpty()) {
            actionCombo.setValue(actionCombo.getItems().get(0));
        }
    }

    /**
     * Handles room filter change.
     */
    @FXML
    void handleRoomFilterChange() {
        String selected = roomFilterCombo.getValue();
        if (selected == null || ALL_ROOMS_OPTION.equals(selected)) {
            clearRoomFilterSelection();
        } else {
            selectedRoomFilterId = roomService.getRooms().stream()
                    .filter(room -> room.getName().equals(selected))
                    .map(Room::getId)
                    .findFirst()
                    .orElse(null);
            if (selectedRoomFilterId == null) {
                clearRoomFilterSelection();
            }
        }
        loadDevices();
    }
    
    /**
     * Clears the room filter.
     */
    @FXML
    void handleClearRoomFilter() {
        clearRoomFilterSelection();
        loadDevices();
    }

    private void configureAddDeviceVisibility() {
        if (addDeviceBtn == null) {
            return;
        }

        boolean canManageSystem = userService.canManageSystem();
        addDeviceBtn.setVisible(canManageSystem);
        addDeviceBtn.setManaged(canManageSystem);
        addDeviceBtn.setDisable(!canManageSystem);
    }

    private void observeRooms() {
        roomService.getRooms().forEach(this::attachRoomNameListener);
        roomService.getRooms().addListener(roomListListener);
    }

    private void attachRoomNameListener(Room room) {
        if (room == null || roomNameListeners.containsKey(room)) {
            return;
        }

        ChangeListener<String> listener = (obs, oldName, newName) -> {
            refreshRoomFilterOptions();
            loadDevices();
        };
        room.nameProperty().addListener(listener);
        roomNameListeners.put(room, listener);
    }

    private void detachRoomNameListener(Room room) {
        ChangeListener<String> listener = roomNameListeners.remove(room);
        if (listener != null) {
            room.nameProperty().removeListener(listener);
        }
    }

    private void refreshRoomFilterOptions() {
        Set<String> roomOptions = new LinkedHashSet<>();
        roomOptions.add(ALL_ROOMS_OPTION);
        for (Room room : roomService.getRooms()) {
            roomOptions.add(room.getName());
        }

        roomFilterCombo.getItems().setAll(roomOptions);

        if (selectedRoomFilterId != null) {
            Room selectedRoom = roomService.getRoomById(selectedRoomFilterId);
            if (selectedRoom != null && roomOptions.contains(selectedRoom.getName())) {
                roomFilterCombo.setValue(selectedRoom.getName());
                return;
            }
            clearRoomFilterSelection();
            return;
        }

        if (roomFilterCombo.getValue() == null || !roomOptions.contains(roomFilterCombo.getValue())) {
            roomFilterCombo.setValue(ALL_ROOMS_OPTION);
        }
    }

    private void clearRoomFilterSelection() {
        selectedRoomFilterId = null;
        roomFilterCombo.setValue(ALL_ROOMS_OPTION);
    }

    private void updateBrightnessLabel(Label brightnessLabel, int brightness) {
        brightnessLabel.setText("Brightness: " + brightness + "%");
    }

    private void updateDimmerStateControls(ToggleButton toggleBtn, Label stateLabel, boolean isOn) {
        toggleBtn.setSelected(isOn);
        toggleBtn.setText(isOn ? "ON" : "OFF");
        stateLabel.setText("State: " + (isOn ? "ON" : "OFF"));
    }

    private void commitDimmerBrightnessChange(Device device, Room room, Slider brightnessSlider, int[] committedBrightness) {
        int value = (int) Math.round(brightnessSlider.getValue());
        if (value == committedBrightness[0]) {
            return;
        }

        if (roomService.updateDeviceBrightness(device.getId(), value)) {
            logService.addLogEntry(device.getName(), room.getName(),
                "Set brightness to " + value + "%", "User");
        }
    }
}
