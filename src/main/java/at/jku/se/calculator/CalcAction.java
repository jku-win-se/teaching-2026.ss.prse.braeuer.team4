package at.jku.se.calculator;

/**
 * Enumeration of all calculation actions supported by the calculator.
 * Each action has a display symbol used in the calculator text field.
 */
public enum CalcAction {

	ADD("+"), SUB("-"), MULT("x"), DIV("/"), POW("^");

	private final String symbol;

	CalcAction(String symbol) {
		this.symbol = symbol;
	}

	/**
	 * @return the symbol representing this operation, as shown on the UI button.
	 */
	public String getSymbol() {
		return symbol;
	}
}
