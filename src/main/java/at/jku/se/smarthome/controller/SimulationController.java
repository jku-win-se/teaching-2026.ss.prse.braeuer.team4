package at.jku.se.smarthome.controller;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import at.jku.se.smarthome.model.Rule;
import at.jku.se.smarthome.model.SimulationDeviceState;
import at.jku.se.smarthome.service.mock.MockRuleService;
import at.jku.se.smarthome.service.mock.MockSimulationService;
import at.jku.se.smarthome.service.mock.MockSimulationService.SimulationConfiguration;
import at.jku.se.smarthome.service.mock.MockSimulationService.SimulationEvent;
import at.jku.se.smarthome.service.mock.MockSimulationService.SimulationPlan;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Controller for the simulation view.
 */
public class SimulationController {
    
    /** Text field for entering simulation start time. */
    @FXML
    private TextField startTimeSpinner;
    
    /** Combo box for selecting simulation speed. */
    @FXML
    private ComboBox<String> speedCombo;
    
    /** Text field for entering temperature. */
    @FXML
    private TextField temperatureField;
    
    /** Text field for entering humidity. */
    @FXML
    private TextField humidityField;
    
    /** VBox container for rules checkboxes. */
    @FXML
    private VBox rulesCheckBox;
    
    /** Button to start simulation. */
    @FXML
    private Button startBtn;
    
    /** Button to pause simulation. */
    @FXML
    private Button pauseBtn;
    
    /** Button to reset simulation. */
    @FXML
    private Button resetBtn;
    
    /** Slider to control simulation progress. */
    @FXML
    private Slider progressSlider;
    
    /** Label displaying current simulation progress. */
    @FXML
    private Label progressLabel;

    /** Label displaying simulation summary. */
    @FXML
    private Label summaryLabel;
    
    /** Text area for simulation logs. */
    @FXML
    private TextArea logOutput;

    /** Table view displaying simulated device states. */
    @FXML
    private TableView<SimulationDeviceState> simulatedDevicesTable;

    /** Column displaying device names. */
    @FXML
    private TableColumn<SimulationDeviceState, String> simDeviceColumn;

    /** Column displaying room names. */
    @FXML
    private TableColumn<SimulationDeviceState, String> simRoomColumn;

    /** Column displaying device types. */
    @FXML
    private TableColumn<SimulationDeviceState, String> simTypeColumn;

    /** Column displaying current device state. */
    @FXML
    private TableColumn<SimulationDeviceState, String> simStateColumn;

    /** Column displaying last change timestamp. */
    @FXML
    private TableColumn<SimulationDeviceState, String> simLastChangedColumn;
    
    /** Rule service instance. */
    private final MockRuleService ruleService = MockRuleService.getInstance();
    /** Simulation service instance. */
    private final MockSimulationService simulationService = MockSimulationService.getInstance();
    /** Observable list of simulated device states. */
    private final ObservableList<SimulationDeviceState> simulatedDeviceStates = FXCollections.observableArrayList();

    /** Timeline for simulation playback animation. */
    private Timeline playbackTimeline;
    /** Current simulation plan being executed. */
    private SimulationPlan currentPlan;
    /** Configured start time for the simulation. */
    private LocalTime configuredStartTime;
    /** Index of the current event in the simulation. */
    private int currentEventIndex;
    /** Flag indicating if simulation is currently running. */
    private boolean isRunning = false;
    
    @FXML
    private void initialize() {
        speedCombo.getItems().addAll("1x (Real-time)", "10x", "100x");
        speedCombo.setValue("10x");
        startTimeSpinner.setText("06:00:00");
        temperatureField.setText("20");
        humidityField.setText("50");
        progressSlider.setDisable(true);

        simDeviceColumn.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        simRoomColumn.setCellValueFactory(new PropertyValueFactory<>("room"));
        simTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        simStateColumn.setCellValueFactory(new PropertyValueFactory<>("state"));
        simLastChangedColumn.setCellValueFactory(new PropertyValueFactory<>("lastChanged"));
        simulatedDevicesTable.setItems(simulatedDeviceStates);

        populateRuleSelection();
        summaryLabel.setText("Configure the start time, initial sensors, and active rules. The replay uses a cloned device snapshot and never changes live devices.");
    }

    private void populateRuleSelection() {
        rulesCheckBox.getChildren().clear();
        for (Rule rule : ruleService.getRules()) {
            CheckBox checkBox = new CheckBox(rule.getName() + " (" + rule.getTriggerType() + ")");
            checkBox.setUserData(rule);
            checkBox.setSelected(rule.isEnabled());
            checkBox.setDisable(!rule.isEnabled());
            rulesCheckBox.getChildren().add(checkBox);
        }
    }
    
    @FXML
    private void handleStart() {
        if (!isRunning) {
            boolean shouldPrepare = currentPlan == null || currentEventIndex >= currentPlan.events().size();
            if (shouldPrepare) {
                try {
                    prepareSimulation();
                } catch (IllegalArgumentException exception) {
                    logOutput.appendText("[SIM] " + exception.getMessage() + "\n");
                    summaryLabel.setText(exception.getMessage());
                    shouldPrepare = false;
                }
            }
            
            if (shouldPrepare || currentPlan != null) {
                if (playbackTimeline != null) {
                    playbackTimeline.play();
                }
                isRunning = true;
                startBtn.setDisable(true);
                pauseBtn.setDisable(false);
                summaryLabel.setText("Running a full-day replay in accelerated time. Live devices remain unchanged.");
                logOutput.appendText("[SIM] Simulation started\n");
            }
        }
    }
    
