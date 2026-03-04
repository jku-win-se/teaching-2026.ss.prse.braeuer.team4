package at.jku.se.calculator;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Entry point for the JavaFX calculator application.
 *
 * <p>Loads the FXML layout and wires it to {@link CalculatorFXController},
 * which in turn delegates all business logic to {@link CalculatorController}.</p>
 */
public class SimpleCalculator extends Application {

	/**
	 * Launch the application.
	 *
	 * @param args command-line arguments (not used)
	 */
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("calculator.fxml"));
		Parent root = loader.load();

		primaryStage.setTitle("Calculator");
		primaryStage.setScene(new Scene(root));
		primaryStage.setResizable(false);
		primaryStage.show();
	}
}