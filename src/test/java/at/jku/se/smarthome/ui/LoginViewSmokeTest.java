package at.jku.se.smarthome.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Harness engineering demo: AI-readable UI smoke test for the login screen.
 *
 * <p>This test demonstrates the feedback loop concept from harness engineering:
 * instead of a human manually verifying the UI, an AI agent can run this test,
 * read the pass/fail output, fix broken code, and re-run — all without human
 * involvement in the verification step.
 *
 * <p>Requires a real display (run locally on macOS, not on headless CI).
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods",
        "PMD.MethodNamingConventions", "PMD.UnitTestContainsTooManyAsserts",
        "PMD.CommentRequired"})
public class LoginViewSmokeTest extends ApplicationTest {

    /**
     * Skips the test if running on CI (headless environment).
     */
    @org.junit.BeforeClass
    public static void skipIfHeadless() {
        assumeTrue("Skipping UI test on CI (headless)", System.getenv("CI") == null);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/at/jku/se/smarthome/view/login-view.fxml")
        );
        stage.setScene(new Scene(loader.load()));
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        org.testfx.api.FxToolkit.hideStage();
    }

    @Test
    public void loginFormRendersAllRequiredControls() {
        TextField emailField = lookup("#emailField").query();
        PasswordField passwordField = lookup("#passwordField").query();
        Button loginButton = lookup("#loginButton").query();

        assertNotNull("Email field must be present", emailField);
        assertNotNull("Password field must be present", passwordField);
        assertNotNull("Login button must be present", loginButton);

        assertTrue("Login button must be enabled initially", !loginButton.isDisabled());
        assertTrue("Email field must be editable", !emailField.isDisabled());
        assertTrue("Password field must be editable", !passwordField.isDisabled());
    }

    @Test
    public void emptyCredentialsDoNotCrashApp() {
        // Simulate what AI agent would check: clicking login with empty fields
        // must not crash or leave the UI in a broken state
        clickOn("#loginButton");

        // After the click, the login button must still be present and interactive
        Button loginButton = lookup("#loginButton").query();
        assertNotNull("Login button must still exist after failed attempt", loginButton);
        assertFalse("Login button must be re-enabled after failed attempt", loginButton.isDisabled());
    }

    @Test
    public void emailFieldAcceptsInput() {
        TextField emailField = lookup("#emailField").query();
        interact(() -> emailField.setText("test@example.com"));
        assertTrue("Email field must contain typed text",
                emailField.getText().contains("test@example.com"));
    }
}
