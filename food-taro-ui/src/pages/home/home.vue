<template>
  <view class="page-shell home-page">
    <section class="hero">
      <view>
        <text class="eyebrow">下午好，{{ displayName }}</text>
      </view>
      <button class="avatar" @tap="goToProfile">{{ displayName.slice(0, 1) }}</button>
    </section>

    <section class="search-bar">
      <text class="search-icon">⌕</text>
      <text>搜索菜谱...</text>
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
          <text class="section-title">我的菜品</text>
        </view>
      </view>

      <view v-if="filteredDishes.length" class="recent-row">
        <button v-for="dish in filteredDishes" :key="dish.id" class="recent-card" @tap="goToDetail(dish.id)">
          <SmartImage :src="dish.image" class-name="recent-image" />
          <view class="recent-copy">
            <text class="recent-name">{{ dish.name }}</text>
            <text class="recent-category">{{ dish.categoryName }}</text>
          </view>
        </button>
      </view>

      <view v-else class="empty-state">
        <text>你还没有添加自己的菜品，先从第一道开始吧。</text>
        <button class="primary-button small" @tap="goToAdd">添加菜谱</button>
      </view>
    </section>

    <button class="floating-add" @tap="goToAdd">＋</button>
    <AppTabBar active="home" />
  </view>
</template>

<script setup lang="ts">
import Taro from '@tarojs/taro'
import { computed, nextTick, onMounted, ref } from 'vue'
import AppTabBar from '@/components/AppTabBar.vue'
import SmartImage from '@/components/SmartImage.vue'
import { Message } from '@/lib/feedback'
import { push } from '@/lib/navigation'
import { requireAuth } from '@/lib/auth'
import { FoodService, type DishSummary, type HomeResponse } from '@/services/food-service'
import { useAuthStore } from '@/stores/auth-store'

const foodService = new FoodService()
const authStore = useAuthStore()

const homeData = ref<HomeResponse>({
  categories: [],
  featuredDishes: [],
  recentDishes: [],
  featuredByCategory: [],
})
const selectedCategoryId = ref('')
const categoryScrollLeft = ref(0)
const displayName = computed(() => authStore.user?.nickname ?? 'meow')
const filteredDishes = computed(() => {
  if (!selectedCategoryId.value) return homeData.value.recentDishes
  return homeData.value.recentDishes.filter((dish: DishSummary) => dish.categoryId === selectedCategoryId.value)
})

onMounted(async () => {
  if (!(await requireAuth('home'))) return
  await loadHome()
})

async function loadHome() {
  try {
    const { data } = await foodService.getHomeData()
    homeData.value = data
  } catch (error) {
    Message.error('首页数据加载失败')
  }
}

function goToDetail(id: string) {
  push({ name: 'dish-detail', params: { id } })
}

function goToAdd() {
  push('add-dish')
}

function goToProfile() {
  push('profile')
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

.eyebrow {
  color: #8b8b8b;
  font-size: var(--text-sm);
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 999px;
  background: #7a9e7e;
  color: #fff;
  font-weight: 800;
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
