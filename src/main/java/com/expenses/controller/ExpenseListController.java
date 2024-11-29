package com.expenses.controller;

import com.expenses.dao.CategoryDAO;
import com.expenses.dao.ExpenseDAO;
import com.expenses.model.Category;
import com.expenses.model.Expense;
import com.expenses.model.User;
import com.expenses.service.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ExpenseListController {
    private static final Logger logger = LoggerFactory.getLogger(ExpenseListController.class);
    
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private TableView<Expense> expenseTable;
    @FXML private TableColumn<Expense, String> dateColumn;
    @FXML private TableColumn<Expense, String> descriptionColumn;
    @FXML private TableColumn<Expense, String> categoryColumn;
    @FXML private TableColumn<Expense, String> amountColumn;
    @FXML private TableColumn<Expense, Void> actionsColumn;
    @FXML private Label totalLabel;
    
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    
    private User currentUser;

    @FXML
    public void initialize() {
        setupDatePickers();
        setupCategoryComboBox();
        setupExpenseTable();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadData();
    }

    private void setupDatePickers() {
        startDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        endDatePicker.setValue(LocalDate.now());
        
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> refreshExpenses());
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> refreshExpenses());
    }

    private void setupCategoryComboBox() {
        categoryComboBox.setPromptText("Todas as Categorias");
        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> refreshExpenses());
    }

    private void setupExpenseTable() {
        dateColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                dateFormatter.format(cellData.getValue().getDate())
            )
        );
        
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        categoryColumn.setCellValueFactory(cellData -> {
            Category category = findCategoryById(cellData.getValue().getCategoryId());
            return new javafx.beans.property.SimpleStringProperty(
                category != null ? category.getName() : ""
            );
        });
        
        amountColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                currencyFormatter.format(cellData.getValue().getAmount())
            )
        );

        setupActionsColumn();
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Editar");
            private final Button deleteButton = new Button("Excluir");
            private final HBox buttons = new HBox(5, editButton, deleteButton);

            {
                editButton.setOnAction(event -> {
                    Expense expense = getTableRow().getItem();
                    if (expense != null) {
                        handleEditExpense(expense);
                    }
                });

                deleteButton.setOnAction(event -> {
                    Expense expense = getTableRow().getItem();
                    if (expense != null) {
                        handleDeleteExpense(expense);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    private void loadData() {
        loadCategories();
        refreshExpenses();
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryDAO.findAllByUserId(SessionManager.getCurrentUser().getId());
            categoryComboBox.setItems(FXCollections.observableArrayList(categories));
        } catch (Exception e) {
            showErrorMessage("Erro ao carregar categorias: " + e.getMessage());
        }
    }

    public void refreshExpenses() {
        if (currentUser == null) return;

        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        Category selectedCategory = categoryComboBox.getValue();

        List<Expense> expenses = expenseDAO.findWithFilters(
            currentUser.getId(),
            startDate,
            endDate,
            selectedCategory != null ? selectedCategory.getId() : null,
            null,
            null
        );

        expenseTable.setItems(FXCollections.observableArrayList(expenses));
        updateTotal(expenses);
    }

    private void updateTotal(List<Expense> expenses) {
        BigDecimal total = expenses.stream()
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        totalLabel.setText("Total: " + currencyFormatter.format(total));
    }

    private void handleEditExpense(Expense expense) {
        // Implementar edição de despesa
    }

    private void handleDeleteExpense(Expense expense) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Exclusão");
        alert.setHeaderText("Excluir despesa");
        alert.setContentText("Tem certeza que deseja excluir esta despesa?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            expenseDAO.delete(expense.getId(), currentUser.getId());
            refreshExpenses();
        }
    }

    private void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Category findCategoryById(Long categoryId) {
        if (categoryId == null) return null;
        return categoryDAO.findById(categoryId, SessionManager.getCurrentUser().getId())
            .orElse(null);
    }
}
