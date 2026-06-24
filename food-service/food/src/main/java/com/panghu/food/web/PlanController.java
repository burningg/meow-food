package com.panghu.food.web;

import com.panghu.food.dto.PlanAiArrangeRequest;
import com.panghu.food.dto.PlanAiArrangementConfirmRequest;
import com.panghu.food.dto.PlanAiArrangementResponse;
import com.panghu.food.dto.PlanCreateRequest;
import com.panghu.food.dto.PlanDetailResponse;
import com.panghu.food.dto.PlanMonthResponse;
import com.panghu.food.dto.PlanRecipesUpdateRequest;
import com.panghu.food.dto.PlanRecipeCandidatesResponse;
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
    public ResponseEntity<PlanMonthResponse> getPlans(@RequestParam(required = false) String month,
                                                      @RequestParam(required = false) String sharedPlanId,
                                                      @RequestParam(required = false) String shareToken) {
        return ResponseEntity.ok(planService.getPlans(month, sharedPlanId, shareToken));
    }

    @PostMapping
    public ResponseEntity<PlanDetailResponse> createPlan(@RequestBody PlanCreateRequest request) {
        return ResponseEntity.ok(planService.createPlan(request));
    }

    @PostMapping("/ai-arrangements")
    public ResponseEntity<PlanAiArrangementResponse> arrangePlanByAi(@RequestBody PlanAiArrangeRequest request) {
        return ResponseEntity.ok(planService.arrangePlanByAi(request));
    }

    @PostMapping("/ai-arrangements/confirm")
    public ResponseEntity<PlanDetailResponse> confirmAiArrangement(@RequestBody PlanAiArrangementConfirmRequest request) {
        return ResponseEntity.ok(planService.confirmAiArrangement(request));
    }

    @GetMapping("/{planId}")
    public ResponseEntity<PlanDetailResponse> getPlanDetail(@PathVariable String planId,
                                                            @RequestParam(required = false) String shareToken) {
        return ResponseEntity.ok(planService.getPlanDetail(planId, shareToken));
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deletePlan(@PathVariable String planId) {
        planService.deletePlan(planId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{planId}/recipes")
    public ResponseEntity<PlanDetailResponse> addRecipes(@PathVariable String planId,
                                                         @RequestBody PlanRecipesUpdateRequest request,
                                                         @RequestParam(required = false) String shareToken) {
        return ResponseEntity.ok(planService.addRecipes(planId, request, shareToken));
    }

    @GetMapping("/{planId}/recipe-candidates")
    public ResponseEntity<PlanRecipeCandidatesResponse> getRecipeCandidates(@PathVariable String planId,
                                                                            @RequestParam(required = false) String shareToken) {
        return ResponseEntity.ok(planService.getRecipeCandidates(planId, shareToken));
    }

    @PutMapping("/{planId}/recipes/sort")
    public ResponseEntity<PlanDetailResponse> sortRecipes(@PathVariable String planId,
                                                          @RequestBody PlanRecipesUpdateRequest request) {
        return ResponseEntity.ok(planService.sortRecipes(planId, request));
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
