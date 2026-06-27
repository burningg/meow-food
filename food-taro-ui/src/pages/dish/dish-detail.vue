<template>
  <view v-if="dish" class="detail-page">
    <section class="hero-shell">
      <SmartImage :src="dish.image" variant="hero" class-name="hero-image" @tap="previewHeroImage" />
      <view class="hero-overlay">
        <button v-if="isOwner" class="icon-button text-button" @tap="openOwnerActions">管理</button>
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
          <text class="meta-strong">{{ dish.cookTimeMinutes == null ? '-' : `${dish.cookTimeMinutes} min` }}</text>
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
          <view
            v-for="(ingredient, index) in dish.ingredients"
            :key="`ingredient-${index}`"
            class="ingredient-row"
            :class="{ 'ingredient-row--linked': rawMaterialFor(ingredient.name) }"
            @tap="openRawMaterialInfo(ingredient.name)"
          >
            <view class="ingredient-left">
              <text class="ingredient-dot"></text>
              <text class="ingredient-name">{{ ingredient.name }}</text>
              <text v-if="rawMaterialFor(ingredient.name)" class="ingredient-file-tag">档案</text>
            </view>
            <view class="ingredient-right">
              <text class="ingredient-amount">{{ ingredient.amount }}</text>
              <text v-if="rawMaterialFor(ingredient.name)" class="ingredient-arrow">›</text>
            </view>
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
    </section>

    <footer class="bottom-bar actions-bar">
      <button class="secondary-button action-button" open-type="share">分享菜谱</button>
      <button class="primary-button action-button" @tap="startCooking">开始烹饪</button>
    </footer>

    <CookingStepSheet
      :visible="cookingVisible"
      :dish-id="String(params.id || '')"
      :dish-name="dish.name"
      :steps="dish.steps"
      @close="cookingVisible = false"
    />

    <view v-if="selectedRawMaterial" class="raw-material-overlay" @tap="closeRawMaterialInfo">
      <view class="raw-material-sheet" @tap.stop>
        <view class="sheet-handle"></view>

        <view class="raw-material-head">
          <view class="raw-material-title-stack">
            <text class="raw-material-category">分类：{{ selectedRawMaterial.category || '其他' }}</text>
            <text class="raw-material-name">{{ selectedRawMaterial.name }}</text>
          </view>
        </view>

        <view class="common-name-card">
          <text class="common-name-label">常见名 / 学名 / 缩写</text>
          <text class="common-name-value">{{ formatCommonNames(selectedRawMaterial.commonNames) }}</text>
        </view>

        <view class="cooking-capsule">
          <view class="heat-capsule">
            <text class="heat-symbol">火</text>
            <text class="heat-value">{{ displayText(selectedRawMaterial.defaultHeatTemperature, '未填') }}</text>
          </view>
          <view class="cooking-methods">
            <view
              v-for="item in cookingItems(selectedRawMaterial)"
              :key="item.key"
              class="cooking-token"
              :class="`cooking-token--${item.tone}`"
            >
              <text class="cooking-mark">{{ item.glyph }}</text>
              <text class="cooking-time">{{ item.value }}</text>
            </view>
          </view>
        </view>

        <view class="raw-material-signals">
          <view class="signal-island signal-island--warn">
            <text class="signal-mark">敏</text>
            <text class="signal-text">{{ displayText(selectedRawMaterial.allergenFlag, '暂无过敏原标识') }}</text>
          </view>
          <view class="signal-island signal-island--calorie">
            <text class="signal-mark">热</text>
            <text class="signal-text">{{ displayText(selectedRawMaterial.calorieEstimate, '暂无热量预估') }}</text>
          </view>
          <view class="signal-island signal-island--nutrition">
            <text class="signal-mark">营</text>
            <text class="signal-text">{{ displayText(selectedRawMaterial.nutritionInfo, '暂无营养信息') }}</text>
          </view>
          <view class="signal-island signal-island--substitute">
            <text class="signal-mark">替</text>
            <text class="signal-text">{{ displayText(selectedRawMaterial.substituteIngredients, '暂无可替代食材') }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>

  <view v-else class="page-shell loading-state">正在加载菜谱详情...</view>
</template>

<script setup lang="ts">
import Taro, { useShareAppMessage } from '@tarojs/taro'
import { computed, onMounted, ref } from 'vue'
import CookingStepSheet from '@/components/CookingStepSheet.vue'
import SmartImage from '@/components/SmartImage.vue'
import { requireAuth } from '@/lib/auth'
import { confirmDialog, Message } from '@/lib/feedback'
import { getRouteParams, push, replace, resolveSharePath } from '@/lib/navigation'
import { FoodService, type DishDetail, type IngredientItem, type RawMaterialInfo } from '@/services/food-service'
import { useAuthStore } from '@/stores/auth-store'

const params = getRouteParams() as { id?: string }
const foodService = new FoodService()
const authStore = useAuthStore()
const dish = ref<DishDetail | null>(null)
const rawMaterialMap = ref<Record<string, RawMaterialInfo>>({})
const selectedRawMaterial = ref<RawMaterialInfo | null>(null)
const cookingVisible = ref(false)
const isOwner = computed(() => Boolean(dish.value && authStore.user?.id === dish.value.ownerUserId))

useShareAppMessage(() => {
  const currentDish = dish.value
  const dishId = String(params.id || '')

  return {
    title: currentDish ? `分享一道菜给你：${currentDish.name}` : '分享一道 meoi 食堂里的菜谱给你',
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
    void loadRawMaterials(data.ingredients)
  } catch (error) {
    Message.error('详情加载失败')
    replace('home')
  }
}

async function loadRawMaterials(ingredients: IngredientItem[]) {
  rawMaterialMap.value = {}
  const names = normalizeIngredientNames(ingredients)
  if (!names.length) return

  try {
    const { data } = await foodService.matchRawMaterials(names)
    rawMaterialMap.value = data.reduce<Record<string, RawMaterialInfo>>((result, material) => {
      if (material.ingredientName) {
        result[material.ingredientName.trim()] = material
      }
      return result
    }, {})
  } catch (error) {
    rawMaterialMap.value = {}
  }
}

function goBack() {
  push('home')
}

function goEdit() {
  push({ name: 'edit-dish', params: { id: params.id } })
}

async function openOwnerActions() {
  try {
    // 作者管理动作收纳到原生操作菜单，避免挤占底部主操作区。
    const { tapIndex } = await Taro.showActionSheet({
      itemList: ['继续完善', '删除菜谱'],
      itemColor: '#2c211b',
    })

    if (tapIndex === 0) {
      goEdit()
      return
    }
    if (tapIndex === 1) {
      await confirmDelete()
    }
  } catch (error) {
    // 用户取消操作菜单时无需提示，保持浏览体验安静。
  }
}

function previewHeroImage() {
  const image = dish.value?.image
  if (!image) return

  // 使用平台原生图片预览能力，保留长按保存、缩放查看等系统交互。
  void Taro.previewImage({
    current: image,
    urls: [image],
  })
}

function difficultyLabel(value: string) {
  if (value === 'easy') return '简单'
  if (value === 'hard') return '困难'
  return '中等'
}

function normalizeIngredientNames(ingredients: IngredientItem[]) {
  const seen = new Set<string>()
  const names: string[] = []
  for (const ingredient of ingredients) {
    const name = normalizeIngredientName(ingredient.name)
    if (!name || seen.has(name)) continue
    seen.add(name)
    names.push(name)
  }
  return names
}

function normalizeIngredientName(name?: string) {
  return String(name || '').trim()
}

function rawMaterialFor(name?: string) {
  return rawMaterialMap.value[normalizeIngredientName(name)]
}

function openRawMaterialInfo(name?: string) {
  const material = rawMaterialFor(name)
  if (!material) return
  selectedRawMaterial.value = material
}

function closeRawMaterialInfo() {
  selectedRawMaterial.value = null
}

function displayText(value?: string, fallback = '-') {
  const text = String(value || '').trim()
  return text || fallback
}

function formatCommonNames(value?: string) {
  const names = String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
  return names.length ? names.join('、') : '暂无常见名'
}

function compactTime(value?: string) {
  const text = displayText(value)
  const minuteMatch = text.match(/^约?(\d+)\s*分钟$/)
  return minuteMatch ? `${minuteMatch[1]}min` : text
}

function cookingItems(material: RawMaterialInfo) {
  return [
    { key: 'steam', glyph: '蒸', value: compactTime(material.steamTime), tone: 'green' },
    { key: 'boil', glyph: '煮', value: compactTime(material.boilTime), tone: 'green' },
    { key: 'fry', glyph: '炸', value: compactTime(material.fryTime), tone: 'warm' },
    { key: 'bake', glyph: '烤', value: compactTime(material.bakeTime), tone: 'gold' },
    { key: 'stir-fry', glyph: '炒', value: compactTime(material.stirFryTime), tone: 'warm' },
  ]
}

function startCooking() {
  const hasCookingSteps = dish.value?.steps.some((step) => step.content.trim())
  if (!hasCookingSteps) {
    Message.info('这道菜还没有步骤')
    return
  }
  cookingVisible.value = true
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 45px;
  padding: 11px 0;
}

.ingredient-row--linked {
  margin: 8px 0;
  border: 1px solid #efe3d1;
  border-radius: 14px;
  background: #fbf8f4;
  min-height: 52px;
  padding: 8px 12px;
}

.ingredient-left {
  display: flex;
  min-width: 0;
  flex: 1;
  align-items: center;
  gap: 9px;
}

.ingredient-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: var(--accent);
}

.ingredient-row--linked .ingredient-dot {
  background: var(--text-main);
}

.ingredient-file-tag {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 20px;
  flex: 0 0 auto;
  border-radius: 999px;
  background: var(--accent-soft);
  padding: 0 7px;
  color: var(--text-main);
  font-size: 10px;
  font-weight: 800;
  line-height: 20px;
}

.ingredient-right {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
}

.ingredient-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 999px;
  background: #f2e9e2;
  color: var(--accent-dark);
  font-size: 18px;
  font-weight: 800;
  line-height: 1;
}

