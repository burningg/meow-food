<template>
  <view class="plan-page-root">
    <PullRefreshPage @refresh="refreshWeek">
      <view class="page-shell plan-page">
        <header class="plan-nav">
          <view class="plan-nav-side"></view>
          <button class="plan-nav-action" @tap="openCreateSheet">＋</button>
        </header>

        <section class="week-header-card">
          <view class="week-header-copy">
            <text class="week-eyebrow">本周计划</text>
            <text class="week-title">{{ weekLabel }}</text>
          </view>
          <view class="week-switch">
            <button class="week-switch-button" @tap="changeWeek(-1)">‹</button>
            <button class="week-switch-button" @tap="changeWeek(1)">›</button>
          </view>
        </section>

        <section class="week-calendar-card">
          <view class="weekday-row">
            <view v-for="day in weekdayLabels" :key="day" class="weekday-item">
              <text class="weekday-label">{{ day }}</text>
            </view>
          </view>

          <view class="week-date-row">
            <button
              v-for="date in weekDates"
              :key="date.key"
              :class="['week-date-card', { active: date.key === selectedDateKey, muted: !date.inCurrentMonth }]"
              @tap="selectDate(date.key)"
            >
              <text class="week-date-label">{{ date.day }}</text>
              <view class="week-date-dots">
                <view v-for="index in date.dotCount" :key="`${date.key}-${index}`" class="week-date-dot"></view>
              </view>
            </button>
          </view>
        </section>

        <section class="plan-list-section">
          <view v-if="selectedPlans.length" class="plan-list">
            <view
              v-for="plan in selectedPlans"
              :key="plan.id"
              :class="['plan-card', { expanded: expandedPlanId === plan.id, shopping: plan.shoppingStarted }]"
            >
              <button class="plan-card-head" @tap="toggleExpandedPlan(plan.id)">
                <view class="plan-card-copy">
                  <text class="plan-card-title">{{ plan.title }}</text>
                  <text class="plan-card-meta">{{ plan.circleName }} · {{ plan.creatorNickname }}创建</text>
                </view>
                <view :class="['plan-card-badge', { shopping: plan.shoppingStarted }]">
                  <text class="plan-card-badge-text">{{ plan.shoppingStarted ? '采购中' : '待加采购单' }}</text>
                </view>
              </button>

              <view v-if="expandedPlanId === plan.id" class="plan-card-body">
                <view v-if="expandedRecipes(plan.id).length" class="checked-recipe-row">
                  <view
                    v-for="recipe in expandedRecipes(plan.id)"
                    :key="recipe.id"
                    class="checked-recipe-card"
                  >
                    <button class="checked-recipe-main" @tap="openDish(recipe.id)">
                      <SmartImage :src="recipe.image" class-name="checked-recipe-image" />
                      <view class="checked-recipe-copy">
                        <text class="checked-recipe-title">{{ recipe.name }}</text>
                        <text class="checked-recipe-meta">
                          {{ recipe.categoryName || '共享菜谱' }}{{ recipe.addedByNickname ? ` · ${recipe.addedByNickname}加入` : '' }}
                        </text>
                      </view>
                    </button>
                    <view class="checked-recipe-side">
                      <button
                        class="checked-recipe-remove"
                        :disabled="removingRecipeKey === `${plan.id}:${recipe.id}`"
                        @tap.stop="removeRecipeFromPlan(plan.id, recipe.id, recipe.name)"
                      >
                        <view v-if="removingRecipeKey === `${plan.id}:${recipe.id}`" class="checked-recipe-remove-loading"></view>
                        <view v-else class="checked-recipe-trash-icon">
                          <view class="checked-recipe-trash-lid"></view>
                          <view class="checked-recipe-trash-body"></view>
                        </view>
                      </button>
                    </view>
                  </view>
                </view>
                <view v-else class="checked-empty-card">
                  <text class="checked-empty-title">这条计划还没加菜谱</text>
                  <text class="checked-empty-desc">先从圈内共享菜谱里挑几道，采购清单会自动跟着生成。</text>
                </view>

                <button class="plan-secondary-button" @tap="goAddRecipes(plan)">添加菜谱</button>
                <button class="plan-primary-button" @tap="openShopping(plan)">
                  {{ plan.shoppingStarted ? '查看采购' : '开始采购' }}
                </button>
              </view>
            </view>
          </view>

          <view v-else class="empty-card plan-empty-card">
            <text class="plan-empty-title">{{ selectedDateTitle }}还没有计划</text>
            <text class="plan-empty-desc">点右上角新建一条，让圈内成员一起补菜谱和采购清单。</text>
          </view>
        </section>
      </view>
    </PullRefreshPage>

    <view v-if="createSheetVisible" class="create-overlay" @tap="closeCreateSheet">
      <section class="create-sheet" @tap.stop>
        <view class="create-sheet-handle"></view>

        <view class="create-sheet-head">
          <text class="create-sheet-title">新建计划</text>
          <view class="create-date-chip">
            <text class="create-date-chip-text">{{ selectedDateShort }}</text>
          </view>
        </view>

        <view class="create-sheet-fields">
          <view class="create-field-card">
            <text class="create-field-label">计划标题</text>
            <input
              v-model.trim="draftTitle"
              class="create-field-input"
              maxlength="40"
              placeholder="例如：周五下班拼饭局"
              placeholder-class="create-field-placeholder"
            />
          </view>

          <picker mode="selector" :range="circleOptions" :value="selectedCircleIndex" @change="handleCircleChange">
            <view class="create-field-card create-picker-card">
              <text class="create-field-label">选择圈子</text>
              <text :class="['create-picker-value', { empty: !selectedCircleName }]">
                {{ selectedCircleName || (circles.length ? '选择圈子' : '先创建圈子') }}
              </text>
            </view>
          </picker>
        </view>

        <view class="create-sheet-actions">
          <button class="create-cancel-button" @tap="closeCreateSheet">取消</button>
          <button v-if="circles.length" class="create-submit-button" :disabled="creating" @tap="submitPlan">
            {{ creating ? '创建中...' : '创建计划' }}
          </button>
          <button v-else class="create-submit-button" @tap="goCreateCircle">创建圈子</button>
        </view>
      </section>
    </view>

    <AppTabBar active="plan" />
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import AppTabBar from '@/components/AppTabBar.vue'
import PullRefreshPage from '@/components/PullRefreshPage.vue'
import SmartImage from '@/components/SmartImage.vue'
import { requireAuth } from '@/lib/auth'
import { confirmDialog, Message } from '@/lib/feedback'
import { getRouteParams, push } from '@/lib/navigation'
import { PlanService, type PlanDayPlans, type PlanDetail, type PlanSummary } from '@/services/plan-service'
import { SocialService, type BuddyCircleSummary } from '@/services/social-service'

