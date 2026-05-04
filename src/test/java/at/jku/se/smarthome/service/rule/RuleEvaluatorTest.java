package at.jku.se.smarthome.service.rule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Rule;

/**
 * Unit tests for RuleEvaluator.
 *
 * No mocks needed: RuleEvaluator is stateless and accepts plain model objects.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods",
        "PMD.CommentRequired", "PMD.MethodNamingConventions",
        "PMD.UnitTestContainsTooManyAsserts"})
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

    // ── Sensor Threshold reads currentValue ─────────────────────────────────

    /**
     * Test: sensor threshold reads currentValue, not temperature.
     */
    @Test
    public void sensorThreshold_readsCurrentValueNotTemperature_returnsTrue() {
        Rule rule = rule("Sensor Threshold", "Value > 25");
        Device device = deviceWithCurrentValue(30.0);
        device.setTemperature(0.0);
        assertTrue(evaluator.evaluate(rule, device));
    }

    /**
     * Test: sensor threshold ignores temperature when currentValue is low.
     */
    @Test
    public void sensorThreshold_readsCurrentValueNotTemperature_returnsFalse() {
        Rule rule = rule("Sensor Threshold", "Value > 25");
        Device device = deviceWithCurrentValue(10.0);
        device.setTemperature(99.0);
        assertFalse(evaluator.evaluate(rule, device));
    }

    // ── Time trigger with weekday prefix ────────────────────────────────────

    /**
     * Test: explicit Daily prefix matches regardless of weekday.
     */
    @Test
    public void time_dailyExplicit_returnsTrueWithinSameMinute() {
        Rule rule = rule("Time", "Daily " + nowTime());
        assertTrue(evaluator.evaluate(rule, null));
    }

    /**
     * Test: time without prefix matches within same minute (backward compat).
     */
    @Test
    public void time_noPrefix_returnsTrueWithinSameMinute() {
        Rule rule = rule("Time", nowTime());
        assertTrue(evaluator.evaluate(rule, null));
    }

    /**
     * Test: Weekdays prefix matches when today is a weekday.
     */
    @Test
    public void time_weekdays_onMonday_returnsTrue() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        Assume.assumeTrue(today != DayOfWeek.SATURDAY && today != DayOfWeek.SUNDAY);
        Rule rule = rule("Time", "Weekdays " + nowTime());
        assertTrue(evaluator.evaluate(rule, null));
    }

    /**
     * Test: Weekends prefix does not match on a weekday.
     */
    @Test
    public void time_weekends_onMonday_returnsFalse() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        Assume.assumeTrue(today != DayOfWeek.SATURDAY && today != DayOfWeek.SUNDAY);
        Rule rule = rule("Time", "Weekends " + nowTime());
        assertFalse(evaluator.evaluate(rule, null));
    }

    /**
     * Test: single-day prefix matches when it is that day.
     */
    @Test
    public void time_singleDay_matches_returnsTrue() {
        String dayLabel = dayLabel(LocalDate.now().getDayOfWeek());
        Rule rule = rule("Time", dayLabel + " " + nowTime());
        assertTrue(evaluator.evaluate(rule, null));
    }

    /**
     * Test: single-day prefix does not match on a different day.
     */
    @Test
    public void time_singleDay_doesNotMatch_returnsFalse() {
        DayOfWeek tomorrow = LocalDate.now().getDayOfWeek().plus(1);
        String dayLabel = dayLabel(tomorrow);
        Rule rule = rule("Time", dayLabel + " " + nowTime());
        assertFalse(evaluator.evaluate(rule, null));
    }

    /**
     * Test: day list matches when today is one of the listed days.
     */
    @Test
    public void time_dayList_matchesOneOfList_returnsTrue() {
        String todayLabel = dayLabel(LocalDate.now().getDayOfWeek());
        Rule rule = rule("Time", todayLabel + ",Wed,Fri " + nowTime());
        assertTrue(evaluator.evaluate(rule, null));
    }

    /**
     * Test: day range matches when today is inside the range.
     */
    @Test
    public void time_dayRange_matchesInsideRange_returnsTrue() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        Assume.assumeTrue(today != DayOfWeek.SATURDAY && today != DayOfWeek.SUNDAY);
        Rule rule = rule("Time", "Mon-Fri " + nowTime());
        assertTrue(evaluator.evaluate(rule, null));
    }

    /**
     * Test: day range does not match when today is outside the range.
     */
    @Test
    public void time_dayRange_doesNotMatchOutsideRange_returnsFalse() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        Assume.assumeTrue(today != DayOfWeek.MONDAY && today != DayOfWeek.TUESDAY);
        Rule rule = rule("Time", "Mon-Tue " + nowTime());
        assertFalse(evaluator.evaluate(rule, null));
    }

    /**
     * Test: unknown weekday token causes evaluation to return false.
     */
    @Test
    public void time_unknownWeekdayToken_returnsFalse() {
        Rule rule = rule("Time", "Funday " + nowTime());
        assertFalse(evaluator.evaluate(rule, null));
    }

    /**
     * Test: inverted range causes evaluation to return false.
     */
    @Test
    public void time_invertedRange_returnsFalse() {
        Rule rule = rule("Time", "Fri-Mon " + nowTime());
        assertFalse(evaluator.evaluate(rule, null));
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
        Device testDevice = new Device("d-id", "Sensor", "Thermostat", "Room", true);
        testDevice.setTemperature(temperature);
        testDevice.setCurrentValue(temperature);
        return testDevice;
    }

    /**
     * Helper: creates a sensor device with specified currentValue.
     *
     * @param currentValue the sensor reading value
     * @return the test device
     */
    private Device deviceWithCurrentValue(double currentValue) {
        Device testDevice = new Device("d-id", "Sensor", "sensor", "Room", true);
        testDevice.setCurrentValue(currentValue);
        return testDevice;
    }

    /**
     * Helper: returns the current time formatted as HH:mm.
     *
     * @return current hour and minute string
     */
    private String nowTime() {
        LocalTime now = LocalTime.now();
        return String.format("%02d:%02d", now.getHour(), now.getMinute());
    }

    /**
     * Helper: returns a three-letter capitalised label for a day of week.
     *
     * @param day the day of week
     * @return e.g. "Mon", "Tue"
     */
    private String dayLabel(DayOfWeek day) {
        String name = day.name();
        return name.charAt(0) + name.substring(1, 3).toLowerCase();
    }
}
