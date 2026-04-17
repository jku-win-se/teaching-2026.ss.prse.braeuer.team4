package at.jku.se.smarthome.service.rule;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Rule;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for RuleEvaluator.
 *
 * No mocks needed: RuleEvaluator is stateless and accepts plain model objects.
 */
public class RuleEvaluatorTest {

    private RuleEvaluator evaluator;

    @Before
    public void setUp() {
        evaluator = new RuleEvaluator();
    }

    // ── Device State trigger ────────────────────────────────────────────────

    @Test
    public void deviceStateActiveDeviceIsOnReturnsTrue() {
        Rule rule = rule("Device State", "State = Active");
        Device device = device(true);
        assertTrue(evaluator.evaluate(rule, device));
    }

    @Test
    public void deviceStateActiveDeviceIsOffReturnsFalse() {
        Rule rule = rule("Device State", "State = Active");
        Device device = device(false);
        assertFalse(evaluator.evaluate(rule, device));
    }

    @Test
    public void deviceStateInactiveDeviceIsOffReturnsTrue() {
        Rule rule = rule("Device State", "State = Inactive");
        Device device = device(false);
        assertTrue(evaluator.evaluate(rule, device));
    }

    @Test
    public void deviceStateInactiveDeviceIsOnReturnsFalse() {
        Rule rule = rule("Device State", "State = Inactive");
        Device device = device(true);
        assertFalse(evaluator.evaluate(rule, device));
    }

    @Test
    public void deviceStateNullDeviceReturnsFalse() {
        Rule rule = rule("Device State", "State = Active");
        assertFalse(evaluator.evaluate(rule, null));
    }

    // ── Sensor Threshold trigger ────────────────────────────────────────────

    @Test
    public void sensorThresholdGreaterThanTemperatureAboveReturnsTrue() {
        Rule rule = rule("Sensor Threshold", "Value > 20");
        Device device = deviceWithTemperature(25.0);
        assertTrue(evaluator.evaluate(rule, device));
    }

    @Test
    public void sensorThresholdGreaterThanTemperatureBelowReturnsFalse() {
        Rule rule = rule("Sensor Threshold", "Value > 20");
        Device device = deviceWithTemperature(15.0);
        assertFalse(evaluator.evaluate(rule, device));
    }

    @Test
    public void sensorThresholdLessThanTemperatureBelowReturnsTrue() {
        Rule rule = rule("Sensor Threshold", "Value < 18");
        Device device = deviceWithTemperature(15.0);
        assertTrue(evaluator.evaluate(rule, device));
    }

    @Test
    public void sensorThresholdLessThanOrEqualExactMatchReturnsTrue() {
        Rule rule = rule("Sensor Threshold", "Value <= 20");
        Device device = deviceWithTemperature(20.0);
        assertTrue(evaluator.evaluate(rule, device));
    }

    @Test
    public void sensorThresholdGreaterThanOrEqualExactMatchReturnsTrue() {
        Rule rule = rule("Sensor Threshold", "Value >= 22");
        Device device = deviceWithTemperature(22.0);
        assertTrue(evaluator.evaluate(rule, device));
    }

    @Test
    public void sensorThresholdNullDeviceReturnsFalse() {
        Rule rule = rule("Sensor Threshold", "Value > 20");
        assertFalse(evaluator.evaluate(rule, null));
    }

    @Test
    public void sensorThresholdMalformedConditionReturnsFalse() {
        Rule rule = rule("Sensor Threshold", "temp > hot");
        Device device = deviceWithTemperature(25.0);
        assertFalse(evaluator.evaluate(rule, device));
    }

    // ── Guard cases ─────────────────────────────────────────────────────────

    @Test
    public void nullRuleReturnsFalse() {
        assertFalse(evaluator.evaluate(null, device(true)));
    }

    @Test
    public void unknownTriggerTypeReturnsFalse() {
        Rule rule = rule("Unknown", "anything");
        assertFalse(evaluator.evaluate(rule, device(true)));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Rule rule(String triggerType, String condition) {
        return new Rule("t-id", "Test Rule", triggerType, "Source", condition,
                "Turn On", "Target", true, "Active");
    }

    private Device device(boolean state) {
        return new Device("d-id", "Test Device", "Switch", "Room", state);
    }

    private Device deviceWithTemperature(double temperature) {
        Device d = new Device("d-id", "Sensor", "Thermostat", "Room", true);
        d.setTemperature(temperature);
        return d;
    }
}
