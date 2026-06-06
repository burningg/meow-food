package com.panghu.food.service;

import com.panghu.food.dto.DishDetailResponse;
import com.panghu.food.dto.DishSummaryResponse;
import com.panghu.food.dto.DishAiAnalysisRequest;
import com.panghu.food.dto.DishAiAnalysisResponse;
import com.panghu.food.dto.DishAiImportRequest;
import com.panghu.food.dto.DishUpsertRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DishService {
    List<DishSummaryResponse> getDishes(String categoryId, String ownerUserId, String scope, String circleId);
    List<DishSummaryResponse> getFeaturedDishes(String categoryId);
    List<DishSummaryResponse> getRecentDishes();
    DishDetailResponse getDishById(String id);
    String uploadImage(MultipartFile file);
    DishDetailResponse createDish(DishUpsertRequest dishCreateDTO);
    DishDetailResponse updateDish(String id, DishUpsertRequest dishUpdateDTO);
    DishAiAnalysisResponse analyzeDishByAi(DishAiAnalysisRequest request);
    DishAiAnalysisResponse importDishByAi(DishAiImportRequest request);
    void deleteDish(String id);
}
