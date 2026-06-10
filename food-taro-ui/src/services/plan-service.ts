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

export interface PlanSummary {
  id: string
  title: string
  planDate: string
  circleId: string
  circleName: string
  creatorUserId: string
  creatorNickname: string
  recipeCount: number
  shoppingStatus: PlanShoppingStatus
  shoppingStarted: boolean
  shoppingTotalItemCount: number
  shoppingPurchasedItemCount: number
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
  shoppingStatus: PlanShoppingStatus
  shoppingStarted: boolean
  shoppingRestartCount: number
  shoppingTotalItemCount: number
  shoppingPurchasedItemCount: number
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
  getPlans(month: string) {
    return http.get<PlanMonthResponse>('/api/plans', { params: { month } })
  }

  createPlan(payload: { circleId: string; planDate: string; title: string }) {
    return http.post<PlanDetail>('/api/plans', payload)
  }

  getPlanDetail(planId: string) {
    return http.get<PlanDetail>(`/api/plans/${planId}`)
  }

  deletePlan(planId: string) {
    return http.delete<void>(`/api/plans/${planId}`)
  }

  addRecipes(planId: string, dishIds: string[]) {
    return http.post<PlanDetail>(`/api/plans/${planId}/recipes`, { dishIds })
  }

  sortRecipes(planId: string, dishIds: string[]) {
    return http.put<PlanDetail>(`/api/plans/${planId}/recipes/sort`, { dishIds })
  }

  removeRecipe(planId: string, dishId: string) {
    return http.delete<PlanDetail>(`/api/plans/${planId}/recipes/${dishId}`)
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