.ingredient-name,
.ingredient-amount,
.step-text {
  color: var(--text-main);
  font-size: var(--text-md);
  line-height: 1.35;
}

.ingredient-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ingredient-amount {
  min-width: 34px;
  text-align: right;
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

.actions-bar {
  gap: 12px;
}

.action-button {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
}

.raw-material-overlay {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(21, 21, 21, 0.45);
}

.raw-material-sheet {
  width: min(390px, 100vw);
  border: 1px solid #efe3d1;
  border-radius: 24px 24px 0 0;
  background: #fff;
  padding: 12px 20px calc(24px + env(safe-area-inset-bottom));
  box-shadow: 0 -10px 36px rgba(138, 78, 41, 0.1);
}

.sheet-handle {
  width: 42px;
  height: 4px;
  margin: 0 auto 12px;
  border-radius: 999px;
  background: #e6dbcf;
}

.raw-material-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.raw-material-title-stack {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.raw-material-category {
  align-self: flex-start;
  border-radius: 999px;
  background: var(--accent-soft);
  padding: 5px 10px;
  color: var(--text-main);
  font-size: var(--text-xs);
  font-weight: 800;
}

.raw-material-name {
  color: #151515;
  font-size: 26px;
  font-weight: 800;
  line-height: 1.16;
}

.common-name-card,
.cooking-capsule,
.signal-island {
  border: 1px solid #f0e7dc;
  border-radius: 18px;
}

.common-name-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  background: #fbf8f4;
  padding: 14px;
}

.common-name-label {
  color: var(--text-muted);
  font-size: var(--text-xs);
  font-weight: 800;
}

.common-name-value {
  color: var(--text-main);
  font-size: var(--text-sm);
  line-height: 1.45;
}

.cooking-capsule {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  background: #fbf7f1;
  padding: 8px 10px;
}

.heat-capsule {
  display: flex;
  width: 54px;
  height: 50px;
  flex: 0 0 54px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  border-radius: 14px;
  background: #f2e9e2;
}

.heat-symbol,
.heat-value {
  color: var(--accent-dark);
  font-weight: 900;
}

.heat-symbol {
  font-size: 13px;
}

.heat-value {
  font-size: 10px;
}

.cooking-methods {
  display: flex;
  flex: 1;
  gap: 5px;
}

.cooking-token {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
}

.cooking-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 900;
}

