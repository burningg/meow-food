<template>
  <div v-if="dish" class="detail-page">
    <section class="hero-image">
      <img :src="dish.image" :alt="dish.name" />
      <div class="hero-overlay">
        <button class="circle-btn" type="button" @click="goBack">‹</button>
        <button class="circle-btn" type="button" @click="goEdit">编辑</button>
      </div>
    </section>

    <section class="detail-card">
      <div class="title-row">
        <div>
          <h1>{{ dish.name }}</h1>
          <p>{{ dish.description }}</p>
        </div>
        <div class="chip-stack">
          <span class="category-chip">{{ dish.categoryName }}</span>
          <span class="visibility-chip">{{ visibilityLabel(dish.effectiveVisibility) }}</span>
        </div>
      </div>

      <div class="info-row">
        <div class="info-item">
          <strong>{{ dish.cookTimeMinutes ?? '--' }} min</strong>
          <span>烹饪时间</span>
        </div>
        <div class="info-item">
          <strong>{{ difficultyLabel(dish.difficulty) }}</strong>
          <span>难度</span>
        </div>
        <div class="info-item">
          <strong>{{ dish.servings ?? '--' }} 人</strong>
          <span>份量</span>
        </div>
      </div>

      <div class="divider"></div>

      <section class="content-section">
        <h2>食材清单</h2>
        <div v-for="(ingredient, index) in dish.ingredients" :key="`ingredient-${index}`" class="ingredient-row">
          <div class="ingredient-left">
            <span class="dot"></span>
            <span>{{ ingredient.name }}</span>
          </div>
          <span class="muted">{{ ingredient.amount }}</span>
        </div>
      </section>

      <div class="divider"></div>

      <section class="content-section">
        <h2>烹饪步骤</h2>
        <div v-for="(step, index) in dish.steps" :key="`step-${index}`" class="step-detail-row">
          <span class="step-number">{{ index + 1 }}</span>
          <p>{{ step.content }}</p>
        </div>
      </section>
    </section>

    <footer class="bottom-bar">
      <button class="secondary-button" type="button" @click="confirmDelete">删除菜谱</button>
      <button class="primary-button" type="button" @click="startCooking">开始烹饪</button>
    </footer>
  </div>

  <div v-else class="page-shell loading-state">正在加载菜谱详情...</div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { showConfirmDialog } from 'vant'
import { FoodService, type DishDetail } from '@/services/food-service'

const route = useRoute()
const router = useRouter()
const foodService = new FoodService()
const dish = ref<DishDetail | null>(null)

onMounted(async () => {
  await loadDish()
})

async function loadDish() {
  try {
    const { data } = await foodService.getDishDetail(String(route.params.id))
    dish.value = data
  } catch (error) {
    Message.error('详情加载失败')
    router.replace({ name: 'home' })
  }
}

function goBack() {
  router.push({ name: 'home' })
}

function goEdit() {
  router.push({ name: 'edit-dish', params: { id: route.params.id } })
}

function difficultyLabel(value: string) {
  if (value === 'easy') return '简单'
  if (value === 'hard') return '困难'
  return '中等'
}

function visibilityLabel(value?: string) {
  if (value === 'friends') return '好友可见'
  if (value === 'private') return '仅自己可见'
  return '公开'
}

function startCooking() {
  Message.success('开火吧，祝你做菜顺利')
}

async function confirmDelete() {
  try {
    await showConfirmDialog({
      title: '删除菜谱',
      message: '删除后将同时移除食材和步骤，确认继续吗？',
    })
    await foodService.deleteDish(String(route.params.id))
    Message.success('菜谱已删除')
    router.replace({ name: 'home' })
  } catch (error) {
    if (error) {
      Message.info('已取消删除')
    }
  }
}
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #f5f2ed;
  padding-bottom: 104px;
}

.hero-image {
  position: relative;
  height: 280px;
}

.hero-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 52px 20px 0;
}

.circle-btn {
  min-width: 36px;
  height: 36px;
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: #1b3a2d;
  padding: 0 14px;
  font-weight: 700;
  backdrop-filter: blur(12px);
}

.detail-card {
  margin: -20px 16px 0;
  background: #fff;
  border-radius: 24px 24px 0 0;
  padding: 24px 20px 24px;
  box-shadow: 0 -4px 18px rgba(0, 0, 0, 0.06);
}

.title-row,
.info-row,
.ingredient-row,
.ingredient-left,
.step-detail-row,
.chip-stack,
.bottom-bar {
  display: flex;
}

.title-row,
.ingredient-row {
  justify-content: space-between;
  gap: 16px;
}

.title-row h1,
.content-section h2 {
  margin: 0;
  color: #1b3a2d;
}

.title-row h1 {
  font-family: 'Playfair Display', serif;
  font-size: 1.7rem;
  line-height: 1.1;
}

.title-row p {
  margin-top: 10px;
  color: #5b645e;
  line-height: 1.65;
}

.category-chip {
  align-self: flex-start;
  border-radius: 999px;
  background: #e8f0e9;
  color: #7a9e7e;
  padding: 5px 10px;
  font-size: 0.76rem;
  font-weight: 700;
}

.chip-stack {
  flex-direction: column;
  gap: 8px;
}

.visibility-chip {
  align-self: flex-start;
  border-radius: 999px;
  background: #f4ece6;
  color: #c4704b;
  padding: 5px 10px;
  font-size: 0.74rem;
  font-weight: 700;
}

.info-row {
  justify-content: space-around;
  padding: 18px 0;
}

.info-item {
  text-align: center;
}

.info-item strong {
  display: block;
  color: #1b3a2d;
  font-size: 1rem;
}

.info-item span,
.muted {
  color: #8b8b8b;
  font-size: 0.82rem;
}

.divider {
  height: 1px;
  background: #e8e5e0;
}

.content-section {
  padding: 18px 0;
}

.content-section h2 {
  font-family: 'Playfair Display', serif;
  font-size: 1.15rem;
  margin-bottom: 12px;
}

.ingredient-row {
  align-items: center;
  padding: 8px 0;
}

.ingredient-left {
  align-items: center;
  gap: 10px;
  color: #3d3d3d;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #7a9e7e;
}

.step-detail-row {
  gap: 12px;
  align-items: flex-start;
  padding: 8px 0;
}

.step-number {
  width: 24px;
  height: 24px;
  flex: 0 0 auto;
  border-radius: 999px;
  background: #c4704b;
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 0.76rem;
  font-weight: 700;
}

.step-detail-row p {
  color: #3d3d3d;
  line-height: 1.6;
  margin: 0;
}

.bottom-bar {
  position: fixed;
  left: 50%;
  bottom: 0;
  width: min(390px, 100%);
  transform: translateX(-50%);
  gap: 10px;
  padding: 12px 20px 20px;
  background: rgba(245, 242, 237, 0.96);
  backdrop-filter: blur(14px);
}

.secondary-button,
.primary-button {
  border: none;
  border-radius: 14px;
  min-height: 50px;
}

.secondary-button {
  min-width: 110px;
  background: #fff;
  color: #7d6d63;
}

.primary-button {
  flex: 1;
}

.loading-state {
  color: #5b645e;
  display: grid;
  place-items: center;
}
</style>
