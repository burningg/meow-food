<template>
  <view class="page-shell pet-detail-page">
    <view v-if="loading" class="status-card">
      <text>宠物状态加载中...</text>
    </view>

    <template v-else-if="pet?.claimed">
      <section class="pet-detail-hero">
        <view class="detail-glow detail-glow-green"></view>
        <view class="detail-glow detail-glow-gold"></view>
        <view class="pet-soft-shadow"></view>
        <image class="detail-pet-image" :src="petDefinition.assets.lounge" :svg="true" mode="aspectFit" />
        <view class="pet-name-panel">
          <text class="detail-pet-name">{{ pet.name }}</text>
          <view class="breed-pill">
            <text class="breed-text">{{ pet.petTypeName || petDefinition.label }}</text>
          </view>
        </view>
        <view class="pet-whisper-bubble">
          <text class="pet-whisper-star">✦</text>
          <text class="pet-whisper-text">{{ whisperText }}</text>
        </view>
      </section>

      <section class="pet-stats-row">
        <view class="pet-stat-card">
          <text class="pet-stat-value">{{ companionLabel }}</text>
          <text class="pet-stat-label">陪伴</text>
        </view>
        <view class="pet-stat-card">
          <text class="pet-stat-value mood">{{ pet.moodLabel || '开心' }}</text>
          <text class="pet-stat-label">心情</text>
        </view>
        <view class="pet-stat-card">
          <text class="pet-stat-value fullness">{{ fullnessLabel }}</text>
          <text class="pet-stat-label">饱腹</text>
        </view>
      </section>

      <section class="today-story-card">
        <view class="today-story-head">
          <text class="today-story-title">今日小事</text>
          <text class="today-story-time">刚刚</text>
        </view>
        <text class="today-story-body">{{ pet.todayStory }}</text>
      </section>

      <button class="go-recipe-button" @tap="openPlanner">
        <text class="go-recipe-text">让{{ pet.name }}去帮你计划今天的菜谱</text>
      </button>

      <view v-if="plannerVisible" class="planner-overlay" @tap="closePlanner">
        <section class="planner-sheet" @tap.stop>
          <view class="planner-handle"></view>

          <template v-if="arranging && !arrangement">
            <view class="planner-head">
              <text class="planner-title">{{ pet.name }}正在翻菜单</text>
              <text class="planner-desc">正在查看最近 30 次计划、圈内可见菜谱和你的口味记录。</text>
            </view>
            <section class="planner-progress-card">
              <text class="planner-progress-icon">✦</text>
              <view class="planner-progress-row step-1">
                <view class="planner-progress-dot"></view>
                <text class="planner-progress-text">读圈子菜谱</text>
              </view>
              <view class="planner-progress-row step-2">
                <view class="planner-progress-dot"></view>
                <text class="planner-progress-text">看历史偏好</text>
              </view>
              <view class="planner-progress-row step-3">
                <view class="planner-progress-dot"></view>
                <text class="planner-progress-text">写健康建议</text>
              </view>
            </section>
            <button class="planner-primary-button planner-loading-button" disabled>
              <text class="planner-primary-text">排菜中...</text>
            </button>
          </template>

          <template v-else-if="arrangement">
            <view class="planner-head">
              <text class="planner-title">{{ arrangement.title }}</text>
              <text class="planner-desc">{{ arrangement.petText }}</text>
            </view>
            <section class="planner-note-card">
              <text class="planner-note-title">健康小建议</text>
              <text class="planner-note-body">{{ arrangement.healthText || arrangement.suggestionText }}</text>
            </section>
            <view class="planner-recipes">
              <view v-for="recipe in arrangement.recipes" :key="recipe.dish.id" class="planner-recipe-card">
                <SmartImage :src="recipe.dish.image" class-name="planner-recipe-image" />
                <view class="planner-recipe-copy">
                  <text class="planner-recipe-title">{{ recipe.dish.name }}</text>
                  <text class="planner-recipe-reason">{{ recipe.reason }}</text>
                </view>
              </view>
            </view>
            <text class="planner-quota-text">{{ previewQuotaText }}</text>
            <view class="planner-actions">
              <button class="planner-secondary-button" :disabled="confirming || arranging" @tap="generateArrangement">
                <text class="planner-secondary-text">重新生成</text>
              </button>
              <button class="planner-primary-button" :disabled="confirming" @tap="confirmArrangement">
                <text class="planner-primary-text">{{ confirming ? '创建中...' : '确定创建计划' }}</text>
              </button>
            </view>
          </template>

          <template v-else>
            <view class="planner-head">
              <text class="planner-title">宠物帮你安排一餐</text>
              <text class="planner-desc">选择圈子和饭点，{{ pet.name }}会从最近计划和圈内菜谱里挑一桌。</text>
            </view>

            <view class="planner-fields">
              <picker mode="selector" :range="circleOptions" :value="selectedCircleIndex" :disabled="plannerOptionsLoading || arranging" @change="handleCircleChange">
                <view class="planner-field-card">
                  <text class="planner-field-label">圈子</text>
                  <text class="planner-field-value">{{ selectedCircleName || (circles.length ? '选择圈子' : '先创建圈子') }}</text>
                </view>
              </picker>

              <view class="planner-meal-tabs">
                <button :class="['planner-meal-tab', { active: selectedMealType === 'lunch' }]" @tap="setMealType('lunch')">
                  <text class="planner-meal-text">午餐</text>
                </button>
                <button :class="['planner-meal-tab', { active: selectedMealType === 'dinner' }]" @tap="setMealType('dinner')">
                  <text class="planner-meal-text">晚餐</text>
                </button>
              </view>

              <view class="planner-row">
                <picker mode="date" :value="selectedPlanDate" :disabled="arranging" @change="handlePlanDateChange">
                  <view class="planner-field-card compact">
                    <text class="planner-field-label">日期</text>
                    <text class="planner-field-value">{{ planDateLabel }}</text>
                  </view>
                </picker>
                <view class="planner-field-card compact">
                  <text class="planner-field-label">菜数</text>
                  <view class="planner-stepper">
                    <button class="planner-stepper-button" :disabled="dishCount <= 1" @tap="changeDishCount(-1)">-</button>
                    <text class="planner-stepper-value">{{ dishCount }} 道</text>
                    <button class="planner-stepper-button" :disabled="dishCount >= 8" @tap="changeDishCount(1)">+</button>
                  </view>
                </view>
              </view>

              <view class="planner-field-card advice">
                <text class="planner-field-label">健康建议</text>
                <textarea
                  v-model="healthAdvice"
                  class="planner-advice-input"
                  maxlength="80"
                  placeholder="少油、清爽、荤素搭配..."
                  placeholder-class="planner-advice-placeholder"
                />
              </view>
            </view>

            <text class="planner-quota-text">{{ formQuotaText }}</text>
            <view class="planner-actions">
              <button class="planner-secondary-button" :disabled="arranging" @tap="closePlanner">
                <text class="planner-secondary-text">取消</text>
              </button>
              <button class="planner-primary-button" :disabled="arranging || plannerOptionsLoading || !circles.length" @tap="generateArrangement">
                <text class="planner-primary-text">{{ plannerOptionsLoading ? '加载中...' : '开始规划' }}</text>
              </button>
            </view>
          </template>
        </section>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useDidShow } from '@tarojs/taro'
