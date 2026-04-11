package at.jku.se.smarthome.controller;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Scene;
import at.jku.se.smarthome.service.api.RoomService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.mock.MockSceneService;
import at.jku.se.smarthome.service.mock.MockUserService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Controller for the scenes view.
 */
public class ScenesController {

    @FXML
    private Button newSceneBtn;
    
    @FXML
    private GridPane scenesGrid;
    
    private final MockSceneService sceneService = MockSceneService.getInstance();
    private final RoomService roomService = ServiceRegistry.getRoomService();
    private final MockUserService userService = MockUserService.getInstance();
    private boolean refreshingDeviceOptions;
    
    @FXML
    private void initialize() {
        newSceneBtn.setDisable(!userService.canManageSystem());
        loadScenes();
    }
    
    private void loadScenes() {
        scenesGrid.getChildren().clear();
        int row = 0, col = 0;
        
        for (Scene scene : sceneService.getScenes()) {
            VBox card = createSceneCard(scene);
            scenesGrid.add(card, col, row);
            
            col++;
            if (col >= 3) {
                col = 0;
                row++;
            }
        }
    }
    
    private VBox createSceneCard(Scene scene) {
        VBox card = new VBox(10);
        card.setStyle("-fx-border-color: #bdc3c7; -fx-border-radius: 8; " +
                     "-fx-background-color: #ffffff; -fx-padding: 15;");
        card.setPrefWidth(200);
        
        Label nameLabel = new Label(scene.getName());
        nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label descLabel = new Label(scene.getDescription());
        descLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #7f8c8d;");
        descLabel.setWrapText(true);

        Label statesLabel = new Label(buildStateSummary(scene));
        statesLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #34495e;");
        statesLabel.setWrapText(true);
        
        Button activateBtn = new Button("Activate");
        activateBtn.setMaxWidth(Double.MAX_VALUE);
        activateBtn.setStyle("-fx-padding: 8;");
        activateBtn.setOnAction(e -> sceneService.activateScene(scene.getId()));

        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-padding: 6 12;");
        editBtn.setDisable(!userService.canManageSystem());
        editBtn.setOnAction(e -> handleEditScene(scene));

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-padding: 6 12; -fx-text-fill: #e74c3c;");
        deleteBtn.setDisable(!userService.canManageSystem());
        deleteBtn.setOnAction(e -> handleDeleteScene(scene));

        HBox actions = new HBox(8, editBtn, deleteBtn);
        
        card.getChildren().addAll(nameLabel, descLabel, statesLabel, activateBtn, actions);
        return card;
    }
    
    @FXML
    private void handleCreateScene() {
        if (!userService.canManageSystem()) {
            return;
        }

        Optional<SceneInput> result = showSceneDialog(null);
        result.ifPresent(input -> {
            sceneService.addScene(input.name(), input.description(), input.deviceStates());
            loadScenes();
        });
    }

    private void handleEditScene(Scene scene) {
        if (!userService.canManageSystem()) {
            return;
        }

        Optional<SceneInput> result = showSceneDialog(scene);
        result.ifPresent(input -> {
            sceneService.updateScene(scene.getId(), input.name(), input.description(), input.deviceStates());
            loadScenes();
        });
    }