.cooking-time {
  color: var(--text-main);
  font-size: 9px;
  font-weight: 800;
  line-height: 1;
}

.cooking-token--green .cooking-mark {
  background: var(--accent-soft);
  color: var(--text-main);
}

.cooking-token--warm .cooking-mark {
  background: #f2e9e2;
  color: var(--accent-dark);
}

.cooking-token--gold .cooking-mark {
  background: #fff4db;
  color: #8e5c19;
}

.raw-material-signals {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.signal-island {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
}

.signal-island--warn {
  background: #fff8ef;
  border-color: #ead9c4;
}

.signal-island--nutrition {
  background: var(--accent-soft);
  border-color: #dce7da;
}

.signal-island--calorie {
  background: #fff4db;
  border-color: #ead8a8;
}

.signal-island--substitute {
  background: #f7f4ef;
}

.signal-mark {
  display: flex;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  color: #fff;
  font-size: 14px;
  font-weight: 900;
}

.signal-island--warn .signal-mark {
  background: var(--accent-dark);
}

.signal-island--nutrition .signal-mark {
  background: var(--text-main);
}

.signal-island--calorie .signal-mark {
  background: #8e5c19;
}

.signal-island--substitute .signal-mark {
  background: #6f5c4a;
}

.signal-text {
  flex: 1;
  color: var(--text-main);
  font-size: var(--text-sm);
  line-height: 1.45;
}
</style>
