package at.jku.se.calculator;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for {@code calculator.fxml}.
 *
 * <p>Handles all button events from the FXML view and delegates the
 * actual calculation logic to {@link CalculatorController}.</p>
 */
public class CalculatorFXController {

	@FXML
	private TextField display;

	private final CalculatorController controller = new CalculatorController();

	/**
	 * Called when any digit button (0–9) is pressed.
	 * The digit value is stored in the button's {@code userData} attribute.
	 */
	@FXML
	private void onDigit(ActionEvent event) {
		int digit = Integer.parseInt(((Button) event.getSource()).getUserData().toString());
		display.setText(controller.appendDigit(display.getText(), digit));
	}

	/**
	 * Called when an operator button (+, −, x, /, ^) is pressed.
	 * The {@link CalcAction} name is stored in the button's {@code userData} attribute.
	 */
	@FXML
	private void onOperator(ActionEvent event) {
		String actionName = ((Button) event.getSource()).getUserData().toString();
		CalcAction action = CalcAction.valueOf(actionName);
		display.setText(controller.applyAction(display.getText(), action));
	}

	/**
	 * Called when the "=" button is pressed — evaluates the pending operation.
	 */
	@FXML
	private void onEquals(ActionEvent event) {
		display.setText(controller.applyAction(display.getText(), null));
	}

	/**
	 * Called when the "CE" (clear entry) button is pressed — resets the display.
	 */
	@FXML
	private void onClear(ActionEvent event) {
		display.setText(controller.clear());
	}
}
