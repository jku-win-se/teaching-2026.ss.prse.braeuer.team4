package at.jku.se.calculator.factory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import at.jku.se.calculator.CalcAction;
import at.jku.se.calculator.operators.AddOperation;
import at.jku.se.calculator.operators.NullOperation;

/**
 * Factory class for {@link ICalculationOperation}.
 *
 * <p>Returns the appropriate {@link ICalculationOperation} implementation for a
 * given {@link CalcAction}. Operations that are not yet implemented return a
 * {@link NullOperation} as a safe placeholder.</p>
 */
public class OperationFactory {

	private OperationFactory() {
		// utility class – do not instantiate
	}

	private static final Logger LOGGER = LogManager.getLogger(OperationFactory.class);

	/**
	 * 
	 * @param action. The calculation operation to be performed.
	 * @return Returns the respective implementation for the requested operation.
	 */
	public static ICalculationOperation getOperation(CalcAction action) {

		switch (action) {
		case POW:
			return new NullOperation();
		case ADD:
			return new AddOperation();
		case DIV:
			return new NullOperation();
		case MULT:
			return new NullOperation();
		case SUB:
			return new NullOperation();
		default:
			LOGGER.error("Sorry this operation is not yet implemented!");
			return new NullOperation();
		}
	}

}