type WeekDateItem = {
  key: string
  day: number
  dotCount: number
  inCurrentMonth: boolean
}

const weekdayLabels = ['一', '二', '三', '四', '五', '六', '日']
const routeParams = getRouteParams<{ date?: string; planId?: string }>()
const initialDate = parseDateKey(routeParams.date) || new Date()
const planService = new PlanService()
const socialService = new SocialService()

const circles = ref<BuddyCircleSummary[]>([])
const monthCache = ref<Record<string, PlanDayPlans[]>>({})
const detailCache = ref<Record<string, PlanDetail>>({})
const viewedWeekStart = ref(startOfWeek(initialDate))
const selectedDateKey = ref(formatDateKey(initialDate))
const expandedPlanId = ref(routeParams.planId || '')
const selectedCircleId = ref('')
const draftTitle = ref(buildDefaultPlanTitle(formatDateKey(initialDate)))
const creating = ref(false)
const createSheetVisible = ref(false)
const removingRecipeKey = ref('')

const weekDates = computed<WeekDateItem[]>(() => {
  return Array.from({ length: 7 }, (_, index) => {
    const date = addDays(viewedWeekStart.value, index)
    const key = formatDateKey(date)
    const planCount = plansByDate.value[key]?.length || 0
    return {
      key,
      day: date.getDate(),
      dotCount: Math.min(planCount, 3),
      inCurrentMonth: date.getMonth() === selectedDate.value.getMonth(),
    }
  })
})

