package com.expenses.controller;

import com.expenses.dao.CategoryDAO;
import com.expenses.model.Category;
import com.expenses.model.Expense;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

public class ExpenseDialogController {
    private static final Logger logger = LoggerFactory.getLogger(ExpenseDialogController.class);
    private final CategoryDAO categoryDAO = new CategoryDAO();
    
    @FXML private TextField descriptionField;
    @FXML private TextField amountField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label messageLabel;
    
    private Long userId;
    private Expense expense;
    private Consumer<Expense> onSave;
    
    @FXML
    public void initialize() {
        setupValidation();
        setupButtons();
    }
    
    public void setData(Long userId, Expense expense, Consumer<Expense> onSave) {
        this.userId = userId;
        this.expense = expense;
        this.onSave = onSave;
        
        loadCategories();
        
        if (expense != null) {
            descriptionField.setText(expense.getDescription());
            amountField.setText(expense.getAmount().toString());
            datePicker.setValue(expense.getDate());
            categoryComboBox.getItems().stream()
                .filter(c -> c.getId().equals(expense.getCategoryId()))
                .findFirst()
                .ifPresent(categoryComboBox::setValue);
        } else {
            datePicker.setValue(LocalDate.now());
        }
    }
    
    private void setupValidation() {
        // Permitir apenas números e ponto decimal no campo de valor
        amountField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                amountField.setText(oldValue);
            }
        });
    }
    
    private void setupButtons() {
        saveButton.setOnAction(event -> handleSave());
        cancelButton.setOnAction(event -> handleCancel());
    }
    
    private void loadCategories() {
        List<Category> categories = categoryDAO.findAllByUserId(userId);
        categoryComboBox.getItems().addAll(categories);
        
        // Configurar como as categorias são exibidas
        categoryComboBox.setCellFactory(param -> new ListCell<Category>() {
            @Override
            protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);
                if (empty || category == null) {
                    setText(null);
                } else {
                    setText(category.getName());
                }
            }
        });
        
        categoryComboBox.setButtonCell(new ListCell<Category>() {
            @Override
            protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);
                if (empty || category == null) {
                    setText(null);
                } else {
                    setText(category.getName());
                }
            }
        });
    }
    
    private void handleSave() {
        if (!validateFields()) {
            return;
        }
        
        try {
            String description = descriptionField.getText().trim();
            BigDecimal amount = new BigDecimal(amountField.getText());
            LocalDate date = datePicker.getValue();
            Category category = categoryComboBox.getValue();
            
            Expense newExpense = (expense != null) ? expense : Expense.builder().build();
            newExpense.setDescription(description);
            newExpense.setAmount(amount);
            newExpense.setDate(date);
            newExpense.setCategoryId(category.getId());
            newExpense.setUserId(userId);
            
            onSave.accept(newExpense);
            closeDialog();
            
        } catch (Exception e) {
            logger.error("Error saving expense", e);
            showError("Erro ao salvar despesa");
        }
    }
    
    private boolean validateFields() {
        if (descriptionField.getText().trim().isEmpty()) {
            showError("Por favor, informe a descrição");
            return false;
        }
        
        if (amountField.getText().trim().isEmpty()) {
            showError("Por favor, informe o valor");
            return false;
        }
        
        try {
            BigDecimal amount = new BigDecimal(amountField.getText());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showError("O valor deve ser maior que zero");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Valor inválido");
            return false;
        }
        
        if (datePicker.getValue() == null) {
            showError("Por favor, selecione a data");
            return false;
        }
        
        if (categoryComboBox.getValue() == null) {
            showError("Por favor, selecione a categoria");
            return false;
        }
        
        return true;
    }
    
    private void handleCancel() {
        closeDialog();
    }
    
    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
    
    private void showError(String message) {
        messageLabel.setTextFill(Color.RED);
        messageLabel.setText(message);
    }
}
