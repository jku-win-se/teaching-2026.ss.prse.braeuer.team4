package at.jku.se.calculator.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the {@link DivideOperation} class.
 */
public class TestDivideOperation {

	private DivideOperation divide;

	@Before
	public void setup() {
		divide = new DivideOperation();
	}

	/**
	 * Test integer result: 10 / 2 = 5.
	 */
	@Test
	public void testCalculateIntegerResult() {
		String result = divide.calculate("10/2");
		assertEquals(5.0, Double.parseDouble(result), 0.001);
	}

	/**
	 * Test decimal result: 7 / 2 = 3.5.
	 */
	@Test
	public void testCalculateDecimalResult() {
		String result = divide.calculate("7/2");
		assertEquals(3.5, Double.parseDouble(result), 0.001);
	}

	/**
	 * Test division by zero: 10 / 0 should throw IllegalArgumentException.
	 */
	@Test
	public void testCalculateDivisionByZero() {
		assertThrows(IllegalArgumentException.class, () -> divide.calculate("10/0"));
	}

	/**
	 * Test illegal input: a string instead of a number.
	 */
	@Test
	public void testCalculateExceptionInvalidFirstOperand() {
		assertThrows(IllegalArgumentException.class, () -> divide.calculate("xyz/3"));
	}

	/**
	 * Test illegal input: an invalid second operand.
	 */
	@Test
	public void testCalculateExceptionSecondOperand() {
		assertThrows(IllegalArgumentException.class, () -> divide.calculate("3/abc"));
	}

	/**
	 * Test malformed input: missing '/' sign.
	 */
	@Test
	public void testCalculateMalformedInput() {
		assertThrows(IllegalArgumentException.class, () -> divide.calculate("10+2"));
	}
}
