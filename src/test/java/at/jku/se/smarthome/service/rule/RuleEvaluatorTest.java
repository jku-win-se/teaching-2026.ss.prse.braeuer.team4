package at.jku.se.smarthome.service.rule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Rule;

/**
 * Unit tests for RuleEvaluator.
 *
 * No mocks needed: RuleEvaluator is stateless and accepts plain model objects.
 */
public class RuleEvaluatorTest {

    /** Rule evaluator under test. */
    private RuleEvaluator evaluator;

    /**
     * Sets up test fixtures before each test.
     */
    @Before
    public void setUp() {
        evaluator = new RuleEvaluator();
    }

    // ── Device State trigger ────────────────────────────────────────────────

    /**
     * Test: device state active trigger returns true when device is on.
     */
    @Test
    public void deviceStateActiveDeviceIsOnReturnsTrue() {
        Rule rule = rule("Device State", "State = Active");
        Device device = device(true);
        assertTrue(evaluator.evaluate(rule, device));
    }

    /**
     * Test: device state active trigger returns false when device is off.
     */
    @Test
    public void deviceStateActiveDeviceIsOffReturnsFalse() {
        Rule rule = rule("Device State", "State = Active");
        Device device = device(false);
        assertFalse(evaluator.evaluate(rule, device));
    }

    /**
     * Test: device state inactive trigger returns true when device is off.
     */
    @Test
    public void deviceStateInactiveDeviceIsOffReturnsTrue() {
        Rule rule = rule("Device State", "State = Inactive");
        Device device = device(false);
        assertTrue(evaluator.evaluate(rule, device));
    }

    /**
     * Test: device state inactive trigger returns false when device is on.
     */
    @Test
    public void deviceStateInactiveDeviceIsOnReturnsFalse() {
        Rule rule = rule("Device State", "State = Inactive");
        Device device = device(true);
        assertFalse(evaluator.evaluate(rule, device));
    }

    /**
     * Test: device state trigger returns false for null device.
     */
    @Test
    public void deviceStateNullDeviceReturnsFalse() {
        Rule rule = rule("Device State", "State = Active");
        assertFalse(evaluator.evaluate(rule, null));
    }

    // ── Sensor Threshold trigger ────────────────────────────────────────────

    /**
     * Test: sensor threshold greater-than returns true when temperature is above.
     */
    @Test
    public void sensorThresholdGreaterThanTemperatureAboveReturnsTrue() {
        Rule rule = rule("Sensor Threshold", "Value > 20");
        Device device = deviceWithTemperature(25.0);
        assertTrue(evaluator.evaluate(rule, device));
    }

    /**
     * Test: sensor threshold greater-than returns false when temperature is below.
     */
    @Test
    public void sensorThresholdGreaterThanTemperatureBelowReturnsFalse() {
        Rule rule = rule("Sensor Threshold", "Value > 20");
        Device device = deviceWithTemperature(15.0);
        assertFalse(evaluator.evaluate(rule, device));
    }

    /**
     * Test: sensor threshold less-than returns true when temperature is below.
     */
    @Test
    public void sensorThresholdLessThanTemperatureBelowReturnsTrue() {
        Rule rule = rule("Sensor Threshold", "Value < 18");
        Device device = deviceWithTemperature(15.0);
        assertTrue(evaluator.evaluate(rule, device));
    }

    /**
     * Test: sensor threshold less-than-or-equal returns true on exact match.
     */
    @Test
    public void sensorThresholdLessThanOrEqualExactMatchReturnsTrue() {
        Rule rule = rule("Sensor Threshold", "Value <= 20");
        Device device = deviceWithTemperature(20.0);
        assertTrue(evaluator.evaluate(rule, device));
    }

    /**
     * Test: sensor threshold greater-than-or-equal returns true on exact match.
     */
    @Test
    public void sensorThresholdGreaterThanOrEqualExactMatchReturnsTrue() {
        Rule rule = rule("Sensor Threshold", "Value >= 22");
        Device device = deviceWithTemperature(22.0);
        assertTrue(evaluator.evaluate(rule, device));
    }

    /**
     * Test: sensor threshold returns false for null device.
     */
    @Test
    public void sensorThresholdNullDeviceReturnsFalse() {
        Rule rule = rule("Sensor Threshold", "Value > 20");
        assertFalse(evaluator.evaluate(rule, null));
    }

    /**
     * Test: malformed condition returns false.
     */
    @Test
    public void sensorThresholdMalformedConditionReturnsFalse() {
        Rule rule = rule("Sensor Threshold", "temp > hot");
        Device device = deviceWithTemperature(25.0);
        assertFalse(evaluator.evaluate(rule, device));
    }

    // ── Guard cases ─────────────────────────────────────────────────────────
    /**
     * Test: null rule returns false.
     */    @Test
    public void nullRuleReturnsFalse() {
        assertFalse(evaluator.evaluate(null, device(true)));
    }

    /**
     * Test: unknown trigger type returns false.
     */
    @Test
    public void unknownTriggerTypeReturnsFalse() {
        Rule rule = rule("Unknown", "anything");
        assertFalse(evaluator.evaluate(rule, device(true)));
    }

    // ─ Helpers ─────────────────────────────────────────────────────────────

    /**
     * Helper: creates a rule for testing.
     *
     * @param triggerType the trigger type
     * @param condition the condition
     * @return the test rule
     */
    private Rule rule(String triggerType, String condition) {
        return new Rule("t-id", "Test Rule", triggerType, "Source", condition,
                "Turn On", "Target", true, "Active");
    }

    /**
     * Helper: creates a device with specified state.
     *
     * @param state the device state
     * @return the test device
     */
    private Device device(boolean state) {
        return new Device("d-id", "Test Device", "Switch", "Room", state);
    }

    /**
     * Helper: creates a device with specified temperature.
     *
     * @param temperature the temperature value
     * @return the test device
     */
    private Device deviceWithTemperature(double temperature) {
        Device d = new Device("d-id", "Sensor", "Thermostat", "Room", true);
        d.setTemperature(temperature);
        return d;
    }
}
