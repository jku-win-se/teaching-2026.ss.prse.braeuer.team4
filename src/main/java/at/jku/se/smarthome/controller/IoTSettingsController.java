package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.IntegrationDevice;
import at.jku.se.smarthome.service.mock.MockIoTIntegrationService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller for the IoT settings view.
 */
public class IoTSettingsController {
    
    /** Toggle button to enable/disable IoT integration. */
    @FXML
    private ToggleButton enableToggle;
    
    /** Label displaying IoT integration status. */
    @FXML
    private Label statusLabel;

    /** Label displaying protocol information. */
    @FXML
    private Label protocolLabel;

    /** Label displaying integration summary. */
    @FXML
    private Label integrationSummaryLabel;

    /** Label displaying last synchronization time. */
    @FXML
    private Label lastSyncLabel;
    
    /** Text field for broker address. */
    @FXML
    private TextField brokerField;
    
    /** Text field for broker port. */
    @FXML
    private TextField portField;
    
    /** Text field for broker username. */
    @FXML
    private TextField usernameField;
    
    /** Password field for broker password. */
    @FXML
    private PasswordField passwordField;
    
    /** Label displaying test/operation results. */
    @FXML
    private Label resultLabel;

    /** Button to test IoT connection. */
    @FXML
    private Button testBtn;

    /** Button to save configuration. */
    @FXML
    private Button saveBtn;

    /** Button to discover IoT devices. */
    @FXML
    private Button discoverBtn;

    /** Table view displaying discovered IoT devices. */
    @FXML
    private TableView<IntegrationDevice> devicesTable;

    /** Column displaying device names. */
    @FXML
    private TableColumn<IntegrationDevice, String> deviceNameColumn;

    /** Column displaying device types. */
    @FXML
    private TableColumn<IntegrationDevice, String> deviceTypeColumn;

    /** Column displaying MQTT topics. */
    @FXML
    private TableColumn<IntegrationDevice, String> topicColumn;

    /** Column displaying device status. */
    @FXML
    private TableColumn<IntegrationDevice, String> deviceStatusColumn;
    
    /** IoT integration service instance. */
    private final MockIoTIntegrationService integrationService = MockIoTIntegrationService.getInstance();
    
    @FXML
    private void initialize() {
        deviceNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        deviceTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        topicColumn.setCellValueFactory(new PropertyValueFactory<>("topic"));
        deviceStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        devicesTable.setItems(integrationService.getDiscoveredDevices());

        protocolLabel.setText(integrationService.getProtocolName());

        MockIoTIntegrationService.IoTConfiguration configuration = integrationService.getConfiguration();
        enableToggle.setSelected(configuration.enabled());
        enableToggle.setText(configuration.enabled() ? "ON" : "OFF");
        brokerField.setText(configuration.broker());
        portField.setText(String.valueOf(configuration.port()));
        usernameField.setText(configuration.username());
        passwordField.setText(configuration.password());

        applyEnabledState(configuration.enabled());
        refreshStatusLabels(configuration.enabled(), integrationService.isConnected());
    }
    
    @FXML
    private void handleToggleIntegration() {
        boolean enabled = enableToggle.isSelected();
        enableToggle.setText(enabled ? "ON" : "OFF");
        applyEnabledState(enabled);

        if (!enabled) {
            integrationService.saveConfiguration(false, brokerField.getText(), parsePort(), usernameField.getText(), passwordField.getText());
            resultLabel.setText("Integration disabled. Virtual devices continue to work without MQTT.");
            resultLabel.setStyle("-fx-text-fill: #7f8c8d;");
            refreshStatusLabels(false, false);
        }
    }

    private void applyEnabledState(boolean enabled) {
        brokerField.setDisable(!enabled);
        portField.setDisable(!enabled);
        usernameField.setDisable(!enabled);
        passwordField.setDisable(!enabled);
        testBtn.setDisable(!enabled);
        saveBtn.setDisable(!enabled);
        discoverBtn.setDisable(!enabled || !integrationService.isConnected());
    }
    
    @FXML
    private void handleTestConnection() {
        String broker = brokerField.getText();
        if (!enableToggle.isSelected()) {
            showError("Enable the MQTT integration layer before testing the connection");
            return;
        }

        boolean success = integrationService.testConnection(broker, portField.getText());
        if (success) {
            resultLabel.setText("Connection test successful. Broker and port look valid for a mock MQTT setup.");
            resultLabel.setStyle("-fx-text-fill: #27ae60;");
        } else {
            refreshStatusLabels(true, false);
            showError("Connection failed. Please provide a valid broker and port.");
        }
    }
    
    @FXML
    private void handleSaveSettings() {
        if (!enableToggle.isSelected()) {
            showError("Enable the integration layer before saving MQTT settings");
            return;
        }

        if (!integrationService.testConnection(brokerField.getText(), portField.getText())) {
            showError("Please provide a valid broker and port before saving");
            return;
        }

        integrationService.saveConfiguration(
                true,
                brokerField.getText().trim(),
                parsePort(),
                usernameField.getText().trim(),
                passwordField.getText()
        );

        boolean connected = integrationService.connect();
        refreshStatusLabels(true, connected);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("MQTT Settings Saved");
        alert.setContentText(connected
                ? "Mock MQTT integration is now connected and ready to discover physical devices."
                : "Settings were saved, but the mock integration could not connect.");
        alert.showAndWait();

        resultLabel.setText(connected
                ? "MQTT integration connected. You can now discover physical devices."
                : "Settings saved, but the connection is still offline.");
        resultLabel.setStyle(connected ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: #e67e22;");
    }

    @FXML
    private void handleDiscoverDevices() {
        if (!integrationService.refreshDevices()) {
            showError("Connect to the mock MQTT broker before discovering devices");
            return;
        }

        refreshStatusLabels(true, true);
        resultLabel.setText("Mock device discovery completed. Physical MQTT devices are listed below.");
        resultLabel.setStyle("-fx-text-fill: #27ae60;");
    }

    private void refreshStatusLabels(boolean enabled, boolean connected) {
        if (!enabled) {
            statusLabel.setText("Disabled");
            statusLabel.setStyle("-fx-text-fill: #7f8c8d;");
            integrationSummaryLabel.setText("Optional integration layer is disabled.");
        } else if (connected) {
            statusLabel.setText("Connected");
            statusLabel.setStyle("-fx-text-fill: #27ae60;");
            integrationSummaryLabel.setText("MQTT mock gateway is connected and ready for physical devices.");
        } else {
            statusLabel.setText("Disconnected");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            integrationSummaryLabel.setText("MQTT mock gateway is configured but not connected.");
        }

        lastSyncLabel.setText(integrationService.getLastSync());
        discoverBtn.setDisable(!enabled || !connected);
    }

    private int parsePort() {
        try {
            return Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException exception) {
            return -1;
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
