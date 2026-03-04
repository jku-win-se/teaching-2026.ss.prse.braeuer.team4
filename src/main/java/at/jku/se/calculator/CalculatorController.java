package at.jku.se.calculator;

import at.jku.se.calculator.factory.OperationFactory;

/**
 * Controller for {@link SimpleCalculator}.
 *
 * <p>Handles all calculator business logic, decoupled from the Swing UI.
 * The controller maintains the pending operation as explicit state, avoiding
 * fragile operator detection by scanning the display string.</p>
 */
public class CalculatorController {

	private CalcAction pendingAction = null;

	/**
	 * Evaluates any pending operation on the current display text, then appends
	 * the symbol of the new {@code action} to the display. Passing {@code null}
	 * as {@code action} acts as the equals (=) key: only the result is returned.
	 *
	 * @param displayText current content of the calculator display
	 * @param action      the operation button pressed, or {@code null} for "="
	 * @return new text to show on the display
	 */
	public String applyAction(String displayText, CalcAction action) {
		String result = displayText;

		if (pendingAction != null) {
			try {
				result = OperationFactory.getOperation(pendingAction).calculate(displayText);
				result = normalizeToInteger(result);
			} catch (IllegalArgumentException e) {
				pendingAction = action;
				return "Error";
			}
		}

		pendingAction = action;
		return result + (action == null ? "" : action.getSymbol());
	}

	/**
	 * Appends a digit to the display text.
	 *
	 * @param displayText current content of the calculator display
	 * @param digit       digit 0–9 to append
	 * @return new text to show on the display
	 */
	public String appendDigit(String displayText, int digit) {
		if (displayText == null || displayText.isEmpty()) {
			return String.valueOf(digit);
		}
		return displayText + digit;
	}

	/**
	 * Resets the controller state and returns the cleared display value.
	 *
	 * @return {@code "0"}
	 */
	public String clear() {
		pendingAction = null;
		return "0";
	}

	// ---

	/**
	 * Converts a decimal result string to an integer string when the fractional
	 * part is zero (e.g. {@code "8.0"} → {@code "8"}), leaving non-integer
	 * results unchanged.
	 */
	private String normalizeToInteger(String value) {
		try {
			return String.valueOf((int) Double.parseDouble(value));
		} catch (NumberFormatException e) {
			return value;
		}
	}
}
