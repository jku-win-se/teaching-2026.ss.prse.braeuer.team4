package at.jku.se.calculator.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import org.junit.Before;
import org.junit.Test;

/**
 * This test class performs tests for the {@link MultiplyOperation} class.
 * 
 * @author Team 4
 */
public class TestMultiplication {

	private MultiplyOperation multiply;

	@Before
	public void setup() {
		multiply = new MultiplyOperation();
	}

	/**
	 * This test case tests the calculate method in the MultiplyOperation class with
	 * basic multiplication.
	 * 
	 */
	@Test
	public void testCalculate() {
		String result = multiply.calculate("3x4");
		assertEquals(12, Integer.parseInt(result));
	}

	/**
	 * This test case tests the calculate method in the MultiplyOperation class with
	 * multiplication by zero.
	 * 
	 */
	@Test
	public void testCalculateZero() {
		String result = multiply.calculate("5x0");
		assertEquals(0, Integer.parseInt(result));
	}

	/**
	 * This test case tests the calculate method in the MultiplyOperation class with
	 * leading zeros.
	 * 
	 */
	@Test
	public void testCalculateLeadingZeros() {
		String result = multiply.calculate("000x5");
		assertEquals(0, Integer.parseInt(result));
	}

	/**
	 * This test case tests an illegal input for a {@link MultiplyOperation}. A
	 * String is entered instead of a number for the first operand. An
	 * {@link IllegalArgumentException} should be thrown.
	 * 
	 */
	@Test
	public void testCalculateExceptionFirstOperand() {
		assertThrows(IllegalArgumentException.class, () -> multiply.calculate("abcx3"));
	}

	/**
	 * Tests that an invalid second operand throws an {@link IllegalArgumentException}.
	 */
	@Test
	public void testCalculateExceptionSecondOperand() {
		assertThrows(IllegalArgumentException.class, () -> multiply.calculate("3xabc"));
	}

	/**
	 * Tests that a malformed input without an 'x' sign throws an
	 * {@link IllegalArgumentException}.
	 */
	@Test
	public void testCalculateMalformedInput() {
		assertThrows(IllegalArgumentException.class, () -> multiply.calculate("3+3"));
	}

}
