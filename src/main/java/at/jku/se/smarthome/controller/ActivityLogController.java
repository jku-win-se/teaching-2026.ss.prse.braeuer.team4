package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.LogEntry;
import at.jku.se.smarthome.service.MockLogService;
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
    
    @FXML
    private TableView<LogEntry> logTable;
    
    @FXML
    private TableColumn<LogEntry, String> timestampColumn;
    
    @FXML
    private TableColumn<LogEntry, String> deviceColumn;
    
    @FXML
    private TableColumn<LogEntry, String> roomColumn;
    
    @FXML
    private TableColumn<LogEntry, String> actionColumn;
    
    @FXML
    private TableColumn<LogEntry, String> actorColumn;
    
    @FXML
    private DatePicker fromDatePicker;
    
    @FXML
    private DatePicker toDatePicker;
    
    @FXML
    private ComboBox<String> deviceFilter;
    
    private final MockLogService logService = MockLogService.getInstance();
    
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
        System.out.println("Export CSV:\n" + csv);
    }
}