    @FXML
    private void handlePause() {
        if (isRunning) {
            if (playbackTimeline != null) {
                playbackTimeline.pause();
            }
            isRunning = false;
            startBtn.setDisable(false);
            pauseBtn.setDisable(true);
            logOutput.appendText("[SIM] Simulation paused\n");
        }
    }
    
    @FXML
    private void handleReset() {
        if (playbackTimeline != null) {
            playbackTimeline.stop();
        }
        isRunning = false;
        currentPlan = null;
        currentEventIndex = 0;
        startBtn.setDisable(false);
        pauseBtn.setDisable(true);
        progressSlider.setValue(0);
        progressLabel.setText("00:00:00");
        summaryLabel.setText("Simulation reset. Configure a new day replay without affecting the live system.");
        simulatedDeviceStates.clear();
        logOutput.clear();
        logOutput.appendText("[SIM] Simulation reset\n");
    }

    private void prepareSimulation() {
        configuredStartTime = simulationService.parseStartTime(startTimeSpinner.getText());
        double initialTemperature = parseDoubleField(temperatureField.getText(), "temperature");
        double initialHumidity = parseDoubleField(humidityField.getText(), "humidity");
        List<Rule> selectedRules = getSelectedRules();

        currentPlan = simulationService.buildPlan(new SimulationConfiguration(
                configuredStartTime,
                initialTemperature,
                initialHumidity,
                selectedRules,
                resolveSpeedMultiplier()
        ));

        simulatedDeviceStates.setAll(currentPlan.simulatedDeviceStates());
        currentEventIndex = 0;
        progressSlider.setValue(0);
        progressLabel.setText(configuredStartTime.toString());
        logOutput.clear();
        logOutput.appendText("[SIM] Prepared replay for " + selectedRules.size() + " active rule(s)\n");
        logOutput.appendText("[SIM] Start time " + configuredStartTime + ", temperature " + initialTemperature + "°C, humidity " + initialHumidity + "%\n");

        if (currentPlan.events().isEmpty()) {
            summaryLabel.setText("No device state changes would be triggered under the selected start time, sensor values, and active rules.");
            logOutput.appendText("[SIM] No state changes scheduled for this replay\n");
        } else {
            summaryLabel.setText("Prepared " + currentPlan.events().size() + " simulated device state change(s) for the accelerated replay.");
        }

        playbackTimeline = new Timeline(new KeyFrame(Duration.millis(resolvePlaybackMillis()), event -> playNextSimulationEvent()));
        playbackTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void playNextSimulationEvent() {
        if (currentPlan != null && currentEventIndex < currentPlan.events().size()) {
            SimulationEvent event = currentPlan.events().get(currentEventIndex);
            simulationService.applyEvent(simulatedDeviceStates, event);
            progressLabel.setText(event.simulatedTime().toString());
            progressSlider.setValue(resolveProgress(event.simulatedTime()));
            logOutput.appendText(String.format("[SIM %s] %s -> %s (%s)\n",
                    event.simulatedTime(),
                    event.deviceName(),
                    event.resultingState(),
                    event.triggerSource()));
            currentEventIndex++;
        }
        
        if (currentPlan == null || currentEventIndex >= currentPlan.events().size()) {
            finishSimulation();
        }
    }

    private void finishSimulation() {
        if (playbackTimeline != null) {
            playbackTimeline.stop();
        }
        isRunning = false;
        startBtn.setDisable(false);
        pauseBtn.setDisable(true);
        progressSlider.setValue(100);
        progressLabel.setText(configuredStartTime.plusHours(23).plusMinutes(59).truncatedTo(ChronoUnit.SECONDS).toString());
        summaryLabel.setText("Simulation complete. Replay finished without changing the live system.");
        logOutput.appendText("[SIM] Simulation finished\n");
    }

    private List<Rule> getSelectedRules() {
        List<Rule> selectedRules = new ArrayList<>();
        for (javafx.scene.Node child : rulesCheckBox.getChildren()) {
            if (child instanceof CheckBox checkBox && checkBox.isSelected() && checkBox.getUserData() instanceof Rule rule) {
                selectedRules.add(rule);
            }
        }
        return selectedRules;
    }

    private double parseDoubleField(String value, String label) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Please enter a valid numeric " + label + " value");
        }
    }

    private int resolveSpeedMultiplier() {
        String selected = speedCombo.getValue();
        return (selected != null && selected.startsWith("100x")) ? 100 : 
               (selected != null && selected.startsWith("10x")) ? 10 : 1;
    }

    private double resolvePlaybackMillis() {
        return switch (resolveSpeedMultiplier()) {
            case 100 -> 90;
            case 10 -> 300;
            default -> 900;
        };
    }

    private double resolveProgress(LocalTime eventTime) {
        long startSecond = configuredStartTime.toSecondOfDay();
        long eventSecond = eventTime.toSecondOfDay();
        long elapsed = eventSecond >= startSecond
                ? eventSecond - startSecond
                : (24 * 60 * 60L - startSecond) + eventSecond;
        return Math.min(100.0, (elapsed / (24d * 60d * 60d)) * 100d);
    }
}
