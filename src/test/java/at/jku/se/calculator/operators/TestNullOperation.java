package at.jku.se.calculator.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

/**
 * This test class performs tests for the {@link NullOperation} class.
 */
public class TestNullOperation {

	private NullOperation nullOp;

	@Before
	public void setup() {
		nullOp = new NullOperation();
	}

	/**
	 * Tests that a NullOperation instance can be created successfully.
	 */
	@Test
	public void testInstantiation() {
		assertNotNull(nullOp);
	}

	/**
	 * Tests that calculate always returns "0" regardless of input.
	 */
	@Test
	public void testCalculateReturnsZero() {
		String result = nullOp.calculate("5+3");
		assertEquals("0", result);
	}

	/**
	 * Tests that calculate returns "0" for an empty string input.
	 */
	@Test
	public void testCalculateEmptyInput() {
		String result = nullOp.calculate("");
		assertEquals("0", result);
	}

	/**
	 * Tests that calculate returns "0" for a null input.
	 */
	@Test
	public void testCalculateNullInput() {
		String result = nullOp.calculate(null);
		assertEquals("0", result);
	}
}
