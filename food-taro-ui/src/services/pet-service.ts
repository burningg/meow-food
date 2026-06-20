import { http } from './http'
import type { PetType } from '@/lib/pet'

export interface PetResponse {
  claimed: boolean
  id?: string
  petType?: PetType | string
  petTypeName?: string
  name?: string
  experience?: number
  companionDays?: number
  moodCode?: 'happy' | 'bored' | string
  moodLabel?: string
  fullnessPercent?: number
  todayStory?: string
  claimedAt?: string
}

export interface PetClaimRequest {
  petType: PetType
  name: string
}

export class PetService {
  getMyPet() {
    return http.get<PetResponse>('/api/pets/me')
  }

  claimPet(data: PetClaimRequest) {
    return http.post<PetResponse>('/api/pets/claim', data)
  }
}
