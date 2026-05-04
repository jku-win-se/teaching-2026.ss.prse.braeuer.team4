package at.jku.se.smarthome.ui;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.mock.MockRoomService;
import at.jku.se.smarthome.service.mock.MockRuleService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

/**
 * FR-10: The system shall provide a rule engine that evaluates condition-action rules.
 *
 * <p>These tests drive the real JavaFX automation view to verify that the rule engine
 * correctly gates actions behind condition evaluation:
 * <ul>
 *   <li>When the condition IS met, the action fires and the target device state changes.</li>
 *   <li>When the condition is NOT met, the action is blocked and the device state is unchanged.</li>
 * </ul>
 *
 * <p>Test rule: IF Motion Sensor active (State = Active) THEN Turn On Main Light.
 * Motion Sensor starts ON; Main Light starts OFF so we can observe the state change.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods",
        "PMD.MethodNamingConventions", "PMD.UnitTestContainsTooManyAsserts",
        "PMD.CommentRequired"})
public class FR10RuleEngineTest extends ApplicationTest {

    private MockRoomService roomService;

    /**
     * Skips the test if running on CI (headless environment).
     */
    @org.junit.BeforeClass
    public static void skipIfHeadless() {
        assumeTrue("Skipping UI test on CI (headless)", System.getenv("CI") == null);
    }

    @Override
    public void start(Stage stage) throws Exception {
        // Reset singletons in dependency order before loading FXML
        MockRoomService.resetForTesting();
        MockRuleService.resetForTesting();

        roomService = MockRoomService.getInstance();
        MockRuleService ruleService = MockRuleService.getInstance();

        ServiceRegistry.setRoomServiceForTesting(roomService);
        ServiceRegistry.setRuleServiceForTesting(ruleService);

        // Pre-condition: Main Light starts OFF so state-change is observable
        roomService.getDeviceByName("Main Light").setState(false);

        // Add the FR-10 test rule: IF Motion Sensor active THEN Turn On Main Light
        ruleService.addRule(
                "Motion → Light",
                "Device State",
                "Motion Sensor",
                "State = Active",
                "Turn On",
                "Main Light"
        );

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/at/jku/se/smarthome/view/automation-view.fxml")
        );
        stage.setScene(new Scene(loader.load(), 1000, 600));
        stage.show();
    }

    /**
     * FR-10 happy path: Motion Sensor is ON → condition "State = Active" is met →
     * rule fires → Main Light turns ON.
     */
    @Test
    public void conditionMet_motionSensorOn_ruleFiresAndLightTurnsOn() {
        Device motionSensor = roomService.getDeviceByName("Motion Sensor");
        assertTrue("Pre-condition: Motion Sensor must be ON", motionSensor.getState());

        Device mainLight = roomService.getDeviceByName("Main Light");
        assertFalse("Pre-condition: Main Light must start OFF", mainLight.getState());

        clickRunFor("Motion → Light");

        assertTrue("Main Light must be ON after rule fires (condition was met)", mainLight.getState());
    }

    /**
     * FR-10 blocked path: Motion Sensor is turned OFF → condition "State = Active" is not met →
     * rule engine blocks the action → Main Light stays OFF.
     *
     * <p>This is the key FR-10 behavior: the IF part is actually evaluated.
     */
    @Test
    public void conditionNotMet_motionSensorOff_ruleBlockedAndLightStaysOff() {
        Device motionSensor = roomService.getDeviceByName("Motion Sensor");
        interact(() -> motionSensor.setState(false));
        assertFalse("Pre-condition: Motion Sensor must be OFF", motionSensor.getState());

        Device mainLight = roomService.getDeviceByName("Main Light");
        assertFalse("Pre-condition: Main Light must start OFF", mainLight.getState());

        clickRunFor("Motion → Light");

        assertFalse("Main Light must remain OFF — condition was not met, action was blocked",
                mainLight.getState());
    }

    /**
     * Finds the Run button in the table row whose name column shows the given rule name,
     * then fires it on the JavaFX Application Thread.
     *
     * <p>Lookup chain: rule name text → TableCell → TableRow → scoped .button lookup.
     *
     * @param ruleName the display name of the rule (e.g. "Motion → Light")
     */
    private void clickRunFor(String ruleName) {
        Node nameCell = lookup(hasText(ruleName)).query();
        // nameCell is a TableCell; its parent is the TableRow
        Node tableRow = nameCell.getParent();
        Button runButton = from(tableRow).lookup(".button")
                .queryAllAs(Button.class)
                .stream()
                .filter(b -> "Run".equals(b.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Run button not found for rule: " + ruleName));
        interact(runButton::fire);
    }
}
