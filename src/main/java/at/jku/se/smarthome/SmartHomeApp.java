package at.jku.se.smarthome;

import java.io.IOException;

import at.jku.se.smarthome.controller.LoginController;
import at.jku.se.smarthome.controller.MainController;
import at.jku.se.smarthome.controller.RegisterController;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
@SuppressWarnings("PMD.AtLeastOneConstructor")
public class SmartHomeApp extends Application {

    /** Logger instance for application logging. */
    private static final Logger LOGGER = LogManager.getLogger(SmartHomeApp.class);
    
    /** Primary application stage. */
    private Stage primaryStage;
    /** Login screen scene. */
    private Scene loginScene;
    /** User registration screen scene. */
    private Scene registerScene;
    /** Main application screen scene. */
    private Scene mainScene;
    /** Controller for main application view. */
    private MainController mainController;
    
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
        
        // Load register scene
        loadRegisterScene();
        
        // Load main scene
        loadMainScene();

        // Start recurring schedule processing after the JavaFX toolkit is fully available.
        ServiceRegistry.getScheduleService().startRecurringExecution();
        
        // Show login scene first
        primaryStage.setScene(loginScene);
        primaryStage.setMaximized(true);
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
        loginController.setLoginCallback(new LoginController.LoginCallback() {
            @Override
            public void onLoginSuccess() {
                showMainScene();
            }
            
            @Override
            public void onRegisterClick() {
                showRegisterScene();
            }
        });
        
        loginScene = new Scene(loginRoot);
    }
    
    /**
     * Loads the register FXML and scene.
     * 
     * @throws IOException if register FXML cannot be loaded
     */
    private void loadRegisterScene() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/at/jku/se/smarthome/view/register-view.fxml")
        );
        BorderPane registerRoot = loader.load();
        
        RegisterController registerController = loader.getController();
        registerController.setRegisterCallback(new RegisterController.RegisterCallback() {
            @Override
            public void onBackToLogin() {
                showLoginScene();
            }
        });
        
        registerScene = new Scene(registerRoot);
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
        
        mainController = loader.getController();
        mainController.setMainCallback(this::showLoginScene);
        
        mainScene = new Scene(mainRoot);
    }
    
    /**
     * Shows the main application scene.
     */
    private void showMainScene() {
        if (mainController != null) {
            mainController.refreshSessionState();
        }
        primaryStage.setScene(mainScene);
    }
    
    /**
     * Shows the register scene.
     */
    private void showRegisterScene() {
        primaryStage.setScene(registerScene);
    }
    
    /**
     * Shows the login scene.
     */
    private void showLoginScene() {
        try {
            // Reload login scene to clear fields
            loadLoginScene();
        } catch (IOException e) {
            LOGGER.error("Failed to reload the login scene.", e);
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

    @Override
    public void stop() {
        ServiceRegistry.getScheduleService().stopRecurringExecution();
    }
}
