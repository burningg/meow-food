<template>
  <view class="plan-picker-page">
    <view v-if="detail" class="page-shell picker-shell">
      <view class="picker-sticky-top">


        <view class="picker-top-nav">
          <view class="picker-top-nav-inner">
            <view class="picker-top-nav-spacer"></view>
            <text class="picker-top-nav-title">添加菜谱</text>
            <button class="picker-save-button" @tap="submitSelectedRecipes">保存</button>
          </view>
        </view>
      </view>

      <section class="picker-hero">
        <view class="picker-hero-top">
          <view class="picker-hero-copy">
            <text class="picker-hero-eyebrow">{{ detail.title }} · {{ planDateShort }}</text>
            <text class="picker-hero-title">从圈内共享菜谱里挑几道</text>
          </view>
        </view>

        <view class="picker-hero-stats">
          <view class="picker-stat-card">
            <text class="picker-stat-label">可选菜谱</text>
            <text class="picker-stat-value">{{ filteredMenus.length }}道</text>
          </view>
          <view class="picker-stat-card">
            <text class="picker-stat-label">计划内已有</text>
            <text class="picker-stat-value">{{ detail.recipes.length }}道</text>
          </view>
          <view class="picker-stat-card">
            <text class="picker-stat-label">当前已选</text>
            <text class="picker-stat-value">{{ selectedDishIds.length }}道</text>
          </view>
        </view>
      </section>

      <view class="picker-search">
        <text class="picker-search-icon">⌕</text>
        <input
          v-model.trim="searchKeyword"
          class="picker-search-input"
          maxlength="24"
          placeholder="搜索菜谱名或食材"
          placeholder-class="picker-search-placeholder"
        />
      </view>

      <section v-if="categoryItems.length" class="picker-category-section">

        <scroll-view class="picker-category-scroll" :scroll-x="true">
          <view class="picker-category-row">
            <button
              :class="['picker-category-pill', { active: !selectedCategoryId }]"
              @tap="selectCategory('')"
            >
              全部
            </button>
            <button
              v-for="category in categoryItems"
              :key="category.id"
              :class="['picker-category-pill', { active: selectedCategoryId === category.id }]"
              @tap="selectCategory(category.id)"
            >
              {{ category.name }}
            </button>
          </view>
        </scroll-view>
      </section>

      <section class="picker-list-section">

        <view v-if="filteredMenus.length" class="picker-list">
          <button
            v-for="menu in filteredMenus"
            :key="menu.id"
            class="picker-recipe-card"
            @tap="toggleSelectedDish(menu.id)"
          >
            <SmartImage :src="menu.image" class-name="picker-recipe-image" />

            <view class="picker-recipe-body">
              <view class="picker-recipe-top">
                <text class="picker-recipe-title">{{ menu.name }}</text>
                <text class="picker-recipe-meta">
                  {{ menu.categoryName || '共享菜谱' }}
                  <text v-if="menu.cookTimeMinutes"> · {{ menu.cookTimeMinutes }} 分钟</text>
                </text>
              </view>

              <view class="picker-recipe-owner-row">
                <view class="picker-recipe-owner-left">
                  <view class="picker-owner-chip">
                    <text class="picker-owner-chip-text">{{ menu.ownerNickname }}</text>
                  </view>
                </view>
                <view :class="['picker-recipe-radio', { active: selectedDishIds.includes(menu.id) }]">
                  <text v-if="selectedDishIds.includes(menu.id)" class="picker-recipe-radio-check">✓</text>
                </view>
              </view>
            </view>
          </button>
        </view>

        <view v-else class="empty-card picker-empty-card">
          <text class="picker-empty-title">没有更多可加入的菜谱了</text>
          <text class="picker-empty-desc">可以换个关键词试试，或者先回去把圈内菜谱分享出来。</text>
        </view>
      </section>
    </view>

    <view v-else class="page-shell picker-loading">正在加载可选菜谱...</view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import SmartImage from '@/components/SmartImage.vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { getRouteParams, replace } from '@/lib/navigation'
import { PlanService, type PlanDetail } from '@/services/plan-service'
import { SocialService, type BuddyCircleDetail } from '@/services/social-service'

const params = getRouteParams<{ id?: string }>()
const planService = new PlanService()
const socialService = new SocialService()

