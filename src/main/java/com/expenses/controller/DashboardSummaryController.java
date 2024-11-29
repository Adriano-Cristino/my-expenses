package com.expenses.controller;

import com.expenses.dao.CategoryDAO;
import com.expenses.dao.ExpenseDAO;
import com.expenses.model.Category;
import com.expenses.model.Expense;
import com.expenses.model.User;
import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardSummaryController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardSummaryController.class);

    @FXML private ComboBox<String> periodComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    
    @FXML private Label totalPeriodLabel;
    @FXML private Label periodComparisonLabel;
    @FXML private Label monthlyAverageLabel;
    @FXML private Label monthlyTrendLabel;
    @FXML private Label highestExpenseLabel;
    @FXML private Label highestExpenseCategoryLabel;
    @FXML private Label savingsLabel;
    @FXML private Label savingsPercentLabel;
    
    @FXML private PieChart expensesPieChart;
    @FXML private BarChart<String, Number> monthlyTrendsChart;
    @FXML private LineChart<String, Number> dailyExpensesChart;
    @FXML private StackedBarChart<String, Number> categoryComparisonChart;
    
    private final CategoryDAO categoryDAO;
    private final ExpenseDAO expenseDAO;
    private final NumberFormat currencyFormatter;
    private User currentUser;

    public DashboardSummaryController() {
        this.categoryDAO = new CategoryDAO();
        this.expenseDAO = new ExpenseDAO();
        this.currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    }

    @FXML
    private void initialize() {
        setupPeriodComboBox();
        setupDatePickers();
        setupChartListeners();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        updateDashboard();
    }

    private void setupPeriodComboBox() {
        periodComboBox.getItems().addAll(
            "Últimos 7 dias",
            "Últimos 30 dias",
            "Este mês",
            "Mês passado",
            "Este ano",
            "Personalizado"
        );
        periodComboBox.setValue("Este mês");
        periodComboBox.setOnAction(e -> handlePeriodChange());
    }

    private void setupDatePickers() {
        startDatePicker.setDisable(true);
        endDatePicker.setDisable(true);
        
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) updateDashboard();
        });
        
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) updateDashboard();
        });
    }

    private void setupChartListeners() {
        expensesPieChart.setTitle("Distribuição de Despesas por Categoria");
        monthlyTrendsChart.setTitle("Tendência Mensal de Despesas");
        dailyExpensesChart.setTitle("Despesas Diárias");
        categoryComparisonChart.setTitle("Comparação de Categorias");
    }

    private void handlePeriodChange() {
        LocalDate now = LocalDate.now();
        boolean isCustomPeriod = "Personalizado".equals(periodComboBox.getValue());
        
        startDatePicker.setDisable(!isCustomPeriod);
        endDatePicker.setDisable(!isCustomPeriod);
        
        if (!isCustomPeriod) {
            switch (periodComboBox.getValue()) {
                case "Últimos 7 dias":
                    startDatePicker.setValue(now.minusDays(7));
                    endDatePicker.setValue(now);
                    break;
                case "Últimos 30 dias":
                    startDatePicker.setValue(now.minusDays(30));
                    endDatePicker.setValue(now);
                    break;
                case "Este mês":
                    startDatePicker.setValue(now.withDayOfMonth(1));
                    endDatePicker.setValue(now);
                    break;
                case "Mês passado":
                    LocalDate firstDayLastMonth = now.minusMonths(1).withDayOfMonth(1);
                    startDatePicker.setValue(firstDayLastMonth);
                    endDatePicker.setValue(firstDayLastMonth.plusMonths(1).minusDays(1));
                    break;
                case "Este ano":
                    startDatePicker.setValue(now.withDayOfYear(1));
                    endDatePicker.setValue(now);
                    break;
            }
            updateDashboard();
        }
    }

    public void updateDashboard() {
        if (currentUser == null || startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            return;
        }

        try {
            updateSummaryCards();
            updateCharts();
        } catch (Exception e) {
            logger.error("Erro ao atualizar dashboard", e);
        }
    }

    private void updateSummaryCards() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        Long userId = currentUser.getId();

        // Encontrar maior despesa do período
        List<Expense> expenses = expenseDAO.findByDateRange(userId, startDate, endDate);
        Optional<Expense> highestExpense = expenses.stream()
            .max(Comparator.comparing(e -> e.getAmount()));
            
        if (highestExpense.isPresent()) {
            Expense expense = highestExpense.get();
            highestExpenseLabel.setText(currencyFormatter.format(expense.getAmount()));
            // TODO: Buscar nome da categoria
            highestExpenseCategoryLabel.setText("Categoria " + expense.getCategoryId());
        } else {
            highestExpenseLabel.setText(currencyFormatter.format(0));
            highestExpenseCategoryLabel.setText("-");
        }

        // Calcular total do período
        BigDecimal totalPeriod = expenses.stream()
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalPeriodLabel.setText(currencyFormatter.format(totalPeriod));

        // Calcular média mensal
        long monthsBetween = java.time.temporal.ChronoUnit.MONTHS.between(startDate, endDate) + 1;
        if (monthsBetween > 0) {
            BigDecimal monthlyAverage = totalPeriod.divide(BigDecimal.valueOf(monthsBetween), 2, BigDecimal.ROUND_HALF_UP);
            monthlyAverageLabel.setText(currencyFormatter.format(monthlyAverage));
        } else {
            monthlyAverageLabel.setText(currencyFormatter.format(totalPeriod));
        }

        // Comparar com período anterior
        LocalDate previousStartDate = startDate.minusDays(startDate.until(endDate).getDays() + 1);
        LocalDate previousEndDate = startDate.minusDays(1);
        
        List<Expense> previousExpenses = expenseDAO.findByDateRange(userId, previousStartDate, previousEndDate);
        BigDecimal previousTotal = previousExpenses.stream()
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        if (previousTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentChange = totalPeriod.subtract(previousTotal)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousTotal, 2, BigDecimal.ROUND_HALF_UP);
            
            String trend = percentChange.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            periodComparisonLabel.setText(trend + percentChange + "% em relação ao período anterior");
        } else {
            periodComparisonLabel.setText("Primeiro período");
        }
    }

    private void updateCharts() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        Long userId = currentUser.getId();

        List<Expense> expenses = expenseDAO.findByDateRange(userId, startDate, endDate);
        
        // Agrupar despesas por categoria
        Map<Long, BigDecimal> expensesByCategory = expenses.stream()
            .collect(Collectors.groupingBy(
                Expense::getCategoryId,
                Collectors.mapping(
                    Expense::getAmount,
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
            ));
            
        // Atualizar gráfico de pizza
        expensesPieChart.getData().clear();
        expensesByCategory.forEach((categoryId, total) -> {
            // TODO: Buscar nome da categoria
            PieChart.Data slice = new PieChart.Data("Categoria " + categoryId, total.doubleValue());
            expensesPieChart.getData().add(slice);
        });

        // Agrupar despesas por mês
        Map<String, BigDecimal> expensesByMonth = expenses.stream()
            .collect(Collectors.groupingBy(
                e -> e.getDate().format(DateTimeFormatter.ofPattern("MM/yyyy")),
                Collectors.mapping(
                    Expense::getAmount,
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
            ));
            
        // Atualizar gráfico de barras
        monthlyTrendsChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Total Mensal");
        expensesByMonth.forEach((month, total) -> {
            series.getData().add(new XYChart.Data<>(month, total));
        });
        monthlyTrendsChart.getData().add(series);
    }
}
