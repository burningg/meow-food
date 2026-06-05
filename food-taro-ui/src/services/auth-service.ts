import { http } from './http'

export type MenuVisibility = 'inherit' | 'public' | 'private' | 'circle' | 'friends'

export interface AuthUser {
  id: string
  account: string
  nickname: string
  avatar: string
  bio: string
  defaultMenuVisibility: Exclude<MenuVisibility, 'inherit'>
  defaultMenuCircleIds: string[]
  vip: boolean
  vipLevel?: string
}

export interface LoginRequest {
  account: string
  password: string
}

export interface RegisterRequest {
  account: string
  password: string
}

export interface WechatLoginRequest {
  code: string
  nickname?: string
  avatar?: string
}

export interface LoginResponse {
  token: string
  user: AuthUser
}

export class AuthService {
  login(data: LoginRequest) {
    return http.post<LoginResponse>('/api/auth/login', data)
  }

  register(data: RegisterRequest) {
    return http.post<LoginResponse>('/api/auth/register', data)
  }

  wechatLogin(data: WechatLoginRequest) {
    return http.post<LoginResponse>('/api/auth/wechat-login', data)
  }

  me() {
    return http.get<AuthUser>('/api/auth/me')
  }
}