const plansByDate = computed<Record<string, PlanSummary[]>>(() => {
  const map: Record<string, PlanSummary[]> = {}
  Object.values(monthCache.value).forEach((days) => {
    days.forEach((entry) => {
      map[entry.date] = entry.plans
    })
  })
  return map
})

const selectedDate = computed(() => parseDateKey(selectedDateKey.value) || new Date())
const selectedPlans = computed(() => plansByDate.value[selectedDateKey.value] || [])
const selectedDateTitle = computed(() => formatDisplayDate(selectedDateKey.value, true))
const selectedDateShort = computed(() => formatDisplayDate(selectedDateKey.value, false))
const circleOptions = computed(() => circles.value.map((circle) => circle.name))
const selectedCircleIndex = computed(() => {
  const index = circles.value.findIndex((circle) => circle.id === selectedCircleId.value)
  return index < 0 ? 0 : index
})
const selectedCircleName = computed(() => circles.value[selectedCircleIndex.value]?.name || '')
const weekLabel = computed(() => {
  const start = weekDates.value[0]?.key
  const end = weekDates.value[6]?.key
  if (!start || !end) return ''
  return `${formatDisplayDate(start, true)} - ${formatDisplayDate(end, true)}`
})

onMounted(async () => {
  if (!(await requireAuth('plan'))) return
  await loadInitial()
})

watch(
  selectedPlans,
  async (plans) => {
    if (!plans.length) {
      expandedPlanId.value = ''
      return
    }
    if (!plans.some((plan) => plan.id === expandedPlanId.value)) {
      expandedPlanId.value = plans[0].id
    }
    if (expandedPlanId.value) {
      await ensurePlanDetail(expandedPlanId.value)
    }
  },
  { immediate: true },
)

async function loadInitial() {
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
    await loadWeek(true)
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '计划页加载失败')
  }
}

async function refreshWeek() {
  await loadWeek(true)
}

async function loadWeek(force = false) {
  const monthKeys = Array.from(new Set(weekDates.value.map((item) => item.key.slice(0, 7))))
  await Promise.all(monthKeys.map((monthKey) => loadMonth(monthKey, force)))
}

async function loadMonth(monthKey: string, force = false) {
  if (!force && monthCache.value[monthKey]) return
  const { data } = await planService.getPlans(monthKey)
  monthCache.value = {
    ...monthCache.value,
    [monthKey]: data.days,
  }
}

async function ensurePlanDetail(planId: string, force = false) {
  if (!force && detailCache.value[planId]) return detailCache.value[planId]
  const { data } = await planService.getPlanDetail(planId)
  detailCache.value = {
    ...detailCache.value,
    [planId]: data,
  }
  return data
}

function openCreateSheet() {
  draftTitle.value = buildDefaultPlanTitle(selectedDateKey.value)
  createSheetVisible.value = true
}

function closeCreateSheet() {
  createSheetVisible.value = false
}

function changeWeek(offset: number) {
  const currentSelectedDate = parseDateKey(selectedDateKey.value) || new Date()
  const weekdayIndex = getWeekdayIndex(currentSelectedDate)
  viewedWeekStart.value = addDays(viewedWeekStart.value, offset * 7)
  selectedDateKey.value = formatDateKey(addDays(viewedWeekStart.value, weekdayIndex))
  void loadWeek()
}

function selectDate(key: string) {
  selectedDateKey.value = key
}

async function toggleExpandedPlan(planId: string) {
  expandedPlanId.value = planId
  await ensurePlanDetail(planId)
}

function expandedRecipes(planId: string) {
  return detailCache.value[planId]?.recipes || []
}

function handleCircleChange(event: { detail: { value: string } }) {
  const index = Number(event.detail.value || 0)
  selectedCircleId.value = circles.value[index]?.id || ''
}

