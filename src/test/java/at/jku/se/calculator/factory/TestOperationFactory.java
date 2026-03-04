package at.jku.se.calculator.factory;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import at.jku.se.calculator.CalcAction;
import at.jku.se.calculator.operators.AddOperation;
import at.jku.se.calculator.operators.NullOperation;

/**
 * This test class performs tests for the {@link OperationFactory} class.
 */
public class TestOperationFactory {

	/**
	 * This test case tests the getOperation Method of the OperationFactory. A
	 * method is requested for the add operation and the respective implementation
	 * should be returned.
	 * 
	 */
	@Test
	public void testAdditionOperation() {
		ICalculationOperation operation = OperationFactory.getOperation(CalcAction.ADD);
		assertTrue(operation instanceof AddOperation);
	}

	/**
	 * Tests that SUB returns a {@link NullOperation} (not yet implemented).
	 */
	@Test
	public void testSubtractionOperation() {
		ICalculationOperation operation = OperationFactory.getOperation(CalcAction.SUB);
		assertTrue(operation instanceof NullOperation);
	}

	/**
	 * Tests that MULT returns a {@link NullOperation} (not yet implemented).
	 */
	@Test
	public void testMultiplicationOperation() {
		ICalculationOperation operation = OperationFactory.getOperation(CalcAction.MULT);
		assertTrue(operation instanceof NullOperation);
	}

	/**
	 * Tests that DIV returns a {@link NullOperation} (not yet implemented).
	 */
	@Test
	public void testDivisionOperation() {
		ICalculationOperation operation = OperationFactory.getOperation(CalcAction.DIV);
		assertTrue(operation instanceof NullOperation);
	}

	/**
	 * Tests that POW returns a {@link NullOperation} (not yet implemented).
	 */
	@Test
	public void testPowerOperation() {
		ICalculationOperation operation = OperationFactory.getOperation(CalcAction.POW);
		assertTrue(operation instanceof NullOperation);
	}
}