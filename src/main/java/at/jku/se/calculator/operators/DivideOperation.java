package at.jku.se.calculator.operators;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import at.jku.se.calculator.factory.ICalculationOperation;

/**
 * {@link ICalculationOperation} that divides two operands.
 *
 * <p>Expects an input string of the form {@code "a/b"} where both {@code a}
 * and {@code b} are valid numbers. Throws {@link IllegalArgumentException}
 * if the input is malformed or if division by zero is attempted.</p>
 */
public class DivideOperation implements ICalculationOperation {

	private static final Logger LOGGER = LogManager.getLogger(DivideOperation.class);

	@Override
	public String calculate(String txt) {
		LOGGER.info("Divide Operation executed: " + txt);
		String[] terms = txt.split("/");
		if (terms.length == 2) {
			double dividend;
			double divisor;
			try {
				dividend = Double.parseDouble(terms[0]);
			} catch (NumberFormatException e) {
				LOGGER.error("Invalid Value: " + terms[0]);
				throw new IllegalArgumentException(String.format("%s is not a valid number", terms[0]));
			}
			try {
				divisor = Double.parseDouble(terms[1]);
			} catch (NumberFormatException e) {
				LOGGER.error("Invalid Value: " + terms[1]);
				throw new IllegalArgumentException(String.format("%s is not a valid number", terms[1]));
			}

			if (divisor == 0) {
				LOGGER.error("Division by zero");
				throw new IllegalArgumentException("Division by zero is not allowed");
			}

			return String.valueOf(dividend / divisor);
		} else {
			throw new IllegalArgumentException("Input not correct!");
		}
	}

}
