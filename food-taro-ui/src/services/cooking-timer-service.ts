import { http } from './http'

export interface CookingTimerCreateRequest {
  dishName: string
  stepText: string
  seconds: number
  page?: string
}

export interface CookingTimerCreateResponse {
  timerId: string
}

export class CookingTimerService {
  createTimer(data: CookingTimerCreateRequest) {
    return http.post<CookingTimerCreateResponse>('/api/cooking-timers', data)
  }

  cancelTimer(timerId: string) {
    return http.post<void>(`/api/cooking-timers/${timerId}/cancel`)
  }
}