async function submitPlan() {
  if (!draftTitle.value.trim()) {
    Message.info('先写一个计划标题')
    return
  }
  if (!selectedCircleId.value) {
    Message.info('先选择圈子')
    return
  }

  creating.value = true
  try {
    const { data } = await planService.createPlan({
      circleId: selectedCircleId.value,
      planDate: selectedDateKey.value,
      title: draftTitle.value.trim(),
    })
    await socialService.updateLastSelectedCircle(selectedCircleId.value)
    draftTitle.value = buildDefaultPlanTitle(selectedDateKey.value)
    closeCreateSheet()
    expandedPlanId.value = data.id
    await loadWeek(true)
    await ensurePlanDetail(data.id, true)
    Message.success('计划已创建')
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '计划创建失败')
  } finally {
    creating.value = false
  }
}

function goAddRecipes(plan: PlanSummary) {
  push({
    name: 'plan-detail',
    params: { id: plan.id },
    query: {
      date: plan.planDate,
    },
  })
}

async function openShopping(plan: PlanSummary) {
  try {
    if (!plan.shoppingStarted) {
      const { data } = await planService.startShoppingList(plan.id)
      syncPlanState(plan.id, {
        shoppingStarted: data.shoppingStarted,
        shoppingPurchasedItemCount: data.purchasedItemCount,
        shoppingTotalItemCount: data.totalItemCount,
      })
      if (detailCache.value[plan.id]) {
        detailCache.value = {
          ...detailCache.value,
          [plan.id]: {
            ...detailCache.value[plan.id],
            shoppingStarted: data.shoppingStarted,
            shoppingPurchasedItemCount: data.purchasedItemCount,
            shoppingTotalItemCount: data.totalItemCount,
          },
        }
      }
    }
    push({ name: 'plan-shopping', params: { id: plan.id } })
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '采购清单启动失败')
  }
}

async function removeRecipeFromPlan(planId: string, dishId: string, dishName: string) {
  const recipeKey = `${planId}:${dishId}`
  if (removingRecipeKey.value) return
  try {
    await confirmDialog({
      title: '移除菜谱',
      message: `确认把“${dishName}”从这条计划里移除吗？如果已开始采购，采购单会按剩余菜谱重新计算。`,
    })
    removingRecipeKey.value = recipeKey
    const { data } = await planService.removeRecipe(planId, dishId)
    detailCache.value = {
      ...detailCache.value,
      [planId]: data,
    }
    syncPlanState(planId, {
      recipeCount: data.recipes.length,
      shoppingStarted: data.shoppingStarted,
      shoppingPurchasedItemCount: data.shoppingPurchasedItemCount,
      shoppingTotalItemCount: data.shoppingTotalItemCount,
    })
    Message.success('菜谱已移出计划')
  } catch (error: any) {
    if (error?.message === 'cancelled') return
    Message.error(error?.response?.data?.message || '移除菜谱失败')
  } finally {
    removingRecipeKey.value = ''
  }
}

function syncPlanState(planId: string, patch: Partial<PlanSummary>) {
  const nextCache: Record<string, PlanDayPlans[]> = {}
  Object.entries(monthCache.value).forEach(([monthKey, days]) => {
    nextCache[monthKey] = days.map((day) => ({
      ...day,
      plans: day.plans.map((plan) => (plan.id === planId ? { ...plan, ...patch } : plan)),
    }))
  })
  monthCache.value = nextCache
}

function openDish(dishId: string) {
  push({ name: 'dish-detail', params: { id: dishId } })
}

function goCreateCircle() {
  push('create-circle')
}

function startOfWeek(date: Date) {
  return addDays(stripTime(date), -getWeekdayIndex(date))
}

