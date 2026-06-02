import { computed, ref } from 'vue'
import Taro from '@tarojs/taro'
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

  async function wechatLogin() {
    const wechatUser = await getWechatUserInfo()
    const loginResult = await Taro.login()
    if (!loginResult.code) {
      throw new Error(loginResult.errMsg || '微信登录失败')
    }
    const { data } = await authService.wechatLogin({
      code: loginResult.code,
      nickname: wechatUser.nickname,
      avatar: wechatUser.avatar,
    })
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
    wechatLogin,
    restore,
    logout,
    setSession,
  }
})

async function getWechatUserInfo(): Promise<{ nickname?: string; avatar?: string }> {
  if (process.env.TARO_ENV !== 'weapp') {
    return {}
  }

  try {
    const setting = await Taro.getSetting()
    if (setting.authSetting?.['scope.userInfo']) {
      const { userInfo } = await Taro.getUserInfo({ lang: 'zh_CN' })
      return {
        nickname: userInfo.nickName,
        avatar: userInfo.avatarUrl,
      }
    }
  } catch {
  }

  try {
    const { userInfo } = await Taro.getUserProfile({
      desc: '用于完善用户资料',
      lang: 'zh_CN',
    })
    console.log(userInfo);
    return {
      nickname: userInfo.nickName,
      avatar: userInfo.avatarUrl,
    }
  } catch {
    return {}
  }
}
