<template>
  <view class="page-shell circles-page">
    <header class="top-nav circle-nav">
      <button class="nav-button" @tap="goBack('home')">‹</button>
      <text class="page-title">美食搭子</text>
      <button class="nav-button" @tap="createCircle">＋</button>
    </header>

    <template v-if="activeDetail">
      <main class="circle-content">
        <section class="circle-switch-panel">
          <view class="switch-panel-header">
            <text class="strong">圈子</text>
            <text class="muted">当前 · {{ activeDetail.circle.name }}</text>
          </view>
          <view class="current-circle-summary">
            <view class="current-circle-copy">
              <text class="circle-name">{{ activeDetail.circle.name }}</text>
              <text class="muted">{{ activeDetail.stats.memberCount }}人 · {{ activeDetail.stats.sharedMenuCount }}菜谱 · 正在使用</text>
            </view>
            <button class="circle-switch-button" :disabled="presentedCircles.length <= 1" @tap="openCircleSwitcher">
              切换 ⌄
            </button>
          </view>
        </section>

        <section class="section-block">
          <view class="section-head">
            <text class="section-title">搭子圈成员</text>
            <text class="section-link">共 {{ activeDetail.stats.memberCount }} 人</text>
          </view>
          <button class="member-entry" @tap="openMembers">
            <view class="member-avatars">
              <view
                v-for="member in previewMembers"
                :key="member.id"
                class="avatar-badge"
                :style="{ background: avatarPalette[member.avatarTone].bg, color: avatarPalette[member.avatarTone].fg }"
              >
                {{ member.initial }}
              </view>
            </view>
            <text class="member-entry-arrow">›</text>
          </button>
        </section>

        <section class="section-block recipes-section">
          <view class="section-head">
            <view>
              <text class="section-title">圈内菜谱</text>
              <text class="muted">按分类快速看圈内最近共享的菜</text>
            </view>
            <text class="section-link">最近更新</text>
          </view>

          <scroll-view class="category-group" scroll-x>
            <button
              v-for="category in categories"
              :key="category"
              :class="['category-chip', { active: category === activeCategory }]"
              @tap="activeCategory = category"
            >
              {{ category }}
            </button>
          </scroll-view>

          <view v-if="visibleMenus.length" class="recipe-grid">
            <button v-for="menu in visibleMenus" :key="menu.id" class="recipe-card" @tap="openDish(menu.id)">
              <image class="recipe-image" :src="menu.image" mode="aspectFill" />
              <view class="recipe-copy">
                <text class="recipe-name">{{ menu.name }}</text>
                <text class="muted">{{ menu.categoryName }}</text>
                <text class="recipe-owner">{{ menu.ownerNickname }}创建</text>
              </view>
            </button>
          </view>

          <view v-else class="empty-card">这个分类还没有共享菜谱</view>
        </section>
      </main>
    </template>

    <section v-else class="status-card">
      <text>{{ statusText }}</text>
      <button v-if="!isLoading" class="create-button" @tap="createCircle">新建圈子</button>
    </section>

    <view v-if="switcherVisible" class="circle-switch-overlay" @tap="closeCircleSwitcher">
      <section class="circle-switch-sheet" @tap.stop>
        <view class="sheet-handle"></view>
        <view class="sheet-head">
          <view>
            <text class="eyebrow">切换圈子</text>
            <text class="sheet-title">选择搭子圈</text>
          </view>
          <button class="sheet-close" @tap="closeCircleSwitcher">×</button>
        </view>
        <view class="circle-option-list">
          <button
            v-for="circle in presentedCircles"
            :key="circle.id"
            :class="['circle-option', { active: circle.id === activeCircleId }]"
            @tap="selectCircle(circle.id)"
          >
            <text class="circle-option-mark">{{ circle.name.slice(0, 1) }}</text>
            <view class="circle-option-copy">
              <text class="strong">{{ circle.name }}</text>
              <text class="muted">{{ circle.memberCount }}人 · {{ circle.sharedMenuCount }}菜谱</text>
            </view>
            <text class="circle-option-state">{{ circle.id === activeCircleId ? '当前' : '切换' }}</text>
          </button>
        </view>
      </section>
    </view>

    <AppTabBar active="circles" />
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import AppTabBar from '@/components/AppTabBar.vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { goBack, push } from '@/lib/navigation'
import type { DishSummary } from '@/services/food-service'
import {
  SocialService,
  type BuddyCircleDetail,
  type BuddyCircleMember,
  type BuddyCircleSummary,
} from '@/services/social-service'

type PreviewMember = {
  id: string
  initial: string
  avatarTone: number
}

const socialService = new SocialService()
const circles = ref<BuddyCircleSummary[]>([])
const activeDetail = ref<BuddyCircleDetail | null>(null)
const activeCircleId = ref('')
const activeCategory = ref('全部')
const isLoading = ref(true)
const switcherVisible = ref(false)
let detailRequestToken = 0

const avatarPalette = [
  { bg: '#edf3ec', fg: '#346538' },
  { bg: '#f9ebdd', fg: '#9f5c38' },
  { bg: '#eeeaf7', fg: '#6c58a5' },
]

