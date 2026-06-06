package com.panghu.food.web;

import com.panghu.food.dto.PlanCreateRequest;
import com.panghu.food.dto.PlanDetailResponse;
import com.panghu.food.dto.PlanMonthResponse;
import com.panghu.food.dto.PlanRecipesUpdateRequest;
import com.panghu.food.dto.PlanShoppingListResponse;
import com.panghu.food.service.PlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans")
public class PlanController {
    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public ResponseEntity<PlanMonthResponse> getPlans(@RequestParam(required = false) String month) {
        return ResponseEntity.ok(planService.getPlans(month));
    }

    @PostMapping
    public ResponseEntity<PlanDetailResponse> createPlan(@RequestBody PlanCreateRequest request) {
        return ResponseEntity.ok(planService.createPlan(request));
    }

    @GetMapping("/{planId}")
    public ResponseEntity<PlanDetailResponse> getPlanDetail(@PathVariable String planId) {
        return ResponseEntity.ok(planService.getPlanDetail(planId));
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deletePlan(@PathVariable String planId) {
        planService.deletePlan(planId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{planId}/recipes")
    public ResponseEntity<PlanDetailResponse> addRecipes(@PathVariable String planId,
                                                         @RequestBody PlanRecipesUpdateRequest request) {
        return ResponseEntity.ok(planService.addRecipes(planId, request));
    }

    @DeleteMapping("/{planId}/recipes/{dishId}")
    public ResponseEntity<PlanDetailResponse> removeRecipe(@PathVariable String planId,
                                                           @PathVariable String dishId) {
        return ResponseEntity.ok(planService.removeRecipe(planId, dishId));
    }

    @PostMapping("/{planId}/shopping-list/start")
    public ResponseEntity<PlanShoppingListResponse> startShoppingList(@PathVariable String planId) {
        return ResponseEntity.ok(planService.startShoppingList(planId));
    }

    @GetMapping("/{planId}/shopping-list")
    public ResponseEntity<PlanShoppingListResponse> getShoppingList(@PathVariable String planId) {
        return ResponseEntity.ok(planService.getShoppingList(planId));
    }

    @PostMapping("/{planId}/shopping-list/items/{itemId}/toggle")
    public ResponseEntity<PlanShoppingListResponse> toggleShoppingItem(@PathVariable String planId,
                                                                       @PathVariable String itemId) {
        return ResponseEntity.ok(planService.toggleShoppingItem(planId, itemId));
    }

    @PostMapping("/{planId}/shopping-list/reset")
    public ResponseEntity<PlanShoppingListResponse> resetShoppingList(@PathVariable String planId) {
        return ResponseEntity.ok(planService.resetShoppingList(planId));
    }
}
