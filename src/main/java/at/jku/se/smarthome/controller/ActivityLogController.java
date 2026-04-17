package at.jku.se.smarthome.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    /** Logger for export and diagnostics. */
    private static final Logger LOGGER = LogManager.getLogger(ActivityLogController.class);
    
    /** Table showing all recorded log entries. */
    @FXML
    private TableView<LogEntry> logTable;
    
    /** Column showing event timestamps. */
    @FXML
    private TableColumn<LogEntry, String> timestampColumn;
    
    /** Column showing affected device names. */
    @FXML
    private TableColumn<LogEntry, String> deviceColumn;
    
    /** Column showing associated room names. */
    @FXML
    private TableColumn<LogEntry, String> roomColumn;
    
    /** Column showing performed actions. */
    @FXML
    private TableColumn<LogEntry, String> actionColumn;
    
    /** Column showing actor (user/rule/schedule). */
    @FXML
    private TableColumn<LogEntry, String> actorColumn;
    
    /** Lower date bound for filtering. */
    @FXML
    private DatePicker fromDatePicker;
    
    /** Upper date bound for filtering. */
    @FXML
    private DatePicker toDatePicker;
    
    /** Device selector for filtering log entries. */
    @FXML
    private ComboBox<String> deviceFilter;

    /** Backing service for log queries and export. */
    private final LogService logService;

    /**
     * Creates a controller with the default log service.
     */
    public ActivityLogController() {
        this.logService = ServiceRegistry.getLogService();
    }

    /**
     * Initializes table columns, filter widgets, and initial data.
     */
    @FXML
    public void initialize() {
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

    /**
     * Exports the current log data as CSV content.
     */
    @FXML
    public void handleExportCSV() {
        String csv = logService.exportToCSV();
        // In a real application, this would save to a file or copy to clipboard
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Export CSV:{}{}", System.lineSeparator(), csv);
        }
    }
}
