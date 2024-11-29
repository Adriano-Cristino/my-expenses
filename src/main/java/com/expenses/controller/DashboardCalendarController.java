package com.expenses.controller;

import com.expenses.dao.ExpenseDAO;
import com.expenses.model.Expense;
import com.expenses.model.User;
import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardCalendarController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardCalendarController.class);

    @FXML private GridPane calendarGrid;
    @FXML private Label currentMonthLabel;
    @FXML private JFXButton previousMonthButton;
    @FXML private JFXButton nextMonthButton;
    
    private final ExpenseDAO expenseDAO;
    private final NumberFormat currencyFormatter;
    private LocalDate currentCalendarMonth;
    private User currentUser;

    public DashboardCalendarController() {
        this.expenseDAO = new ExpenseDAO();
        this.currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        this.currentCalendarMonth = LocalDate.now();
    }

    @FXML
    private void initialize() {
        setupCalendarNavigation();
        updateCalendarMonth();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        updateCalendar();
    }

    private void setupCalendarNavigation() {
        previousMonthButton.setOnAction(e -> navigateCalendarMonth(-1));
        nextMonthButton.setOnAction(e -> navigateCalendarMonth(1));
    }

    private void navigateCalendarMonth(int months) {
        currentCalendarMonth = currentCalendarMonth.plusMonths(months);
        updateCalendarMonth();
    }

    private void updateCalendarMonth() {
        currentMonthLabel.setText(currentCalendarMonth.format(
            DateTimeFormatter.ofPattern("MMMM 'de' yyyy", new Locale("pt", "BR"))
        ));
        updateCalendar();
    }

    public void updateCalendar() {
        if (currentUser == null) {
            return;
        }

        try {
            calendarGrid.getChildren().clear();
            
            YearMonth yearMonth = YearMonth.from(currentCalendarMonth);
            LocalDate firstOfMonth = yearMonth.atDay(1);
            int dayOfWeek = firstOfMonth.getDayOfWeek().getValue();
            
            // Adicionar células vazias para os dias antes do primeiro dia do mês
            for (int i = 1; i < dayOfWeek; i++) {
                calendarGrid.add(createEmptyDayCell(), i - 1, 0);
            }
            
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();
            List<Expense> monthExpenses = expenseDAO.findByDateRange(
                currentUser.getId(),
                startDate,
                endDate
            );
            
            // Agrupar despesas por dia
            Map<LocalDate, List<Expense>> expensesByDay = monthExpenses.stream()
                .collect(Collectors.groupingBy(Expense::getDate));
            
            int day = 1;
            int week = 0;
            
            while (day <= yearMonth.lengthOfMonth()) {
                LocalDate currentDate = yearMonth.atDay(day);
                int column = (dayOfWeek - 1 + day - 1) % 7;
                
                if (column == 0 && day > 1) {
                    week++;
                }
                
                VBox dayCell = createDayCell(currentDate, expensesByDay.get(currentDate));
                calendarGrid.add(dayCell, column, week);
                
                day++;
            }
        } catch (Exception e) {
            logger.error("Erro ao atualizar calendário", e);
        }
    }

    private VBox createEmptyDayCell() {
        VBox cell = new VBox();
        cell.getStyleClass().add("calendar-cell");
        cell.setAlignment(Pos.CENTER);
        return cell;
    }

    private VBox createDayCell(LocalDate date, List<Expense> dayExpenses) {
        VBox cell = new VBox(5);
        cell.getStyleClass().add("calendar-cell");
        cell.setAlignment(Pos.TOP_CENTER);
        
        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.getStyleClass().add("calendar-day-label");
        
        cell.getChildren().add(dayLabel);
        
        if (dayExpenses != null && !dayExpenses.isEmpty()) {
            BigDecimal totalAmount = dayExpenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Label totalLabel = new Label(currencyFormatter.format(totalAmount));
            totalLabel.getStyleClass().add("calendar-total-label");
            cell.getChildren().add(totalLabel);
            
            VBox expensesContainer = new VBox(2);
            expensesContainer.setMaxHeight(100);
            
            for (Expense expense : dayExpenses) {
                Label expenseLabel = new Label(
                    // TODO: Buscar nome da categoria
                    String.format("%s: %s",
                        "Categoria " + expense.getCategoryId(),
                        currencyFormatter.format(expense.getAmount())
                    )
                );
                expenseLabel.getStyleClass().add("calendar-expense-label");
                expensesContainer.getChildren().add(expenseLabel);
            }
            
            ScrollPane scrollPane = new ScrollPane(expensesContainer);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(60);
            scrollPane.getStyleClass().add("calendar-scroll-pane");
            
            cell.getChildren().add(scrollPane);
        }
        
        return cell;
    }
}
