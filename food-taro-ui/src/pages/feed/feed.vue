<template>
  <view class="plan-page-root">
    <PullRefreshPage @refresh="loadPlans">
      <view class="page-shell plan-page">
        <header class="top-nav plan-nav">
          <view class="nav-placeholder"></view>
          <text class="page-title">计划</text>
          <view class="nav-placeholder"></view>
        </header>

        <section class="card month-card">
          <view class="month-card-head">
            <view>
              <text class="eyebrow">圈子计划</text>
              <text class="month-title">{{ monthLabel }}</text>
            </view>
            <view class="month-switch">
              <button class="month-button" @tap="changeMonth(-1)">‹</button>
              <button class="month-button" @tap="changeMonth(1)">›</button>
            </view>
          </view>
          <text class="month-hint">点击日期就能创建共享计划，同一天也可以安排多场局。</text>
        </section>

        <section class="calendar-card">
          <view class="weekday-row">
            <text v-for="day in weekdayLabels" :key="day" class="weekday-label">{{ day }}</text>
          </view>

          <view v-for="(week, index) in calendarWeeks" :key="`week-${index}`" class="calendar-row">
            <button
              v-for="cell in week"
              :key="cell.key"
              :class="[
                'day-cell',
                {
                  active: cell.key === selectedDateKey,
                  muted: !cell.inMonth,
                },
              ]"
              @tap="selectDate(cell.key)"
            >
              <text class="day-text">{{ cell.day }}</text>
              <view class="day-dots">
                <text v-if="cell.planCount" class="day-count">{{ cell.planCount }}</text>
              </view>
            </button>
          </view>
        </section>

        <section class="card create-card">
          <view class="create-head">
            <view>
              <text class="section-title">{{ selectedDateTitle }} · 新建计划</text>
              <text class="section-subtitle">圈内成员都能看到，也都能继续加菜和采购。</text>
            </view>
            <text class="selected-chip">已选日期</text>
          </view>

          <view class="field-group">
            <text class="field-label">计划标题</text>
            <input v-model.trim="draftTitle" class="line-input create-input" maxlength="40" placeholder="例如：周五下班拼饭局" />
          </view>

          <view class="field-group">
            <text class="field-label">选择圈子</text>
            <picker mode="selector" :range="circleOptions" :value="selectedCircleIndex" @change="handleCircleChange">
              <view class="line-input create-input picker-line">
                {{ selectedCircleName || (circles.length ? '选择圈子' : '先创建圈子') }}
              </view>
            </picker>
          </view>

          <view class="create-footer">
            <text class="footer-hint">当天可以创建多个计划卡片。</text>
            <button
              v-if="circles.length"
              class="create-submit"
              :disabled="creating"
              @tap="submitPlan"
            >
              {{ creating ? '创建中...' : '创建计划' }}
            </button>
            <button v-else class="create-submit" @tap="goCreateCircle">创建圈子</button>
          </view>
        </section>

        <section class="plan-list-section">
          <view class="section-head">
            <view>
              <text class="section-title">{{ selectedDateTitle }}的计划</text>
              <text class="section-subtitle">点进卡片继续加菜谱，或直接进入采购清单。</text>
            </view>
            <text class="section-link">{{ selectedPlans.length }} 条</text>
          </view>

          <view v-if="selectedPlans.length" class="plan-list">
            <button
              v-for="plan in selectedPlans"
              :key="plan.id"
              class="plan-card"
              @tap="openPlan(plan.id)"
            >
              <view class="plan-card-head">
                <view>
                  <text class="plan-card-title">{{ plan.title }}</text>
                  <text class="plan-card-meta">{{ plan.circleName }} · {{ plan.creatorNickname }}创建</text>
                </view>
                <text :class="['plan-badge', { active: plan.shoppingStarted }]">
                  {{ plan.shoppingStarted ? '采购中' : '待加采购单' }}
                </text>
              </view>

              <text class="plan-card-desc">
                已加 {{ plan.recipeCount }} 道菜谱
                <text v-if="plan.shoppingStarted">
                  ，已采买 {{ plan.shoppingPurchasedItemCount }}/{{ plan.shoppingTotalItemCount }}
                </text>
              </text>

              <view class="plan-card-footer">
                <text class="plan-card-footnote">
                  {{ plan.shoppingStarted ? '成员都能继续勾选采购项' : '点进计划后可先挑菜，再开始采购' }}
                </text>
                <text class="plan-card-cta">{{ plan.shoppingStarted ? '查看采购' : '进入计划' }}</text>
              </view>
            </button>
          </view>

          <view v-else class="empty-card">这一天还没有计划，先在上方写个标题创建一条吧。</view>
        </section>
      </view>
    </PullRefreshPage>

    <AppTabBar active="feed" />
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppTabBar from '@/components/AppTabBar.vue'
import PullRefreshPage from '@/components/PullRefreshPage.vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { push } from '@/lib/navigation'
import { PlanService, type PlanMonthResponse, type PlanSummary } from '@/services/plan-service'
import { SocialService, type BuddyCircleSummary } from '@/services/social-service'