import SmartImage from '@/components/SmartImage.vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { getPetDefinition } from '@/lib/pet'
import { openPrimaryRoute, replace } from '@/lib/navigation'
import { PetService, type PetResponse } from '@/services/pet-service'
import { PlanService, type PlanAiArrangeResponse, type PlanAiMealType } from '@/services/plan-service'
import { SocialService, type BuddyCircleSummary } from '@/services/social-service'

const petService = new PetService()
const planService = new PlanService()
const socialService = new SocialService()
const DEFAULT_HEALTH_ADVICE = '荤素搭配'
const pet = ref<PetResponse | null>(null)
const loading = ref(true)
const plannerVisible = ref(false)
const plannerOptionsLoading = ref(false)
const plannerOptionsLoaded = ref(false)
const circles = ref<BuddyCircleSummary[]>([])
const selectedCircleId = ref('')
const selectedMealType = ref<PlanAiMealType>('lunch')
const selectedPlanDate = ref(formatDateKey(new Date()))
const dishCount = ref(3)
const healthAdvice = ref(DEFAULT_HEALTH_ADVICE)
const arrangement = ref<PlanAiArrangeResponse | null>(null)
const arranging = ref(false)
const confirming = ref(false)
const remainingPlanAiCount = ref<number | null>(null)

