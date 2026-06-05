package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DishAiAnalysisResponse {
    private String name;
    private List<IngredientItem> ingredients = new ArrayList<>();
    private List<StepItem> steps = new ArrayList<>();
    private VipUsageResponse usage;
}
