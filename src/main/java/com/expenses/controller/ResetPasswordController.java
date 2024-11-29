package com.expenses.controller;

import com.expenses.dao.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ResetPasswordController {
    private static final Logger logger = LoggerFactory.getLogger(ResetPasswordController.class);
    private final UserDAO userDAO = new UserDAO();
    private String token;
    
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button resetButton;
    @FXML private Label messageLabel;
    
    @FXML
    public void initialize() {
        resetButton.setOnAction(event -> handleResetPassword());
        
        // Limpar mensagem quando o usuário digita
        newPasswordField.textProperty().addListener((obs, old, newValue) -> messageLabel.setText(""));
        confirmPasswordField.textProperty().addListener((obs, old, newValue) -> messageLabel.setText(""));
    }
    
    public void setToken(String token) {
        this.token = token;
        
        // Valida o token assim que for definido
        if (!userDAO.validateRecoveryToken(token)) {
            showError("Link de recuperação inválido ou expirado");
            resetButton.setDisable(true);
        }
    }
    
    private void handleResetPassword() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Por favor, preencha todos os campos");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showError("As senhas não coincidem");
            return;
        }
        
        if (newPassword.length() < 6) {
            showError("A senha deve ter pelo menos 6 caracteres");
            return;
        }
        
        try {
            userDAO.updatePassword(token, newPassword);
            showSuccess("Senha atualizada com sucesso!");
            
            // Aguarda um pouco antes de redirecionar para o login
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(this::navigateToLogin);
                } catch (InterruptedException e) {
                    logger.error("Error during redirect delay", e);
                }
            }).start();
            
        } catch (Exception e) {
            showError("Erro ao atualizar senha. Tente novamente.");
            logger.error("Error resetting password", e);
        }
    }
    
    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            Stage stage = (Stage) newPasswordField.getScene().getWindow();
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
}