const petDefinition = computed(() => getPetDefinition(pet.value?.petType))
const companionLabel = computed(() => `${pet.value?.companionDays ?? 0} 天`)
const fullnessLabel = computed(() => `${pet.value?.fullnessPercent ?? 0}%`)
const whisperText = computed(() => {
  if (pet.value?.moodCode === 'bored') return '有点想你，带我看看新菜单吧。'
  return '今天想趴在菜谱边，等你开饭。'
})
const circleOptions = computed(() => circles.value.map((circle) => circle.name))
const selectedCircleIndex = computed(() => {
  const index = circles.value.findIndex((circle) => circle.id === selectedCircleId.value)
  return index < 0 ? 0 : index
})
const selectedCircleName = computed(() => circles.value[selectedCircleIndex.value]?.name || '')
const planDateLabel = computed(() => {
  if (selectedPlanDate.value === formatDateKey(new Date())) return '今天'
  const date = parseDateKey(selectedPlanDate.value)
  if (!date) return selectedPlanDate.value
  return `${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
})
const formQuotaText = computed(() => {
  if (remainingPlanAiCount.value === null) return '本月 AI 排菜剩余 -- 次'
  return `本月 AI 排菜剩余 ${remainingPlanAiCount.value} 次`
})
const previewQuotaText = computed(() => {
  const remaining = arrangement.value?.usage?.remainingThisMonth
  if (typeof remaining !== 'number') return formQuotaText.value
  return `生成后已使用 1 次，本月剩余 ${remaining} 次`
})

onMounted(async () => {
  if (!(await requireAuth('pet-detail'))) return
  await loadPet()
})

useDidShow(async () => {
  if (!(await requireAuth('pet-detail'))) return
  await loadPet()
})

async function loadPet() {
  loading.value = true
  try {
    const { data } = await petService.getMyPet()
    pet.value = data
    if (!data.claimed) {
      await replace('pet-adoption')
    }
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '宠物状态加载失败')
  } finally {
    loading.value = false
  }
}

async function openPlanner() {
  plannerVisible.value = true
  selectedPlanDate.value = formatDateKey(new Date())
  healthAdvice.value = DEFAULT_HEALTH_ADVICE
  arrangement.value = null
  await loadPlannerOptions()
}

function closePlanner() {
  if (arranging.value || confirming.value) return
  plannerVisible.value = false
  arrangement.value = null
}

async function loadPlannerOptions() {
  if (plannerOptionsLoaded.value || plannerOptionsLoading.value) return
  plannerOptionsLoading.value = true
  try {
    const [{ data: circlesData }, { data: profileData }] = await Promise.all([
      socialService.getCircles(),
      socialService.getProfile(),
    ])
    circles.value = circlesData
    selectedCircleId.value =
      circlesData.find((circle) => circle.id === profileData.lastSelectedCircleId)?.id ||
      circlesData[0]?.id ||
      ''
    remainingPlanAiCount.value = profileData.vipInfo.monthlyPlanAiRemaining ?? null
    plannerOptionsLoaded.value = true
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '排菜信息加载失败')
  } finally {
    plannerOptionsLoading.value = false
  }
}

function handleCircleChange(event: { detail: { value: string } }) {
  const index = Number(event.detail.value || 0)
  selectedCircleId.value = circles.value[index]?.id || ''
}

function handlePlanDateChange(event: { detail: { value: string } }) {
  selectedPlanDate.value = event.detail.value
}

function setMealType(mealType: PlanAiMealType) {
  selectedMealType.value = mealType
}

function changeDishCount(offset: number) {
  dishCount.value = Math.max(1, Math.min(8, dishCount.value + offset))
}

async function generateArrangement() {
  if (!selectedCircleId.value) {
    Message.info('先选择圈子')
    return
  }
  arranging.value = true
  arrangement.value = null
  try {
    const { data } = await planService.arrangePlanByAi({
      circleId: selectedCircleId.value,
      mealType: selectedMealType.value,
      planDate: selectedPlanDate.value,
      dishCount: dishCount.value,
      healthAdvice: healthAdvice.value.trim() || undefined,
    })
    arrangement.value = data
    remainingPlanAiCount.value = data.usage?.remainingThisMonth ?? remainingPlanAiCount.value
  } catch (error: any) {
    Message.error(error?.response?.data?.message || 'AI 排菜失败')
  } finally {
    arranging.value = false
  }
}

async function confirmArrangement() {
  if (!arrangement.value || !arrangement.value.recipes.length) {
    Message.info('先生成一份排菜建议')
    return
  }
  confirming.value = true
  try {
    const { data } = await planService.confirmAiArrangement({
      circleId: selectedCircleId.value,
      planDate: selectedPlanDate.value,
      title: arrangement.value.title,
      dishIds: arrangement.value.recipes.map((recipe) => recipe.dish.id),
    })
    plannerVisible.value = false
    Message.success('计划已创建')
    await openPrimaryRoute({
      name: 'plan',
      query: {
        date: data.planDate,
        planId: data.id,
      },
    })
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '计划创建失败')
  } finally {
    confirming.value = false
  }
}

function formatDateKey(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function parseDateKey(value: string) {
  const parts = value.split('-').map((item) => Number(item))
  if (parts.length !== 3 || parts.some((item) => Number.isNaN(item))) return null
  return new Date(parts[0], parts[1] - 1, parts[2])
}
</script>

<style>
.pet-detail-page {
  min-height: 100vh;
  padding-bottom: 32px;
  background: linear-gradient(180deg, #fff7ea 0%, #f7f6f3 44%, #edf3ec 100%);
}

.pet-detail-hero {
  position: relative;
  overflow: hidden;
  height: 252px;
  border: 1px solid #ead9c4;
  border-radius: 28px;
  background: #fff8ef;
  box-shadow: 0 18px 42px rgba(138, 78, 41, 0.12);
}

.detail-glow,
.pet-soft-shadow {
  position: absolute;
  border-radius: 999px;
}

.detail-glow-green {
  left: 0;
  bottom: 0;
  width: 126px;
  height: 126px;
  background: radial-gradient(circle, rgba(141, 176, 125, 0.28), rgba(141, 176, 125, 0));
}

.detail-glow-gold {
  top: 0;
  right: 20px;
  width: 168px;
  height: 168px;
  background: radial-gradient(circle, rgba(255, 229, 168, 0.5), rgba(255, 229, 168, 0));
}

.pet-soft-shadow {
  left: 50%;
  bottom: 42px;
  width: 198px;
  height: 38px;
  transform: translateX(-50%);
  background: #dce7da;
  filter: blur(1px);
}

.detail-pet-image {
  position: absolute;
  left: 50%;
  top: 42px;
  width: 164px;
  height: 164px;
  transform: translateX(-42%);
  image-rendering: pixelated;
}

.pet-name-panel {
  position: absolute;
  left: 18px;
  top: 18px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.detail-pet-name,
.today-story-title {
  color: #1b3a2d;
  font-size: 30px;
  font-weight: 800;
}

.breed-pill,
.pet-whisper-bubble,
.pet-stats-row,
.today-story-head,
.go-recipe-button {
  display: flex;
  align-items: center;
}

.breed-pill {
  gap: 6px;
  border-radius: 999px;
  background: #edf3ec;
  padding: 6px 10px;
}

.breed-dot {
  color: #346538;
  font-size: 11px;
  font-weight: 800;
}

.breed-text {
  color: #346538;
  font-size: 12px;
  font-weight: 700;
}

.pet-whisper-bubble {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 14px;
  justify-content: center;
  gap: 8px;
  height: 34px;
  border: 1px solid #efe3d1;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  padding: 0 12px;
}

.pet-whisper-star {
  flex: 0 0 auto;
  color: #c8873b;
  font-size: 15px;
  font-weight: 800;
}

.pet-whisper-text {
  min-width: 0;
  color: #5b4a39;
  font-size: 12px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pet-stats-row {
  gap: 10px;
  margin-top: 18px;
}

.pet-stat-card {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 76px;
  gap: 6px;
  border: 1px solid #efe3d1;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 10px 20px rgba(27, 58, 45, 0.06);
}

.pet-stat-value {
  color: #1b3a2d;
  font-size: 21px;
  font-weight: 800;
}

.pet-stat-value.mood {
  color: #9f5c38;
}

.pet-stat-value.fullness {
  color: #346538;
}

.pet-stat-label {
  color: #787774;
  font-size: 11px;
  font-weight: 700;
}

.today-story-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 28px;
  border: 1px solid #efe3d1;
  border-radius: 22px;
  background: #fff;
  padding: 16px;
  box-shadow: 0 12px 26px rgba(138, 78, 41, 0.08);
}

.today-story-head {
  justify-content: space-between;
}

.today-story-title {
  color: #151515;
  font-size: 21px;
}

.today-story-time {
  color: #9f5c38;
  font-size: 12px;
  font-weight: 700;
}

.today-story-body {
  color: #2f3437;
  font-size: 13px;
  line-height: 1.55;
}

.go-recipe-button {
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 50px;
  margin-top: 28px;
  border-radius: 999px;
  background: linear-gradient(135deg, #9f5c38, #c8873b);
  box-shadow: 0 10px 24px rgba(164, 106, 31, 0.2);
}

.go-recipe-text,
.go-recipe-arrow {
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  line-height: 1;
  text-align: center;
}

.planner-overlay {
  position: fixed;
  z-index: 50;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  align-items: flex-end;
  background: rgba(21, 21, 21, 0.44);
}

.planner-sheet {
  display: flex;
  width: 100%;
  max-height: 82vh;
  flex-direction: column;
  gap: 14px;
  border: 1px solid #efe3d1;
  border-radius: 24px 24px 0 0;
  background: #fff;
  padding: 12px 20px 24px;
  box-shadow: 0 -10px 36px rgba(138, 78, 41, 0.12);
  box-sizing: border-box;
}

.planner-handle {
  align-self: center;
  width: 42px;
  height: 4px;
  border-radius: 999px;
  background: #e6dbcf;
}

.planner-head,
.planner-fields,
.planner-note-card,
.planner-progress-card,
.planner-recipe-copy {
  display: flex;
  flex-direction: column;
}

.planner-head {
  gap: 6px;
}

.planner-title {
  color: #1b3a2d;
  font-size: 22px;
  font-weight: 800;
}

.planner-desc {
  color: #787774;
  font-size: 12px;
  line-height: 1.55;
}

.planner-fields {
  gap: 8px;
}

.planner-field-card {
  display: flex;
  min-height: 52px;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  border-radius: 14px;
  background: #f7f3ee;
  padding: 12px 14px;
  box-sizing: border-box;
}

.planner-field-card.compact {
  min-height: 68px;
}

.planner-field-card.advice {
  min-height: 106px;
  justify-content: flex-start;
}

.planner-field-label,
.planner-note-title,
.planner-quota-text {
  font-size: 12px;
  font-weight: 700;
}

.planner-field-label {
  color: #7f7a72;
}

.planner-field-value {
  color: #1b3a2d;
  font-size: 15px;
  font-weight: 800;
}

.planner-meal-tabs,
.planner-row,
.planner-actions,
.planner-stepper,
.planner-progress-row,
.planner-recipe-card {
  display: flex;
  align-items: center;
}

.planner-meal-tabs {
  gap: 6px;
  height: 44px;
  border-radius: 999px;
  background: #f7f3ee;
  padding: 4px;
}

.planner-meal-tab {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  height: 36px;
  border-radius: 999px;
  background: transparent;
}

.planner-meal-tab.active {
  background: #9f5c38;
}

.planner-meal-text {
  color: #6f5c4a;
  font-size: 14px;
  font-weight: 800;
  text-align: center;
}

.planner-meal-tab.active .planner-meal-text {
  color: #fff;
}

.planner-row {
  gap: 8px;
}

.planner-row > picker,
.planner-row > .planner-field-card {
  min-width: 0;
  flex: 1;
}

.planner-stepper {
  justify-content: space-between;
  gap: 8px;
}

.planner-stepper-button {
  display: flex;
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #fff;
  color: #9f5c38;
  font-size: 16px;
  font-weight: 900;
  line-height: 1;
  text-align: center;
}

.planner-stepper-value {
  color: #1b3a2d;
  font-size: 14px;
  font-weight: 800;
  text-align: center;
}

.planner-advice-input {
  width: 100%;
  min-height: 54px;
  color: #1b3a2d;
  font-size: 14px;
  line-height: 1.45;
}

.planner-advice-placeholder {
  color: #a69b90;
}

.planner-quota-text {
  color: #9f5c38;
}

.planner-actions {
  gap: 8px;
}

.planner-secondary-button,
.planner-primary-button {
  display: flex;
  flex: 1;
  height: 48px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
}

.planner-secondary-button {
  background: #f4eee7;
}

.planner-primary-button {
  background: linear-gradient(135deg, #9f5c38, #c8873b);
  box-shadow: 0 8px 18px rgba(164, 106, 31, 0.16);
}

.planner-loading-button {
  width: 100%;
  flex: none;
  background: #d8c3b6;
  box-shadow: none;
}

.planner-primary-button.planner-loading-button[disabled] {
  opacity: 1;
}

.planner-loading-button .planner-primary-text {
  font-weight: 700;
}

.planner-primary-button[disabled],
.planner-secondary-button[disabled],
.planner-stepper-button[disabled] {
  opacity: 0.58;
}

.planner-secondary-text,
.planner-primary-text {
  font-size: 15px;
  font-weight: 800;
  line-height: 1;
  text-align: center;
}

.planner-secondary-text {
  color: #6f5c4a;
}

.planner-primary-text {
  color: #fff;
}

.planner-progress-card {
  gap: 12px;
  border: 1px solid #ead9c4;
  border-radius: 18px;
  background: #fff8ef;
  padding: 16px;
}

.planner-progress-icon {
  color: #c8873b;
  font-size: 28px;
  font-weight: 800;
  line-height: 1;
}

.planner-progress-row {
  gap: 10px;
  opacity: 0;
  transform: translateY(8px);
  animation: planner-progress-appear 0.72s ease forwards;
}

.planner-progress-row.step-1 {
  animation-delay: 0.16s;
}

.planner-progress-row.step-2 {
  animation-delay: 0.64s;
}

.planner-progress-row.step-3 {
  animation-delay: 1.12s;
}

.planner-progress-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #9f5c38;
  animation: planner-dot-breathe 1.4s ease-in-out infinite;
}

.planner-progress-text {
  color: #1b3a2d;
  font-size: 13px;
  font-weight: 800;
}

@keyframes planner-progress-appear {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes planner-dot-breathe {
  0%,
  100% {
    opacity: 0.45;
    transform: scale(0.86);
  }

  50% {
    opacity: 1;
    transform: scale(1);
  }
}

.planner-note-card {
  gap: 6px;
  border: 1px solid #dce7da;
  border-radius: 18px;
  background: #edf3ec;
  padding: 14px;
}

.planner-note-title {
  color: #346538;
}

.planner-note-body {
  color: #1b3a2d;
  font-size: 13px;
  line-height: 1.55;
}

.planner-recipes {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
}

.planner-recipe-card {
  gap: 10px;
  border: 1px solid #efe3d1;
  border-radius: 16px;
  background: #fff8ef;
  padding: 10px;
}

.planner-recipe-image {
  flex: 0 0 auto;
  width: 52px;
  height: 52px;
  border-radius: 12px;
}

.planner-recipe-copy {
  min-width: 0;
  flex: 1;
  gap: 4px;
}

.planner-recipe-title {
  color: #1b3a2d;
  font-size: 15px;
  font-weight: 800;
}

.planner-recipe-reason {
  color: #6f5c4a;
  font-size: 12px;
  line-height: 1.45;
}
</style>
