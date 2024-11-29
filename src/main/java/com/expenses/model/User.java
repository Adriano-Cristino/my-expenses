package com.expenses.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private Long id;
    private String name;
    private String email;
    private String password;
    private String role;  // ADMIN ou USER
    private LocalDateTime createdAt;
    
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
