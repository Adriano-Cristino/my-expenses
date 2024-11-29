package com.expenses.dao;

import com.expenses.config.DatabaseConfig;
import com.expenses.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import at.favre.lib.crypto.bcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);
    private final Connection connection;

    public UserDAO() {
        try {
            this.connection = DatabaseConfig.getConnection();
            if (!adminExists()) {
                createAdminUser();
            }
        } catch (SQLException e) {
            logger.error("Error initializing UserDAO", e);
            throw new RuntimeException("Error initializing UserDAO", e);
        }
    }

    private boolean adminExists() {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            logger.error("Error checking admin existence", e);
            return false;
        }
    }
    
    private void createAdminUser() {
        String sql = """
            INSERT INTO users (name, email, password, role)
            VALUES (?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "Administrador");
            stmt.setString(2, "admin@myexpenses.com");
            stmt.setString(3, "$2a$10$8KxX1P1QEZdPyNOqEKJ5g.KxQgc3alF7EGnUE8U9aH7AUsnQmz4/.");  // senha: admin123
            stmt.setString(4, "ADMIN");
            stmt.executeUpdate();
            logger.info("Admin user created successfully");
        } catch (SQLException e) {
            logger.error("Error creating admin user", e);
            throw new RuntimeException("Error creating admin user", e);
        }
    }

    public User create(User user) {
        String sql = """
            INSERT INTO users (name, email, password, role)
            VALUES (?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getRole() != null ? user.getRole() : "USER");

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }

            return user;
        } catch (SQLException e) {
            logger.error("Error creating user", e);
            throw new RuntimeException("Error creating user", e);
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by email", e);
            throw new RuntimeException("Error finding user by email", e);
        }

        return Optional.empty();
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by id", e);
            throw new RuntimeException("Error finding user by id", e);
        }

        return Optional.empty();
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY name";
        List<User> users = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all users", e);
            throw new RuntimeException("Error finding all users", e);
        }

        return users;
    }

    public void update(User user) {
        String sql = """
            UPDATE users
            SET name = ?, email = ?, password = ?, role = ?
            WHERE id = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getRole());
            stmt.setLong(5, user.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating user", e);
            throw new RuntimeException("Error updating user", e);
        }
    }

    public void delete(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting user", e);
            throw new RuntimeException("Error deleting user", e);
        }
    }

    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking if email exists", e);
        }
        return false;
    }

    private void checkDatabaseStructure() {
        try {
            // Verifica a estrutura da tabela users
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet columns = meta.getColumns(null, null, "users", null);
            
            logger.debug("Verificando estrutura da tabela users:");
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");
                int columnSize = columns.getInt("COLUMN_SIZE");
                logger.debug("Coluna: {} (Tipo: {}, Tamanho: {})", columnName, columnType, columnSize);
            }
            
            // Verifica se há registros na tabela
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
                if (rs.next()) {
                    int count = rs.getInt(1);
                    logger.debug("Total de usuários na tabela: {}", count);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Erro ao verificar estrutura do banco de dados", e);
        }
    }

    public Optional<User> authenticate(String email, String password) {
        // Verifica a estrutura do banco antes de autenticar
        checkDatabaseStructure();
        
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                logger.debug("Tentando autenticar usuário: {}", email);
                logger.debug("Senha armazenada: {}", storedPassword);
                
                BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), storedPassword);
                
                if (result.verified) {
                    logger.info("Autenticação bem-sucedida para o email: {}", email);
                    return Optional.of(mapResultSetToUser(rs));
                } else {
                    logger.warn("Senha incorreta para o email: {}", email);
                }
            } else {
                logger.warn("Email não encontrado: {}", email);
            }
            
            return Optional.empty();
            
        } catch (SQLException e) {
            logger.error("Erro ao autenticar usuário: {}", email, e);
            return Optional.empty();
        }
    }

    public void saveRecoveryToken(String email, String token) {
        String sql = "UPDATE users SET recovery_token = ?, recovery_token_expiry = ? WHERE email = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Token expira em 24 horas
            Timestamp expiry = new Timestamp(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
            
            stmt.setString(1, token);
            stmt.setTimestamp(2, expiry);
            stmt.setString(3, email);
            
            stmt.executeUpdate();
            logger.info("Recovery token saved for email: {}", email);
            
        } catch (SQLException e) {
            logger.error("Error saving recovery token", e);
            throw new RuntimeException("Error saving recovery token", e);
        }
    }
    
    public boolean validateRecoveryToken(String token) {
        String sql = "SELECT email, recovery_token_expiry FROM users WHERE recovery_token = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Timestamp expiry = rs.getTimestamp("recovery_token_expiry");
                return expiry != null && expiry.after(new Timestamp(System.currentTimeMillis()));
            }
            
            return false;
            
        } catch (SQLException e) {
            logger.error("Error validating recovery token", e);
            throw new RuntimeException("Error validating recovery token", e);
        }
    }
    
    public void updatePassword(String token, String newPassword) {
        String sql = "UPDATE users SET password = ?, recovery_token = NULL, recovery_token_expiry = NULL " +
                    "WHERE recovery_token = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String hashedPassword = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
            stmt.setString(1, hashedPassword);
            stmt.setString(2, token);
            
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new RuntimeException("Invalid or expired recovery token");
            }
            
            logger.info("Password updated successfully for token");
            
        } catch (SQLException e) {
            logger.error("Error updating password", e);
            throw new RuntimeException("Error updating password", e);
        }
    }

    public boolean register(User user) {
        try {
            // Verifica se o email já existe
            if (emailExists(user.getEmail())) {
                logger.warn("Tentativa de registro com email já existente: {}", user.getEmail());
                return false;
            }

            logger.debug("Iniciando registro do usuário: {}", user.getEmail());

            // Criptografa a senha
            String hashedPassword = BCrypt.withDefaults().hashToString(12, user.getPassword().toCharArray());
            logger.debug("Senha criptografada gerada: {}", hashedPassword);
            user.setPassword(hashedPassword);
            
            // Garante que seja um usuário normal
            user.setRole("USER");
            user.setCreatedAt(LocalDateTime.now());

            // Cria o usuário
            User createdUser = create(user);
            logger.info("Usuário registrado com sucesso: {}", user.getEmail());
            logger.debug("ID do usuário criado: {}", createdUser.getId());
            return true;
            
        } catch (Exception e) {
            logger.error("Erro ao registrar usuário: {}. Erro: {}", user.getEmail(), e.getMessage(), e);
            return false;
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return User.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .email(rs.getString("email"))
                .password(rs.getString("password"))
                .role(rs.getString("role"))
                .build();
    }
}
