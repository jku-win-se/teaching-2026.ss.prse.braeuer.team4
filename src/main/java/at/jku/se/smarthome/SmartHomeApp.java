package at.jku.se.smarthome;

import java.io.IOException;

import at.jku.se.smarthome.controller.LoginController;
import at.jku.se.smarthome.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Main application class for Smart Home Orchestrator.
 * 
 * Initializes the JavaFX application with a login screen and handles
 * navigation to the main application after successful authentication.
 */
public class SmartHomeApp extends Application {
    
    private Stage primaryStage;
    private Scene loginScene;
    private Scene mainScene;
    
    /**
     * Starts the JavaFX application.
     * 
     * @param primaryStage the primary stage
     * @throws IOException if FXML files cannot be loaded
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        this.primaryStage = primaryStage;
        
        primaryStage.setTitle("Smart Home Orchestrator");
        primaryStage.setWidth(1000);
        primaryStage.setHeight(700);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        
        // Load login scene
        loadLoginScene();
        
        // Load main scene
        loadMainScene();
        
        // Show login scene first
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }
    
    /**
     * Loads the login FXML and scene.
     * 
     * @throws IOException if login FXML cannot be loaded
     */
    private void loadLoginScene() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/at/jku/se/smarthome/view/login-view.fxml")
        );
        BorderPane loginRoot = loader.load();
        
        LoginController loginController = loader.getController();
        loginController.setLoginCallback(this::showMainScene);
        
        loginScene = new Scene(loginRoot);
    }
    
    /**
     * Loads the main application FXML and scene.
     * 
     * @throws IOException if main FXML cannot be loaded
     */
    private void loadMainScene() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/at/jku/se/smarthome/view/main-shell.fxml")
        );
        BorderPane mainRoot = loader.load();
        
        MainController mainController = loader.getController();
        mainController.setMainCallback(this::showLoginScene);
        
        mainScene = new Scene(mainRoot);
    }
    
    /**
     * Shows the main application scene.
     */
    private void showMainScene() {
        primaryStage.setScene(mainScene);
    }
    
    /**
     * Shows the login scene.
     */
    private void showLoginScene() {
        try {
            // Reload login scene to clear fields
            loadLoginScene();
        } catch (IOException e) {
            e.printStackTrace();
        }
        primaryStage.setScene(loginScene);
    }
    
    /**
     * Main entry point for the application.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
