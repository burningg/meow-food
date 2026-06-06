package com.panghu.food.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DishSummaryResponse {
    private String id;
    private String ownerUserId;
    private String ownerNickname;
    private String addedByUserId;
    private String addedByNickname;
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
    private List<String> visibilityCircleIds = new ArrayList<>();
    private List<String> effectiveCircleIds = new ArrayList<>();
    private Boolean isFeatured;
    private LocalDateTime createdAt;
    private List<String> ingredientNames = new ArrayList<>();
}
