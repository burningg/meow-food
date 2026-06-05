<template>
  <view v-if="dish" class="detail-page">
    <section class="hero-shell">
      <SmartImage :src="dish.image" variant="hero" class-name="hero-image" />
      <view class="hero-overlay">
        <button v-if="isOwner" class="icon-button text-button" @tap="goEdit">编辑</button>
      </view>
    </section>

    <section class="detail-card">
      <view class="title-row">
        <text class="dish-title">{{ dish.name }}</text>
        <text class="category-badge">{{ dish.categoryName }}</text>
      </view>

      <text class="description-text">{{ dish.description }}</text>

      <view class="meta-row">
        <view class="meta-item">
          <text class="meta-icon">◷</text>
          <text class="meta-strong">{{ dish.cookTimeMinutes ?? '--' }} min</text>
          <text class="meta-small">烹饪时间</text>
        </view>
        <view class="meta-item">
          <text class="meta-icon">✦</text>
          <text class="meta-strong">{{ difficultyLabel(dish.difficulty) }}</text>
          <text class="meta-small">难度</text>
        </view>
        <view class="meta-item">
          <text class="meta-icon">◉</text>
          <text class="meta-strong">{{ dish.servings ?? '--' }} 人</text>
          <text class="meta-small">份量</text>
        </view>
      </view>

      <view class="divider"></view>

      <section class="content-section">
        <view class="section-head">
          <text class="section-title">食材清单</text>
          <text v-if="dish.ingredients.length" class="section-count">{{ dish.ingredients.length }} 项</text>
        </view>
        <view v-if="dish.ingredients.length">
          <view v-for="(ingredient, index) in dish.ingredients" :key="`ingredient-${index}`" class="ingredient-row">
            <view class="ingredient-left">
              <text class="ingredient-dot"></text>
              <text class="ingredient-name">{{ ingredient.name }}</text>
            </view>
            <text class="ingredient-amount">{{ ingredient.amount }}</text>
          </view>
        </view>
        <text v-else class="empty-line">这道菜暂时还没有填写食材。</text>
      </section>

      <view class="divider"></view>

      <section class="content-section">
        <view class="section-head">
          <text class="section-title">烹饪步骤</text>
          <text v-if="dish.steps.length" class="section-count">{{ dish.steps.length }} 步</text>
        </view>
        <view v-if="dish.steps.length" class="step-list">
          <view v-for="(step, index) in dish.steps" :key="`step-${index}`" class="step-row">
            <text class="step-number">{{ index + 1 }}</text>
            <text class="step-text">{{ step.content }}</text>
          </view>
        </view>
        <text v-else class="empty-line">这道菜暂时还没有填写步骤。</text>
      </section>

      <view class="divider"></view>

      <section v-if="isOwner" class="owner-actions">
        <button class="ghost-button" @tap="goEdit">继续完善</button>
        <button class="ghost-button danger-button" @tap="confirmDelete">删除菜谱</button>
      </section>
    </section>

    <footer class="bottom-bar actions-bar">
      <button class="secondary-button action-button" open-type="share">分享菜谱</button>
      <button class="primary-button action-button" @tap="startCooking">开始烹饪</button>
    </footer>
  </view>

  <view v-else class="page-shell loading-state">正在加载菜谱详情...</view>
</template>

<script setup lang="ts">
import { useShareAppMessage } from '@tarojs/taro'
import { computed, onMounted, ref } from 'vue'
import SmartImage from '@/components/SmartImage.vue'
import { requireAuth } from '@/lib/auth'
import { confirmDialog, Message } from '@/lib/feedback'
import { getRouteParams, push, replace, resolveSharePath } from '@/lib/navigation'
import { FoodService, type DishDetail } from '@/services/food-service'
import { useAuthStore } from '@/stores/auth-store'

const params = getRouteParams() as { id?: string }
const foodService = new FoodService()
const authStore = useAuthStore()
const dish = ref<DishDetail | null>(null)
const isOwner = computed(() => Boolean(dish.value && authStore.user?.id === dish.value.ownerUserId))