    private void handleDeleteScene(Scene scene) {
        if (!userService.canManageSystem()) {
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Scene");
        confirmation.setHeaderText("Delete scene: " + scene.getName());
        confirmation.setContentText("This removes the saved grouped device states from the prototype.");

        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                sceneService.deleteScene(scene.getId());
                loadScenes();
            }
        });
    }

    private Optional<SceneInput> showSceneDialog(Scene existingScene) {
        Dialog<SceneInput> dialog = new Dialog<>();
        dialog.setTitle(existingScene == null ? "Create Scene" : "Edit Scene");
        dialog.setHeaderText("Define a named group of device states that can be activated with one action.");
        dialog.setResizable(true);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField nameField = new TextField();
        nameField.setPromptText("Movie Night");

        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Dims lights and sets the home for a movie.");

        VBox deviceStatesBox = new VBox(8);

        Button addDeviceStateBtn = new Button("+ Add Device State");
        addDeviceStateBtn.setOnAction(event -> {
            deviceStatesBox.getChildren().add(createDeviceStateRow(null, null, deviceStatesBox, dialog));
            refreshDeviceOptions(deviceStatesBox);
            resizeDialog(dialog);
        });

        Label hintLabel = new Label("Choose each device once. State choices are filtered to values valid for that device type.");
        hintLabel.setWrapText(true);
        hintLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #7f8c8d;");

        if (existingScene != null) {
            nameField.setText(existingScene.getName());
            descriptionField.setText(existingScene.getDescription());
            for (String deviceState : existingScene.getDeviceStates()) {
                Map.Entry<String, String> parsedState = parseSceneState(deviceState);
                deviceStatesBox.getChildren().add(createDeviceStateRow(parsedState.getKey(), parsedState.getValue(), deviceStatesBox, dialog));
            }
        }

        if (deviceStatesBox.getChildren().isEmpty()) {
            deviceStatesBox.getChildren().add(createDeviceStateRow(null, null, deviceStatesBox, dialog));
        }
        refreshDeviceOptions(deviceStatesBox);

        grid.add(new Label("Scene Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionField, 1, 1);
        grid.add(new Label("Device States:"), 0, 2);
        grid.add(deviceStatesBox, 1, 2);
        grid.add(addDeviceStateBtn, 1, 3);
        grid.add(hintLabel, 1, 4);

        dialog.getDialogPane().setContent(grid);
        resizeDialog(dialog);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!isValidSceneInput(nameField.getText(), deviceStatesBox)) {
                event.consume();
                showSceneValidationError();
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                return new SceneInput(
                        nameField.getText().trim(),
                        descriptionField.getText().trim(),
                        collectDeviceStates(deviceStatesBox)
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private HBox createDeviceStateRow(String selectedDeviceName, String selectedState, VBox parentBox, Dialog<?> dialog) {
        ComboBox<Device> deviceCombo = new ComboBox<>();
        deviceCombo.setPrefWidth(230);
        deviceCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Device device) {
                return device == null ? "" : formatDeviceLabel(device);
            }

            @Override
            public Device fromString(String string) {
                return null;
            }
        });

        ComboBox<String> stateCombo = new ComboBox<>();
        stateCombo.setPrefWidth(170);

        Button removeBtn = new Button("Remove");
        removeBtn.setOnAction(event -> {
            parentBox.getChildren().removeIf(node -> node == removeBtn.getParent());
            if (parentBox.getChildren().isEmpty()) {
                parentBox.getChildren().add(createDeviceStateRow(null, null, parentBox, dialog));
            }
            refreshDeviceOptions(parentBox);
            resizeDialog(dialog);
        });

        HBox row = new HBox(8, deviceCombo, stateCombo, removeBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(deviceCombo, Priority.ALWAYS);
        HBox.setHgrow(stateCombo, Priority.NEVER);

        deviceCombo.setOnAction(event -> {
            if (refreshingDeviceOptions) {
                return;
            }
            updateStateOptions(deviceCombo, stateCombo, null);
            refreshDeviceOptions(parentBox);
        });

        if (selectedDeviceName != null) {
            deviceCombo.setValue(roomService.getDeviceByName(selectedDeviceName));
        }
        updateStateOptions(deviceCombo, stateCombo, selectedState);

        return row;
    }

    private void resizeDialog(Dialog<?> dialog) {
        Platform.runLater(() -> {
            if (dialog.getDialogPane().getScene() != null && dialog.getDialogPane().getScene().getWindow() != null) {
                dialog.getDialogPane().getScene().getWindow().sizeToScene();
            }
        });
    }

    private void updateStateOptions(ComboBox<Device> deviceCombo, ComboBox<String> stateCombo, String selectedState) {
        stateCombo.getItems().clear();

        Device device = deviceCombo.getValue();
        if (device == null) {
            return;
        }

        stateCombo.getItems().setAll(getValidStates(device));

        if (selectedState != null && stateCombo.getItems().contains(selectedState)) {
            stateCombo.setValue(selectedState);
        } else if (!stateCombo.getItems().isEmpty()) {
            stateCombo.setValue(stateCombo.getItems().get(0));
        }
    }

    private void refreshDeviceOptions(VBox deviceStatesBox) {
        if (refreshingDeviceOptions) {
            return;
        }

        refreshingDeviceOptions = true;

        List<SceneStateRow> rows = getSceneStateRows(deviceStatesBox);
        List<Device> allDevices = getSceneCompatibleDevices();

        try {
            for (SceneStateRow row : rows) {
                Device currentSelection = row.deviceCombo().getValue();
                String selectedDeviceName = currentSelection != null ? currentSelection.getName() : null;

                List<String> selectedInOtherRows = rows.stream()
                        .map(SceneStateRow::deviceCombo)
                        .map(ComboBox::getValue)
                        .filter(device -> device != null && (selectedDeviceName == null || !selectedDeviceName.equals(device.getName())))
                        .map(Device::getName)
                        .toList();

                List<Device> availableDevices = allDevices.stream()
                        .filter(device -> selectedDeviceName != null && selectedDeviceName.equals(device.getName())
                                || !selectedInOtherRows.contains(device.getName()))
                        .collect(Collectors.toList());

                row.deviceCombo().getItems().setAll(availableDevices);

                Device restoredSelection = selectedDeviceName == null ? null : availableDevices.stream()
                        .filter(device -> selectedDeviceName.equals(device.getName()))
                        .findFirst()
                        .orElse(null);

                if (restoredSelection != null) {
                    row.deviceCombo().setValue(restoredSelection);
                } else if (!availableDevices.isEmpty()) {
                    row.deviceCombo().setValue(availableDevices.get(0));
                } else {
                    row.deviceCombo().setValue(null);
                }

                String selectedState = row.stateCombo().getValue();
                updateStateOptions(row.deviceCombo(), row.stateCombo(), selectedState);
            }
        } finally {
            refreshingDeviceOptions = false;
        }
    }

    private List<SceneStateRow> getSceneStateRows(VBox deviceStatesBox) {
        List<SceneStateRow> rows = new ArrayList<>();
        for (javafx.scene.Node child : deviceStatesBox.getChildren()) {
            if (child instanceof HBox row && row.getChildren().size() >= 2
                    && row.getChildren().get(0) instanceof ComboBox<?> rawDeviceCombo
                    && row.getChildren().get(1) instanceof ComboBox<?> rawStateCombo) {
                @SuppressWarnings("unchecked")
                ComboBox<Device> deviceCombo = (ComboBox<Device>) rawDeviceCombo;
                @SuppressWarnings("unchecked")
                ComboBox<String> stateCombo = (ComboBox<String>) rawStateCombo;
                rows.add(new SceneStateRow(deviceCombo, stateCombo));
            }
        }
        return rows;
    }

    private List<Device> getSceneCompatibleDevices() {
        return roomService.getAllDevices().stream()
                .filter(device -> !"sensor".equalsIgnoreCase(device.getType()))
                .sorted(Comparator.comparing(Device::getRoom).thenComparing(Device::getName))
                .collect(Collectors.toList());
    }

    private List<String> getValidStates(Device device) {
        return switch (device.getType().toLowerCase()) {
            case "switch" -> List.of("ON", "OFF");
            case "dimmer" -> List.of("0%", "25%", "50%", "75%", "100%");
            case "thermostat" -> List.of("18°C", "20°C", "22°C", "24°C");
            case "cover/blind" -> List.of("OPEN", "CLOSED");
            default -> List.of("ON", "OFF");
        };
    }

    private boolean isValidSceneInput(String sceneName, VBox deviceStatesBox) {
        return !sceneName.trim().isEmpty()
                && !collectDeviceStates(deviceStatesBox).isEmpty();
    }

    private List<String> collectDeviceStates(VBox deviceStatesBox) {
        List<String> deviceStates = new ArrayList<>();
        for (SceneStateRow row : getSceneStateRows(deviceStatesBox)) {
            Device device = row.deviceCombo().getValue();
            String state = row.stateCombo().getValue();
            if (device != null && state != null) {
                deviceStates.add(device.getName() + ": " + state);
            }
        }
        return deviceStates;
    }

    private Map.Entry<String, String> parseSceneState(String deviceState) {
        String[] parts = deviceState.split(":", 2);
        if (parts.length == 2) {
            return new AbstractMap.SimpleEntry<>(parts[0].trim(), parts[1].trim());
        }
        return new AbstractMap.SimpleEntry<>(deviceState.trim(), null);
    }

    private void showSceneValidationError() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Incomplete Scene");
        alert.setHeaderText("Scene needs at least one complete device state");
        alert.setContentText("Please enter a scene name and select both a device and a valid target state.");
        alert.showAndWait();
    }

    private String buildStateSummary(Scene scene) {
        if (scene.getDeviceStates().isEmpty()) {
            return "No device states configured yet.";
        }

        return scene.getDeviceStates().stream()
                .limit(4)
                .map(this::formatCompactState)
                .collect(Collectors.joining("\n"))
                + (scene.getDeviceStates().size() > 4
                ? "\n+ " + (scene.getDeviceStates().size() - 4) + " more state(s)"
                : "");
    }

    private String formatCompactState(String stateDefinition) {
        Map.Entry<String, String> parsed = parseSceneState(stateDefinition);
        Device device = roomService.getDeviceByName(parsed.getKey());
        if (device == null) {
            return parsed.getKey() + " -> " + parsed.getValue();
        }
        return device.getRoom() + " | " + device.getName() + " -> " + parsed.getValue();
    }

    private String formatDeviceLabel(Device device) {
        return device.getName() + " (" + device.getRoom() + " • " + device.getType() + ")";
    }

    private record SceneInput(String name, String description, List<String> deviceStates) {
    }

    private record SceneStateRow(ComboBox<Device> deviceCombo, ComboBox<String> stateCombo) {
    }
}
