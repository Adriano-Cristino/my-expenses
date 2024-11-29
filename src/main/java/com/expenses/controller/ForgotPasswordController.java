package com.expenses.controller;

import com.expenses.dao.UserDAO;
import com.expenses.service.EmailService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

public class ForgotPasswordController {
    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordController.class);
    private final UserDAO userDAO = new UserDAO();
    private final EmailService emailService = new EmailService();
    
    @FXML private TextField emailField;
    @FXML private Button sendButton;
    @FXML private Hyperlink loginLink;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        sendButton.setOnAction(event -> handleSendRecoveryEmail());
        loginLink.setOnAction(event -> navigateToLogin());
        
        // Limpar mensagem quando o usuário digita
        emailField.textProperty().addListener((obs, old, newValue) -> messageLabel.setText(""));
    }

    private void handleSendRecoveryEmail() {
        String email = emailField.getText().trim();
        
        if (email.isEmpty()) {
            showError("Por favor, informe seu email");
            return;
        }
        
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Email inválido");
            return;
        }
        
        try {
            if (!userDAO.emailExists(email)) {
                showError("Email não encontrado");
                return;
            }
            
            // Gera um token único para recuperação
            String token = UUID.randomUUID().toString();
            
            // Salva o token no banco
            userDAO.saveRecoveryToken(email, token);
            
            // Envia o email com o link de recuperação
            String recoveryLink = "http://localhost:8080/reset-password?token=" + token;
            emailService.sendRecoveryEmail(email, recoveryLink);
            
            showSuccess("Email de recuperação enviado com sucesso!");
            logger.info("Recovery email sent to: {}", email);
            
            // Aguarda um pouco antes de redirecionar para o login
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    javafx.application.Platform.runLater(this::navigateToLogin);
                } catch (InterruptedException e) {
                    logger.error("Error during redirect delay", e);
                }
            }).start();
            
        } catch (Exception e) {
            showError("Erro ao enviar email de recuperação. Tente novamente.");
            logger.error("Error sending recovery email", e);
        }
    }
    
    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle("MyExpenses - Login");
            stage.setScene(scene);
            
        } catch (IOException e) {
            logger.error("Error navigating to login", e);
            showError("Erro ao navegar para a tela de login");
        }
    }
    
    private void showError(String message) {
        messageLabel.setTextFill(Color.RED);
        messageLabel.setText(message);
    }
    
    private void showSuccess(String message) {
        messageLabel.setTextFill(Color.GREEN);
        messageLabel.setText(message);
    }
    
    private void showInfo(String message) {
        messageLabel.setTextFill(Color.BLUE);
        messageLabel.setText(message);
    }
}
