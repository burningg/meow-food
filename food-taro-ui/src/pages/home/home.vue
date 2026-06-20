<template>
  <view class="home-page-root">
    <PullRefreshPage @refresh="loadHome">
      <view class="page-shell home-page">
        <section class="hero">
          <view>
            <text class="eyebrow">下午好，{{ displayName }}</text>
          </view>
          <view v-if="authStore.isLoggedIn" class="hero-right">
            <view v-if="vipChipLabel" class="vip-chip">
              <text class="vip-chip-label">{{ vipChipLabel }}</text>
            </view>
            <button class="avatar" @tap="goToProfile">{{ displayName.slice(0, 1) }}</button>
          </view>
        </section>

        <section class="search-bar">
          <text class="search-icon">⌕</text>
          <input v-model.trim="searchKeyword" class="search-input" maxlength="24" placeholder="搜索菜谱名或食材" />
        </section>

        <scroll-view class="category-strip" :scroll-x="true" :scroll-left="categoryScrollLeft" :scroll-with-animation="true">
          <view class="category-strip-content">
            <button :class="['category-pill', { active: !selectedCategoryId }]" @tap="selectCategory('')">
              全部
            </button>
            <button
              v-for="category in homeData.categories"
              :key="category.id"
              :class="['category-pill', { active: category.id === selectedCategoryId }]"
              @tap="selectCategory(category.id)"
            >
              {{ category.name }}
            </button>
          </view>
        </scroll-view>

        <section class="section-block">
          <view class="section-head">
            <view>
              <text class="section-title">我的菜谱</text>
            </view>
          </view>

          <view v-if="filteredDishes.length" class="recent-row">
            <button v-for="dish in filteredDishes" :key="dish.id" class="recent-card" @tap="goToDetail(dish.id)">
              <SmartImage :src="dish.image" class-name="recent-image" />
              <view class="recent-copy">
                <text class="recent-name">{{ dish.name }}</text>
                <text class="recent-category">{{ dish.categoryName }}</text>
                <text v-if="dish.matchedIngredientNames.length" class="recent-hit">
                  命中食材：{{ dish.matchedIngredientNames.join(' / ') }}
                </text>
              </view>
            </button>
          </view>

          <view v-else class="empty-state">
            <text>{{ emptyStateText }}</text>
            <button v-if="showEmptyLoginButton" class="primary-button small" @tap="goToLogin">去登录</button>
            <button v-else-if="showEmptyAddButton" class="primary-button small" @tap="goToAdd">添加菜谱</button>
          </view>
        </section>
      </view>
    </PullRefreshPage>

    <button v-if="authStore.isLoggedIn" class="floating-add" @tap="goToAdd">＋</button>
    <PetMascot
      v-if="pet?.claimed"
      card-selector=".recent-card"
      :pet-type="pet.petType"
      @tap="goToPetDetail"
    />
    <AppTabBar active="home" :show-add="authStore.isLoggedIn" />
    <NotificationModalCard
      :visible="Boolean(importantNotification)"
      :title="importantNotification?.title || ''"
      :body="importantNotification?.body || ''"
      @confirm="closeImportantNotification"
    />
  </view>
</template>

<script setup lang="ts">
import Taro, { useDidShow, useShareAppMessage } from '@tarojs/taro'
import { computed, nextTick, onMounted, ref } from 'vue'
import AppTabBar from '@/components/AppTabBar.vue'
import NotificationModalCard from '@/components/NotificationModalCard.vue'
import PetMascot from '@/components/PetMascot.vue'
import PullRefreshPage from '@/components/PullRefreshPage.vue'
import SmartImage from '@/components/SmartImage.vue'
import { Message } from '@/lib/feedback'
import { push, resolveRoute } from '@/lib/navigation'
import { createHomeShareMessage } from '@/lib/share'
import { FoodService, type DishSummary, type HomeResponse } from '@/services/food-service'
import { NotificationService, type NotificationItem } from '@/services/notification-service'
import { PetService, type PetResponse } from '@/services/pet-service'
import { useAuthStore } from '@/stores/auth-store'

type VisibleDish = DishSummary & {
  matchedIngredientNames: string[]
}

const foodService = new FoodService()
const notificationService = new NotificationService()
const petService = new PetService()
const authStore = useAuthStore()

