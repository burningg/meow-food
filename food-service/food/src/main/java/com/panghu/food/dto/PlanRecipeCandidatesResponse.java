package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PlanRecipeCandidatesResponse {
    private boolean viewerCanAddRecipes;
    private boolean viewerIsCircleMember;
    private String sourceLabel;
    private List<DishSummaryResponse> recipes = new ArrayList<>();
}