useShareAppMessage(() => {
  const currentDish = dish.value
  const dishId = String(params.id || '')

  return {
    title: currentDish ? `分享一道菜给你：${currentDish.name}` : '分享一道 meow 食堂里的菜谱给你',
    path: resolveSharePath({ name: 'dish-detail', params: { id: dishId } }),
  }
})

onMounted(async () => {
  if (!(await requireAuth('dish-detail'))) return
  await loadDish()
})

async function loadDish() {
  try {
    const { data } = await foodService.getDishDetail(String(params.id || ''))
    dish.value = data
  } catch (error) {
    Message.error('详情加载失败')
    replace('home')
  }
}

function goBack() {
  push('home')
}

function goEdit() {
  push({ name: 'edit-dish', params: { id: params.id } })
}

function difficultyLabel(value: string) {
  if (value === 'easy') return '简单'
  if (value === 'hard') return '困难'
  return '中等'
}

function startCooking() {
  Message.success('开火吧，祝你做菜顺利')
}

async function confirmDelete() {
  try {
    await confirmDialog({
      title: '删除菜谱',
      message: '删除后将同时移除食材和步骤，确认继续吗？',
    })
    await foodService.deleteDish(String(params.id || ''))
    Message.success('菜谱已删除')
    replace('home')
  } catch (error: any) {
    if (error?.message === 'cancelled') {
      Message.info('已取消删除')
    }
  }
}
</script>

<style>
.detail-page {
  min-height: 100vh;
  padding-bottom: 108px;
  background: #f7f6f3;
}

.hero-shell {
  position: relative;
}

.hero-image {
  width: 100%;
  height: 280px;
}

.hero-overlay {
  position: absolute;
  top: 18px;
  left: 16px;
  right: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.text-button {
  width: auto;
  min-width: 54px;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 800;
}

.detail-card {
  position: relative;
  z-index: 1;
  margin: -28px 16px 0;
  border-radius: 24px 24px 18px 18px;
  background: #fff;
  padding: 20px;
  box-shadow: var(--shadow);
}

.title-row,
.meta-row,
.section-head,
.ingredient-row,
.ingredient-left,
.owner-actions,
.actions-bar {
  display: flex;
}

.title-row,
.section-head,
.ingredient-row {
  align-items: center;
  justify-content: space-between;
}

.dish-title {
  flex: 1;
  color: var(--text-main);
  font-size: 28px;
  font-weight: 800;
  line-height: 1.16;
}

.category-badge {
  border-radius: 999px;
  background: #f4ece6;
  color: var(--accent);
  padding: 5px 10px;
  font-size: var(--text-xs);
  font-weight: 800;
}

.description-text {
  display: block;
  margin-top: 12px;
  color: var(--text-muted);
  font-size: var(--text-sm);
  line-height: 1.7;
}

.meta-row {
  gap: 10px;
  margin-top: 18px;
}

.meta-item {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  border-radius: 16px;
  background: #f7f4ef;
  padding: 12px 8px;
}

.meta-icon {
  color: var(--accent);
}

.meta-strong {
  color: var(--text-main);
  font-size: 14px;
  font-weight: 800;
}

.meta-small,
.section-count,
.empty-line {
  color: var(--text-muted);
  font-size: var(--text-xs);
}

.divider {
  height: 1px;
  margin: 20px 0;
  background: var(--line);
}

.section-title {
  color: var(--text-main);
  font-size: var(--title-md);
  font-weight: 800;
}

.ingredient-row {
  padding: 11px 0;
}

.ingredient-left {
  align-items: center;
  gap: 9px;
}

.ingredient-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: var(--accent);
}

.ingredient-name,
.ingredient-amount,
.step-text {
  color: var(--text-main);
  font-size: var(--text-md);
}

.step-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 0;
}

.step-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  flex: 0 0 auto;
  border-radius: 999px;
  background: var(--accent);
  color: #fff;
  font-size: var(--text-xs);
  font-weight: 800;
}

.owner-actions {
  gap: 10px;
}

.ghost-button {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  border-radius: 14px;
  background: #f5f2ed;
  color: var(--text-main);
  font-weight: 800;
}

.danger-button {
  color: #b2483d;
}

.actions-bar {
  gap: 12px;
}

.action-button {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
}
</style>
