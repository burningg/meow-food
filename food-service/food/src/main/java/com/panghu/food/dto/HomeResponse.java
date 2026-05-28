package com.panghu.food.dto;

import com.panghu.food.entity.Category;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HomeResponse {
    private List<Category> categories = new ArrayList<>();
    private List<DishSummaryResponse> featuredDishes = new ArrayList<>();
    private List<DishSummaryResponse> recentDishes = new ArrayList<>();
    private List<CategoryRecommendationGroup> featuredByCategory = new ArrayList<>();
}
