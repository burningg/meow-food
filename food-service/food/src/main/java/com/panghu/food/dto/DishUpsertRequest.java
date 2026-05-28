package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DishUpsertRequest {
    private String name;
    private String image;
    private String description;
    private String categoryId;
    private Integer cookTimeMinutes;
    private String difficulty;
    private Integer servings;
    private String visibility;
    private Boolean isFeatured;
    private List<IngredientItem> ingredients = new ArrayList<>();
    private List<StepItem> steps = new ArrayList<>();
}
