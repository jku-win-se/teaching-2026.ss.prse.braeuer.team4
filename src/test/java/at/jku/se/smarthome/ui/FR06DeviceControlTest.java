package at.jku.se.smarthome.ui;

import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.mock.MockLogService;
import at.jku.se.smarthome.service.mock.MockRoomService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

/**
 * FR-06: The system shall allow users to manually control a device.
 *
 * <p>Harness engineering demo: these TestFX tests drive the real JavaFX UI,
 * click controls, and assert state changes — producing machine-readable
 * pass/fail output that an AI agent can consume to verify device control works.
 *
 * <p>Device layout from MockRoomService (in order):
 * <ul>
 *   <li>Main Light  — Switch, Living Room, starts ON  (dev-001)</li>
 *   <li>Dimmer Light — Dimmer, Living Room, starts ON (dev-002)</li>
 *   <li>Bed Light    — Switch, Bedroom,    starts OFF (dev-003)</li>
 * </ul>
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods",
        "PMD.MethodNamingConventions", "PMD.UnitTestContainsTooManyAsserts",
        "PMD.CommentRequired", "PMD.TooManyStaticImports"})
public class FR06DeviceControlTest extends ApplicationTest {

    /**
     * Skips the test if running on CI (headless environment).
     */
    @org.junit.BeforeClass
    public static void skipIfHeadless() {
        assumeTrue("Skipping UI test on CI (headless)", System.getenv("CI") == null);
    }

    @Override
    public void start(Stage stage) throws Exception {
        // Inject mock services BEFORE FXML loads (controllers read ServiceRegistry at field init)
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
     * FR-06: Clicking the toggle on a device that is ON turns it OFF.
     * Main Light starts ON (selected=true, text="ON") and must become OFF after one click.
     */
    @Test
    public void switchDevice_mainLight_onToOff() {
        ToggleButton toggle = findToggleFor("Main Light");

        assertTrue("Pre-condition: Main Light must start ON", toggle.isSelected());
        assertEquals("Pre-condition: toggle text must be ON", "ON", toggle.getText());

        // interact() executes on the JavaFX Application Thread — more reliable than
        // clickOn() inside a ScrollPane where screen-coordinate translation can fail
        interact(toggle::fire);

        assertFalse("Main Light must be OFF after toggle", toggle.isSelected());
        assertEquals("Toggle text must update to OFF", "OFF", toggle.getText());
    }

    /**
     * FR-06: Clicking the toggle on a device that is OFF turns it ON.
     * Bed Light starts OFF (selected=false, text="OFF") and must become ON after one click.
     */
    @Test
    public void switchDevice_bedLight_offToOn() {
        ToggleButton toggle = findToggleFor("Bed Light");

        assertFalse("Pre-condition: Bed Light must start OFF", toggle.isSelected());
        assertEquals("Pre-condition: toggle text must be OFF", "OFF", toggle.getText());

        interact(toggle::fire);

        assertTrue("Bed Light must be ON after toggle", toggle.isSelected());
        assertEquals("Toggle text must update to ON", "ON", toggle.getText());
    }

    /**
     * Navigates from a device name label to its card's ToggleButton.
     *
     * <p>Device cards have no fx:id (generated dynamically). The lookup chain is:
     * device name Label → HBox header → VBox card → scoped .toggle-button lookup.
     *
     * @param deviceName the display name of the device (e.g. "Main Light")
     * @return the ToggleButton in that device's card
     */
    private ToggleButton findToggleFor(String deviceName) {
        Node nameLabel = lookup(hasText(deviceName)).query();
        // nameLabel.parent = HBox (header), HBox.parent = VBox (card)
        VBox card = (VBox) nameLabel.getParent().getParent();
        return from(card).lookup(".toggle-button").queryAs(ToggleButton.class);
    }
}
