package at.jku.se.smarthome.controller;

import java.time.LocalDate;
import java.util.ArrayList;

import at.jku.se.smarthome.model.LogEntry;
import at.jku.se.smarthome.service.api.LogService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import javafx.collections.transformation.FilteredList;
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
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.UnusedPrivateMethod"})
public class ActivityLogController {

    /** Sentinel value for the device combo box meaning "no device filter". */
    private static final String ALL_DEVICES = "All Devices";
    /** ISO date prefix length ("yyyy-MM-dd") within stored timestamps. */
    private static final int ISO_DATE_LENGTH = 10;

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

    /** Filtered view of the log list — predicate is rebuilt on filter changes. */
    private FilteredList<LogEntry> filteredLogs;

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
        deviceFilter.getItems().add(0, ALL_DEVICES);
        deviceFilter.setValue(ALL_DEVICES);

        // Wrap the source list in a FilteredList so the table updates live
        // whenever new log entries are appended or filter inputs change.
        filteredLogs = new FilteredList<>(logService.getLogs(), entry -> true);
        logTable.setItems(filteredLogs);

        // Add listeners for filtering
        fromDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        toDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        deviceFilter.setOnAction(e -> applyFilters());
    }

    /**
     * Rebuilds the FilteredList predicate from the current device + date inputs.
     * Date comparison uses the leading "yyyy-MM-dd" prefix of the stored timestamp;
     * lexicographic comparison on ISO dates is equivalent to chronological order.
     */
    private void applyFilters() {
        final LocalDate from = fromDatePicker.getValue();
        final LocalDate to = toDatePicker.getValue();
        final String selectedDevice = deviceFilter.getValue();
        final boolean filterByDevice = selectedDevice != null && !ALL_DEVICES.equals(selectedDevice);
        final String fromIso = from == null ? null : from.toString();
        final String toIso = to == null ? null : to.toString();

        filteredLogs.setPredicate(entry -> matches(entry, filterByDevice, selectedDevice, fromIso, toIso));
    }

    private boolean matches(LogEntry entry, boolean filterByDevice, String selectedDevice,
                            String fromIso, String toIso) {
        if (filterByDevice && !selectedDevice.equals(entry.getDevice())) {
            return false;
        }
        String timestamp = entry.getTimestamp();
        String date = timestamp == null || timestamp.length() < ISO_DATE_LENGTH
                ? "" : timestamp.substring(0, ISO_DATE_LENGTH);
        if (fromIso != null && date.compareTo(fromIso) < 0) {
            return false;
        }
        return toIso == null || date.compareTo(toIso) <= 0;
    }

    @FXML
    private void handleExportCSV() {
        // Export respects the active filter: pass the currently visible (filtered) rows.
        // The CSV string is returned for the caller (FR's CSV export feature) to persist.
        logService.exportToCSV(new ArrayList<>(logTable.getItems()));
    }
}