const homeData = ref<HomeResponse>({
  categories: [],
  featuredDishes: [],
  recentDishes: [],
  featuredByCategory: [],
})
const selectedCategoryId = ref('')
const categoryScrollLeft = ref(0)
const searchKeyword = ref('')
const importantNotification = ref<NotificationItem | null>(null)
const pet = ref<PetResponse | null>(null)
const shownImportantNotificationId = ref('')
const displayName = computed(() => authStore.user?.nickname ?? 'meoi')
const vipChipLabel = computed(() => formatVipLabel(authStore.user?.vip ? authStore.user?.vipLevel : undefined))
const showEmptyLoginButton = computed(() => !authStore.isLoggedIn && !searchKeyword.value.trim() && !homeData.value.recentDishes.length)
const showEmptyAddButton = computed(() => authStore.isLoggedIn && !searchKeyword.value.trim() && !homeData.value.recentDishes.length)
const filteredDishes = computed<VisibleDish[]>(() => {
  const categoryDishes = !selectedCategoryId.value
    ? homeData.value.recentDishes
    : homeData.value.recentDishes.filter((dish: DishSummary) => dish.categoryId === selectedCategoryId.value)
  const keyword = searchKeyword.value.trim().toLowerCase()

  if (!keyword) {
    return categoryDishes.map((dish) => ({ ...dish, matchedIngredientNames: [] }))
  }

  return categoryDishes
    .map((dish) => {
      const nameMatched = dish.name.toLowerCase().includes(keyword)
      const matchedIngredientNames = Array.from(
        new Set((dish.ingredientNames || []).filter((ingredientName) => ingredientName.toLowerCase().includes(keyword))),
      )
      if (!nameMatched && !matchedIngredientNames.length) return null
      return { ...dish, matchedIngredientNames }
    })
    .filter((dish): dish is VisibleDish => Boolean(dish))
})
const emptyStateText = computed(() => {
  if (searchKeyword.value.trim()) return '没有找到相关菜谱，换个名字或食材试试。'
  if (!authStore.isLoggedIn) return '先登录，查看你的菜谱与更多内容。'
  return '你还没有添加自己的菜品，先从第一道开始吧。'
})

onMounted(async () => {
  await loadHome()
  await Promise.all([syncImportantNotification(), syncMyPet()])
})

useDidShow(async () => {
  await syncImportantNotification()
  await syncMyPet()
})

useShareAppMessage(() =>
  createHomeShareMessage({
    title: '我在 meoi食堂发现了不少灵感菜谱，来看看吧',
  }),
)

async function loadHome() {
  try {
    const { data } = await foodService.getHomeData()
    homeData.value = data
  } catch (error) {
    Message.error('首页数据加载失败')
  }
}

async function syncImportantNotification() {
  if (!authStore.isLoggedIn) {
    importantNotification.value = null
    shownImportantNotificationId.value = ''
    return
  }

  try {
    const { data } = await notificationService.getBootstrap()
    if (!data.importantNotification) return
    if (shownImportantNotificationId.value === data.importantNotification.id) return
    importantNotification.value = data.importantNotification
    shownImportantNotificationId.value = data.importantNotification.id
    void notificationService.markRead(data.importantNotification.id).catch(() => {})
  } catch {
    // Keep home usable even if notification bootstrap fails.
  }
}

async function syncMyPet() {
  if (!authStore.isLoggedIn) {
    pet.value = null
    return
  }

  try {
    const { data } = await petService.getMyPet()
    pet.value = data.claimed ? data : null
  } catch {
    pet.value = null
  }
}

function closeImportantNotification() {
  importantNotification.value = null
}

function goToDetail(id: string) {
  push({ name: 'dish-detail', params: { id } })
}

function goToAdd() {
  push('add-dish')
}

function goToLogin() {
  push({
    name: 'login',
    query: {
      redirect: resolveRoute('home'),
    },
  })
}

function goToProfile() {
  push('profile')
}

function goToPetDetail() {
  push('pet-detail')
}

function selectCategory(categoryId: string) {
  selectedCategoryId.value = categoryId
  void centerSelectedCategory()
}

