package at.jku.se.calculator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link CalculatorController} covering all public methods and all
 * significant branches: no pending action, pending ADD succeeds, pending action
 * with invalid input (error path), appendDigit edge cases, clear, and a full
 * end-to-end calculation flow.
 */
public class TestCalculatorController {

	private CalculatorController controller;

	@Before
	public void setup() {
		controller = new CalculatorController();
	}

	// --- appendDigit ---

	/**
	 * Appending a digit to a normal display string concatenates it.
	 */
	@Test
	public void testAppendDigitNormal() {
		assertEquals("31", controller.appendDigit("3", 1));
	}

	/**
	 * Appending a digit to an empty string returns only the digit.
	 */
	@Test
	public void testAppendDigitEmpty() {
		assertEquals("5", controller.appendDigit("", 5));
	}

	/**
	 * Appending a digit to a null display returns only the digit.
	 */
	@Test
	public void testAppendDigitNull() {
		assertEquals("7", controller.appendDigit(null, 7));
	}

	// --- clear ---

	/**
	 * clear() returns "0" and resets pending action so the next applyAction
	 * treats the display as a plain value (no calculation attempted).
	 */
	@Test
	public void testClearReturnsZero() {
		assertEquals("0", controller.clear());
	}

	/**
	 * After clear(), applyAction with a new operator simply appends the symbol
	 * (no pending action means no calculation attempt on the prior display value).
	 */
	@Test
	public void testClearResetsPendingAction() {
		// Set a pending action first
		controller.applyAction("5", CalcAction.ADD);
		// Clear resets state
		controller.clear();
		// applyAction should NOT try to calculate — just append the new operator
		String result = controller.applyAction("3", CalcAction.ADD);
		assertEquals("3+", result);
	}

	// --- applyAction: no pending action ---

	/**
	 * With no pending action, pressing an operator just appends its symbol.
	 */
	@Test
	public void testApplyActionNoPendingOperator() {
		String result = controller.applyAction("8", CalcAction.ADD);
		assertEquals("8+", result);
	}

	/**
	 * With no pending action, pressing "=" (null) returns the display unchanged.
	 */
	@Test
	public void testApplyActionNoPendingEquals() {
		String result = controller.applyAction("8", null);
		assertEquals("8", result);
	}

	// --- applyAction: with pending ADD ---

	/**
	 * Full ADD flow: enter 4, press +, enter 5, press = → result is "9".
	 */
	@Test
	public void testApplyActionAddEquals() {
		controller.applyAction("4", CalcAction.ADD);   // pending = ADD, display = "4+"
		String result = controller.applyAction("4+5", null); // calculate 4+5
		assertEquals("9", result);
	}

	/**
	 * Pressing another operator after a complete operand evaluates and appends the
	 * new operator symbol: 3 + [press +] 4 [press -] → evaluates 3+4=7, then "7-".
	 */
	@Test
	public void testApplyActionChainedOperators() {
		controller.applyAction("3", CalcAction.ADD);          // pending = ADD
		String result = controller.applyAction("3+4", CalcAction.SUB); // evaluate 3+4, then append "-"
		assertEquals("7-", result);
	}

	/**
	 * When the calculation input is invalid, applyAction returns "Error".
	 */
	@Test
	public void testApplyActionInvalidInputReturnsError() {
		controller.applyAction("abc", CalcAction.ADD); // pending = ADD
		String result = controller.applyAction("abc+xyz", null);
		assertEquals("Error", result);
	}

	// --- CalcAction symbol ---

	/**
	 * Verify CalcAction symbols are correct for all values.
	 */
	@Test
	public void testCalcActionSymbols() {
		assertEquals("+", CalcAction.ADD.getSymbol());
		assertEquals("-", CalcAction.SUB.getSymbol());
		assertEquals("x", CalcAction.MULT.getSymbol());
		assertEquals("/", CalcAction.DIV.getSymbol());
		assertEquals("^", CalcAction.POW.getSymbol());
	}

	// --- full end-to-end flow ---

	/**
	 * Simulates a full user interaction: 1, 2, +, 8, = → "20".
	 */
	@Test
	public void testFullFlow() {
		String display = "0";
		display = controller.appendDigit(display, 1);   // "01"
		display = controller.appendDigit(display, 2);   // "012"
		display = controller.applyAction(display, CalcAction.ADD); // "012+" (no pending yet, just appends)
		display = controller.appendDigit(display, 8);   // "012+8"
		display = controller.applyAction(display, null); // evaluate 12+8 = 20
		assertEquals("20", display);
	}

	// --- controller is instantiable ---

	@Test
	public void testControllerNotNull() {
		assertNotNull(controller);
	}
}
