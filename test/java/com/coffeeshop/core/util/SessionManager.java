package com.coffeeshop.core.util;

import com.coffeeshop.core.model.User;
import com.coffeeshop.core.model.User.UserRole;

public class SessionManager {

    private static User currentUser; // Người dùng hiện tại đang đăng nhập
    private static boolean loggedIn = false;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
        loggedIn = (user != null);
    }

    public static boolean isLoggedIn() {
        return loggedIn;
    }

    public static boolean isAdmin() {
        return loggedIn && currentUser != null && UserRole.ADMIN.equals(currentUser.getRole());
    }

    public static void logout() {
        currentUser = null;
        loggedIn = false;
    }
}
