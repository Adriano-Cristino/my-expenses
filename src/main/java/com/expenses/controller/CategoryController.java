package com.expenses.controller;

import com.expenses.dao.CategoryDAO;
import com.expenses.model.Category;
import com.expenses.model.User;
import com.expenses.service.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CategoryController {
    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, String> nameColumn;
    @FXML private TableColumn<Category, String> descriptionColumn;
    @FXML private TableColumn<Category, Void> actionsColumn;

    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    private User currentUser;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupActionsColumn();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadCategories();
    }

    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Editar");
            private final Button deleteButton = new Button("Excluir");

            {
                editButton.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    handleEditCategory(category);
                });

                deleteButton.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    handleDeleteCategory(category);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(new HBox(5, editButton, deleteButton));
                }
            }
        });
    }

    private void loadCategories() {
        try {
            List<Category> userCategories = categoryDAO.findAllByUserId(SessionManager.getCurrentUser().getId());
            categories.setAll(userCategories);
            categoryTable.setItems(categories);
        } catch (Exception e) {
            logger.error("Erro ao carregar categorias", e);
            showError("Erro ao carregar categorias");
        }
    }

    @FXML
    private void handleAddCategory() {
        Category category = Category.builder()
            .name("")
            .description("")
            .userId(SessionManager.getCurrentUser().getId())
            .build();
        showCategoryDialog(category);
    }

    private void handleEditCategory(Category category) {
        Category categoryToEdit = Category.builder()
            .id(category.getId())
            .name(category.getName())
            .description(category.getDescription())
            .userId(category.getUserId())
            .build();
        showCategoryDialog(categoryToEdit);
    }

    private void handleDeleteCategory(Category category) {
        if (showConfirmationDialog("Confirmar exclusão", "Tem certeza que deseja excluir esta categoria?")) {
            try {
                categoryDAO.delete(category.getId(), SessionManager.getCurrentUser().getId());
                loadCategories();
                showSuccess("Categoria excluída com sucesso!");
            } catch (Exception e) {
                logger.error("Erro ao excluir categoria", e);
                showError("Erro ao excluir categoria");
            }
        }
    }

    private void showCategoryDialog(Category category) {
        TextInputDialog dialog = new TextInputDialog(category.getName());
        dialog.setTitle(category.getId() == null ? "Nova Categoria" : "Editar Categoria");
        dialog.setHeaderText(category.getId() == null ? "Adicionar nova categoria" : "Editar categoria");
        dialog.setContentText("Nome da categoria:");

        dialog.showAndWait().ifPresent(name -> {
            try {
                category.setName(name);
                if (category.getId() == null) {
                    categoryDAO.create(category);
                } else {
                    categoryDAO.update(category);
                }
                loadCategories();
                showSuccess(category.getId() == null ? "Categoria adicionada com sucesso!" : "Categoria atualizada com sucesso!");
            } catch (Exception e) {
                logger.error("Erro ao salvar categoria", e);
                showError("Erro ao salvar categoria");
            }
        });
    }

    private boolean showConfirmationDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
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
}
