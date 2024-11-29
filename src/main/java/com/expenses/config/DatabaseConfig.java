package com.expenses.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import at.favre.lib.crypto.bcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final String DB_URL = "jdbc:sqlite:myexpenses.db";
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
    
    private static void createAdminUser(Connection conn) throws SQLException {
        // Verifica se já existe um admin
        try (Statement checkStmt = conn.createStatement()) {
            ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'");
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Admin já existe
            }
        }
        
        // Cria o usuário admin
        String sql = """
            INSERT INTO users (name, email, password, role)
            VALUES (?, ?, ?, ?)
        """;
        
        try (var stmt = conn.prepareStatement(sql)) {
            String hashedPassword = BCrypt.withDefaults().hashToString(12, "admin123".toCharArray());
            
            stmt.setString(1, "Administrador");
            stmt.setString(2, "admin@myexpenses.com");
            stmt.setString(3, hashedPassword);
            stmt.setString(4, "ADMIN");
            stmt.executeUpdate();
            logger.info("Admin user created successfully with password hash: {}", hashedPassword);
        }
    }
    
    public static void initDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Criar tabela de usuários
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) NOT NULL UNIQUE,
                    password VARCHAR(100) NOT NULL,
                    role VARCHAR(20) NOT NULL DEFAULT 'USER',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Adiciona colunas para recuperação de senha se não existirem
            try (Statement stmt2 = conn.createStatement()) {
                DatabaseMetaData meta = conn.getMetaData();
                ResultSet rs = meta.getColumns(null, null, "users", "recovery_token");
                
                if (!rs.next()) {
                    stmt2.execute("ALTER TABLE users ADD COLUMN recovery_token TEXT");
                    stmt2.execute("ALTER TABLE users ADD COLUMN recovery_token_expiry TIMESTAMP");
                    logger.info("Added recovery token columns to users table");
                }
            }
            
            // Criar usuário admin
            createAdminUser(conn);
            
            // Criar tabela de categorias
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name VARCHAR(50) NOT NULL,
                    description VARCHAR(200),
                    user_id INTEGER,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);
            
            // Criar tabela de despesas
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS expenses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    description VARCHAR(200) NOT NULL,
                    amount DECIMAL(10,2) NOT NULL,
                    date DATE NOT NULL,
                    category_id INTEGER,
                    user_id INTEGER,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (category_id) REFERENCES categories(id),
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);
            
            logger.info("Database initialized successfully");
            
        } catch (SQLException e) {
            logger.error("Error initializing database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
}