type CalendarCell = {
  key: string
  day: number
  inMonth: boolean
  planCount: number
}

const weekdayLabels = ['一', '二', '三', '四', '五', '六', '日']
const planService = new PlanService()
const socialService = new SocialService()

const circles = ref<BuddyCircleSummary[]>([])
const monthData = ref<PlanMonthResponse>({ month: '', days: [] })
const viewedMonth = ref(startOfMonth(new Date()))
const selectedDateKey = ref(formatDateKey(new Date()))
const selectedCircleId = ref('')
const draftTitle = ref('')
const creating = ref(false)

const plansByDate = computed<Record<string, PlanSummary[]>>(() =>
  monthData.value.days.reduce<Record<string, PlanSummary[]>>((map, entry) => {
    map[entry.date] = entry.plans
    return map
  }, {}),
)

const calendarWeeks = computed(() => buildCalendarWeeks(viewedMonth.value, plansByDate.value))
const selectedPlans = computed(() => plansByDate.value[selectedDateKey.value] || [])
const monthLabel = computed(() => `${viewedMonth.value.getFullYear()} 年 ${viewedMonth.value.getMonth() + 1} 月`)
const selectedDateTitle = computed(() => formatChineseDate(selectedDateKey.value))
const circleOptions = computed(() => circles.value.map((circle) => circle.name))
const selectedCircleIndex = computed(() => {
  const index = circles.value.findIndex((circle) => circle.id === selectedCircleId.value)
  return index < 0 ? 0 : index
})
const selectedCircleName = computed(() => circles.value[selectedCircleIndex.value]?.name || '')

onMounted(async () => {
  if (!(await requireAuth('feed'))) return
  await loadInitial()
})

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
    ensureSelectedDateInViewedMonth()
    await loadPlans()
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '计划页加载失败')
  }
}

async function loadPlans() {
  try {
    const monthKey = formatMonthKey(viewedMonth.value)
    const { data } = await planService.getPlans(monthKey)
    monthData.value = data
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '计划加载失败')
  }
}

function ensureSelectedDateInViewedMonth() {
  const [year, month] = selectedDateKey.value.split('-').map((value, index) => (index < 2 ? Number(value) : value))
  const viewedYear = viewedMonth.value.getFullYear()
  const viewedMonthNo = viewedMonth.value.getMonth() + 1
  if (Number(year) === viewedYear && Number(month) === viewedMonthNo) return
  selectedDateKey.value = formatDateKey(viewedMonth.value)
}

function changeMonth(offset: number) {
  const next = new Date(viewedMonth.value)
  next.setMonth(next.getMonth() + offset)
  viewedMonth.value = startOfMonth(next)
  selectedDateKey.value = formatDateKey(viewedMonth.value)
  void loadPlans()
}

function selectDate(key: string) {
  selectedDateKey.value = key
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
    await planService.createPlan({
      circleId: selectedCircleId.value,
      planDate: selectedDateKey.value,
      title: draftTitle.value.trim(),
    })
    await socialService.updateLastSelectedCircle(selectedCircleId.value)
    draftTitle.value = ''
    await loadPlans()
    Message.success('计划已创建')
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '计划创建失败')
  } finally {
    creating.value = false
  }
}

function openPlan(id: string) {
  push({ name: 'plan-detail', params: { id } })
}

function goCreateCircle() {
  push('create-circle')
}

function startOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

function buildCalendarWeeks(baseMonth: Date, datePlans: Record<string, PlanSummary[]>) {
  const monthStart = startOfMonth(baseMonth)
  const day = monthStart.getDay() || 7
  const gridStart = new Date(monthStart)
  gridStart.setDate(monthStart.getDate() - day + 1)

  const cells: CalendarCell[] = []
  for (let index = 0; index < 42; index += 1) {
    const current = new Date(gridStart)
    current.setDate(gridStart.getDate() + index)
    const key = formatDateKey(current)
    cells.push({
      key,
      day: current.getDate(),
      inMonth: current.getMonth() === baseMonth.getMonth(),
      planCount: datePlans[key]?.length || 0,
    })
  }

  return Array.from({ length: 6 }, (_, index) => cells.slice(index * 7, index * 7 + 7))
}

