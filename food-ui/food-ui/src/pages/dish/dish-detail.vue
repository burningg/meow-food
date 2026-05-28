<template>
  <div v-if="dish" class="detail-page">
    <section class="hero-shell">
      <div class="hero-image">
        <img :src="dish.image" :alt="dish.name" />
      </div>
      <div class="hero-overlay">
        <button class="icon-button" type="button" aria-label="返回" @click="goBack">‹</button>
        <button class="icon-button text-button" type="button" @click="goEdit">编辑</button>
      </div>
    </section>

    <section class="detail-card">
      <div class="title-row">
        <h1>{{ dish.name }}</h1>
        <span class="category-badge">{{ dish.categoryName }}</span>
      </div>

      <p class="description-text">{{ dish.description }}</p>

      <div class="meta-row">
        <div class="meta-item">
          <span class="meta-icon">◷</span>
          <strong>{{ dish.cookTimeMinutes ?? '--' }} min</strong>
          <small>烹饪时间</small>
        </div>
        <div class="meta-item">
          <span class="meta-icon">✦</span>
          <strong>{{ difficultyLabel(dish.difficulty) }}</strong>
          <small>难度</small>
        </div>
        <div class="meta-item">
          <span class="meta-icon">◉</span>
          <strong>{{ dish.servings ?? '--' }} 人</strong>
          <small>份量</small>
        </div>
      </div>

      <div class="divider"></div>

      <section class="content-section">
        <div class="section-head">
          <h2>食材清单</h2>
          <span v-if="dish.ingredients.length" class="section-count">{{ dish.ingredients.length }} 项</span>
        </div>
        <div v-if="dish.ingredients.length">
          <div
            v-for="(ingredient, index) in dish.ingredients"
            :key="`ingredient-${index}`"
            class="ingredient-row"
          >
            <div class="ingredient-left">
              <span class="ingredient-dot"></span>
              <span class="ingredient-name">{{ ingredient.name }}</span>
            </div>
            <span class="ingredient-amount">{{ ingredient.amount }}</span>
          </div>
        </div>
        <p v-else class="empty-state">这道菜暂时还没有填写食材。</p>
      </section>

      <div class="divider"></div>

      <section class="content-section">
        <div class="section-head">
          <h2>烹饪步骤</h2>
          <span v-if="dish.steps.length" class="section-count">{{ dish.steps.length }} 步</span>
        </div>
        <div v-if="dish.steps.length" class="step-list">
          <div v-for="(step, index) in dish.steps" :key="`step-${index}`" class="step-row">
            <span class="step-number">{{ index + 1 }}</span>
            <p>{{ step.content }}</p>
          </div>
        </div>
        <p v-else class="empty-state">这道菜暂时还没有填写步骤。</p>
      </section>

      <div class="divider"></div>

      <section class="owner-actions">
        <button class="ghost-button" type="button" @click="goEdit">继续完善</button>
        <button class="ghost-button danger-button" type="button" @click="confirmDelete">删除菜谱</button>
      </section>
    </section>

    <footer class="bottom-bar">
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
  background: #f7f6f3;
  padding-bottom: 108px;
}

.hero-shell {
  position: relative;
}

.hero-image {
  height: 280px;
  overflow: hidden;
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
  align-items: flex-start;
  justify-content: space-between;
  padding: 50px 20px 0;
}

.icon-button {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.6);
  color: #151515;
  display: grid;
  place-items: center;
  font-size: 1.25rem;
  backdrop-filter: blur(12px);
}

.text-button {
  width: auto;
  min-width: 36px;
  padding: 0 12px;
  font-size: 0.9rem;
  font-weight: 600;
}

.detail-card {
  position: relative;
  z-index: 1;
  margin-top: -20px;
  border-radius: 20px 20px 0 0;
  background: #fff;
  padding: 24px 20px 20px;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.05);
}

.title-row,
.meta-row,
.ingredient-row,
.ingredient-left,
.step-row,
.owner-actions,
.bottom-bar,
.section-head {
  display: flex;
}

.title-row,
.ingredient-row,
.section-head {
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title-row h1,
.content-section h2 {
  margin: 0;
  color: #151515;
  font-family: 'Instrument Serif', 'Times New Roman', serif;
}

.title-row h1 {
  font-size: 1.7rem;
  line-height: 1.1;
  font-weight: 400;
}

.category-badge,
.section-count {
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 0.74rem;
  font-weight: 600;
}

.category-badge {
  background: #edf3ec;
  color: #346538;
}

.section-count {
  background: #f3eee8;
  color: #9f5c38;
}

.description-text {
  margin: 12px 0 0;
  color: #5f5d58;
  line-height: 1.7;
  font-size: 0.92rem;
}

.meta-row {
  justify-content: space-around;
  padding: 18px 0;
}

.meta-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.meta-icon {
  color: #9f5c38;
  font-size: 1.1rem;
  line-height: 1;
}

.meta-item strong,
.ingredient-name,
.step-row p {
  color: #151515;
}

.meta-item strong {
  font-family: 'Geist Mono', monospace;
  font-size: 0.92rem;
  font-weight: 600;
}

.meta-item small,
.ingredient-amount,
.empty-state {
  color: #787774;
}

.meta-item small,
.ingredient-amount {
  font-family: 'Geist Mono', monospace;
  font-size: 0.76rem;
}

.divider {
  height: 1px;
  background: #e9e2d8;
}

.content-section {
  padding: 18px 0;
}

.section-head {
  margin-bottom: 10px;
}

.content-section h2 {
  font-size: 1.14rem;
  font-weight: 400;
}

.ingredient-row {
  padding: 8px 0;
}

.ingredient-left {
  align-items: center;
  gap: 10px;
}

.ingredient-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #9f5c38;
}

.ingredient-name {
  font-size: 0.92rem;
}

.step-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.step-row {
  align-items: flex-start;
  gap: 12px;
}

.step-number {
  width: 24px;
  height: 24px;
  flex: 0 0 auto;
  border-radius: 999px;
  background: #9f5c38;
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 0.75rem;
  font-weight: 700;
}

.step-row p,
.empty-state {
  margin: 0;
  font-size: 0.9rem;
  line-height: 1.65;
}

.owner-actions {
  gap: 12px;
  padding-top: 18px;
}

.ghost-button,
.primary-button {
  border: none;
  border-radius: 10px;
  min-height: 48px;
}

.ghost-button {
  flex: 1;
  background: #f3eee8;
  color: #6d655f;
  font-weight: 600;
}

.danger-button {
  color: #b15b50;
}

.bottom-bar {
  position: fixed;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: min(390px, 100%);
  justify-content: center;
  padding: 12px 20px 20px;
  background: rgba(247, 246, 243, 0.96);
  backdrop-filter: blur(14px);
}

.primary-button {
  width: 100%;
  background: #9f5c38;
  color: #fff;
  font-size: 0.95rem;
  font-weight: 600;
}

.loading-state {
  min-height: 100vh;
  color: #5f5d58;
  display: grid;
  place-items: center;
}
</style>
