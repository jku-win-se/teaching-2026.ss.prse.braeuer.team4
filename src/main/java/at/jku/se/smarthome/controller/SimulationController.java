package at.jku.se.smarthome.controller;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import at.jku.se.smarthome.model.Rule;
import at.jku.se.smarthome.model.SimulationDeviceState;
import at.jku.se.smarthome.service.api.RuleService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
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
@SuppressWarnings({
        "PMD.AtLeastOneConstructor",
        "PMD.UnusedPrivateMethod",
        "PMD.TooManyMethods",
        "PMD.TooManyFields",
        "PMD.GodClass",
        "PMD.CouplingBetweenObjects"
})
public class SimulationController {

    /** Default simulation start time shown in the UI. */
    private static final String DEFAULT_START_TIME = "06:00:00";
    /** Default temperature shown in the UI. */
    private static final String DEFAULT_TEMPERATURE = "20";
    /** Default humidity shown in the UI. */
    private static final String DEFAULT_HUMIDITY = "50";
    /** Default replay speed preset shown in the UI. */
    private static final String DEFAULT_SPEED = "x10";
    /** Replay speed preset for three seconds per simulated hour. */
    private static final String SPEED_X100 = "x100";
    /** Replay speed preset for one second per simulated hour. */
    private static final String SPEED_X300 = "x300";
    /** End label shown after the replay reaches the next day. */
    private static final String END_OF_DAY_LABEL = "00:00 (next day)";
    /** Trigger source label used for the synthetic day-end marker event. */
    private static final String SIMULATION_DAY_END_SOURCE = "Simulation Day End";
    /** UI timer interval for smooth replay updates. */
    private static final int PLAYBACK_TICK_MILLIS = 100;
    /** Number of simulated seconds in one replay day. */
    private static final double DAY_SECONDS = 24d * 60d * 60d;
    /** Time formatter for the live replay clock. */
    private static final DateTimeFormatter CLOCK_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

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
    private final RuleService ruleService = ServiceRegistry.getRuleService();
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
    private boolean isRunning;
    /** Elapsed simulated seconds since the configured start time. */
    private double elapsedSimulatedSeconds;
    
    @FXML
    private void initialize() {
        speedCombo.getItems().addAll(DEFAULT_SPEED, SPEED_X100, SPEED_X300);
        speedCombo.setValue(DEFAULT_SPEED);
        startTimeSpinner.setText(DEFAULT_START_TIME);
        temperatureField.setText(DEFAULT_TEMPERATURE);
        humidityField.setText(DEFAULT_HUMIDITY);
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

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
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

            boolean canStart = shouldPrepare || currentPlan != null;
            if (canStart && playbackTimeline != null) {
                processDueSimulationEvents();
                playbackTimeline.play();
            }
            if (canStart) {
                isRunning = true;
                startBtn.setDisable(true);
                pauseBtn.setDisable(false);
                resetBtn.setDisable(false);
                setConfigurationLocked(true);
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
            resetBtn.setDisable(false);
            setConfigurationLocked(true);
            logOutput.appendText("[SIM] Simulation paused\n");
        }
    }
    
