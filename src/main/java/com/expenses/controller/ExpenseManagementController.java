package com.expenses.controller;

import com.expenses.dao.CategoryDAO;
import com.expenses.dao.ExpenseDAO;
import com.expenses.model.Category;
import com.expenses.model.Expense;
import com.expenses.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class ExpenseManagementController {
    private static final Logger logger = LoggerFactory.getLogger(ExpenseManagementController.class);

    @FXML private TableView<Expense> expenseTable;
    @FXML private TableColumn<Expense, LocalDate> dateColumn;
    @FXML private TableColumn<Expense, String> descriptionColumn;
    @FXML private TableColumn<Expense, Category> categoryColumn;
    @FXML private TableColumn<Expense, BigDecimal> amountColumn;
    @FXML private TableColumn<Expense, Void> actionsColumn;

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<Category> categoryFilter;
    @FXML private TextField minValueFilter;
    @FXML private TextField maxValueFilter;
    @FXML private Label totalLabel;

    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final ObservableList<Expense> expenses = FXCollections.observableArrayList();
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private User currentUser;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFilters();
        setupActionsColumn();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadData();
    }

    private void setupTableColumns() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        
        // Formatação personalizada para o valor
        amountColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(currencyFormatter.format(amount));
                }
            }
        });
    }

    private void setupFilters() {
        // Configurar data inicial e final
        startDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        endDatePicker.setValue(LocalDate.now());

        // Carregar categorias no filtro
        loadCategories();

        // Adicionar listeners para atualização automática
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> loadExpenses());
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> loadExpenses());
        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> loadExpenses());
        
        // Validação de valores numéricos
        minValueFilter.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                minValueFilter.setText(oldVal);
            }
        });
        
        maxValueFilter.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                maxValueFilter.setText(oldVal);
            }
        });
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Editar");
            private final Button deleteButton = new Button("Excluir");
            private final HBox container = new HBox(5, editButton, deleteButton);

            {
                editButton.setOnAction(event -> {
                    Expense expense = getTableView().getItems().get(getIndex());
                    handleEditExpense(expense);
                });

                deleteButton.setOnAction(event -> {
                    Expense expense = getTableView().getItems().get(getIndex());
                    handleDeleteExpense(expense);
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

    private void loadData() {
        loadCategories();
        loadExpenses();
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryDAO.findAllByUserId(currentUser.getId());
            categoryFilter.setItems(FXCollections.observableArrayList(categories));
        } catch (Exception e) {
            logger.error("Erro ao carregar categorias", e);
            showError("Erro ao carregar categorias");
        }
    }

    private Category findCategoryById(Long categoryId) {
        if (categoryId == null) return null;
        return categoryDAO.findById(categoryId, currentUser.getId())
            .orElse(null);
    }

    private void loadExpenses() {
        try {
            // Obter filtros
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            Category selectedCategory = categoryFilter.getValue();
            BigDecimal minValue = null;
            BigDecimal maxValue = null;

            if (!minValueFilter.getText().isEmpty()) {
                minValue = new BigDecimal(minValueFilter.getText());
            }
            if (!maxValueFilter.getText().isEmpty()) {
                maxValue = new BigDecimal(maxValueFilter.getText());
            }

            // Carregar despesas com filtros
            List<Expense> filteredExpenses = expenseDAO.findWithFilters(
                currentUser.getId(),
                startDate,
                endDate,
                selectedCategory != null ? selectedCategory.getId() : null,
                minValue != null ? minValue.doubleValue() : null,
                maxValue != null ? maxValue.doubleValue() : null
            );

            expenses.setAll(filteredExpenses);
            expenseTable.setItems(expenses);
            updateTotal();
        } catch (NumberFormatException e) {
            showError("Valores mínimo e máximo devem ser números válidos");
        } catch (Exception e) {
            logger.error("Erro ao filtrar despesas", e);
            showError("Erro ao filtrar despesas");
        }
    }

    private BigDecimal parseValue(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void updateTotal() {
        BigDecimal total = expenses.stream()
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalLabel.setText(currencyFormatter.format(total));
    }

    @FXML
    private void handleAddExpense() {
        Expense expense = Expense.builder()
            .description("")
            .amount(BigDecimal.ZERO)
            .date(LocalDate.now())
            .userId(currentUser.getId())
            .build();
        showExpenseDialog(expense);
    }

    private void handleEditExpense(Expense expense) {
        Expense expenseToEdit = Expense.builder()
            .id(expense.getId())
            .description(expense.getDescription())
            .amount(expense.getAmount())
            .date(expense.getDate())
            .categoryId(expense.getCategoryId())
            .userId(expense.getUserId())
            .build();
        showExpenseDialog(expenseToEdit);
    }

    private void showExpenseDialog(Expense expense) {
        try {
            Dialog<Expense> dialog = new Dialog<>();
            dialog.setTitle(expense.getId() == null ? "Nova Despesa" : "Editar Despesa");
            dialog.setHeaderText(null);

            // Configurar botões
            ButtonType saveButtonType = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            // Criar formulário
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            TextField descriptionField = new TextField();
            DatePicker datePicker = new DatePicker();
            ComboBox<Category> categoryCombo = new ComboBox<>(categoryFilter.getItems());
            TextField amountField = new TextField();

            grid.add(new Label("Descrição:"), 0, 0);
            grid.add(descriptionField, 1, 0);
            grid.add(new Label("Data:"), 0, 1);
            grid.add(datePicker, 1, 1);
            grid.add(new Label("Categoria:"), 0, 2);
            grid.add(categoryCombo, 1, 2);
            grid.add(new Label("Valor:"), 0, 3);
            grid.add(amountField, 1, 3);

            dialog.getDialogPane().setContent(grid);

            // Preencher campos se for edição
            if (expense.getId() != null) {
                descriptionField.setText(expense.getDescription());
                datePicker.setValue(expense.getDate());
                categoryCombo.setValue(categoryDAO.findById(expense.getCategoryId(), currentUser.getId())
                    .orElse(null));
                amountField.setText(expense.getAmount().toString());
            }

            // Converter o resultado
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    try {
                        Expense result = Expense.builder()
                            .id(expense.getId())
                            .description(descriptionField.getText())
                            .amount(new BigDecimal(amountField.getText()))
                            .date(datePicker.getValue())
                            .categoryId(categoryCombo.getValue().getId())
                            .userId(currentUser.getId())
                            .build();
                        return result;
                    } catch (Exception e) {
                        showError("Valores inválidos");
                        return null;
                    }
                }
                return null;
            });

            dialog.showAndWait().ifPresent(result -> {
                try {
                    if (result.getId() == null) {
                        expenseDAO.create(result);
                    } else {
                        expenseDAO.update(result);
                    }
                    loadExpenses();
                    showSuccess("Despesa salva com sucesso!");
                } catch (Exception e) {
                    logger.error("Erro ao salvar despesa", e);
                    showError("Erro ao salvar despesa");
                }
            });

        } catch (Exception e) {
            logger.error("Erro ao abrir diálogo", e);
            showError("Erro ao abrir diálogo");
        }
    }

    private void handleDeleteExpense(Expense expense) {
        if (showConfirmationDialog("Confirmar exclusão", "Tem certeza que deseja excluir esta despesa?")) {
            try {
                expenseDAO.delete(expense.getId(), currentUser.getId());
                loadExpenses();
                showSuccess("Despesa excluída com sucesso!");
            } catch (Exception e) {
                logger.error("Erro ao excluir despesa", e);
                showError("Erro ao excluir despesa");
            }
        }
    }

    @FXML
    private void handleClearFilters() {
        startDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        endDatePicker.setValue(LocalDate.now());
        categoryFilter.setValue(null);
        minValueFilter.clear();
        maxValueFilter.clear();
        loadExpenses();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
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

    private boolean showConfirmationDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}
