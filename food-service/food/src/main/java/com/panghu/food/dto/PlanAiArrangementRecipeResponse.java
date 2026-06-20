package com.panghu.food.dto;

import lombok.Data;

@Data
public class PlanAiArrangementRecipeResponse {
    private DishSummaryResponse dish;
    private String reason;
}
