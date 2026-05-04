package at.jku.se.smarthome.ui;

import at.jku.se.smarthome.model.LogEntry;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

/**
 * FR-08: The system shall record an activity log entry for every manual or
 * automated state change, including timestamp, device, and actor (user or rule).
 *
 * <p>These tests verify the full contract of FR-08:
 * <ul>
 *   <li>A new log entry is appended to the log when a device is toggled.</li>
 *   <li>The entry captures all required fields: timestamp, device, room, action, actor.</li>
 * </ul>
 *
 * <p>The pre-seeded mock log entries are cleared in {@code start()} so tests
 * can assert exact counts and are not confused by seed data.
 *
 * <p>Tests use different devices (Main Light / Bed Light) to avoid state interference:
 * either test can run first without affecting the other's preconditions.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods",
        "PMD.MethodNamingConventions", "PMD.UnitTestContainsTooManyAsserts",
        "PMD.CommentRequired", "PMD.TooManyStaticImports"})
public class FR08ActivityLogTest extends ApplicationTest {

    private MockLogService logService;

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

        this.logService = MockLogService.getInstance();
        // Clear pre-seeded entries so tests start with an empty log
        logService.getLogs().clear();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/at/jku/se/smarthome/view/devices-control-view.fxml")
        );
        stage.setScene(new Scene(loader.load(), 900, 700));
        stage.show();
    }

    /**
     * FR-08: Toggling a switch appends exactly one new log entry.
     * Uses Main Light (Switch, Living Room). Starts with empty log (cleared in start()).
     */
    @Test
    public void switchToggle_appendsExactlyOneLogEntry() {
        int countBefore = logService.getLogs().size(); // 0 after clear

        ToggleButton mainLight = findToggleFor("Main Light");
        interact(mainLight::fire);

        assertEquals("Exactly one log entry must be appended after a device toggle",
                countBefore + 1, logService.getLogs().size());
    }

    /**
     * FR-08: The log entry contains all required fields — timestamp, device, room,
     * action (describing the state change), and actor ("User" for manual actions).
     * Uses Bed Light (Switch, Bedroom) so this test is independent of Test 1.
     */
    @Test
    public void logEntry_containsAllRequiredFields_forManualToggle() {
        ToggleButton bedLight = findToggleFor("Bed Light");
        interact(bedLight::fire);

        // Log entries are prepended (index 0 = most recent)
        LogEntry entry = logService.getLogs().stream()
                .filter(e -> "Bed Light".equals(e.getDevice()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No log entry found for Bed Light after toggle"));

        // FR-08: timestamp must be present
        assertNotNull("Log entry must have a timestamp", entry.getTimestamp());
        assertFalse("Timestamp must not be empty", entry.getTimestamp().isEmpty());

        // FR-08: device and room must be recorded
        assertEquals("Log entry must record the correct device name", "Bed Light", entry.getDevice());
        assertEquals("Log entry must record the correct room", "Bedroom", entry.getRoom());

        // FR-08: action must describe the state change (ON or OFF)
        assertTrue("Action must describe the state transition (ON or OFF)",
                entry.getAction().contains("ON") || entry.getAction().contains("OFF"));

        // FR-08: actor must identify who triggered the change (manual = "User")
        assertEquals("Manual toggle must record actor as 'User'", "User", entry.getActor());
    }

    /**
     * Finds the ToggleButton inside a device card identified by device name.
     */
    private ToggleButton findToggleFor(String deviceName) {
        Node nameLabel = lookup(hasText(deviceName)).query();
        VBox card = (VBox) nameLabel.getParent().getParent();
        return from(card).lookup(".toggle-button").queryAs(ToggleButton.class);
    }
}
