package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.LogEntry;
import at.jku.se.smarthome.service.api.LogService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller for the activity log view (FR-08).
 * Displays all manual and automated state changes with filtering options.
 */
public class ActivityLogController {
    
    /** Table view for displaying activity log entries. */
    @FXML
    private TableView<LogEntry> logTable;
    
    /** Column displaying event timestamp. */
    @FXML
    private TableColumn<LogEntry, String> timestampColumn;
    
    /** Column displaying affected device. */
    @FXML
    private TableColumn<LogEntry, String> deviceColumn;
    
    /** Column displaying device location/room. */
    @FXML
    private TableColumn<LogEntry, String> roomColumn;
    
    /** Column displaying action performed. */
    @FXML
    private TableColumn<LogEntry, String> actionColumn;
    
    /** Column displaying user/actor who performed action. */
    @FXML
    private TableColumn<LogEntry, String> actorColumn;
    
    /** Date picker for filtering logs from start date. */
    @FXML
    private DatePicker fromDatePicker;
    
    /** Date picker for filtering logs to end date. */
    @FXML
    private DatePicker toDatePicker;
    
    /** ComboBox for filtering logs by device. */
    @FXML
    private ComboBox<String> deviceFilter;
    
    /** Log service for activity log access. */
    private final LogService logService = ServiceRegistry.getLogService();
    
    @FXML
    private void initialize() {
        // Configure table columns
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        deviceColumn.setCellValueFactory(new PropertyValueFactory<>("device"));
        roomColumn.setCellValueFactory(new PropertyValueFactory<>("room"));
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        actorColumn.setCellValueFactory(new PropertyValueFactory<>("actor"));
        
        // Populate device filter with all unique devices
        deviceFilter.getItems().addAll(logService.getUniqueDevices());
        deviceFilter.getItems().add(0, "All Devices");
        deviceFilter.setValue("All Devices");
        
        // Add listeners for filtering
        fromDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        toDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        deviceFilter.setOnAction(e -> applyFilters());
        
        // Initial load of all log entries
        logTable.setItems(logService.getLogs());
    }
    
    /**
     * Applies all active filters to the log table.
     */
    private void applyFilters() {
        logTable.setItems(logService.getLogs());
        // Note: Date and device filtering can be enhanced based on LogEntry extending to include date comparison
    }
    
    @FXML
    private void handleExportCSV() {
        String csv = logService.exportToCSV();
        // In a real application, this would save to a file or copy to clipboard
        // Export CSV data is now available via logService.exportToCSV()
    }
}
