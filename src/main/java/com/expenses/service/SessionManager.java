package com.expenses.service;

import com.expenses.model.User;
import lombok.Getter;
import lombok.Setter;

/**
 * Gerenciador de sessão do usuário
 */
public class SessionManager {
    private static SessionManager instance;
    @Getter @Setter
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public static User getCurrentUser() {
        return getInstance().currentUser;
    }

    public static void setCurrentUser(User user) {
        getInstance().currentUser = user;
    }

    public static void clearSession() {
        getInstance().currentUser = null;
    }
}
