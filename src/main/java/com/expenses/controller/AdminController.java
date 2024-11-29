package com.expenses.controller;

import com.expenses.dao.ExpenseDAO;
import com.expenses.dao.UserDAO;
import com.expenses.model.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final UserDAO userDAO = new UserDAO();
    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private User currentUser;

    @FXML private Label welcomeLabel;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, Void> actionsColumn;
    
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TableView<UserReport> reportTable;
    @FXML private TableColumn<UserReport, String> userNameColumn;
    @FXML private TableColumn<UserReport, String> totalExpensesColumn;
    @FXML private TableColumn<UserReport, Integer> categoryCountColumn;
    @FXML private TableColumn<UserReport, Integer> expenseCountColumn;

    public void setCurrentUser(User user) {
        this.currentUser = user;
        welcomeLabel.setText("Bem-vindo(a), " + user.getName());
        loadData();
    }

    @FXML
    public void initialize() {
        setupUsersTable();
        setupReportTable();
        setupDatePickers();
    }

    private void setupUsersTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        setupActionsColumn();
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Editar");
            private final Button deleteButton = new Button("Excluir");
            private final HBox container = new HBox(5, editButton, deleteButton);

            {
                editButton.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleEditUser(user);
                });

                deleteButton.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDeleteUser(user);
                });

                editButton.getStyleClass().add("edit-button");
                deleteButton.getStyleClass().add("delete-button");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void setupReportTable() {
        userNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUserName()));
        totalExpensesColumn.setCellValueFactory(data -> new SimpleStringProperty(
            String.format("R$ %.2f", data.getValue().getTotalExpenses().doubleValue())
        ));
        categoryCountColumn.setCellValueFactory(new PropertyValueFactory<>("categoryCount"));
        expenseCountColumn.setCellValueFactory(new PropertyValueFactory<>("expenseCount"));
    }

    private void setupDatePickers() {
        startDatePicker.setValue(LocalDate.now().minusMonths(1));
        endDatePicker.setValue(LocalDate.now());
    }

    private void loadData() {
        loadUsers();
        generateReport();
    }

    private void loadUsers() {
        List<User> users = userDAO.findAll();
        usersTable.setItems(FXCollections.observableArrayList(users));
    }

    @FXML
    private void handleGenerateReport() {
        generateReport();
    }

    private void generateReport() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            showError("Por favor, selecione as datas para o relatório");
            return;
        }

        if (startDate.isAfter(endDate)) {
            showError("A data inicial deve ser anterior à data final");
            return;
        }

        List<User> users = userDAO.findAll();
        List<UserReport> reports = users.stream()
            .map(user -> {
                BigDecimal total = expenseDAO.getTotalExpensesByUser(user.getId(), startDate, endDate);
                int categoryCount = expenseDAO.getCategoryCountByUser(user.getId());
                int expenseCount = expenseDAO.getExpenseCountByUser(user.getId(), startDate, endDate);
                
                return new UserReport(user.getName(), total, categoryCount, expenseCount);
            })
            .toList();

        reportTable.setItems(FXCollections.observableArrayList(reports));
    }

    private void handleEditUser(User user) {
        // TODO: Implementar edição de usuário
        showInfo("Funcionalidade em desenvolvimento");
    }

    private void handleDeleteUser(User user) {
        if (user.isAdmin()) {
            showError("Não é possível excluir o usuário administrador");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Exclusão");
        alert.setHeaderText(null);
        alert.setContentText("Tem certeza que deseja excluir o usuário " + user.getName() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userDAO.delete(user.getId());
                loadUsers();
                showSuccess("Usuário excluído com sucesso!");
            } catch (Exception e) {
                logger.error("Error deleting user", e);
                showError("Erro ao excluir usuário");
            }
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();

            MainController controller = loader.getController();
            controller.setCurrentUser(currentUser);

            Scene scene = new Scene(root);
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(scene);

        } catch (IOException e) {
            logger.error("Error loading main screen", e);
            showError("Erro ao carregar tela principal");
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(scene);

        } catch (IOException e) {
            logger.error("Error during logout", e);
            showError("Erro ao fazer logout");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informação");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sucesso");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class UserReport {
        private final String userName;
        private final BigDecimal totalExpenses;
        private final int categoryCount;
        private final int expenseCount;

        public UserReport(String userName, BigDecimal totalExpenses, int categoryCount, int expenseCount) {
            this.userName = userName;
            this.totalExpenses = totalExpenses;
            this.categoryCount = categoryCount;
            this.expenseCount = expenseCount;
        }

        public String getUserName() {
            return userName;
        }

        public BigDecimal getTotalExpenses() {
            return totalExpenses;
        }

        public int getCategoryCount() {
            return categoryCount;
        }

        public int getExpenseCount() {
            return expenseCount;
        }
    }
}
