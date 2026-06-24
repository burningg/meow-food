import { useAuthStore } from '@/stores/auth-store'
import { currentPageUrl, replace, type RouteName } from './navigation'

let restored = false

export async function requireAuth(currentRoute: RouteName, redirectPath = currentPageUrl()) {
  const authStore = useAuthStore()
  if (!restored) {
    restored = true
    await authStore.restore()
  }
  if (!authStore.isLoggedIn) {
    await replace({
      name: 'login',
      query: {
        redirect: redirectPath,
        from: currentRoute,
      },
    })
    return false
  }
  return true
}
