package com.expenses.controller;

import com.expenses.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML private BorderPane mainContainer;
    @FXML private Label welcomeLabel;
    @FXML private StackPane contentArea;

    private User currentUser;
    private final Map<String, Parent> screenCache = new HashMap<>();
    private final Map<String, Object> controllerCache = new HashMap<>();

    @FXML
    public void initialize() {
        // Inicializa com a dashboard
        navigateTo("Dashboard");
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        welcomeLabel.setText("Bem-vindo(a), " + user.getName());
        
        // Atualiza o usuário nos controladores em cache
        controllerCache.values().forEach(controller -> {
            if (controller instanceof DashboardController) {
                ((DashboardController) controller).setCurrentUser(user);
            } else if (controller instanceof ExpenseListController) {
                ((ExpenseListController) controller).setCurrentUser(user);
            } else if (controller instanceof CategoryController) {
                ((CategoryController) controller).setCurrentUser(user);
            }
        });
    }

    @FXML
    private void navigateToDashboard() {
        navigateTo("Dashboard");
    }

    @FXML
    private void navigateToExpenses() {
        navigateTo("ExpenseList");
    }

    @FXML
    private void navigateToCategories() {
        navigateTo("Category");
    }

    @FXML
    private void navigateToProfile() {
        navigateTo("Profile");
    }

    private void navigateTo(String screenName) {
        try {
            logger.debug("Iniciando navegação para: {}", screenName);
            
            if (!screenCache.containsKey(screenName)) {
                logger.debug("Tela {} não está em cache, carregando...", screenName);
                loadScreen(screenName);
            }
            
            Parent screen = screenCache.get(screenName);
            if (screen == null) {
                throw new RuntimeException("Tela não foi carregada corretamente: " + screenName);
            }
            
            contentArea.getChildren().setAll(screen);
            logger.info("Navegou para a tela: {}", screenName);
            
        } catch (Exception e) {
            logger.error("Erro ao navegar para: {}. Erro: {}", screenName, e.getMessage(), e);
            showError("Erro ao carregar a tela: " + screenName + "\nErro: " + e.getMessage());
        }
    }

    private void loadScreen(String screenName) throws IOException {
        logger.debug("Carregando tela: {}", screenName);
        String fxmlPath = "/fxml/" + screenName.toLowerCase() + ".fxml";
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            if (loader.getLocation() == null) {
                throw new IOException("Arquivo FXML não encontrado: " + fxmlPath);
            }
            
            Parent screen = loader.load();
            Object controller = loader.getController();
            
            if (controller == null) {
                throw new RuntimeException("Controlador não foi carregado para: " + screenName);
            }

            logger.debug("Configurando usuário atual para o controlador: {}", controller.getClass().getSimpleName());
            
            // Configura o usuário atual no controlador
            if (currentUser != null) {
                try {
                    if (controller instanceof DashboardController) {
                        ((DashboardController) controller).setCurrentUser(currentUser);
                    } else if (controller instanceof ExpenseListController) {
                        ((ExpenseListController) controller).setCurrentUser(currentUser);
                    } else if (controller instanceof CategoryController) {
                        ((CategoryController) controller).setCurrentUser(currentUser);
                    }
                } catch (Exception e) {
                    logger.error("Erro ao configurar usuário no controlador: {}", controller.getClass().getSimpleName(), e);
                    throw new RuntimeException("Erro ao configurar usuário no controlador", e);
                }
            }

            screenCache.put(screenName, screen);
            controllerCache.put(screenName, controller);
            logger.debug("Tela {} carregada com sucesso", screenName);
            
        } catch (IOException e) {
            logger.error("Erro ao carregar arquivo FXML: {}", fxmlPath, e);
            throw e;
        } catch (Exception e) {
            logger.error("Erro ao carregar tela: {}", screenName, e);
            throw new RuntimeException("Erro ao carregar tela: " + screenName, e);
        }
    }

    private void showError(String message) {
        // Implementar exibição de erro (pode usar AlertHelper ou outro método)
        logger.error(message);
    }

    @FXML
    private void logout() {
        try {
            // Limpa os caches
            screenCache.clear();
            controllerCache.clear();

            // Carrega a tela de login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent loginScreen = loader.load();

            // Substitui a cena atual
            mainContainer.getScene().setRoot(loginScreen);
            logger.info("Usuário deslogado com sucesso");
        } catch (IOException e) {
            logger.error("Erro ao fazer logout", e);
            showError("Erro ao fazer logout");
        }
    }
}