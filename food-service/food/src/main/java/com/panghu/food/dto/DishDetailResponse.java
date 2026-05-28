package com.panghu.food.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DishDetailResponse {
    private String id;
    private Long ownerUserId;
    private String ownerNickname;
    private String name;
    private String image;
    private BigDecimal price;
    private String description;
    private String categoryId;
    private String categoryName;
    private Integer cookTimeMinutes;
    private String difficulty;
    private Integer servings;
    private String visibility;
    private String effectiveVisibility;
    private Boolean isFeatured;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<IngredientItem> ingredients = new ArrayList<>();
    private List<StepItem> steps = new ArrayList<>();
}