const detail = ref<PlanDetail | null>(null)
const circleDetail = ref<BuddyCircleDetail | null>(null)
const searchKeyword = ref('')
const selectedCategoryId = ref('')
const selectedDishIds = ref<string[]>([])

const availableMenus = computed(() => {
  const existingIds = new Set((detail.value?.recipes || []).map((recipe) => recipe.id))
  return (circleDetail.value?.sharedMenus || []).filter((menu) => !existingIds.has(menu.id))
})

const categoryItems = computed(() => {
  const categoryMap = new Map<string, { id: string; name: string }>()
  availableMenus.value.forEach((menu) => {
    if (!menu.categoryId || !menu.categoryName) return
    if (!categoryMap.has(menu.categoryId)) {
      categoryMap.set(menu.categoryId, { id: menu.categoryId, name: menu.categoryName })
    }
  })
  return Array.from(categoryMap.values())
})

const filteredMenus = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return availableMenus.value.filter((menu) => {
    if (selectedCategoryId.value && menu.categoryId !== selectedCategoryId.value) return false
    if (!keyword) return true
    return (
      menu.name.toLowerCase().includes(keyword) ||
      menu.ingredientNames.some((ingredient) => ingredient.toLowerCase().includes(keyword))
    )
  })
})

const planDateShort = computed(() => {
  if (!detail.value) return ''
  const [, month, day] = detail.value.planDate.split('-')
  return `${Number(month)}月${Number(day)}日`
})

onMounted(async () => {
  if (!(await requireAuth('plan-detail'))) return
  if (!params.id) {
    replace('plan')
    return
  }
  await loadData()
})

async function loadData() {
  try {
    const { data } = await planService.getPlanDetail(String(params.id))
    detail.value = data
    const circleResult = await socialService.getCircleDetail(data.circleId)
    circleDetail.value = circleResult.data
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '可选菜谱加载失败')
    replace('plan')
  }
}

function selectCategory(categoryId: string) {
  selectedCategoryId.value = categoryId
}

function toggleSelectedDish(dishId: string) {
  if (selectedDishIds.value.includes(dishId)) {
    selectedDishIds.value = selectedDishIds.value.filter((id) => id !== dishId)
    return
  }
  selectedDishIds.value = [...selectedDishIds.value, dishId]
}

async function submitSelectedRecipes() {
  if (!detail.value) return
  if (!selectedDishIds.value.length) {
    Message.info('先选中要加入的菜谱')
    return
  }
  try {
    await planService.addRecipes(detail.value.id, selectedDishIds.value)
    Message.success('菜谱已加入计划')
    replace({
      name: 'plan',
      query: {
        date: detail.value.planDate,
        planId: detail.value.id,
      },
    })
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '添加菜谱失败')
  }
}


</script>

<style>
.plan-picker-page {
  min-height: 100vh;
  background: #f7f6f3;
}

.picker-shell {
  padding: 0 20px 24px;
  background: #f7f6f3;
}

.picker-sticky-top {
  position: sticky;
  top: 0;
  z-index: 20;
  padding-bottom: 10px;
  background: #f7f6f3;
}

.picker-status-bar,
.picker-status-right,
.picker-top-nav-inner,
.picker-hero-top,
.picker-hero-stats,
.picker-search,
.picker-section-head,
.picker-category-row,
.picker-recipe-card,
.picker-recipe-owner-row,
.picker-recipe-owner-left {
  display: flex;
}

.picker-status-bar,
.picker-hero-top,
.picker-section-head,
.picker-recipe-owner-row {
  align-items: center;
  justify-content: space-between;
}

.picker-status-bar {
  height: 54px;
  padding: 14px 4px 0;
}

.picker-status-time {
  color: #151515;
  font-size: 15px;
  font-weight: 600;
}

.picker-status-right {
  gap: 6px;
}

.picker-status-dot {
  height: 10px;
  border-radius: 999px;
  background: #151515;
}

.picker-status-dot.short {
  width: 16px;
}

.picker-status-dot.long {
  width: 24px;
}

.picker-top-nav {
  margin-top: 10px;
}

.picker-top-nav-inner {
  position: relative;
  min-height: 68px;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-radius: 12px;
  background: #ffffff;
}

.picker-top-nav-spacer,
.picker-save-button {
  width: 56px;
  height: 36px;
  flex-shrink: 0;
}

