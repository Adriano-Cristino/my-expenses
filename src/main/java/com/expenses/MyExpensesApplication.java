package com.expenses;

import com.expenses.config.DatabaseConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

public class MyExpensesApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MyExpensesApplication.class);
    private static final String PREF_DARK_THEME = "darkTheme";

    @Override
    public void start(Stage stage) {
        try {
            // Inicializa o banco de dados
            DatabaseConfig.initDatabase();
            
            // Carrega a tela de login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent loginContent = loader.load();
            
            // Cria um container para o fundo
            StackPane root = new StackPane(loginContent);
            root.getStyleClass().add("root");
            
            // Carrega o tema inicial
            Preferences prefs = Preferences.userNodeForPackage(MyExpensesApplication.class);
            boolean isDarkTheme = prefs.getBoolean(PREF_DARK_THEME, false);
            if (isDarkTheme) {
                root.getStyleClass().add("dark");
            }
            
            // Configura a cena
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
            
            // Configura a janela
            stage.setTitle("MyExpenses - Login");
            stage.setScene(scene);
            stage.setMinWidth(400);
            stage.setMinHeight(600);
            stage.show();
            
            logger.info("Application started successfully");
            
        } catch (Exception e) {
            logger.error("Error starting application", e);
            throw new RuntimeException("Failed to start application", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
