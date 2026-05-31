import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth-store'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../pages/auth/login.vue'),
      meta: { transition: 'forward' },
    },
    {
      path: '/',
      redirect: '/home',
    },
    {
      path: '/home',
      name: 'home',
      component: () => import('../pages/home/home.vue'),
      meta: { transition: 'tab' },
    },
    {
      path: '/dish/add',
      name: 'add-dish',
      component: () => import('../pages/dish/add-dish.vue'),
      meta: { requiresAuth: true, transition: 'forward' },
    },
    {
      path: '/dish/:id',
      name: 'dish-detail',
      component: () => import('../pages/dish/dish-detail.vue'),
      meta: { transition: 'forward' },
    },
    {
      path: '/dish/:id/edit',
      name: 'edit-dish',
      component: () => import('../pages/dish/add-dish.vue'),
      meta: { requiresAuth: true, transition: 'forward' },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../pages/profile/profile.vue'),
      meta: { requiresAuth: true, transition: 'tab' },
    },
    {
      path: '/profile/edit',
      name: 'edit-profile',
      component: () => import('../pages/profile/edit-profile.vue'),
      meta: { requiresAuth: true, transition: 'forward' },
    },
    {
      path: '/profile/avatar',
      name: 'edit-avatar',
      component: () => import('../pages/profile/edit-avatar.vue'),
      meta: { requiresAuth: true, transition: 'forward' },
    },
    {
      path: '/friends',
      name: 'friends',
      component: () => import('../pages/profile/friends.vue'),
      meta: { requiresAuth: true, transition: 'forward' },
    },
    {
      path: '/friends/requests',
      name: 'friend-requests',
      component: () => import('../pages/profile/friend-requests.vue'),
      meta: { requiresAuth: true, transition: 'forward' },
    },
    {
      path: '/feed',
      name: 'feed',
      component: () => import('../pages/feed/feed.vue'),
      meta: { requiresAuth: true, transition: 'tab' },
    },
    {
      path: '/users/:id/menu',
      name: 'user-menu',
      component: () => import('../pages/user/user-menu.vue'),
      meta: { requiresAuth: true, transition: 'forward' },
    },
    {
      path: '/circles',
      name: 'circles',
      component: () => import('../pages/circles/circles.vue'),
      meta: { requiresAuth: true, transition: 'tab' },
    },
    {
      path: '/circles/:id',
      name: 'circle-detail',
      component: () => import('../pages/circles/circle-detail.vue'),
      meta: { requiresAuth: true, transition: 'forward' },
    },
  ],
})

let restored = false

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (!restored) {
    restored = true
    await authStore.restore()
  }
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }
  if (to.name === 'login' && authStore.isLoggedIn) {
    return { name: 'home' }
  }
  return true
})

export default router
