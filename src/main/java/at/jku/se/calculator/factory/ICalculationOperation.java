package at.jku.se.calculator.factory;

/**
 * Calculation Operation interface.<br>
 * Each Operation provided by the {@link OperationFactory} has to implement this
 * interface.
 *
 */
public interface ICalculationOperation {

	String calculate(String input);
}
