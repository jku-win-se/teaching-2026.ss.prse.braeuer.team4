package at.jku.se.smarthome.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import at.jku.se.smarthome.model.LogEntry;
import at.jku.se.smarthome.service.api.LogService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.api.UserService;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

/**
 * Controller for the activity log view (FR-08, FR-16).
 * Displays all manual and automated state changes with filtering options.
 * Owners can export the currently filtered rows as a CSV file (FR-16).
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

    /** Export CSV button — hidden for Members, visible for Owners only (FR-16). */
    @FXML
    private Button exportCsvBtn;

    /** Log service for activity log access. */
    private final LogService logService = ServiceRegistry.getLogService();

    /** User service for role-based visibility checks. */
    private final UserService userService = ServiceRegistry.getUserService();

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
        deviceFilter.getItems().addFirst(ALL_DEVICES);
        deviceFilter.setValue(ALL_DEVICES);

        // Wrap the source list in a FilteredList so the table updates live.
        filteredLogs = new FilteredList<>(logService.getLogs(), entry -> true);
        logTable.setItems(filteredLogs);

        // Add listeners for filtering
        fromDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        toDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        deviceFilter.setOnAction(e -> applyFilters());

        // FR-16: hide export button for Members; only Owners may export
        if (exportCsvBtn != null) {
            boolean owner = userService.isOwner();
            exportCsvBtn.setVisible(owner);
            exportCsvBtn.setManaged(owner);
        }
    }

    /**
     * Rebuilds the FilteredList predicate from the current device + date inputs.
     * Date comparison uses the leading "yyyy-MM-dd" prefix of the stored timestamp;
     * lexicographic comparison on ISO dates is equivalent to chronological order.
     */
    private void applyFilters() {
        final LocalDate fromDate = fromDatePicker.getValue();
        final LocalDate toDate = toDatePicker.getValue();
        final String selectedDevice = deviceFilter.getValue();
        final boolean filterByDevice = selectedDevice != null && !ALL_DEVICES.equals(selectedDevice);
        final String fromIso = fromDate == null ? null : fromDate.toString();
        final String toIso = toDate == null ? null : toDate.toString();

        filteredLogs.setPredicate(entry -> matches(entry, filterByDevice, selectedDevice, fromIso, toIso));
    }

    private boolean matches(LogEntry entry, boolean filterByDevice, String selectedDevice,
                            String fromIso, String toIso) {
        final String timestamp = entry.getTimestamp();
        final String date = timestamp == null || timestamp.length() < ISO_DATE_LENGTH
                ? "" : timestamp.substring(0, ISO_DATE_LENGTH);
        final boolean deviceOk = !filterByDevice || selectedDevice.equals(entry.getDevice());
        final boolean fromOk = fromIso == null || date.compareTo(fromIso) >= 0;
        final boolean toOk = toIso == null || date.compareTo(toIso) <= 0;
        return deviceOk && fromOk && toOk;
    }

    /**
     * Exports the currently visible (filtered) log rows to a user-chosen CSV file (FR-16).
     * Opens a system Save dialog with a default filename of activity-log_YYYY-MM-DD.csv.
     * If the filtered result is empty, a header-only CSV is written (valid file).
     * If the user cancels the dialog, no action is taken.
     */
    @FXML
    private void handleExportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Activity Log");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        fileChooser.setInitialFileName("activity-log_" + today + ".csv");

        File file = fileChooser.showSaveDialog(logTable.getScene().getWindow());
        if (file == null) {
            return; // user cancelled — do nothing
        }

        String csv = logService.exportToCSV(new ArrayList<>(logTable.getItems()));
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.print(csv);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Failed");
            alert.setHeaderText("Could not write CSV file");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
