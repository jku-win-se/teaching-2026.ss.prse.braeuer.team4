package at.jku.se.smarthome.ui;

import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.mock.MockLogService;
import at.jku.se.smarthome.service.mock.MockRoomService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

/**
 * FR-07: The system shall maintain and display the current state of each device
 * in the user interface in real-time.
 *
 * <p>These tests verify that:
 * <ol>
 *   <li>The state label in each device card shows the correct initial state.</li>
 *   <li>The state label updates <em>immediately</em> when the device is toggled,
 *       driven by JavaFX property listeners — no manual refresh required.</li>
 * </ol>
 *
 * <p>Test 1 only reads (no click), so it is independent of Test 2 which clicks.
 * Different devices are used to avoid cross-test interference.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods",
        "PMD.MethodNamingConventions", "PMD.UnitTestContainsTooManyAsserts",
        "PMD.CommentRequired"})
public class FR07DeviceStateTest extends ApplicationTest {

    /**
     * Skips the test if running on CI (headless environment).
     */
    @org.junit.BeforeClass
    public static void skipIfHeadless() {
        assumeTrue("Skipping UI test on CI (headless)", System.getenv("CI") == null);
    }

    @Override
    public void start(Stage stage) throws Exception {
        MockRoomService.resetForTesting();
        MockLogService.resetForTesting();
        ServiceRegistry.setRoomServiceForTesting(MockRoomService.getInstance());
        ServiceRegistry.setLogServiceForTesting(MockLogService.getInstance());

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/at/jku/se/smarthome/view/devices-control-view.fxml")
        );
        stage.setScene(new Scene(loader.load(), 900, 700));
        stage.show();
    }

    /**
     * FR-07: State label shows the correct initial state without any interaction.
     * Bed Light starts OFF — its state label must read "State: OFF" on load.
     * (Uses Bed Light so this test does not interfere with Test 2 which uses Main Light.)
     */
    @Test
    public void stateLabel_showsCorrectInitialState_bedLightStartsOff() {
        Label stateLabel = findStateLabelFor("Bed Light");

        assertNotNull("State label must be present in device card", stateLabel);
        assertEquals("Bed Light starts OFF — state label must reflect this",
                "State: OFF", stateLabel.getText());
    }

    /**
     * FR-07: State label updates in real-time when the device is toggled.
     *
     * <p>Main Light starts ON. After clicking its toggle, the state label must
     * change to "State: OFF" immediately — driven by the JavaFX property listener
     * in DevicesController without any manual UI refresh.
     */
    @Test
    public void stateLabel_updatesRealTimeAfterToggle_mainLightOnToOff() {
        Label stateLabel = findStateLabelFor("Main Light");
        ToggleButton toggle = findToggleFor("Main Light");

        assertEquals("Pre-condition: Main Light state label starts as ON",
                "State: ON", stateLabel.getText());

        interact(toggle::fire);

        // The state label is bound via device.stateProperty() listener —
        // it must update synchronously on the FX thread without any manual refresh
        assertEquals("State label must reflect new state immediately after toggle",
                "State: OFF", stateLabel.getText());
    }

    /**
     * Finds the "State: ..." label inside a device card by device name.
     * The label is identified by its text prefix "State:".
     */
    private Label findStateLabelFor(String deviceName) {
        Node nameLabel = lookup(hasText(deviceName)).query();
        VBox card = (VBox) nameLabel.getParent().getParent();
        return from(card)
                .lookup((Node n) -> n instanceof Label && ((Label) n).getText().startsWith("State:"))
                .queryAs(Label.class);
    }

    /**
     * Finds the ToggleButton inside a device card by device name.
     */
    private ToggleButton findToggleFor(String deviceName) {
        Node nameLabel = lookup(hasText(deviceName)).query();
        VBox card = (VBox) nameLabel.getParent().getParent();
        return from(card).lookup(".toggle-button").queryAs(ToggleButton.class);
    }
}
