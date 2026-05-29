import { http } from './http'

export type MenuVisibility = 'inherit' | 'public' | 'friends' | 'private'

export interface AuthUser {
  id: string
  account: string
  nickname: string
  avatar: string
  bio: string
  defaultMenuVisibility: Exclude<MenuVisibility, 'inherit'>
}

export interface LoginRequest {
  account: string
  password: string
}

export interface LoginResponse {
  token: string
  user: AuthUser
}

export class AuthService {
  login(data: LoginRequest) {
    return http.post<LoginResponse>('/api/auth/login', data)
  }

  me() {
    return http.get<AuthUser>('/api/auth/me')
  }
}
