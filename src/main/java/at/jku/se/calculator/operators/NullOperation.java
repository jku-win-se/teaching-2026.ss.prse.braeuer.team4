package at.jku.se.calculator.operators;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import at.jku.se.calculator.factory.ICalculationOperation;

/**
 * Null-object implementation of {@link ICalculationOperation}.
 *
 * <p>Used as a safe placeholder for operations that are not yet implemented.
 * Always returns {@code "0"} and logs a warning.</p>
 */
public class NullOperation implements ICalculationOperation {

	private static final Logger LOGGER = LogManager.getLogger(NullOperation.class);
	
	@Override
	public String calculate(String txt) {
		LOGGER.warn("This Calculation Operation is not implemented!");
		return "0";

	}

}