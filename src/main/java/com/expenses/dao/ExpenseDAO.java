package com.expenses.dao;

import com.expenses.config.DatabaseConfig;
import com.expenses.model.Expense;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseDAO {
    private static final Logger logger = LoggerFactory.getLogger(ExpenseDAO.class);
    private final Connection connection;

    public ExpenseDAO() {
        try {
            this.connection = DatabaseConfig.getConnection();
            createTable();
        } catch (SQLException e) {
            logger.error("Error initializing ExpenseDAO", e);
            throw new RuntimeException("Error initializing ExpenseDAO", e);
        }
    }

    private void createTable() {
        try {
            String sql = """
                CREATE TABLE IF NOT EXISTS expenses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    description TEXT NOT NULL,
                    amount DECIMAL(10,2) NOT NULL,
                    date DATE NOT NULL,
                    category_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (category_id) REFERENCES categories(id),
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """;

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            logger.error("Error creating expenses table", e);
        }
    }

    public Expense create(Expense expense) {
        String sql = """
            INSERT INTO expenses (description, amount, date, category_id, user_id)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, expense.getDescription());
            stmt.setBigDecimal(2, expense.getAmount());
            stmt.setDate(3, Date.valueOf(expense.getDate()));
            stmt.setLong(4, expense.getCategoryId());
            stmt.setLong(5, expense.getUserId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating expense failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    expense.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating expense failed, no ID obtained.");
                }
            }

            return expense;
        } catch (SQLException e) {
            logger.error("Error creating expense", e);
            throw new RuntimeException("Error creating expense", e);
        }
    }

    public void update(Expense expense) {
        String sql = """
            UPDATE expenses
            SET description = ?, amount = ?, date = ?, category_id = ?
            WHERE id = ? AND user_id = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, expense.getDescription());
            stmt.setBigDecimal(2, expense.getAmount());
            stmt.setDate(3, Date.valueOf(expense.getDate()));
            stmt.setLong(4, expense.getCategoryId());
            stmt.setLong(5, expense.getId());
            stmt.setLong(6, expense.getUserId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating expense", e);
            throw new RuntimeException("Error updating expense", e);
        }
    }

    public void delete(Long id, Long userId) {
        String sql = "DELETE FROM expenses WHERE id = ? AND user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting expense", e);
            throw new RuntimeException("Error deleting expense", e);
        }
    }

    public List<Expense> findAllByUserId(Long userId) {
        String sql = "SELECT * FROM expenses WHERE user_id = ? ORDER BY date DESC";
        List<Expense> expenses = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expenses.add(mapResultSetToExpense(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding expenses by user", e);
            throw new RuntimeException("Error finding expenses by user", e);
        }

        return expenses;
    }

    public List<Expense> findByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT * FROM expenses
            WHERE user_id = ? AND date BETWEEN ? AND ?
            ORDER BY date DESC
        """;
        List<Expense> expenses = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expenses.add(mapResultSetToExpense(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding expenses by date range", e);
            throw new RuntimeException("Error finding expenses by date range", e);
        }

        return expenses;
    }

    public List<Expense> findByCategoryId(Long categoryId, Long userId) {
        String sql = "SELECT * FROM expenses WHERE category_id = ? AND user_id = ? ORDER BY date DESC";
        List<Expense> expenses = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, categoryId);
            stmt.setLong(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expenses.add(mapResultSetToExpense(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding expenses by category", e);
            throw new RuntimeException("Error finding expenses by category", e);
        }

        return expenses;
    }

    public List<Expense> findWithFilters(Long userId, LocalDate startDate, LocalDate endDate, 
            Long categoryId, Double minValue, Double maxValue) {
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM expenses WHERE user_id = ?"
        );
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (startDate != null) {
            sql.append(" AND date >= ?");
            params.add(Date.valueOf(startDate));
        }
        if (endDate != null) {
            sql.append(" AND date <= ?");
            params.add(Date.valueOf(endDate));
        }
        if (categoryId != null) {
            sql.append(" AND category_id = ?");
            params.add(categoryId);
        }
        if (minValue != null) {
            sql.append(" AND amount >= ?");
            params.add(BigDecimal.valueOf(minValue));
        }
        if (maxValue != null) {
            sql.append(" AND amount <= ?");
            params.add(BigDecimal.valueOf(maxValue));
        }

        sql.append(" ORDER BY date DESC");

        List<Expense> expenses = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    stmt.setLong(i + 1, (Long) param);
                } else if (param instanceof Date) {
                    stmt.setDate(i + 1, (Date) param);
                } else if (param instanceof BigDecimal) {
                    stmt.setBigDecimal(i + 1, (BigDecimal) param);
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expenses.add(mapResultSetToExpense(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding expenses with filters", e);
            throw new RuntimeException("Error finding expenses with filters", e);
        }

        return expenses;
    }

    public Map<String, BigDecimal> getExpensesByCategory(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT c.name, SUM(e.amount) as total
            FROM expenses e
            JOIN categories c ON e.category_id = c.id
            WHERE e.user_id = ? AND e.date BETWEEN ? AND ?
            GROUP BY c.name
            ORDER BY total DESC
        """;

        Map<String, BigDecimal> results = new HashMap<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String category = rs.getString("name");
                    BigDecimal total = rs.getBigDecimal("total");
                    results.put(category, total);
                }
            }
        } catch (SQLException e) {
            logger.error("Erro ao buscar gastos por categoria", e);
            throw new RuntimeException("Erro ao buscar gastos por categoria", e);
        }
        
        return results;
    }

    private Expense mapResultSetToExpense(ResultSet rs) throws SQLException {
        return Expense.builder()
                .id(rs.getLong("id"))
                .description(rs.getString("description"))
                .amount(rs.getBigDecimal("amount"))
                .date(rs.getDate("date").toLocalDate())
                .categoryId(rs.getLong("category_id"))
                .userId(rs.getLong("user_id"))
                .build();
    }

    public BigDecimal getTotalExpensesByUser(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT COALESCE(SUM(amount), 0) as total
            FROM expenses
            WHERE user_id = ? AND date BETWEEN ? AND ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
            return BigDecimal.ZERO;
        } catch (SQLException e) {
            logger.error("Error getting total expenses by user", e);
            throw new RuntimeException("Error getting total expenses by user", e);
        }
    }

    public int getCategoryCountByUser(Long userId) {
        String sql = """
            SELECT COUNT(DISTINCT category_id) as count
            FROM expenses
            WHERE user_id = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
            return 0;
        } catch (SQLException e) {
            logger.error("Error getting category count by user", e);
            throw new RuntimeException("Error getting category count by user", e);
        }
    }

    public int getExpenseCountByUser(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT COUNT(*) as count
            FROM expenses
            WHERE user_id = ? AND date BETWEEN ? AND ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
            return 0;
        } catch (SQLException e) {
            logger.error("Error getting expense count by user", e);
            throw new RuntimeException("Error getting expense count by user", e);
        }
    }
}
