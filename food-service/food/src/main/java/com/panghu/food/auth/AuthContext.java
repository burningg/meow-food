package com.panghu.food.auth;

import com.panghu.food.exception.ApiException;
import org.springframework.http.HttpStatus;

public final class AuthContext {
    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void setUserId(Long userId) {
        CURRENT_USER.set(userId);
    }

    public static Long getUserId() {
        return CURRENT_USER.get();
    }

    public static Long requireUserId() {
        Long userId = CURRENT_USER.get();
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return userId;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
