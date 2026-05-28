package com.panghu.food.service;

public final class VisibilityUtils {
    private VisibilityUtils() {
    }

    public static String normalizeDishVisibility(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "inherit";
        }
        return value;
    }

    public static String normalizeProfileVisibility(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "friends";
        }
        return value;
    }

    public static String effectiveVisibility(String dishVisibility, String defaultVisibility) {
        String normalizedDish = normalizeDishVisibility(dishVisibility);
        if ("inherit".equals(normalizedDish)) {
            return normalizeProfileVisibility(defaultVisibility);
        }
        return normalizedDish;
    }
}
