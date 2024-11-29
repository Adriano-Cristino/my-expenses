package com.expenses.dao;

import com.expenses.config.DatabaseConfig;
import com.expenses.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryDAO {
    private static final Logger logger = LoggerFactory.getLogger(CategoryDAO.class);

    public void create(Category category) {
        String sql = "INSERT INTO categories (name, description, user_id) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getDescription());
            stmt.setLong(3, category.getUserId());
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    category.setId(rs.getLong(1));
                }
            }
            
            logger.info("Category created successfully: {}", category.getName());
            
        } catch (SQLException e) {
            logger.error("Error creating category", e);
            throw new RuntimeException("Failed to create category", e);
        }
    }

    public void update(Category category) {
        String sql = "UPDATE categories SET name = ?, description = ? WHERE id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getDescription());
            stmt.setLong(3, category.getId());
            stmt.setLong(4, category.getUserId());
            
            stmt.executeUpdate();
            logger.info("Category updated successfully: {}", category.getName());
            
        } catch (SQLException e) {
            logger.error("Error updating category", e);
            throw new RuntimeException("Failed to update category", e);
        }
    }

    public void delete(Long id, Long userId) {
        String sql = "DELETE FROM categories WHERE id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.setLong(2, userId);
            
            stmt.executeUpdate();
            logger.info("Category deleted successfully: {}", id);
            
        } catch (SQLException e) {
            logger.error("Error deleting category", e);
            throw new RuntimeException("Failed to delete category", e);
        }
    }

    public Optional<Category> findById(Long id, Long userId) {
        String sql = "SELECT * FROM categories WHERE id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.setLong(2, userId);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Category category = Category.builder()
                    .id(rs.getLong("id"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .userId(rs.getLong("user_id"))
                    .build();
                    
                return Optional.of(category);
            }
            
            return Optional.empty();
            
        } catch (SQLException e) {
            logger.error("Error finding category by id", e);
            throw new RuntimeException("Failed to find category", e);
        }
    }

    public List<Category> findAllByUserId(Long userId) {
        String sql = "SELECT * FROM categories WHERE user_id = ? ORDER BY name";
        List<Category> categories = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Category category = Category.builder()
                    .id(rs.getLong("id"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .userId(rs.getLong("user_id"))
                    .build();
                    
                categories.add(category);
            }
            
            return categories;
            
        } catch (SQLException e) {
            logger.error("Error finding categories by user id", e);
            throw new RuntimeException("Failed to find categories", e);
        }
    }

    public boolean exists(String name, Long userId) {
        String sql = "SELECT COUNT(*) FROM categories WHERE name = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, name);
            stmt.setLong(2, userId);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
            return false;
            
        } catch (SQLException e) {
            logger.error("Error checking category existence", e);
            throw new RuntimeException("Failed to check category existence", e);
        }
    }
}
