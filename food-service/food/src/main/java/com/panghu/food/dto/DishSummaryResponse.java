package com.panghu.food.dto;

import lombok.Data;

@Data
public class DishSummaryResponse {
    private String id;
    private String ownerUserId;
    private String ownerNickname;
    private String name;
    private String image;
    private String description;
    private String categoryId;
    private String categoryName;
    private Integer cookTimeMinutes;
    private String difficulty;
    private Integer servings;
    private String visibility;
    private String effectiveVisibility;
    private Boolean isFeatured;
}
