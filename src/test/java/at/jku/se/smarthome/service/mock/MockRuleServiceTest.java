package at.jku.se.smarthome.service.mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import at.jku.se.smarthome.model.Rule;

/**
 * Unit tests for rule-level conflict detection in the mock service.
 */
@SuppressWarnings("PMD.AtLeastOneConstructor")
public class MockRuleServiceTest {

    /**
     * Resets the mock service before each test.
     */
    @Before
    public void setup() {
        MockRuleService.resetForTesting();
    }

    /**
     * Tests that conflicts are detected when opposite actions (Turn On/Off) are scheduled for the same time and device.
     */
    @Ignore("Fails in CI due to missing Notification database configuration")
    @Test
    public void testDetectsConflictForOppositeSwitchActions() {
        MockRuleService svc = MockRuleService.getInstance();
        svc.addRule("Morning Off", "Time", "Clock", "06:00 AM", "Turn Off", "Main Light");
        Rule rule2 = svc.addRule("Morning On", "Time", "Clock", "06:00 AM", "Turn On", "Main Light");

        // Expect the second rule to conflict with the first
        assertTrue("Expected conflict detected for opposite switch actions", svc.hasConflicts(rule2.getId()));
    }

    /**
     * Tests that no conflict is detected when actions are scheduled for different times.
     */
    @Ignore("Fails in CI due to missing Notification database configuration")
    @Test
    public void testNoConflictForDifferentTime() {
        MockRuleService svc = MockRuleService.getInstance();
        svc.addRule("Morning Off", "Time", "Clock", "06:00 AM", "Turn Off", "Main Light");
        Rule rule2 = svc.addRule("Later Off", "Time", "Clock", "07:00 AM", "Turn Off", "Main Light");

        assertFalse("No conflict expected for different time", svc.hasConflicts(rule2.getId()));
    }
}