const presentedCircles = computed(() => [...circles.value].sort((left, right) => right.weeklyUpdateCount - left.weeklyUpdateCount))
const categories = computed(() => ['全部', ...Array.from(new Set((activeDetail.value?.sharedMenus || []).map((menu) => menu.categoryName).filter(Boolean)))])
const visibleMenus = computed(() => {
  const menus = activeDetail.value?.sharedMenus || []
  if (activeCategory.value === '全部') return menus
  return menus.filter((menu) => menu.categoryName === activeCategory.value)
})
const previewMembers = computed<PreviewMember[]>(() =>
  (activeDetail.value?.members || []).slice(0, 3).map((member, index) => ({
    id: member.id,
    initial: getInitial(member),
    avatarTone: index % avatarPalette.length,
  })),
)
const statusText = computed(() => (isLoading.value ? '正在加载搭子圈...' : '还没有搭子圈'))

onMounted(async () => {
  if (!(await requireAuth('circles'))) return
  await loadData()
})

watch(categories, (nextCategories) => {
  if (!nextCategories.includes(activeCategory.value)) activeCategory.value = '全部'
})

watch(activeCircleId, (circleId) => {
  if (circleId) void loadCircleDetail(circleId)
})

async function loadData() {
  isLoading.value = true
  try {
    const { data } = await socialService.getCircles()
    circles.value = data
    activeCircleId.value = presentedCircles.value[0]?.id || ''
    if (!activeCircleId.value) activeDetail.value = null
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '搭子圈加载失败')
  } finally {
    if (!activeCircleId.value) isLoading.value = false
  }
}

async function loadCircleDetail(circleId: string) {
  const requestToken = ++detailRequestToken
  isLoading.value = true
  try {
    const { data } = await socialService.getCircleDetail(circleId)
    if (requestToken !== detailRequestToken) return
    activeDetail.value = data
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '圈子详情加载失败')
  } finally {
    if (requestToken === detailRequestToken) isLoading.value = false
  }
}

function openCircleSwitcher() {
  if (presentedCircles.value.length <= 1) return
  switcherVisible.value = true
}

function closeCircleSwitcher() {
  switcherVisible.value = false
}

function selectCircle(circleId: string) {
  closeCircleSwitcher()
  if (!circleId || circleId === activeCircleId.value) return
  activeCategory.value = '全部'
  activeCircleId.value = circleId
}

function createCircle() {
  push('create-circle')
}

function openMembers() {
  if (!activeCircleId.value) return
  push({ name: 'circle-members', params: { id: activeCircleId.value } })
}

function openDish(id: DishSummary['id']) {
  push({ name: 'dish-detail', params: { id } })
}

function getInitial(member: BuddyCircleMember) {
  return (member.nickname || member.account || '?').trim().slice(0, 1).toUpperCase()
}
</script>

<style>
.circles-page {
  min-height: 100vh;
  padding: 0 0 120px;
  background: #f7f6f3;
}

.circle-nav {
  height: 54px;
  padding: 14px 20px 0;
}

.page-title,
.strong,
.section-title,
.circle-name,
.sheet-title,
.recipe-name {
  color: #151515;
  font-weight: 800;
}

.page-title {
  font-size: 16px;
}

.muted,
.eyebrow {
  color: #787774;
  font-size: 12px;
}

.circle-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 12px 20px 24px;
}

.circle-switch-panel,
.section-block {
  border-radius: 14px;
  background: #fff;
  padding: 14px;
}

.switch-panel-header,
.current-circle-summary,
.section-head,
.member-entry,
.member-avatars,
.sheet-head,
.circle-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.current-circle-summary {
  min-height: 54px;
  margin-top: 10px;
  border-radius: 12px;
  background: #edf3ec;
  padding: 0 12px;
  gap: 12px;
}

.current-circle-copy,
.section-head view,
.recipe-copy,
.circle-option-copy,
.sheet-head view {
  display: flex;
  flex-direction: column;
}

.circle-name {
  font-size: 18px;
}

.circle-switch-button,
.create-button {
  border-radius: 999px;
  background: #fff;
  color: #9f5c38;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 800;
}

.section-link,
.recipe-owner,
.eyebrow {
  color: #9f5c38;
  font-size: 12px;
  font-weight: 800;
}

.member-entry {
  width: 100%;
  min-height: 58px;
  margin-top: 12px;
  border-radius: 14px;
  background: #f7f4ef;
  padding: 0 12px;
}

.avatar-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  margin-right: -8px;
  border: 2px solid #fff;
  border-radius: 999px;
  font-weight: 800;
}

.category-group {
  white-space: nowrap;
  margin: 14px 0;
}

.category-chip {
  display: inline-flex;
  margin-right: 8px;
  border-radius: 999px;
  background: #f5f2ed;
  color: #787774;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 800;
}

.category-chip.active {
  background: #1b3a2d;
  color: #fff;
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.recipe-card {
  overflow: hidden;
  border-radius: 16px;
  background: #f7f4ef;
  text-align: left;
}

.recipe-image {
  width: 100%;
  height: 112px;
}

.recipe-copy {
  gap: 3px;
  padding: 10px;
}

.status-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 24px 20px;
}

.circle-switch-overlay {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: flex-end;
  background: rgba(0, 0, 0, 0.35);
}

.circle-switch-sheet {
  width: 100%;
  border-radius: 24px 24px 0 0;
  background: #fff;
  padding: 12px 20px 24px;
}

.sheet-handle {
  width: 42px;
  height: 4px;
  margin: 0 auto 14px;
  border-radius: 999px;
  background: #ded8d0;
}

.sheet-close {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: #f5efe7;
  font-size: 22px;
}

.circle-option {
  width: 100%;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid var(--line);
  text-align: left;
}

.circle-option-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: #edf3ec;
  color: #346538;
  font-weight: 800;
}

.circle-option-copy {
  flex: 1;
}

.circle-option-state {
  color: #9f5c38;
  font-size: 12px;
  font-weight: 800;
}
</style>
