import Taro from '@tarojs/taro'

export type RouteName =
  | 'login'
  | 'register'
  | 'loading'
  | 'home'
  | 'add-dish'
  | 'dish-detail'
  | 'edit-dish'
  | 'profile'
  | 'edit-profile'
  | 'edit-avatar'
  | 'friends'
  | 'friend-requests'
  | 'friend-invite'
  | 'plan'
  | 'plan-detail'
  | 'plan-shopping'
  | 'user-menu'
  | 'circles'
  | 'create-circle'
  | 'circle-members'
  | 'circle-share-invite'

const primaryRouteNames: RouteName[] = ['home', 'plan', 'circles', 'profile']

type RouteLocation = {
  name: RouteName
  params?: Record<string, string | number | undefined>
  query?: Record<string, string | number | undefined>
}

const routePathMap: Record<RouteName, string> = {
  login: '/pages/auth/login',
  register: '/pages/auth/register',
  loading: '/pages/loading/loading',
  home: '/pages/home/home',
  'add-dish': '/pages/dish/add-dish',
  'dish-detail': '/pages/dish/dish-detail',
  'edit-dish': '/pages/dish/add-dish',
  profile: '/pages/profile/profile',
  'edit-profile': '/pages/profile/edit-profile',
  'edit-avatar': '/pages/profile/edit-avatar',
  friends: '/pages/profile/friends',
  'friend-requests': '/pages/profile/friend-requests',
  'friend-invite': '/pages/profile/friend-invite',
  plan: '/pages/plan/plan',
  'plan-detail': '/pages/plan/plan-detail',
  'plan-shopping': '/pages/plan/plan-shopping',
  'user-menu': '/pages/user/user-menu',
  circles: '/pages/circles/circles',
  'create-circle': '/pages/circles/create-circle',
  'circle-members': '/pages/circles/circle-members',
  'circle-share-invite': '/pages/circles/circle-share-invite',
}

function encodeQuery(query: Record<string, string | number | undefined>) {
  return Object.entries(query)
    .filter(([, value]) => value !== undefined && value !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&')
}

export function resolveRoute(location: RouteLocation | RouteName) {
  const target = typeof location === 'string' ? { name: location } : location
  const params = target.params || {}
  const query = { ...(target.query || {}) }

  if (
    target.name === 'dish-detail' ||
    target.name === 'plan-detail' ||
    target.name === 'plan-shopping' ||
    target.name === 'user-menu' ||
    target.name === 'circles' ||
    target.name === 'circle-members'
  ) {
    query.id = params.id
  }

  if (target.name === 'friend-invite') {
    query.inviterId = params.inviterId
  }

  if (target.name === 'circle-share-invite') {
    query.circleId = params.circleId
    query.inviterId = params.inviterId
  }

  if (target.name === 'edit-dish') {
    query.id = params.id
    query.mode = 'edit'
  }

  const path = routePathMap[target.name]
  const queryText = encodeQuery(query)
  return queryText ? `${path}?${queryText}` : path
}

export function resolveSharePath(location: RouteLocation | RouteName) {
  return resolveRoute(location).replace(/^\//, '')
}

export function isPrimaryRoute(routeName: RouteName) {
  return primaryRouteNames.includes(routeName)
}

export function push(location: RouteLocation | RouteName) {
  return Taro.navigateTo({ url: resolveRoute(location) })
}

export function replace(location: RouteLocation | RouteName) {
  return Taro.redirectTo({ url: resolveRoute(location) })
}

export function openPrimaryRoute(location: RouteLocation | RouteName) {
  const target = typeof location === 'string' ? { name: location } : location
  return Taro.reLaunch({ url: resolveRoute(target) })
}

export function goBack(fallback?: RouteLocation | RouteName) {
  const pages = Taro.getCurrentPages()
  if (pages.length > 1) {
    Taro.navigateBack()
    return
  }
  if (fallback) {
    replace(fallback)
  }
}

export function getRouteParams<T extends Record<string, string> = Record<string, string>>() {
  return (Taro.getCurrentInstance().router?.params || {}) as T
}

export function currentPageUrl() {
  const pages = Taro.getCurrentPages()
  const current = pages[pages.length - 1]
  if (!current) return resolveRoute('home')
  const currentRoute = current.route || ''
  const route = currentRoute.startsWith('/') ? currentRoute : `/${currentRoute}`
  const options = (current as unknown as { options?: Record<string, string> }).options || {}
  const query = encodeQuery(options)
  return query ? `${route}?${query}` : route
}

export function navigateByLegacyPath(path: string, fallback: RouteName = 'home') {
  if (!path) return replace(fallback)
  if (path.startsWith('/pages/')) return replaceByUrl(path)
  if (path === '/' || path === '/home') return openPrimaryRoute('home')
  if (path === '/login') return replace('login')
  if (path === '/register') return replace('register')
  if (path === '/profile') return openPrimaryRoute('profile')
  if (path === '/feed' || path === '/plan') return openPrimaryRoute('plan')
  if (path === '/circles') return openPrimaryRoute('circles')
  if (path === '/dish/add') return push('add-dish')

  const dishMatch = path.match(/^\/dish\/([^/?#]+)(\/edit)?/)
  if (dishMatch?.[2]) return replace({ name: 'edit-dish', params: { id: dishMatch[1] } })
  if (dishMatch) return replace({ name: 'dish-detail', params: { id: dishMatch[1] } })

  const userMenuMatch = path.match(/^\/users\/([^/?#]+)\/menu/)
  if (userMenuMatch) return replace({ name: 'user-menu', params: { id: userMenuMatch[1] } })

  const circleMembersMatch = path.match(/^\/circles\/([^/?#]+)\/members/)
  if (circleMembersMatch) return replace({ name: 'circle-members', params: { id: circleMembersMatch[1] } })

  const circleMatch = path.match(/^\/circles\/([^/?#]+)/)
  if (circleMatch) return replace({ name: 'circles', params: { id: circleMatch[1] } })

  return replace(fallback)
}

function replaceByUrl(url: string) {
  return Taro.redirectTo({ url })
}
