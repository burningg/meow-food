import axios from 'axios'

export const TOKEN_KEY = 'food-me-token'

export const http = axios.create({
  baseURL: 'http://localhost:8080',
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
