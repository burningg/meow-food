import { http } from './http'
export type { MenuVisibility } from './auth-service'
import type { MenuVisibility } from './auth-service'

export type Difficulty = 'easy' | 'medium' | 'hard'

export interface Category {
  id: string
  name: string
  sort?: number
}

export interface IngredientItem {
  name: string
  amount: string
  sort?: number
}

export interface StepItem {
  stepNo?: number
  content: string
}

export interface DishSummary {
  id: string
  ownerUserId: number
  ownerNickname: string
  name: string
  image: string
  description: string
  categoryId: string
  categoryName: string
  cookTimeMinutes: number
  difficulty: Difficulty
  servings: number
  visibility: MenuVisibility
  effectiveVisibility: Exclude<MenuVisibility, 'inherit'>
  isFeatured: boolean
}

export interface DishDetail extends DishSummary {
  price?: number
  status?: number
  createdAt?: string
  updatedAt?: string
  ingredients: IngredientItem[]
  steps: StepItem[]
}

export interface DishUpsertRequest {
  name: string
  image: string
  description: string
  categoryId: string
  cookTimeMinutes: number | null
  difficulty: Difficulty
  servings: number | null
  visibility: MenuVisibility
  ingredients: IngredientItem[]
  steps: StepItem[]
}

export interface CategoryRecommendationGroup {
  categoryId: string
  categoryName: string
  dishes: DishSummary[]
}

export interface HomeResponse {
  categories: Category[]
  featuredDishes: DishSummary[]
  recentDishes: DishSummary[]
  featuredByCategory: CategoryRecommendationGroup[]
}

export class FoodService {
  queryCategory() {
    return http.get<Category[]>('/api/categories')
  }

  getHomeData() {
    return http.get<HomeResponse>('/api/home')
  }

  queryDishes(params: { categoryId?: string; ownerUserId?: number; scope?: string; circleId?: number }) {
    return http.get<DishSummary[]>('/api/dishes', { params })
  }

  getFeaturedDishes(categoryId?: string) {
    return http.get<DishSummary[]>('/api/dishes/featured', {
      params: categoryId ? { categoryId } : undefined,
    })
  }

  getDishDetail(id: string) {
    return http.get<DishDetail>(`/api/dishes/${id}`)
  }

  deleteDish(id: string) {
    return http.delete<string>(`/api/dishes/${id}`)
  }

  uploadImage(formData: FormData) {
    return http.post<string>('/api/dishes/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  }

  createDish(data: DishUpsertRequest) {
    return http.post<DishDetail>('/api/dishes', data)
  }

  updateDish(id: string, data: DishUpsertRequest) {
    return http.put<DishDetail>(`/api/dishes/${id}`, data)
  }
}
