package com.panghu.food.web;

import com.panghu.food.dto.DishDetailResponse;
import com.panghu.food.dto.DishAiAnalysisRequest;
import com.panghu.food.dto.DishAiAnalysisResponse;
import com.panghu.food.dto.DishSummaryResponse;
import com.panghu.food.dto.DishUpsertRequest;
import com.panghu.food.service.DishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/dishes")
public class DishController {

    @Autowired
    private DishService dishService;

    @GetMapping
    public ResponseEntity<List<DishSummaryResponse>> getDishes(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String ownerUserId,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String circleId) {
        List<DishSummaryResponse> dishes = dishService.getDishes(categoryId, ownerUserId, scope, circleId);
        return ResponseEntity.ok(dishes);
    }

    @GetMapping("/featured")
    public ResponseEntity<List<DishSummaryResponse>> getFeaturedDishes(
            @RequestParam(required = false) String categoryId) {
        return ResponseEntity.ok(dishService.getFeaturedDishes(categoryId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DishDetailResponse> getDishById(@PathVariable String id) {
        DishDetailResponse dish = dishService.getDishById(id);
        return ResponseEntity.ok(dish);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDishById(@PathVariable String id) {
        dishService.deleteDish(id);
        return ResponseEntity.ok("success");
    }


    @PostMapping
    public ResponseEntity<DishDetailResponse> createDish(@RequestBody DishUpsertRequest dishCreateDTO) {
        DishDetailResponse dishDTO = dishService.createDish(dishCreateDTO);
        return ResponseEntity.ok(dishDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DishDetailResponse> updateDish(
            @PathVariable String id,
            @RequestBody DishUpsertRequest dishUpdateDTO) {
        DishDetailResponse dishDTO = dishService.updateDish(id, dishUpdateDTO);
        return ResponseEntity.ok(dishDTO);
    }

    @PostMapping("/ai-analysis")
    public ResponseEntity<DishAiAnalysisResponse> analyzeDishByAi(@RequestBody DishAiAnalysisRequest request) {
        return ResponseEntity.ok(dishService.analyzeDishByAi(request));
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        String imageUrl = dishService.uploadImage(file);
        return ResponseEntity.ok(imageUrl);
    }

}
