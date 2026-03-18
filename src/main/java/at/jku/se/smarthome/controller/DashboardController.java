package at.jku.se.smarthome.controller;

import java.util.ArrayList;
import java.util.List;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.service.MockSmartHomeService;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the Dashboard view.
 * 
 * Displays favorite devices with interactive controls and energy consumption chart.
 */
public class DashboardController {
    
    @FXML
    private GridPane devicesGrid;
    
    @FXML
    private PieChart energyChart;
    
    @FXML
    private VBox notificationTray;
    
    private final MockSmartHomeService service = MockSmartHomeService.getInstance();
    
    /**
     * Initializes the dashboard after FXML has been loaded.
     * Populates devices and notifications.
     */
    @FXML
    private void initialize() {
        loadFavoriteDevices();
        loadEnergyData();
        loadNotifications();
    }
    
    /**
     * Loads and displays favorite devices as interactive cards in the grid.
     */
    private void loadFavoriteDevices() {
        if (devicesGrid == null) {
            return;
        }
        
        devicesGrid.getChildren().clear();
        
        // Get first 4 devices as favorites
        List<Device> favoriteDevices = new ArrayList<>(service.getDevices());
        if (favoriteDevices.size() > 4) {
            favoriteDevices = favoriteDevices.subList(0, 4);
        }
        
        int row = 0;
        int col = 0;
        
        for (Device device : favoriteDevices) {
            VBox card = createDeviceCard(device);
            devicesGrid.add(card, col, row);
            
            col++;
            if (col >= 2) {
                col = 0;
                row++;
            }
        }
    }
    
    /**
     * Creates a device card with appropriate controls based on device type.
     * 
     * @param device the device to display
     * @return a VBox containing the device card
     */
    private VBox createDeviceCard(Device device) {
        VBox card = new VBox(10);
        card.setStyle("-fx-border-color: #bdc3c7; -fx-border-radius: 8; " +
                      "-fx-background-color: #ffffff; -fx-padding: 15;");
        card.setPrefWidth(200);
        
        // Device name
        Label nameLabel = new Label(device.getName());
        nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        // Device room
        Label roomLabel = new Label("Room: " + device.getRoom());
        roomLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #7f8c8d;");
        
        card.getChildren().addAll(nameLabel, roomLabel);
        
        // Add type-specific controls
        switch (device.getType()) {
            case "Switch":
                card.getChildren().add(createSwitchControl(device));
                break;
            case "Dimmer":
                card.getChildren().add(createDimmerControl(device));
                break;
            case "Thermostat":
                card.getChildren().add(createThermostatControl(device));
                break;
        }
        
        return card;
    }
    
    /**
     * Creates a switch (toggle button) control for a device.
     * 
     * @param device the device
     * @return a ToggleButton
     */
    private ToggleButton createSwitchControl(Device device) {
        ToggleButton toggleButton = new ToggleButton();
        toggleButton.setSelected(device.getState());
        toggleButton.setPrefWidth(Double.MAX_VALUE);
        toggleButton.setStyle("-fx-padding: 10; -fx-font-size: 12;");
        toggleButton.setText(device.getState() ? "ON" : "OFF");
        
        device.stateProperty().addListener((obs, oldVal, newVal) -> {
            toggleButton.setSelected(newVal);
            toggleButton.setText(newVal ? "ON" : "OFF");
        });
        
        toggleButton.setOnAction(e -> {
            boolean success = service.toggleDevice(device.getId());
            if (!success) {
                showError("Failed to toggle device: " + device.getName());
            }
        });
        
        return toggleButton;
    }
    
    /**
     * Creates a dimmer (slider) control for a device.
     * 
     * @param device the device
     * @return a VBox containing slider and label
     */
    private VBox createDimmerControl(Device device) {
        VBox container = new VBox(5);
        
        Label brightnessLabel = new Label("Brightness: " + device.getBrightness() + "%");
        brightnessLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #34495e;");
        
        Slider slider = new Slider(0, 100, device.getBrightness());
        slider.setShowTickLabels(false);
        slider.setPrefWidth(Double.MAX_VALUE);
        
        device.brightnessProperty().addListener((obs, oldVal, newVal) -> {
            brightnessLabel.setText("Brightness: " + newVal + "%");
            slider.setValue(newVal.doubleValue());
        });
        
        slider.setOnMouseReleased(e -> {
            int brightness = (int) slider.getValue();
            boolean success = service.setBrightness(device.getId(), brightness);
            if (!success) {
                showError("Failed to set brightness for: " + device.getName());
            }
        });
        
        container.getChildren().addAll(brightnessLabel, slider);
        return container;
    }
    
    /**
     * Creates a thermostat (temperature) control for a device.
     * 
     * @param device the device
     * @return a VBox containing temperature controls
     */
    private VBox createThermostatControl(Device device) {
        VBox container = new VBox(5);
        
        Label tempLabel = new Label("Temperature: " + String.format("%.1f", device.getTemperature()) + "°C");
        tempLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #34495e;");
        
        device.temperatureProperty().addListener((obs, oldVal, newVal) -> {
            tempLabel.setText("Temperature: " + String.format("%.1f", newVal) + "°C");
        });
        
        Button minusButton = new Button("-");
        minusButton.setPrefWidth(50);
        minusButton.setOnAction(e -> {
            double newTemp = device.getTemperature() - 1;
            boolean success = service.setTemperature(device.getId(), newTemp);
            if (!success) {
                showError("Temperature must be between 10°C and 35°C");
            }
        });
        
        Button plusButton = new Button("+");
        plusButton.setPrefWidth(50);
        plusButton.setOnAction(e -> {
            double newTemp = device.getTemperature() + 1;
            boolean success = service.setTemperature(device.getId(), newTemp);
            if (!success) {
                showError("Temperature must be between 10°C and 35°C");
            }
        });
        
        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(5);
        buttonBox.getChildren().addAll(minusButton, plusButton);
        
        container.getChildren().addAll(tempLabel, buttonBox);
        return container;
    }
    
    /**
     * Loads and displays energy consumption data.
     */
    private void loadEnergyData() {
        if (energyChart == null) {
            return;
        }
        
        energyChart.getData().clear();
        
        // Mock energy data
        energyChart.getData().add(new PieChart.Data("Living Room Light", 25));
        energyChart.getData().add(new PieChart.Data("Bedroom Dimmer", 15));
        energyChart.getData().add(new PieChart.Data("Kitchen Light", 30));
        energyChart.getData().add(new PieChart.Data("Thermostats", 30));
    }
    
    /**
     * Loads and displays notifications.
     */
    private void loadNotifications() {
        if (notificationTray == null) {
            return;
        }
        
        notificationTray.getChildren().clear();
        
        // Add sample notifications
        Label notification1 = new Label("✓ All devices are operational");
        notification1.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12;");
        
        Label notification2 = new Label("ℹ Dashboard loaded successfully");
        notification2.setStyle("-fx-text-fill: #3498db; -fx-font-size: 12;");
        
        notificationTray.getChildren().addAll(notification1, notification2);
    }
    
    /**
     * Shows an error alert to the user.
     * 
     * @param message the error message
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