async function centerSelectedCategory() {
  await nextTick()

  const [stripRect, activeRect, contentRect] = await new Promise<
    [Taro.NodesRef.BoundingClientRectCallbackResult | null, Taro.NodesRef.BoundingClientRectCallbackResult | null, Taro.NodesRef.BoundingClientRectCallbackResult | null]
  >((resolve) => {
    Taro.createSelectorQuery()
      .select('.category-strip')
      .boundingClientRect()
      .select('.category-pill.active')
      .boundingClientRect()
      .select('.category-strip-content')
      .boundingClientRect()
      .exec((result) => {
        resolve([
          (result?.[0] as Taro.NodesRef.BoundingClientRectCallbackResult | null) ?? null,
          (result?.[1] as Taro.NodesRef.BoundingClientRectCallbackResult | null) ?? null,
          (result?.[2] as Taro.NodesRef.BoundingClientRectCallbackResult | null) ?? null,
        ])
      })
  })

  if (!stripRect || !activeRect || !contentRect) return

  const nextScrollLeft =
    categoryScrollLeft.value +
    (activeRect.left - stripRect.left) -
    (stripRect.width - activeRect.width) / 2
  const maxScrollLeft = Math.max(contentRect.width - stripRect.width, 0)

  categoryScrollLeft.value = Math.min(Math.max(nextScrollLeft, 0), maxScrollLeft)
}

function formatVipLabel(level?: string) {
  if (!level) return ''
  const normalized = level.trim().replace(/\s+/g, ' ')
  if (/^vip\b/i.test(normalized)) {
    return normalized.replace(/\s+/g, '·')
  }
  return 'VIP'
}
</script>

<style>
.home-page {
  padding-bottom: 120px;
}

.hero,
.section-head,
.recent-copy,
.search-bar,
.empty-state {
  display: flex;
}

.hero,
.section-head {
  align-items: center;
  justify-content: space-between;
}

.hero {
  margin-bottom: 18px;
}

.hero-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.eyebrow {
  color: #8b8b8b;
  font-size: var(--text-sm);
}

.avatar {
  display: flex;
  box-sizing: border-box;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 999px;
  background: #7a9e7e;
  color: #fff;
  font-weight: 800;
}

.vip-chip {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #6e4317 0%, #b97a2a 100%);
  box-shadow: 0 6px 14px rgba(164, 111, 31, 0.12);
  padding: 5px 9px;
}

.vip-chip-icon,
.vip-chip-label {
  color: #fff3d6;
}

.vip-chip-icon {
  font-size: 10px;
  line-height: 1;
}

.vip-chip-label {
  font-size: 10px;
  font-weight: 800;
  line-height: 1;
}

.search-bar {
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #eeebe6;
  color: #8b8b8b;
  margin-bottom: 16px;
}

.search-input {
  flex: 1;
  min-width: 0;
  color: #151515;
  font-size: 14px;
}

.category-strip {
  padding: 0 2px 8px;
  white-space: nowrap;
  margin-bottom: 18px;
}

.category-strip-content {
  display: inline-flex;
  padding-right: 8px;
}

.category-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
  border-radius: 999px;
  background: #fff;
  padding: 10px 16px;
  font-size: var(--text-sm);
  font-weight: 700;
}

.category-pill.active {
  background: #1b3a2d;
  color: #fff;
}

.section-block {
  padding: 18px;
  border-radius: 22px;
  background: var(--bg-card);
  box-shadow: var(--shadow);
}

.section-title {
  color: var(--text-main);
  font-size: var(--title-md);
  font-weight: 800;
}

.recent-row {
  display: grid;
  margin-top: 18px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.recent-card {
  position: relative;
  height: 128px;
  overflow: hidden;
  border-radius: 18px;
  background: #ddd;
  text-align: left;
}

.recent-image {
  width: 100%;
  height: 100%;
}

.recent-copy {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  flex-direction: column;
  padding: 30px 10px 10px;
  background: linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.62));
}

.recent-name {
  color: #fff;
  font-size: 13px;
  font-weight: 800;
}

.recent-category {
  color: rgba(255, 255, 255, 0.78);
  font-size: 11px;
}

.recent-hit {
  color: #d8f0d5;
  font-size: 11px;
}

.empty-state {
  flex-direction: column;
  gap: 14px;
  align-items: flex-start;
}

.small {
  min-height: 40px;
  padding: 0 18px;
  font-size: 13px;
}

.floating-add {
  position: fixed;
  right: calc(50% - 178px);
  bottom: 92px;
  z-index: 15;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  border-radius: 999px;
  background: #9f5c38;
  color: #fff;
  font-size: 26px;
  box-shadow: 0 16px 32px rgba(159, 92, 56, 0.24);
}
</style>