.picker-top-nav-title {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  color: #151515;
  font-size: 16px;
  font-weight: 600;
}

.picker-save-button {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: auto;
  border-radius: 999px;
  background: #9f5c38;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
}

.picker-hero {
  margin-top: 6px;
  padding: 18px;
  border-radius: 16px;
  background: #1b3a2d;
}

.picker-hero-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.picker-hero-eyebrow {
  color: #d7e5d9;
  font-size: 12px;
  font-weight: 500;
}

.picker-hero-title {
  color: #ffffff;
  font-family: 'Noto Serif SC', serif;
  font-size: 22px;
  font-weight: 600;
}

.picker-hero-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: #335b47;
}

.picker-hero-chip-text {
  color: #e7efe8;
  font-size: 11px;
  font-weight: 600;
}

.picker-hero-stats {
  gap: 10px;
  margin-top: 12px;
}

.picker-stat-card {
  flex: 1;
  padding: 12px;
  border-radius: 12px;
  background: #335b47;
}

.picker-stat-label {
  display: block;
  color: #d7e5d9;
  font-size: 11px;
  font-weight: 500;
}

.picker-stat-value {
  display: block;
  margin-top: 4px;
  color: #ffffff;
  font-size: 16px;
  font-weight: 700;
}

.picker-search {
  align-items: center;
  gap: 8px;
  height: 46px;
  margin-top: 16px;
  padding: 0 14px;
  border-radius: 14px;
  background: #f3efe9;
}

.picker-search-icon {
  color: #8b837c;
  font-size: 16px;
}

.picker-search-input {
  flex: 1;
  min-height: 46px;
  color: #151515;
  font-size: 14px;
}

.picker-search-placeholder {
  color: #8b837c;
}

.picker-category-section,
.picker-list-section {
  margin-top: 18px;
}

.picker-section-head {
  align-items: flex-end;
}

.picker-section-title {
  color: #151515;
  font-size: 15px;
  font-weight: 700;
}

.picker-section-subtitle {
  color: #9f5c38;
  font-size: 12px;
  font-weight: 600;
}

.picker-category-scroll {
  width: 100%;
  margin-top: 10px;
}

.picker-category-row {
  gap: 8px;
  width: max-content;
}

.picker-category-pill {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: #ffffff;
  color: #5a4333;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  text-align: center;
}

.picker-category-pill.active {
  background: #edf3ec;
  color: #346538;
}

.picker-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.picker-recipe-card {
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 10px;
  border-radius: 18px;
  background: #ffffff;
  text-align: left;
}

.picker-recipe-image {
  width: 136px;
  height: 136px;
  flex-shrink: 0;
  border-radius: 18px;
}

.picker-recipe-body {
  display: flex;
  flex: 1;
  min-height: 136px;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.picker-recipe-top {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.picker-recipe-title {
  color: #151515;
  font-family: 'Noto Serif SC', serif;
  font-size: 19px;
  font-weight: 600;
  line-height: 1.35;
}

.picker-recipe-meta {
  color: #2f3437;
  font-size: 12px;
  font-weight: 500;
}

.picker-recipe-owner-left {
  flex: 1;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.picker-owner-chip {
  flex-shrink: 0;
  padding: 6px 10px;
  border-radius: 999px;
  background: #f4eee7;
}

.picker-owner-chip-text {
  color: #5a4333;
  font-size: 11px;
  font-weight: 600;
}

.picker-recipe-desc {
  flex: 1;
  color: #787774;
  font-size: 12px;
  line-height: 1.45;
}

.picker-recipe-radio {
  display: flex;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border: 1px solid #c9c1b8;
  border-radius: 999px;
  background: #ffffff;
}

.picker-recipe-radio.active {
  border-color: #1b3a2d;
  background: #1b3a2d;
}

.picker-recipe-radio-check {
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
}

.picker-empty-card {
  margin-top: 12px;
  padding: 18px;
}

.picker-empty-title {
  display: block;
  color: #151515;
  font-size: 15px;
  font-weight: 600;
}

.picker-empty-desc {
  display: block;
  margin-top: 6px;
  color: #787774;
  font-size: 12px;
  line-height: 1.55;
}

.picker-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  color: #787774;
}
</style>
