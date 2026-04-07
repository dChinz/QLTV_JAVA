package com.example.service;

import com.example.dao.UserDAO;
import com.example.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

public class AuthService {

    private static AuthService instance;
    private final UserDAO userDAO = new UserDAO();
    private User currentUser;

    private AuthService() {}

    public static synchronized AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    /**
     * Validates credentials and sets the current logged-in user.
     * @return true if login succeeds
     */
    public boolean login(String username, String password) {
        Optional<User> opt = userDAO.findByUsername(username);
        if (opt.isEmpty()) return false;
        User user = opt.get();
        if (!user.isActive()) return false;
        if (BCrypt.checkpw(password, user.getPasswordHash())) {
            currentUser = user;
            return true;
        }
        return false;
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public void changePassword(String oldPassword, String newPassword) {
        if (currentUser == null) throw new IllegalStateException("Not logged in.");
        if (!BCrypt.checkpw(oldPassword, currentUser.getPasswordHash()))
            throw new IllegalArgumentException("Mật khẩu cũ không đúng.");
        if (newPassword.length() < 6)
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
        userDAO.updatePassword(currentUser.getId(), newHash);
        currentUser.setPasswordHash(newHash);
    }

    /** Utility to generate a BCrypt hash (used for seeding). */
    public static String hashPassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
    }
}
