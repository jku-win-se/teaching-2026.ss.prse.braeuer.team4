package at.jku.se.calculator.operators;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import at.jku.se.calculator.factory.ICalculationOperation;

/**
 * {@link ICalculationOperation} that adds two integer operands.
 *
 * <p>Expects an input string of the form {@code "a+b"} where both {@code a}
 * and {@code b} are valid integers. Throws {@link IllegalArgumentException}
 * if the input is malformed.</p>
 */
public class AddOperation implements ICalculationOperation {

	private static final Logger LOGGER = LogManager.getLogger(AddOperation.class);

	@Override
	public String calculate(String txt) {
		LOGGER.info("Add Operation executed: " + txt);
		String[] terms = txt.split("\\+");
		if (terms.length == 2) {
			if (!isInteger(terms[0])) {
				LOGGER.error("Invalid Value");
				throw new IllegalArgumentException(String.format("%s is not a valid number", terms[0]));
			}
			if (!isInteger(terms[1])) {
				LOGGER.error("Invalid Value");
				throw new IllegalArgumentException(String.format("%s is not a valid number", terms[1]));
			}

			return (Integer.parseInt(terms[0]) + Integer.parseInt(terms[1])) + "";
		} else {
			throw new IllegalArgumentException("Input not correct!");
		}
	}

	private boolean isInteger(String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

}