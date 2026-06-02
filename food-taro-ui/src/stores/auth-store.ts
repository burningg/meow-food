import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { AuthService, type AuthUser } from '@/services/auth-service'
import { getToken, removeToken, setToken } from '@/services/http'

const authService = new AuthService()

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const user = ref<AuthUser | null>(null)

  const isLoggedIn = computed(() => Boolean(token.value))

  function setSession(nextToken: string, nextUser: AuthUser) {
    token.value = nextToken
    user.value = nextUser
    setToken(nextToken)
  }

  function clearSession() {
    token.value = ''
    user.value = null
    removeToken()
  }

  async function login(account: string, password: string) {
    const { data } = await authService.login({ account, password })
    setSession(data.token, data.user)
    return data.user
  }

  async function register(account: string, password: string) {
    const { data } = await authService.register({ account, password })
    setSession(data.token, data.user)
    return data.user
  }

  async function restore() {
    token.value = getToken()
    if (!token.value) {
      return null
    }
    try {
      const { data } = await authService.me()
      user.value = data
      return data
    } catch (error) {
      clearSession()
      return null
    }
  }

  function logout() {
    clearSession()
  }

  return {
    token,
    user,
    isLoggedIn,
    login,
    register,
    restore,
    logout,
    setSession,
  }
})
