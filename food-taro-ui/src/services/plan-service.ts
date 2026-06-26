import type { DishSummary } from './food-service'
import { http } from './http'

export interface PlanRecipe extends DishSummary {
  addedByUserId?: string
  addedByNickname?: string
}

export type PlanShoppingStatus =
  | 'NOT_STARTED'
  | 'NOT_PURCHASED'
  | 'PARTIALLY_PURCHASED'
  | 'PURCHASED'

export type PlanAiMealType = 'lunch' | 'dinner'

export interface PlanAiUsage {
  monthlyLimit: number
  usedThisMonth: number
  remainingThisMonth: number
}

export interface PlanAiArrangeRequest {
  circleId: string
  mealType: PlanAiMealType
  planDate: string
  dishCount: number
  healthAdvice?: string
}

export interface PlanAiArrangementRecipe {
  dish: DishSummary
  reason: string
}

export interface PlanAiArrangeResponse {
  title: string
  petText: string
  suggestionText: string
  healthText: string
  recipes: PlanAiArrangementRecipe[]
  usage?: PlanAiUsage
}

export interface PlanAiArrangementConfirmRequest {
  circleId: string
  planDate: string
  title: string
  dishIds: string[]
  visibleUserIds?: string[]
}

export interface PlanSummary {
  id: string
  title: string
  planDate: string
  circleId: string
  circleName: string
  creatorUserId: string
  creatorNickname: string
  shareToken?: string
  recipeCount: number
  shoppingStatus: PlanShoppingStatus
  shoppingStarted: boolean
  shoppingTotalItemCount: number
  shoppingPurchasedItemCount: number
  sharedView: boolean
  viewerCanDelete: boolean
  viewerCanAddRecipes: boolean
  viewerCanManageRecipes: boolean
  viewerCanUseShopping: boolean
}

export interface PlanDayPlans {
  date: string
  plans: PlanSummary[]
}

export interface PlanMonthResponse {
  month: string
  days: PlanDayPlans[]
}

export interface PlanDetail {
  id: string
  title: string
  planDate: string
  circleId: string
  circleName: string
  creatorUserId: string
  creatorNickname: string
  shareToken?: string
  shoppingStatus: PlanShoppingStatus
  shoppingStarted: boolean
  shoppingRestartCount: number
  shoppingTotalItemCount: number
  shoppingPurchasedItemCount: number
  sharedView: boolean
  viewerCanDelete: boolean
  viewerCanAddRecipes: boolean
  viewerCanManageRecipes: boolean
  viewerCanUseShopping: boolean
  recipes: PlanRecipe[]
}

export interface PlanRecipeCandidatesResponse {
  viewerCanAddRecipes: boolean
  viewerIsCircleMember: boolean
  sourceLabel: string
  recipes: PlanRecipe[]
}

export interface PlanShoppingItemSource {
  dishId: string
  dishName: string
  amount: string
}

export interface PlanShoppingItem {
  id: string
  ingredientName: string
  purchased: boolean
  purchasedByUserId?: string
  purchasedByNickname?: string
  purchasedAt?: string
  sources: PlanShoppingItemSource[]
}

export interface PlanShoppingList {
  id?: string
  planId: string
  planTitle: string
  planDate: string
  circleId: string
  circleName: string
  shoppingStarted: boolean
  startedByUserId?: string
  startedByNickname?: string
  startedAt?: string
  restartCount: number
  totalItemCount: number
  purchasedItemCount: number
  items: PlanShoppingItem[]
}

export class PlanService {
  getPlans(month: string, options?: { sharedPlanId?: string; shareToken?: string }) {
    return http.get<PlanMonthResponse>('/api/plans', {
      params: {
        month,
        sharedPlanId: options?.sharedPlanId,
        shareToken: options?.shareToken,
      },
    })
  }

  createPlan(payload: { circleId: string; planDate: string; title: string; visibleUserIds?: string[] }) {
    return http.post<PlanDetail>('/api/plans', payload)
  }

  arrangePlanByAi(payload: PlanAiArrangeRequest) {
    return http.post<PlanAiArrangeResponse>('/api/plans/ai-arrangements', payload)
  }

  confirmAiArrangement(payload: PlanAiArrangementConfirmRequest) {
    return http.post<PlanDetail>('/api/plans/ai-arrangements/confirm', payload)
  }

  getPlanDetail(planId: string, shareToken?: string) {
    return http.get<PlanDetail>(`/api/plans/${planId}`, {
      params: shareToken ? { shareToken } : undefined,
    })
  }

  deletePlan(planId: string) {
    return http.delete<void>(`/api/plans/${planId}`)
  }

  getRecipeCandidates(planId: string, shareToken?: string) {
    return http.get<PlanRecipeCandidatesResponse>(`/api/plans/${planId}/recipe-candidates`, {
      params: shareToken ? { shareToken } : undefined,
    })
  }

  addRecipes(planId: string, dishIds: string[], shareToken?: string) {
    return http.post<PlanDetail>(`/api/plans/${planId}/recipes`, { dishIds }, {
      params: shareToken ? { shareToken } : undefined,
    })
  }

  sortRecipes(planId: string, dishIds: string[]) {
    return http.put<PlanDetail>(`/api/plans/${planId}/recipes/sort`, { dishIds })
  }

  removeRecipe(planId: string, dishId: string, shareToken?: string) {
    return http.delete<PlanDetail>(`/api/plans/${planId}/recipes/${dishId}`, {
      params: shareToken ? { shareToken } : undefined,
    })
  }

  startShoppingList(planId: string) {
    return http.post<PlanShoppingList>(`/api/plans/${planId}/shopping-list/start`)
  }

  getShoppingList(planId: string) {
    return http.get<PlanShoppingList>(`/api/plans/${planId}/shopping-list`)
  }

  toggleShoppingItem(planId: string, itemId: string) {
    return http.post<PlanShoppingList>(`/api/plans/${planId}/shopping-list/items/${itemId}/toggle`)
  }

  resetShoppingList(planId: string) {
    return http.post<PlanShoppingList>(`/api/plans/${planId}/shopping-list/reset`)
  }
}
