package com.expenses.controller;

import com.expenses.dao.ExpenseDAO;
import com.expenses.model.Expense;
import com.expenses.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @FXML private DashboardSummaryController summaryController;
    @FXML private DashboardCalendarController calendarController;
    @FXML private DashboardExportController exportController;
    
    @FXML private TableView<Expense> recentExpensesTable;
    @FXML private TableColumn<Expense, LocalDate> dateColumn;
    @FXML private TableColumn<Expense, String> descriptionColumn;
    @FXML private TableColumn<Expense, String> categoryColumn;
    @FXML private TableColumn<Expense, Double> valueColumn;
    @FXML private TableColumn<Expense, Void> actionsColumn;
    
    private final ExpenseDAO expenseDAO;
    private User currentUser;

    public DashboardController() {
        this.expenseDAO = new ExpenseDAO();
    }

    @FXML
    private void initialize() {
        setupRecentExpensesTable();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        logger.debug("Definindo usuário atual no DashboardController: {}", user.getEmail());
        
        try {
            // Verificar se os controladores foram injetados corretamente
            if (summaryController == null) {
                logger.error("summaryController não foi injetado!");
                throw new RuntimeException("summaryController não foi injetado!");
            }
            if (calendarController == null) {
                logger.error("calendarController não foi injetado!");
                throw new RuntimeException("calendarController não foi injetado!");
            }
            if (exportController == null) {
                logger.error("exportController não foi injetado!");
                throw new RuntimeException("exportController não foi injetado!");
            }
            
            // Propagar o usuário atual para os controladores filhos
            logger.debug("Propagando usuário para os controladores filhos");
            summaryController.setCurrentUser(user);
            calendarController.setCurrentUser(user);
            exportController.setCurrentUser(user);
            
            updateDashboard();
            
        } catch (Exception e) {
            logger.error("Erro ao configurar o dashboard para o usuário: {}", user.getEmail(), e);
            throw new RuntimeException("Erro ao configurar o dashboard", e);
        }
    }

    private void setupRecentExpensesTable() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        categoryColumn.setCellValueFactory(param -> {
            Long categoryId = param.getValue().getCategoryId();
            // TODO: Buscar nome da categoria
            return new javafx.beans.property.SimpleStringProperty("Categoria " + categoryId);
        });
        valueColumn.setCellValueFactory(param -> {
            return new javafx.beans.property.SimpleObjectProperty<>(
                param.getValue().getAmount().doubleValue()
            );
        });
        
        setupActionsColumn();
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("Editar");
            private final Button deleteButton = new Button("Excluir");
            
            {
                editButton.setOnAction(e -> {
                    Expense expense = getTableRow().getItem();
                    if (expense != null) {
                        handleEditExpense(expense);
                    }
                });
                
                deleteButton.setOnAction(e -> {
                    Expense expense = getTableRow().getItem();
                    if (expense != null) {
                        handleDeleteExpense(expense);
                    }
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    var container = new javafx.scene.layout.HBox(5);
                    container.getChildren().addAll(editButton, deleteButton);
                    setGraphic(container);
                }
            }
        });
    }

    private void handleEditExpense(Expense expense) {
        // TODO: Implementar edição de despesa
        logger.info("Editar despesa: {}", expense);
    }

    private void handleDeleteExpense(Expense expense) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Exclusão");
        alert.setHeaderText("Excluir Despesa");
        alert.setContentText("Tem certeza que deseja excluir esta despesa?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    expenseDAO.delete(expense.getId(), currentUser.getId());
                    updateDashboard();
                } catch (Exception e) {
                    logger.error("Erro ao excluir despesa", e);
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Erro");
                    errorAlert.setHeaderText("Erro ao excluir despesa");
                    errorAlert.setContentText("Não foi possível excluir a despesa. Por favor, tente novamente.");
                    errorAlert.show();
                }
            }
        });
    }

    public void updateDashboard() {
        if (currentUser == null) {
            return;
        }

        try {
            // Atualizar tabela de despesas recentes
            List<Expense> recentExpenses = expenseDAO.findByDateRange(
                currentUser.getId(),
                LocalDate.now().minusMonths(1),
                LocalDate.now()
            ).stream().limit(10).toList();
            
            recentExpensesTable.getItems().setAll(recentExpenses);
            
            // Os controladores filhos são responsáveis por atualizar suas próprias visualizações
            summaryController.updateDashboard();
            calendarController.updateCalendar();
        } catch (Exception e) {
            logger.error("Erro ao atualizar dashboard", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Erro ao atualizar dashboard");
            alert.setContentText("Não foi possível atualizar os dados do dashboard. Por favor, tente novamente.");
            alert.show();
        }
    }
}