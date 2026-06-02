import Taro from '@tarojs/taro'
import { currentPageUrl, replace } from '@/lib/navigation'

export const TOKEN_KEY = 'food-me-token'

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'
type QueryParams = Record<string, string | number | boolean | undefined>

const PROD_BASE_URL = 'https://meow-service.youl.work'
const DEV_BASE_URL = 'http://localhost:8080'

let isRedirectingToLogin = false

export const API_BASE_URL =
  process.env.TARO_APP_API_BASE || (process.env.NODE_ENV === 'production' ? PROD_BASE_URL : DEV_BASE_URL)

export function getToken() {
  return Taro.getStorageSync<string>(TOKEN_KEY) || ''
}

export function setToken(token: string) {
  Taro.setStorageSync(TOKEN_KEY, token)
}

export function removeToken() {
  Taro.removeStorageSync(TOKEN_KEY)
}

function appendParams(url: string, params?: QueryParams) {
  if (!params) return url
  const query = Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&')
  if (!query) return url
  return `${url}${url.includes('?') ? '&' : '?'}${query}`
}

function createError<T>(statusCode: number, data: T, path: string) {
  return {
    response: {
      status: statusCode,
      statusCode,
      data,
    },
    config: {
      url: path,
    },
  }
}

async function request<T>(method: HttpMethod, path: string, options: { params?: QueryParams; data?: unknown; headers?: Record<string, string> } = {}) {
  const token = getToken()
  const url = appendParams(`${API_BASE_URL}${path}`, options.params)
  const header: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }

  if (token) {
    header.Authorization = `Bearer ${token}`
  }

  const response = await Taro.request<T>({
    url,
    method,
    data: options.data as never,
    header,
  })

  if (response.statusCode >= 200 && response.statusCode < 300) {
    return { data: response.data, status: response.statusCode }
  }

  const isAuthSubmit = path === '/api/auth/login' || path === '/api/auth/register'
  if (response.statusCode === 401 && !isAuthSubmit) {
    removeToken()
    if (!isRedirectingToLogin) {
      isRedirectingToLogin = true
      await replace({
        name: 'login',
        query: {
          redirect: currentPageUrl(),
        },
      })
      isRedirectingToLogin = false
    }
  }

  throw createError(response.statusCode, response.data, path)
}

export const http = {
  get<T>(path: string, options?: { params?: QueryParams }) {
    return request<T>('GET', path, options)
  },
  post<T>(path: string, data?: unknown, options?: { params?: QueryParams; headers?: Record<string, string> }) {
    return request<T>('POST', path, { ...options, data })
  },
  put<T>(path: string, data?: unknown, options?: { params?: QueryParams; headers?: Record<string, string> }) {
    return request<T>('PUT', path, { ...options, data })
  },
  delete<T>(path: string, options?: { params?: QueryParams }) {
    return request<T>('DELETE', path, options)
  },
}