function stripTime(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

function addDays(date: Date, amount: number) {
  const next = new Date(date)
  next.setDate(next.getDate() + amount)
  return next
}

function getWeekdayIndex(date: Date) {
  return (date.getDay() + 6) % 7
}

function formatDateKey(date: Date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function buildDefaultPlanTitle(dateKey: string) {
  const [, month, day] = dateKey.split('-')
  return `${month}月${day}日吃点好的`
}

function parseDateKey(value?: string) {
  if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return null
  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(year, month - 1, day)
  return Number.isNaN(date.getTime()) ? null : date
}

function formatDisplayDate(value: string, withSpace: boolean) {
  const [_, month, day] = value.split('-')
  if (withSpace) {
    return `${Number(month)} 月 ${Number(day)} 日`
  }
  return `${Number(month)}月${Number(day)}日`
}
</script>

<style>
.plan-page {
  padding: 14px 20px 120px;
}

.plan-nav,
.week-switch,
.weekday-row,
.week-date-row,
.plan-card-head,
.create-sheet-head,
.create-sheet-actions {
  display: flex;
}

.plan-nav,
.plan-card-head,
.create-sheet-head,
.create-sheet-actions {
  align-items: center;
  justify-content: space-between;
}

.plan-nav {
  margin-bottom: 12px;
}

.plan-nav-side,
.plan-nav-action {
  width: 36px;
  height: 36px;
}

.plan-nav-action {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #ffffff;
  color: #151515;
  font-size: 18px;
  box-shadow: 0 8px 20px rgba(27, 58, 45, 0.08);
}

.page-title {
  color: #151515;
  font-size: 16px;
  font-weight: 600;
}

.week-header-card,
.week-calendar-card,
.plan-card {
  border-radius: 14px;
}

.week-header-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: #ffffff;
}

.week-header-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.week-eyebrow {
  color: #787774;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.week-title {
  color: #151515;
  font-family: 'Noto Serif SC', serif;
  font-size: 20px;
  font-weight: 600;
}

.week-switch {
  gap: 8px;
}

.week-switch-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 999px;
  background: #f4eee7;
  color: #5a4333;
  font-size: 18px;
}

.week-calendar-card {
  margin-top: 14px;
  padding: 14px;
  background: #fbf8f4;
}

.weekday-row,
.week-date-row {
  justify-content: space-between;
}

.weekday-item {
  width: 40px;
  display: flex;
  justify-content: center;
}

.weekday-label {
  color: #9a887a;
  font-size: 11px;
  font-weight: 600;
}

.week-date-row {
  margin-top: 10px;
}

.week-date-card {
  display: flex;
  width: 40px;
  height: 52px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border-radius: 12px;
  background: #ffffff;
}

.week-date-card.muted {
  background: #f3eee7;
}

.week-date-card.active {
  background: #1b3a2d;
}

.week-date-label {
  color: #151515;
  font-size: 14px;
  font-weight: 600;
}

.week-date-card.muted .week-date-label {
  color: #5a4333;
}

.week-date-card.active .week-date-label {
  color: #ffffff;
  font-weight: 700;
}

.week-date-dots {
  display: flex;
  min-height: 4px;
  gap: 4px;
}

.week-date-dot {
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: #f6d7c2;
}

.week-date-card.muted .week-date-dot {
  background: #d9c8bb;
}

.plan-list-section {
  margin-top: 18px;
}

.plan-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.plan-card {
  padding: 16px;
  background: #ffffff;
}

.plan-card.expanded {
  background: #fbf8f4;
}

.plan-card-title {
  display: block;
  color: #151515;
  font-family: 'Noto Serif SC', serif;
  font-size: 20px;
  font-weight: 600;
}

.plan-card-meta {
  display: block;
  margin-top: 4px;
  color: #787774;
  font-size: 12px;
}

.plan-card-badge {
  padding: 6px 10px;
  border-radius: 999px;
  background: #f7ecea;
}

.plan-card-badge.shopping {
  background: #edf3ec;
}

.plan-card-badge-text {
  color: #9f5c38;
  font-size: 11px;
  font-weight: 600;
}

.plan-card-badge.shopping .plan-card-badge-text {
  color: #346538;
}

.plan-card-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.checked-recipe-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.checked-recipe-card {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 76px;
  padding: 10px;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 10px 22px rgba(27, 58, 45, 0.07);
  text-align: left;
}

.checked-recipe-main {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
  padding: 0;
  background: transparent;
  text-align: left;
}

.checked-recipe-image {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  border-radius: 14px;
}

.checked-recipe-copy {
  flex: 1;
  min-width: 0;
}

.checked-recipe-title {
  display: block;
  color: #151515;
  font-size: 15px;
  font-weight: 600;
}

.checked-recipe-meta {
  display: block;
  margin-top: 4px;
  color: #787774;
  font-size: 12px;
}

.checked-recipe-side {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.checked-recipe-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border-radius: 999px;
  background: transparent;
}

