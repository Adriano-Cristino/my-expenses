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
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

public class RegisterController {
    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);
    private final UserDAO userDAO = new UserDAO();
    private final Preferences prefs = Preferences.userNodeForPackage(RegisterController.class);

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Label errorMessage;
    @FXML private ProgressIndicator registerProgress;
    @FXML private Hyperlink loginLink;
    @FXML private ToggleButton themeToggle;
    @FXML private FontIcon themeIcon;
    @FXML private ProgressBar passwordStrengthBar;
    @FXML private Label passwordStrengthLabel;
    @FXML private StackPane loadingPane;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$");

    @FXML
    public void initialize() {
        setupValidation();
        loadThemePreference();
        Platform.runLater(this::setupKeyboardShortcuts);
    }

    private void setupValidation() {
        emailField.textProperty().addListener((obs, old, newValue) -> {
            errorMessage.setText("");
            validateEmail(newValue);
        });

        passwordField.textProperty().addListener((obs, old, newValue) -> {
            errorMessage.setText("");
            validatePassword(newValue);
        });

        confirmPasswordField.textProperty().addListener((obs, old, newValue) -> {
            errorMessage.setText("");
            validateConfirmPassword(newValue);
        });
    }

    private void validateEmail(String email) {
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            emailField.setStyle("-fx-border-color: red;");
            showError("Email inválido");
        } else {
            emailField.setStyle("");
        }
    }

    private void validatePassword(String password) {
        if (!password.isEmpty() && password.length() < 8) {
            passwordField.setStyle("-fx-border-color: red;");
            showError("A senha deve ter pelo menos 8 caracteres");
        } else {
            passwordField.setStyle("");
        }
    }

    private void validateConfirmPassword(String confirmPassword) {
        if (!confirmPassword.isEmpty() && !confirmPassword.equals(passwordField.getText())) {
            confirmPasswordField.setStyle("-fx-border-color: red;");
            showError("As senhas não coincidem");
        } else {
            confirmPasswordField.setStyle("");
        }
    }

    @FXML
    public void handleRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validação
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Todos os campos são obrigatórios");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("As senhas não coincidem");
            return;
        }

        if (password.length() < 8) {
            showError("A senha deve ter pelo menos 8 caracteres");
            return;
        }

        // Desabilita o botão e mostra loading
        registerButton.setDisable(true);
        registerProgress.setVisible(true);

        Task<Boolean> registerTask = new Task<>() {
            @Override
            protected Boolean call() {
                try {
                    User user = new User();
                    user.setName(name);
                    user.setEmail(email);
                    user.setPassword(password);
                    return userDAO.register(user);
                } catch (Exception e) {
                    logger.error("Error registering user", e);
                    return false;
                }
            }
        };

        registerTask.setOnSucceeded(e -> {
            if (registerTask.getValue()) {
                showSuccess("Conta criada com sucesso! Você será redirecionado para o login em alguns segundos.");
                logger.info("User registered: {}", email);

                // Delay para mostrar a mensagem de sucesso
                PauseTransition delay = new PauseTransition(Duration.seconds(3));
                delay.setOnFinished(event -> navigateToLogin());
                delay.play();
            } else {
                showError("Email já cadastrado ou erro ao criar conta");
                registerButton.setDisable(false);
                registerProgress.setVisible(false);
            }
        });

        registerTask.setOnFailed(e -> {
            showError("Erro ao criar conta. Tente novamente.");
            logger.error("Registration error", registerTask.getException());
            registerButton.setDisable(false);
            registerProgress.setVisible(false);
        });

        new Thread(registerTask).start();
    }

    @FXML
    public void handleLoginClick() {
        navigateToLogin();
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

    private void setupKeyboardShortcuts() {
        Scene scene = nameField.getScene();
        if (scene != null) {
            scene.setOnKeyPressed(event -> {
                switch (event.getCode()) {
                    case ENTER:
                        if (!registerButton.isDisabled()) {
                            handleRegister();
                        } else {
                            showError("Por favor, corrija os erros antes de continuar");
                        }
                        break;
                }
            });
        }
    }

    private void loadThemePreference() {
        boolean isDarkTheme = prefs.getBoolean("darkTheme", false);
        themeToggle.setSelected(isDarkTheme);
        applyTheme(isDarkTheme);
    }

    @FXML
    private void toggleTheme() {
        boolean isDarkTheme = themeToggle.isSelected();
        prefs.putBoolean("darkTheme", isDarkTheme);
        applyTheme(isDarkTheme);
    }

    private void applyTheme(boolean isDarkTheme) {
        Scene scene = nameField.getScene();
        if (scene != null) {
            if (isDarkTheme) {
                scene.getRoot().getStyleClass().add("dark");
            } else {
                scene.getRoot().getStyleClass().remove("dark");
            }
            themeIcon.setIconLiteral(isDarkTheme ? "fas-sun" : "fas-moon");
        }
    }

    private void navigateToLogin() {
        try {
            // Carrega a tela de login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent loginContent = loader.load();

            // Cria um container para o fundo
            StackPane root = new StackPane(loginContent);
            root.getStyleClass().add("root");

            // Configura a cena
            Scene loginScene = new Scene(root, 800, 600);
            loginScene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());

            // Configura a janela
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setTitle("MyExpenses - Login");
            stage.setScene(loginScene);

        } catch (IOException e) {
            logger.error("Error loading login screen", e);
            showError("Erro ao carregar tela de login");
        }
    }
}
