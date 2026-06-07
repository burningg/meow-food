package com.panghu.food.web;

import com.panghu.food.dto.CategoryRecommendationGroup;
import com.panghu.food.dto.DishSummaryResponse;
import com.panghu.food.dto.HomeResponse;
import com.panghu.food.entity.Category;
import com.panghu.food.service.CategoryService;
import com.panghu.food.service.DishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishService dishService;

    @GetMapping
    public ResponseEntity<HomeResponse> getHomeData() {
        List<Category> categories = categoryService.getAllCategories();
        HomeResponse response = new HomeResponse();
        response.setCategories(categories);
        response.setFeaturedDishes(dishService.getFeaturedDishes(null));
        response.setRecentDishes(dishService.getRecentDishes());

        Map<String, List<DishSummaryResponse>> featuredByCategoryId =
                dishService.getFeaturedDishesByCategoryIds(categories.stream()
                        .map(Category::getId)
                        .collect(Collectors.toList()));
        List<CategoryRecommendationGroup> featuredByCategory = new ArrayList<>();
        for (Category category : categories) {
            CategoryRecommendationGroup group = new CategoryRecommendationGroup();
            group.setCategoryId(category.getId());
            group.setCategoryName(category.getName());
            group.setDishes(featuredByCategoryId.getOrDefault(category.getId(), new ArrayList<>()));
            featuredByCategory.add(group);
        }
        response.setFeaturedByCategory(featuredByCategory);
        return ResponseEntity.ok(response);
    }
}