.checked-recipe-trash-icon {
  position: relative;
  width: 14px;
  height: 16px;
}

.checked-recipe-trash-lid {
  position: absolute;
  top: 1px;
  left: 2px;
  width: 10px;
  height: 2px;
  border-radius: 999px;
  background: #9a9894;
}

.checked-recipe-trash-lid::before {
  content: '';
  position: absolute;
  top: -2px;
  left: 3px;
  width: 4px;
  height: 2px;
  border-radius: 999px;
  background: #9a9894;
}

.checked-recipe-trash-body {
  position: absolute;
  top: 4px;
  left: 2px;
  width: 10px;
  height: 10px;
  border: 2px solid #9a9894;
  border-top-width: 1px;
  border-radius: 2px 2px 3px 3px;
  box-sizing: border-box;
}

.checked-recipe-trash-body::before,
.checked-recipe-trash-body::after {
  content: '';
  position: absolute;
  top: 2px;
  width: 1px;
  height: 4px;
  border-radius: 999px;
  background: #9a9894;
}

.checked-recipe-trash-body::before {
  left: 2px;
}

.checked-recipe-trash-body::after {
  right: 2px;
}

.checked-recipe-remove-loading {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(154, 152, 148, 0.2);
  border-top-color: #9a9894;
  border-radius: 999px;
  animation: checked-recipe-spin 0.8s linear infinite;
}

@keyframes checked-recipe-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.checked-empty-card {
  padding: 14px;
  border-radius: 12px;
  background: #ffffff;
}

.checked-empty-title {
  display: block;
  color: #151515;
  font-size: 14px;
  font-weight: 600;
}

.checked-empty-desc {
  display: block;
  margin-top: 4px;
  color: #787774;
  font-size: 12px;
  line-height: 1.5;
}

.plan-secondary-button,
.plan-primary-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 48px;
  border-radius: 16px;
  font-size: 15px;
  font-weight: 600;
}

.plan-secondary-button {
  background: #f3eee7;
  color: #5a4333;
}

.plan-primary-button {
  background: #9f5c38;
  color: #ffffff;
}

.plan-empty-card {
  padding: 18px;
}

.plan-empty-title {
  display: block;
  color: #151515;
  font-size: 15px;
  font-weight: 600;
}

.plan-empty-desc {
  display: block;
  margin-top: 6px;
  color: #787774;
  font-size: 12px;
  line-height: 1.55;
}

.create-overlay {
  position: fixed;
  inset: 0;
  z-index: 35;
  display: flex;
  align-items: flex-end;
  background: rgba(246, 239, 231, 0.72);
}

.create-sheet {
  width: 100%;
  padding: 12px 16px calc(24px + env(safe-area-inset-bottom));
  border-radius: 24px 24px 0 0;
  background: #ffffff;
}

.create-sheet-handle {
  width: 42px;
  height: 4px;
  margin: 0 auto;
  border-radius: 999px;
  background: #e6dbcf;
}

.create-sheet-head {
  margin-top: 14px;
}

.create-sheet-title {
  color: #151515;
  font-family: 'Noto Serif SC', serif;
  font-size: 18px;
  font-weight: 600;
}

.create-date-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: #edf3ec;
}

.create-date-chip-text {
  color: #346538;
  font-size: 11px;
  font-weight: 600;
}

.create-sheet-fields {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 14px;
}

.create-field-card {
  padding: 14px;
  border-radius: 12px;
  background: #f7f3ee;
}

.create-field-label {
  display: block;
  color: #9a887a;
  font-size: 13px;
  font-weight: 500;
}

.create-field-input {
  width: 100%;
  min-height: 24px;
  margin-top: 6px;
  color: #151515;
  font-size: 14px;
}

.create-field-placeholder,
.create-picker-value.empty {
  color: #9a887a;
}

.create-picker-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.create-picker-value {
  color: #151515;
  font-size: 14px;
}

.create-sheet-actions {
  gap: 8px;
  margin-top: 14px;
}

.create-cancel-button,
.create-submit-button {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}

.create-cancel-button {
  background: #f4eee7;
  color: #7b6a5d;
}

.create-submit-button {
  background: #9f5c38;
  color: #ffffff;
}
</style>
