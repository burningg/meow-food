package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryRecommendationGroup {
    private String categoryId;
    private String categoryName;
    private List<DishSummaryResponse> dishes = new ArrayList<>();
}
