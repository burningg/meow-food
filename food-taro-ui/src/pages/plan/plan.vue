<template>
  <view class="plan-page-root">
    <PullRefreshPage @refresh="refreshWeek">
      <view class="page-shell plan-page" @tap="closePlanMenu">
        <section class="week-header-card">
          <view class="week-header-copy">
            <text class="week-eyebrow">本周计划</text>
            <text class="week-title">{{ weekLabel }}</text>
          </view>
          <view class="week-switch">
            <button class="plan-nav-action week-add-button" @tap="openCreateSheet">＋</button>
            <view class="week-switch-controls">
              <button class="week-switch-button" @tap="changeWeek(-1)">‹</button>
              <button class="week-switch-button" @tap="changeWeek(1)">›</button>
            </view>
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
              :class="['plan-card', { expanded: expandedPlanId === plan.id, shopping: hasShoppingStarted(plan) }]"
            >
              <view class="plan-card-head">
                <button class="plan-card-main" @tap="toggleExpandedPlan(plan.id)">
                  <view class="plan-card-copy">
                    <text class="plan-card-title">{{ plan.title }}</text>
                    <text class="plan-card-meta">{{ plan.circleName }} · {{ plan.creatorNickname }}创建</text>
                  </view>
                </button>
                <view class="plan-card-side">
                  <view :class="['plan-card-badge', { shopping: hasShoppingStarted(plan) }]">
                    <text class="plan-card-badge-text">{{ getShoppingStatusLabel(plan.shoppingStatus) }}</text>
                  </view>
                  <view v-if="canDeletePlan(plan)" class="plan-card-menu-wrap">
                    <button class="plan-card-menu-trigger" @tap.stop="togglePlanMenu(plan.id)">···</button>
                    <view v-if="activeMenuPlanId === plan.id" class="plan-card-menu-popover" @tap.stop>
                      <button
                        class="plan-card-menu-delete"
                        :disabled="deletingPlanId === plan.id"
                        @tap.stop="deletePlan(plan)"
                      >
                        {{ deletingPlanId === plan.id ? '删除中...' : '删除计划' }}
                      </button>
                    </view>
                  </view>
                </view>
              </view>

              <view v-if="expandedPlanId === plan.id" class="plan-card-body">
                <view v-if="expandedRecipes(plan.id).length" class="checked-recipe-row">
                  <view
                    v-for="recipe in expandedRecipes(plan.id)"
                    :key="recipe.id"
                    class="checked-recipe-swipe"
                    @touchstart="handleRecipeTouchStart(plan.id, recipe.id, $event)"
                    @touchend="handleRecipeTouchEnd(plan.id, recipe.id, $event)"
                    @touchcancel="resetRecipeSwipeTracking"
                  >
                    <view class="checked-recipe-action">
                      <button
                        class="checked-recipe-remove"
                        :disabled="removingRecipeKey === buildRecipeKey(plan.id, recipe.id)"
                        @tap.stop="removeRecipeFromPlan(plan.id, recipe.id, recipe.name)"
                      >
                        <view
                          v-if="removingRecipeKey === buildRecipeKey(plan.id, recipe.id)"
                          class="checked-recipe-remove-loading"
                        ></view>
                        <text v-else class="checked-recipe-remove-text">删除</text>
                      </button>
                    </view>
                    <view
                      :class="[
                        'checked-recipe-card',
                        { revealed: revealedRecipeKey === buildRecipeKey(plan.id, recipe.id) },
                      ]"
                    >
                      <button class="checked-recipe-main" @tap="handleRecipeTap(plan.id, recipe.id)">
                        <SmartImage :src="recipe.image" class-name="checked-recipe-image" />
                        <view class="checked-recipe-copy">
                          <text class="checked-recipe-title">{{ recipe.name }}</text>
                          <text class="checked-recipe-meta">
                            {{ recipe.categoryName || '共享菜谱' }}{{ recipe.addedByNickname ? ` · ${recipe.addedByNickname}加入` : '' }}
                          </text>
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
                  {{ hasShoppingStarted(plan) ? '查看采购' : '开始采购' }}
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
import {
  PlanService,
  type PlanDayPlans,
  type PlanDetail,
  type PlanShoppingStatus,
  type PlanSummary,
} from '@/services/plan-service'
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
const currentUserId = ref('')
const selectedCircleId = ref('')
const draftTitle = ref(buildDefaultPlanTitle(formatDateKey(initialDate)))
const creating = ref(false)
const createSheetVisible = ref(false)
const activeMenuPlanId = ref('')
const deletingPlanId = ref('')
const removingRecipeKey = ref('')
const revealedRecipeKey = ref('')
const recipeSwipeStart = ref<{ key: string; x: number; y: number } | null>(null)

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
    revealedRecipeKey.value = ''
    if (activeMenuPlanId.value && !plans.some((plan) => plan.id === activeMenuPlanId.value)) {
      activeMenuPlanId.value = ''
    }
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
    currentUserId.value = profileData.user.id || ''
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
  closePlanMenu()
  draftTitle.value = buildDefaultPlanTitle(selectedDateKey.value)
  createSheetVisible.value = true
}

