package com.panghu.food.service;

import com.panghu.food.dto.PlanCreateRequest;
import com.panghu.food.dto.PlanAiArrangeRequest;
import com.panghu.food.dto.PlanAiArrangementConfirmRequest;
import com.panghu.food.dto.PlanAiArrangementResponse;
import com.panghu.food.dto.PlanDetailResponse;
import com.panghu.food.dto.PlanMonthResponse;
import com.panghu.food.dto.PlanRecipesUpdateRequest;
import com.panghu.food.dto.PlanRecipeCandidatesResponse;
import com.panghu.food.dto.PlanShoppingListResponse;
import com.panghu.food.dto.PlanVisibleUsersUpdateRequest;

public interface PlanService {
    PlanMonthResponse getPlans(String month, String sharedPlanId, String shareToken);

    PlanDetailResponse createPlan(PlanCreateRequest request);

    PlanAiArrangementResponse arrangePlanByAi(PlanAiArrangeRequest request);

    PlanDetailResponse confirmAiArrangement(PlanAiArrangementConfirmRequest request);

    PlanDetailResponse getPlanDetail(String planId, String shareToken);

    void deletePlan(String planId);

    PlanDetailResponse updateVisibleUsers(String planId, PlanVisibleUsersUpdateRequest request);

    PlanRecipeCandidatesResponse getRecipeCandidates(String planId, String shareToken);

    PlanDetailResponse addRecipes(String planId, PlanRecipesUpdateRequest request, String shareToken);

    PlanDetailResponse sortRecipes(String planId, PlanRecipesUpdateRequest request);

    PlanDetailResponse removeRecipe(String planId, String dishId, String shareToken);

    PlanShoppingListResponse startShoppingList(String planId);

    PlanShoppingListResponse getShoppingList(String planId);

    PlanShoppingListResponse toggleShoppingItem(String planId, String itemId);

    PlanShoppingListResponse resetShoppingList(String planId);
}
