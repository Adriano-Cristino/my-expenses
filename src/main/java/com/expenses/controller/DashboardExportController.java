package com.expenses.controller;

import com.expenses.dao.ExpenseDAO;
import com.expenses.model.Expense;
import com.expenses.model.User;
import com.expenses.util.ExportUtil;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class DashboardExportController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardExportController.class);

    @FXML private JFXComboBox<String> exportPeriodComboBox;
    @FXML private DatePicker exportStartDate;
    @FXML private DatePicker exportEndDate;
    @FXML private JFXComboBox<String> exportFormatComboBox;
    @FXML private JFXCheckBox includeChartsCheckbox;
    @FXML private JFXCheckBox includeSummaryCheckbox;
    @FXML private JFXCheckBox includeDetailsCheckbox;
    @FXML private JFXButton exportButton;
    @FXML private TableView<ExportHistory> recentExportsTable;
    
    private final ExpenseDAO expenseDAO;
    private User currentUser;

    public DashboardExportController() {
        this.expenseDAO = new ExpenseDAO();
    }

    @FXML
    private void initialize() {
        setupExportPeriodComboBox();
        setupExportFormatComboBox();
        setupExportButton();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    private void setupExportPeriodComboBox() {
        exportPeriodComboBox.getItems().addAll(
            "Últimos 7 dias",
            "Últimos 30 dias",
            "Este mês",
            "Mês passado",
            "Este ano",
            "Personalizado"
        );
        exportPeriodComboBox.setValue("Este mês");
        exportPeriodComboBox.setOnAction(e -> handlePeriodChange());
    }

    private void setupExportFormatComboBox() {
        exportFormatComboBox.getItems().addAll(
            "PDF",
            "Excel (XLSX)"
        );
        exportFormatComboBox.setValue("PDF");
    }

    private void handlePeriodChange() {
        LocalDate now = LocalDate.now();
        boolean isCustomPeriod = "Personalizado".equals(exportPeriodComboBox.getValue());
        
        exportStartDate.setDisable(!isCustomPeriod);
        exportEndDate.setDisable(!isCustomPeriod);
        
        if (!isCustomPeriod) {
            switch (exportPeriodComboBox.getValue()) {
                case "Últimos 7 dias":
                    exportStartDate.setValue(now.minusDays(7));
                    exportEndDate.setValue(now);
                    break;
                case "Últimos 30 dias":
                    exportStartDate.setValue(now.minusDays(30));
                    exportEndDate.setValue(now);
                    break;
                case "Este mês":
                    exportStartDate.setValue(now.withDayOfMonth(1));
                    exportEndDate.setValue(now);
                    break;
                case "Mês passado":
                    LocalDate firstDayLastMonth = now.minusMonths(1).withDayOfMonth(1);
                    exportStartDate.setValue(firstDayLastMonth);
                    exportEndDate.setValue(firstDayLastMonth.plusMonths(1).minusDays(1));
                    break;
                case "Este ano":
                    exportStartDate.setValue(now.withDayOfYear(1));
                    exportEndDate.setValue(now);
                    break;
            }
        }
    }

    private void setupExportButton() {
        exportButton.setOnAction(e -> exportData());
    }

    private void exportData() {
        if (currentUser == null) {
            return;
        }

        try {
            LocalDate startDate = exportStartDate.getValue();
            LocalDate endDate = exportEndDate.getValue();
            
            if (startDate == null || endDate == null) {
                showError("Por favor, selecione o período para exportação.");
                return;
            }
            
            List<Expense> expenses = expenseDAO.findByDateRange(
                currentUser.getId(),
                startDate,
                endDate
            );
            
            if (expenses.isEmpty()) {
                showError("Não há despesas no período selecionado.");
                return;
            }
            
            String format = exportFormatComboBox.getValue();
            String extension = format.equals("PDF") ? ".pdf" : ".xlsx";
            
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salvar Relatório");
            fileChooser.setInitialFileName(
                "despesas_" + 
                startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "_a_" +
                endDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + 
                extension
            );
            
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                    format.equals("PDF") ? "Arquivos PDF" : "Arquivos Excel",
                    "*" + extension
                )
            );
            
            File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
            if (file != null) {
                ExportUtil.exportExpenses(
                    expenses,
                    file,
                    format.equals("PDF"),
                    includeChartsCheckbox.isSelected(),
                    includeSummaryCheckbox.isSelected(),
                    includeDetailsCheckbox.isSelected()
                );
                
                showSuccess("Relatório exportado com sucesso!");
            }
            
        } catch (Exception e) {
            logger.error("Erro ao exportar dados", e);
            showError("Erro ao exportar dados: " + e.getMessage());
        }
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
    
    private static class ExportHistory {
        private final LocalDate date;
        private final String type;
        private final String period;
        private final File file;
        
        public ExportHistory(LocalDate date, String type, String period, File file) {
            this.date = date;
            this.type = type;
            this.period = period;
            this.file = file;
        }
        
        public LocalDate getDate() { return date; }
        public String getType() { return type; }
        public String getPeriod() { return period; }
        public File getFile() { return file; }
    }
}
