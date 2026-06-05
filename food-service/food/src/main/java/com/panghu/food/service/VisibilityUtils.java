package com.panghu.food.service;

public final class VisibilityUtils {
    public static final String DISH_VISIBILITY_INHERIT = "inherit";
    public static final String VISIBILITY_PUBLIC = "public";
    public static final String VISIBILITY_PRIVATE = "private";
    public static final String VISIBILITY_CIRCLE = "circle";
    public static final String VISIBILITY_FRIENDS = "friends";
    public static final String DEFAULT_PROFILE_VISIBILITY = VISIBILITY_PUBLIC;

    private VisibilityUtils() {
    }

    public static String normalizeDishVisibility(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DISH_VISIBILITY_INHERIT;
        }
        return value.trim();
    }

    public static String normalizeProfileVisibility(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_PROFILE_VISIBILITY;
        }
        return value.trim();
    }

    public static String effectiveVisibility(String dishVisibility, String defaultVisibility) {
        String normalizedDish = normalizeDishVisibility(dishVisibility);
        if (DISH_VISIBILITY_INHERIT.equals(normalizedDish)) {
            return normalizeProfileVisibility(defaultVisibility);
        }
        return normalizedDish;
    }

    public static boolean isSupportedDishVisibility(String value) {
        return DISH_VISIBILITY_INHERIT.equals(value)
                || VISIBILITY_PUBLIC.equals(value)
                || VISIBILITY_PRIVATE.equals(value)
                || VISIBILITY_CIRCLE.equals(value)
                || VISIBILITY_FRIENDS.equals(value);
    }

    public static boolean isSupportedProfileVisibility(String value) {
        return VISIBILITY_PUBLIC.equals(value)
                || VISIBILITY_PRIVATE.equals(value)
                || VISIBILITY_CIRCLE.equals(value)
                || VISIBILITY_FRIENDS.equals(value);
    }
}