function closeCreateSheet() {
  createSheetVisible.value = false
}

function changeWeek(offset: number) {
  closePlanMenu()
  const currentSelectedDate = parseDateKey(selectedDateKey.value) || new Date()
  const weekdayIndex = getWeekdayIndex(currentSelectedDate)
  viewedWeekStart.value = addDays(viewedWeekStart.value, offset * 7)
  selectedDateKey.value = formatDateKey(addDays(viewedWeekStart.value, weekdayIndex))
  void loadWeek()
}

function selectDate(key: string) {
  closePlanMenu()
  selectedDateKey.value = key
}

async function toggleExpandedPlan(planId: string) {
  closePlanMenu()
  revealedRecipeKey.value = ''
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
  closePlanMenu()
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
  closePlanMenu()
  try {
    if (!hasShoppingStarted(plan)) {
      const { data } = await planService.startShoppingList(plan.id)
      syncPlanState(plan.id, {
        shoppingStatus: 'NOT_PURCHASED',
        shoppingStarted: data.shoppingStarted,
        shoppingPurchasedItemCount: data.purchasedItemCount,
        shoppingTotalItemCount: data.totalItemCount,
      })
      if (detailCache.value[plan.id]) {
        detailCache.value = {
          ...detailCache.value,
          [plan.id]: {
            ...detailCache.value[plan.id],
            shoppingStatus: 'NOT_PURCHASED',
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
  closePlanMenu()
  const recipeKey = buildRecipeKey(planId, dishId)
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
    if (revealedRecipeKey.value === recipeKey) {
      revealedRecipeKey.value = ''
    }
    syncPlanState(planId, {
      recipeCount: data.recipes.length,
      shoppingStatus: data.shoppingStatus,
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

function canDeletePlan(plan: Pick<PlanSummary, 'creatorUserId'>) {
  return !!currentUserId.value && plan.creatorUserId === currentUserId.value
}

function togglePlanMenu(planId: string) {
  activeMenuPlanId.value = activeMenuPlanId.value === planId ? '' : planId
}

function closePlanMenu() {
  activeMenuPlanId.value = ''
}

async function deletePlan(plan: PlanSummary) {
  if (deletingPlanId.value) return
  try {
    await confirmDialog({
      title: '删除计划',
      message: `确认删除“${plan.title}”吗？计划里的菜谱和采购清单会一起删除。`,
    })
    deletingPlanId.value = plan.id
    closePlanMenu()
    await planService.deletePlan(plan.id)
    removePlanState(plan.id)
    Message.success('计划已删除')
  } catch (error: any) {
    if (error?.message === 'cancelled') return
    Message.error(error?.response?.data?.message || '删除计划失败')
  } finally {
    deletingPlanId.value = ''
  }
}

function removePlanState(planId: string) {
  // 删除后同步清理月缓存和详情缓存，避免页面必须整页刷新才能看到结果。
  const nextCache: Record<string, PlanDayPlans[]> = {}
  Object.entries(monthCache.value).forEach(([monthKey, days]) => {
    nextCache[monthKey] = days
      .map((day) => ({
        ...day,
        plans: day.plans.filter((plan) => plan.id !== planId),
      }))
      .filter((day) => day.plans.length)
  })
  monthCache.value = nextCache

  const nextDetailCache = { ...detailCache.value }
  delete nextDetailCache[planId]
  detailCache.value = nextDetailCache

  if (expandedPlanId.value === planId) {
    expandedPlanId.value = ''
  }
  if (revealedRecipeKey.value.startsWith(`${planId}:`)) {
    revealedRecipeKey.value = ''
  }
}

function buildRecipeKey(planId: string, dishId: string) {
  return `${planId}:${dishId}`
}

function hasShoppingStarted(plan: Pick<PlanSummary, 'shoppingStatus'>) {
  return plan.shoppingStatus !== 'NOT_STARTED'
}

function getShoppingStatusLabel(status: PlanShoppingStatus) {
  switch (status) {
    case 'NOT_PURCHASED':
      return '未采购'
    case 'PARTIALLY_PURCHASED':
      return '部分采购'
    case 'PURCHASED':
      return '采购完成'
    case 'NOT_STARTED':
    default:
      return '待加采购单'
  }
}

function handleRecipeTap(planId: string, dishId: string) {
  const recipeKey = buildRecipeKey(planId, dishId)
  if (revealedRecipeKey.value === recipeKey) {
    revealedRecipeKey.value = ''
    return
  }
  openDish(dishId)
}

function handleRecipeTouchStart(planId: string, dishId: string, event: any) {
  const point = getTouchPoint(event)
  if (!point) return
  recipeSwipeStart.value = {
    key: buildRecipeKey(planId, dishId),
    x: point.x,
    y: point.y,
  }
}

function handleRecipeTouchEnd(planId: string, dishId: string, event: any) {
  const start = recipeSwipeStart.value
  const recipeKey = buildRecipeKey(planId, dishId)
  recipeSwipeStart.value = null
  if (!start || start.key !== recipeKey) return

  const point = getTouchPoint(event)
  if (!point) return

  const deltaX = point.x - start.x
  const deltaY = point.y - start.y
  if (Math.abs(deltaX) <= Math.abs(deltaY) || Math.abs(deltaX) < 36) return

  if (deltaX < 0) {
    revealedRecipeKey.value = recipeKey
    return
  }

  if (revealedRecipeKey.value === recipeKey) {
    revealedRecipeKey.value = ''
  }
}

function resetRecipeSwipeTracking() {
  recipeSwipeStart.value = null
}

function getTouchPoint(event: any) {
  const touch = event?.changedTouches?.[0] || event?.touches?.[0]
  if (!touch) return null
  return {
    x: touch.pageX ?? touch.clientX ?? 0,
    y: touch.pageY ?? touch.clientY ?? 0,
  }
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

.week-switch,
.week-switch-controls,
.weekday-row,
.week-date-row,
.plan-card-head,
.plan-card-side,
.create-sheet-head,
.create-sheet-actions {
  display: flex;
}

.plan-card-head,
.create-sheet-head,
.create-sheet-actions {
  align-items: center;
  justify-content: space-between;
}

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
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.week-switch-controls {
  gap: 8px;
}

.week-add-button {
  width: 30px;
  height: 30px;
  font-size: 16px;
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
  border: 1px solid transparent;
  box-shadow: 0 8px 18px rgba(27, 58, 45, 0.05);
  transition:
    background 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.plan-card.expanded {
  background: #fbf8f4;
  border-color: #e1c1ab;
  box-shadow: 0 16px 30px rgba(159, 92, 56, 0.14);
  transform: translateY(-1px);
}

.plan-card.expanded .plan-card-title {
  color: #8d4b2d;
}

.plan-card.expanded .plan-card-meta {
  color: #6f6258;
}

.plan-card-copy {
  text-align: left;
}

.plan-card-main {
  flex: 1;
  min-width: 0;
  padding: 0;
  background: transparent;
  text-align: left;
}

.plan-card-side {
  position: relative;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
}

.plan-card.expanded .plan-card-badge {
  background: #9f5c38;
}

.plan-card.expanded .plan-card-badge .plan-card-badge-text {
  color: #ffffff;
}

.plan-card.expanded .plan-card-badge.shopping {
  background: #346538;
}

.plan-card.expanded .plan-card-badge.shopping .plan-card-badge-text {
  color: #ffffff;
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

.plan-card-menu-wrap {
  position: relative;
  z-index: 32;
}

.plan-card-menu-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  border-radius: 999px;
  background: #f4eee7;
  color: #7a6657;
  font-size: 16px;
  line-height: 1;
}

.plan-card-menu-popover {
  position: absolute;
  top: 38px;
  right: 0;
  min-width: 92px;
  padding: 6px;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 18px 36px rgba(27, 58, 45, 0.14);
}

.plan-card-menu-delete {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 36px;
  padding: 0 12px;
  border-radius: 10px;
  background: #fff1ef;
  color: #c25549;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.2;
  text-align: center;
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

.checked-recipe-swipe {
  position: relative;
  overflow: hidden;
  border-radius: 18px;
}

.checked-recipe-action {
  position: absolute;
  inset: 0 0 0 auto;
  display: flex;
  width: 76px;
  align-items: stretch;
  justify-content: flex-end;
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
  transition: transform 0.18s ease;
  position: relative;
  z-index: 1;
}

.checked-recipe-card.revealed {
  transform: translateX(-76px);
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

.checked-recipe-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 76px;
  height: 100%;
  padding: 0;
  border-radius: 0 18px 18px 0;
  background: #cf5f52;
}

.checked-recipe-remove-text {
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
}

.checked-recipe-remove-loading {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.28);
  border-top-color: #ffffff;
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
