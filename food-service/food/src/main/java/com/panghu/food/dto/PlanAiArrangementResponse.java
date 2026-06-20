package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PlanAiArrangementResponse {
    private String title;
    private String petText;
    private String suggestionText;
    private String healthText;
    private List<PlanAiArrangementRecipeResponse> recipes = new ArrayList<>();
    private PlanAiUsageResponse usage;
}