    @FXML
    @SuppressWarnings("PMD.NullAssignment")
    private void handleReset() {
        if (playbackTimeline != null) {
            playbackTimeline.stop();
        }
        isRunning = false;
        currentPlan = null;
        currentEventIndex = 0;
        elapsedSimulatedSeconds = 0;
        startBtn.setDisable(false);
        pauseBtn.setDisable(true);
        resetBtn.setDisable(false);
        progressSlider.setValue(0);
        progressLabel.setText(DEFAULT_START_TIME);
        summaryLabel.setText("Simulation restarted. Configure a new day replay without affecting the live system.");
        startTimeSpinner.setText(DEFAULT_START_TIME);
        speedCombo.setValue(DEFAULT_SPEED);
        temperatureField.setText(DEFAULT_TEMPERATURE);
        humidityField.setText(DEFAULT_HUMIDITY);
        populateRuleSelection();
        setConfigurationLocked(false);
        simulatedDeviceStates.clear();
        logOutput.clear();
        logOutput.appendText("[SIM] Simulation restarted\n");
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
                1
        ));

        simulatedDeviceStates.setAll(currentPlan.simulatedDeviceStates());
        currentEventIndex = 0;
        elapsedSimulatedSeconds = 0;
        progressSlider.setValue(0);
        progressLabel.setText(configuredStartTime.format(CLOCK_FORMATTER));
        logOutput.clear();
        logOutput.appendText("[SIM] Prepared replay for " + selectedRules.size() + " active rule(s)\n");
        logOutput.appendText("[SIM] Start time " + configuredStartTime + ", temperature " + initialTemperature + "°C, humidity " + initialHumidity + "%\n");

        if (currentPlan.events().isEmpty()) {
            summaryLabel.setText("No device state changes would be triggered under the selected start time, sensor values, and active rules.");
            logOutput.appendText("[SIM] No state changes scheduled for this replay\n");
        } else {
            summaryLabel.setText("Prepared " + currentPlan.events().size() + " simulated device state change(s) for the accelerated replay.");
        }

        playbackTimeline = new Timeline(new KeyFrame(Duration.millis(PLAYBACK_TICK_MILLIS), event -> advanceSimulationClock()));
        playbackTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void advanceSimulationClock() {
        if (currentPlan == null) {
            finishSimulation();
            return;
        }

        elapsedSimulatedSeconds = Math.min(DAY_SECONDS, elapsedSimulatedSeconds + resolveSimulatedSecondsPerTick());
        updateReplayClock();
        processDueSimulationEvents();

        if (elapsedSimulatedSeconds >= DAY_SECONDS || currentEventIndex >= currentPlan.events().size()) {
            finishSimulation();
        }
    }

    private void processDueSimulationEvents() {
        while (currentPlan != null && currentEventIndex < currentPlan.events().size()) {
            SimulationEvent event = currentPlan.events().get(currentEventIndex);
            if (resolveEventElapsedSeconds(event) > elapsedSimulatedSeconds) {
                break;
            }
            if (!SIMULATION_DAY_END_SOURCE.equals(event.triggerSource())) {
                simulationService.applyEvent(simulatedDeviceStates, event);
                logOutput.appendText(String.format("[SIM %s] %s -> %s (%s)\n",
                        event.simulatedTime(),
                        event.deviceName(),
                        event.resultingState(),
                        event.triggerSource()));
            }
            currentEventIndex++;
        }
    }

    private void updateReplayClock() {
        progressSlider.setValue(Math.min(100.0, elapsedSimulatedSeconds / DAY_SECONDS * 100.0));
        if (elapsedSimulatedSeconds >= DAY_SECONDS) {
            progressLabel.setText(END_OF_DAY_LABEL);
        } else {
            LocalTime replayTime = configuredStartTime.plusSeconds((long) elapsedSimulatedSeconds);
            progressLabel.setText(replayTime.format(CLOCK_FORMATTER));
        }
    }

    private void finishSimulation() {
        if (playbackTimeline != null) {
            playbackTimeline.stop();
        }
        isRunning = false;
        startBtn.setDisable(true);
        pauseBtn.setDisable(true);
        resetBtn.setDisable(false);
        progressSlider.setValue(100);
        progressLabel.setText(END_OF_DAY_LABEL);
        summaryLabel.setText("Simulation complete. Day replay is frozen for review. Use Restart Simulation for a new run.");
        setConfigurationLocked(true);
        if (!logOutput.getText().endsWith("[SIM] Simulation finished\n")) {
            logOutput.appendText("[SIM] Simulation finished\n");
        }
    }

    private void setConfigurationLocked(boolean locked) {
        startTimeSpinner.setDisable(locked);
        speedCombo.setDisable(false);
        temperatureField.setDisable(locked);
        humidityField.setDisable(locked);
        for (javafx.scene.Node child : rulesCheckBox.getChildren()) {
            child.setDisable(locked);
        }
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
            throw new IllegalArgumentException("Please enter a valid numeric " + label + " value", exception);
        }
    }

    private double resolveSimulatedSecondsPerTick() {
        return 3600.0 * PLAYBACK_TICK_MILLIS / (resolveSecondsPerSimulatedHour() * 1000.0);
    }

    private int resolveSecondsPerSimulatedHour() {
        String selected = speedCombo.getValue();
        int secondsPerHour = 10;
        if (selected == null) {
            secondsPerHour = 10;
        } else if (SPEED_X100.equals(selected)) {
            secondsPerHour = 3;
        } else if (SPEED_X300.equals(selected)) {
            secondsPerHour = 1;
        }
        return secondsPerHour;
    }

    private long resolveElapsedSeconds(LocalTime eventTime) {
        long startSecond = configuredStartTime.toSecondOfDay();
        long eventSecond = eventTime.toSecondOfDay();
        return eventSecond >= startSecond
                ? eventSecond - startSecond
                : 24 * 60 * 60L - startSecond + eventSecond;
    }

    private long resolveEventElapsedSeconds(SimulationEvent event) {
        long elapsedSeconds = resolveElapsedSeconds(event.simulatedTime());
        if (SIMULATION_DAY_END_SOURCE.equals(event.triggerSource())) {
            elapsedSeconds = (long) DAY_SECONDS;
        }
        return elapsedSeconds;
    }
}
