package com.expenses.controller;

import com.expenses.dao.UserDAO;
import com.expenses.model.User;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.prefs.Preferences;

public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    private final UserDAO userDAO = new UserDAO();
    private final Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
    
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorMessage;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private ProgressIndicator loginProgress;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private ToggleButton themeToggle;
    @FXML private FontIcon themeIcon;
    @FXML private StackPane loadingPane;
    
    private static final String PREF_DARK_THEME = "darkTheme";
    private static final String PREF_REMEMBER_EMAIL = "rememberEmail";
    private static final String PREF_SAVED_EMAIL = "savedEmail";
    
    private User authenticatedUser;
    
    @FXML
    public void initialize() {
        setupEventHandlers();
        loadThemePreference();
        loadSavedEmail();
        Platform.runLater(this::setupKeyboardShortcuts);
    }
    
    private void setupEventHandlers() {
        loginButton.setOnAction(event -> handleLogin());
        forgotPasswordLink.setOnAction(event -> handleForgotPassword());
        
        // Validação em tempo real
        emailField.textProperty().addListener((obs, old, newValue) -> {
            errorMessage.setText("");
            validateEmail(newValue);
        });
        
        passwordField.textProperty().addListener((obs, old, newValue) -> {
            errorMessage.setText("");
        });
    }
    
    private void setupKeyboardShortcuts() {
        Scene scene = emailField.getScene();
        if (scene != null) {
            scene.setOnKeyPressed(event -> {
                switch (event.getCode()) {
                    case ENTER:
                        if (!loginButton.isDisabled()) {
                            handleLogin();
                        } else {
                            showError("Por favor, corrija os erros antes de continuar");
                        }
                        break;
                }
            });
        }
    }
    
    private void validateEmail(String email) {
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            emailField.setStyle("-fx-border-color: red;");
            showError("Email inválido");
        } else {
            emailField.setStyle("");
        }
    }
    
    private void loadThemePreference() {
        // Carrega tema
        boolean isDarkTheme = prefs.getBoolean(PREF_DARK_THEME, false);
        themeToggle.setSelected(isDarkTheme);
        applyTheme(isDarkTheme);
    }
    
    private void loadSavedEmail() {
        // Carrega email salvo
        if (prefs.getBoolean(PREF_REMEMBER_EMAIL, false)) {
            emailField.setText(prefs.get(PREF_SAVED_EMAIL, ""));
            rememberMeCheckbox.setSelected(true);
        }
    }
    
    @FXML
    private void toggleTheme() {
        boolean isDarkTheme = themeToggle.isSelected();
        prefs.putBoolean(PREF_DARK_THEME, isDarkTheme);
        applyTheme(isDarkTheme);
    }
    
    private void applyTheme(boolean isDarkTheme) {
        Scene scene = emailField.getScene();
        if (scene != null) {
            if (isDarkTheme) {
                scene.getRoot().getStyleClass().add("dark");
            } else {
                scene.getRoot().getStyleClass().remove("dark");
            }
            themeIcon.setIconLiteral(isDarkTheme ? "fas-sun" : "fas-moon");
        }
    }
    
    public void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        
        // Validação básica
        if (email.isEmpty() || password.isEmpty()) {
            showError("Email e senha são obrigatórios");
            return;
        }
        
        // Desabilita o botão e mostra loading
        loginButton.setDisable(true);
        loginProgress.setVisible(true);
        
        Task<Optional<User>> loginTask = new Task<>() {
            @Override
            protected Optional<User> call() {
                try {
                    return userDAO.authenticate(email, password);
                } catch (RuntimeException e) {
                    logger.error("Authentication error", e);
                    Platform.runLater(() -> showError("Erro de conexão com o banco de dados"));
                    return Optional.empty();
                }
            }
        };
        
        loginTask.setOnSucceeded(e -> {
            Optional<User> userOptional = loginTask.getValue();
            if (userOptional.isPresent()) {
                authenticatedUser = userOptional.get();
                // Salva preferências se "Lembrar-me" estiver marcado
                if (rememberMeCheckbox.isSelected()) {
                    prefs.putBoolean(PREF_REMEMBER_EMAIL, true);
                    prefs.put(PREF_SAVED_EMAIL, email);
                } else {
                    prefs.remove(PREF_REMEMBER_EMAIL);
                    prefs.remove(PREF_SAVED_EMAIL);
                }
                
                showSuccess("Login realizado com sucesso!");
                logger.info("User logged in: {}", email);
                
                // Delay para mostrar a mensagem de sucesso
                PauseTransition delay = new PauseTransition(Duration.seconds(1));
                delay.setOnFinished(event -> navigateToMainScreen());
                delay.play();
            } else {
                showError("Não foi possível fazer login. Verifique se o email e senha estão corretos.");
                loginButton.setDisable(false);
                loginProgress.setVisible(false);
                passwordField.clear();
                passwordField.requestFocus();
            }
        });
        
        loginTask.setOnFailed(e -> {
            showError("Erro ao realizar login. Tente novamente.");
            logger.error("Login error", loginTask.getException());
            loginButton.setDisable(false);
            loginProgress.setVisible(false);
        });
        
        new Thread(loginTask).start();
    }
    
    private void handleForgotPassword() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showError("Digite seu email para recuperar a senha");
            emailField.requestFocus();
            return;
        }
        
        // TODO: Implementar recuperação de senha
        showSuccess("Instruções de recuperação de senha foram enviadas para seu email");
    }
    
    @FXML
    public void handleRegisterClick() {
        navigateToRegister();
    }
    
    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.getStyleClass().removeAll("success", "error");
        errorMessage.getStyleClass().add("error");
    }
    
    private void showSuccess(String message) {
        errorMessage.setText(message);
        errorMessage.getStyleClass().removeAll("success", "error");
        errorMessage.getStyleClass().add("success");
    }
    
    private void navigateToRegister() {
        try {
            // Carrega a tela de registro
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent registerContent = loader.load();
            
            // Cria um container para o fundo
            StackPane root = new StackPane(registerContent);
            root.getStyleClass().add("root");
            
            // Configura a cena
            Scene registerScene = new Scene(root, 800, 600);
            registerScene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
            
            // Configura a janela
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle("MyExpenses - Registro");
            stage.setScene(registerScene);
            
        } catch (IOException e) {
            logger.error("Error loading register screen", e);
            showError("Erro ao carregar tela de registro");
        }
    }
    
    private void navigateToMainScreen() {
        try {
            if (authenticatedUser == null) {
                throw new IllegalStateException("No authenticated user found");
            }
            
            // Carrega a tela principal ou de admin dependendo do papel do usuário
            String fxmlFile = authenticatedUser.isAdmin() ? "/fxml/admin.fxml" : "/fxml/main.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent mainContent = loader.load();
            
            // Configura o usuário no controlador
            if (authenticatedUser.isAdmin()) {
                ((AdminController) loader.getController()).setCurrentUser(authenticatedUser);
            } else {
                ((MainController) loader.getController()).setCurrentUser(authenticatedUser);
            }
            
            // Cria um container para o fundo
            StackPane root = new StackPane(mainContent);
            root.getStyleClass().add("root");
            
            // Aplica o tema atual
            if (themeToggle.isSelected()) {
                root.getStyleClass().add("dark");
            }
            
            // Configura a cena
            Scene mainScene = new Scene(root, 800, 600);
            mainScene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
            
            // Configura a janela
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle(authenticatedUser.isAdmin() ? "MyExpenses - Admin" : "MyExpenses");
            stage.setScene(mainScene);
            
            logger.info("Navigated to {} screen", authenticatedUser.isAdmin() ? "admin" : "main");
            
        } catch (Exception e) {
            logger.error("Error loading main screen", e);
            showError("Erro ao carregar tela principal: " + e.getMessage());
            loginButton.setDisable(false);
            loginProgress.setVisible(false);
        }
    }
}
