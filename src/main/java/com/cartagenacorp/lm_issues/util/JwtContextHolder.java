package com.cartagenacorp.lm_issues.util;

import java.util.UUID;

public class JwtContextHolder {
    private static final ThreadLocal<UUID> currentUserId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentToken = new ThreadLocal<>();

    public static void setUserId(UUID userId) {
        currentUserId.set(userId);
    }

    public static UUID getUserId() {
        return currentUserId.get();
    }

    public static void setToken(String token) {
        currentToken.set(token);
    }

    public static String getToken() {
        return currentToken.get();
    }

    public static void clear() {
        currentUserId.remove();
        currentToken.remove();
    }
}

