import Taro from '@tarojs/taro'
import { http, API_BASE_URL, getToken } from './http'
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

export interface RawMaterialInfo {
  ingredientName: string
  name: string
  commonNames: string
  steamTime: string
  boilTime: string
  fryTime: string
  bakeTime: string
  stirFryTime: string
  defaultHeatTemperature: string
  allergenFlag: string
  calorieEstimate: string
  nutritionInfo: string
  substituteIngredients: string
  category: string
}

export interface StepItem {
  stepNo?: number
  content: string
}

export interface VipUsage {
  dailyLimit: number
  usedToday: number
  remainingToday: number
}

export interface DishAiAnalysisRequest {
  image: string
  name?: string
}

export interface DishAiImportRequest {
  text?: string
  images: string[]
}

export interface DishAiAnalysisResponse {
  name?: string
  ingredients: IngredientItem[]
  steps: StepItem[]
  usage: VipUsage
}

export interface DishSummary {
  id: string
  ownerUserId: string
  ownerNickname: string
  name: string
  image: string
  description: string
  categoryId: string
  categoryName: string
  cookTimeMinutes: number | null
  difficulty: Difficulty
  servings: number
  visibility: MenuVisibility
  effectiveVisibility: Exclude<MenuVisibility, 'inherit'>
  visibilityCircleIds: string[]
  effectiveCircleIds: string[]
  isFeatured: boolean
  createdAt?: string
  ingredientNames: string[]
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
  visibility: Exclude<MenuVisibility, 'inherit'>
  visibilityCircleIds: string[]
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

  queryDishes(params: { categoryId?: string; ownerUserId?: string; scope?: string; circleId?: string }) {
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

  matchRawMaterials(names: string[]) {
    return http.post<RawMaterialInfo[]>('/api/raw-materials/match', { names })
  }

  deleteDish(id: string) {
    return http.delete<string>(`/api/dishes/${id}`)
  }

  uploadImage(filePath: string) {
    const token = getToken()
    return new Promise<{ data: string }>((resolve, reject) => {
      Taro.uploadFile({
        url: `${API_BASE_URL}/api/dishes/upload`,
        filePath,
        name: 'file',
        header: token ? { Authorization: `Bearer ${token}` } : undefined,
        success: (response) => {
          if (response.statusCode >= 200 && response.statusCode < 300) {
            resolve({ data: response.data })
            return
          }
          reject({
            response: {
              status: response.statusCode,
              statusCode: response.statusCode,
              data: response.data,
            },
          })
        },
        fail: reject,
      })
    })
  }

  createDish(data: DishUpsertRequest) {
    return http.post<DishDetail>('/api/dishes', data)
  }

  updateDish(id: string, data: DishUpsertRequest) {
    return http.put<DishDetail>(`/api/dishes/${id}`, data)
  }

  analyzeDishByAi(data: DishAiAnalysisRequest) {
    return http.post<DishAiAnalysisResponse>('/api/dishes/ai-analysis', data)
  }

  importDishByAi(data: DishAiImportRequest) {
    return http.post<DishAiAnalysisResponse>('/api/dishes/ai-import', data)
  }
}
