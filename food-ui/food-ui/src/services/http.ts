import axios from 'axios'

export const TOKEN_KEY = 'food-me-token'

let isRedirectingToLogin = false

export const http = axios.create({
  baseURL: import.meta.env.PROD ? 'https://meow-service.youl.work' : 'http://localhost:8080',
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      const requestPath = new URL(error.config?.url || '', window.location.origin).pathname
      const isAuthSubmit = requestPath === '/api/auth/login' || requestPath === '/api/auth/register'

      if (!isAuthSubmit) {
        localStorage.removeItem(TOKEN_KEY)
      }

      const currentPath = `${window.location.pathname}${window.location.search}${window.location.hash}`
      const isAuthPage = window.location.pathname === '/login' || window.location.pathname === '/register'

      if (!isAuthSubmit && !isAuthPage && !isRedirectingToLogin) {
        isRedirectingToLogin = true
        window.location.assign(`/login?redirect=${encodeURIComponent(currentPath)}`)
      }
    }

    return Promise.reject(error)
  },
)