function formatDateKey(date: Date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatMonthKey(date: Date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  return `${year}-${month}`
}

function formatChineseDate(value: string) {
  const [year, month, day] = value.split('-')
  return `${Number(month)} 月 ${Number(day)} 日`
}
</script>

<style>
.plan-page {
  padding-bottom: 120px;
}

.plan-nav {
  display: grid;
  grid-template-columns: 36px 1fr 36px;
  align-items: center;
}

.nav-placeholder {
  width: 36px;
  height: 36px;
}

.page-title {
  justify-self: center;
  color: var(--text-main);
  font-size: var(--title-lg);
  font-weight: 800;
}

.month-card,
.create-card,
.plan-card,
.calendar-card {
  border-radius: 18px;
  box-shadow: var(--shadow);
}

.month-card,
.create-card,
.plan-card {
  padding: 18px;
  background: #fff;
}

.month-card-head,
.month-switch,
.calendar-row,
.create-head,
.create-footer,
.section-head,
.plan-card-head,
.plan-card-footer,
.day-dots,
.weekday-row {
  display: flex;
}

.month-card-head,
.create-head,
.create-footer,
.section-head,
.plan-card-head,
.plan-card-footer {
  align-items: center;
  justify-content: space-between;
}

.month-card-head {
  align-items: flex-start;
}

.month-title {
  display: block;
  margin-top: 4px;
  color: #151515;
  font-family: 'Noto Serif SC', serif;
  font-size: 24px;
  font-weight: 600;
}

.eyebrow,
.field-label {
  color: #8f8377;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.month-switch {
  gap: 8px;
}

.month-button {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  background: #f4eee7;
  color: #5a4333;
  font-size: 20px;
}

.month-hint,
.section-subtitle,
.plan-card-desc,
.plan-card-footnote,
.footer-hint {
  color: #6f6f6f;
  font-size: 12px;
  line-height: 1.55;
}

.calendar-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
  padding: 16px;
  background: #fbf8f4;
}

.weekday-row,
.calendar-row {
  justify-content: space-between;
}

.weekday-label {
  width: 40px;
  color: #9a887a;
  font-size: 11px;
  font-weight: 700;
  text-align: center;
}

.day-cell {
  display: flex;
  width: 40px;
  height: 56px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border-radius: 12px;
  background: #fff;
  color: #151515;
}

.day-cell.muted {
  background: #f3eee7;
  color: #b7a99b;
}

.day-cell.active {
  background: #1b3a2d;
  color: #fff;
}

.day-text {
  font-size: 14px;
  font-weight: 700;
}

.day-dots {
  min-height: 14px;
  align-items: center;
  justify-content: center;
}

.day-count {
  min-width: 16px;
  padding: 1px 5px;
  border-radius: 999px;
  background: rgba(196, 112, 75, 0.16);
  color: var(--accent-dark);
  font-size: 10px;
  font-weight: 700;
}

.day-cell.active .day-count {
  background: rgba(255, 255, 255, 0.16);
  color: #fff6ec;
}

.create-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 16px;
}

.section-title {
  display: block;
  color: #151515;
  font-size: 16px;
  font-weight: 800;
}

.selected-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: #edf3ec;
  color: #346538;
  font-size: 11px;
  font-weight: 700;
}

.create-input,
.picker-line {
  display: flex;
  align-items: center;
  min-height: 46px;
  border-radius: 12px;
  background: #f7f3ee;
  padding: 0 16px;
  color: #151515;
  font-size: 14px;
  font-weight: 600;
}

.create-submit {
  min-width: 104px;
  min-height: 40px;
  padding: 0 16px;
  border-radius: 999px;
  background: var(--accent-dark);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.create-submit[disabled] {
  opacity: 0.68;
}

.plan-list-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}

.section-link {
  color: var(--accent-dark);
  font-size: 12px;
  font-weight: 700;
}

.plan-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.plan-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  text-align: left;
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

.plan-badge {
  padding: 6px 10px;
  border-radius: 999px;
  background: #f7ecea;
  color: var(--accent-dark);
  font-size: 11px;
  font-weight: 700;
}

.plan-badge.active {
  background: #edf3ec;
  color: #346538;
}

.plan-card-cta {
  color: var(--text-main);
  font-size: 12px;
  font-weight: 700;
}
</style>
