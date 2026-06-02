<template>
  <div ref="pageRef" class="page-shell circles-page">
    <header class="top-nav" data-motion="top-nav">
      <button class="nav-button" type="button" aria-label="返回" @click="goBack">
        <span class="icon-back">‹</span>
      </button>
      <h1>美食搭子</h1>
      <button class="nav-button" type="button" aria-label="新建圈子" @click="createCircle">
        <span class="icon-plus">＋</span>
      </button>
    </header>

    <template v-if="activeDetail">
      <main class="circle-content">
        <section class="circle-switch-panel" data-motion="circle-switch-panel">
          <div class="switch-panel-header">
            <strong>圈子</strong>
            <span>当前 · {{ activeDetail.circle.name }}</span>
          </div>

          <div class="current-circle-summary">
            <div class="current-circle-copy">
              <h2>{{ activeDetail.circle.name }}</h2>
              <p>
                {{ activeDetail.stats.memberCount }}人 ·
                {{ activeDetail.stats.sharedMenuCount }}菜谱 · 正在使用
              </p>
            </div>
            <button
              class="circle-switch-button"
              type="button"
              :disabled="presentedCircles.length <= 1"
              @click="openCircleSwitcher"
            >
              切换
              <span aria-hidden="true">⌄</span>
            </button>
          </div>
        </section>

        <section class="section-block" data-motion="members-section">
          <div class="section-head">
            <h3>搭子圈成员</h3>
            <span class="section-link">共 {{ activeDetail.stats.memberCount }} 人</span>
          </div>

          <button class="member-entry" type="button" @click="openMembers">
            <div class="member-avatars">
              <div
                v-for="member in previewMembers"
                :key="member.id"
                class="avatar-badge"
                :style="{
                  background: avatarPalette[member.avatarTone].bg,
                  color: avatarPalette[member.avatarTone].fg,
                }"
              >
                {{ member.initial }}
              </div>
            </div>
            <span class="member-entry-arrow">›</span>
          </button>
        </section>

        <section class="section-block recipes-section" data-motion="recipes-section">
          <div class="section-head">
            <div>
              <h3>圈内菜谱</h3>
              <p>按分类快速看圈内最近共享的菜</p>
            </div>
            <span class="section-link">最近更新</span>
          </div>

          <div class="category-group">
            <button
              v-for="category in categories"
              :key="category"
              class="category-chip"
              :class="{ active: category === activeCategory }"
              type="button"
              @click="activeCategory = category"
            >
              {{ category }}
            </button>
          </div>

          <div v-if="visibleMenus.length" class="recipe-grid">
            <article
              v-for="menu in visibleMenus"
              :key="menu.id"
              class="recipe-card"
              @click="openDish(menu.id)"
            >
              <img class="recipe-image" :src="menu.image" :alt="menu.name" />
              <div class="recipe-copy">
                <h4>{{ menu.name }}</h4>
                <p class="recipe-category">{{ menu.categoryName }}</p>
                <p class="recipe-owner">{{ menu.ownerNickname }}创建</p>
              </div>
            </article>
          </div>

          <div v-else class="empty-card">
            <p>这个分类还没有共享菜谱</p>
          </div>
        </section>
      </main>
    </template>

    <section v-else class="status-card" data-motion="status-card">
      <p>{{ statusText }}</p>
      <button v-if="!isLoading" class="create-button" type="button" @click="createCircle">
        新建圈子
      </button>
    </section>

    <div v-if="switcherVisible" class="circle-switch-overlay" @click.self="closeCircleSwitcher">
      <section class="circle-switch-sheet" aria-label="切换搭子圈">
        <div class="sheet-handle"></div>
        <div class="sheet-head">
          <div>
            <small>切换圈子</small>
            <h2>选择搭子圈</h2>
          </div>
          <button class="sheet-close" type="button" aria-label="关闭" @click="closeCircleSwitcher">
            ×
          </button>
        </div>

        <div class="circle-option-list">
          <button
            v-for="circle in presentedCircles"
            :key="circle.id"
            class="circle-option"
            :class="{ active: circle.id === activeCircleId }"
            type="button"
            @click="selectCircle(circle.id)"
          >
            <span class="circle-option-mark">{{ circle.name.slice(0, 1) }}</span>
            <span class="circle-option-copy">
              <strong>{{ circle.name }}</strong>
              <small>{{ circle.memberCount }}人 · {{ circle.sharedMenuCount }}菜谱</small>
            </span>
            <span class="circle-option-state">
              {{ circle.id === activeCircleId ? '当前' : '切换' }}
            </span>
          </button>
        </div>
      </section>
    </div>

    <AppTabBar active="circles" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppTabBar from '@/components/AppTabBar.vue'
import { animateStagger, attachPressAnimations, runScopedMotion } from '@/lib/motion'
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

const router = useRouter()
const socialService = new SocialService()
const circles = ref<BuddyCircleSummary[]>([])
const activeDetail = ref<BuddyCircleDetail | null>(null)
const activeCircleId = ref('')
const activeCategory = ref('全部')
const isLoading = ref(true)
const switcherVisible = ref(false)
const pageRef = ref<HTMLElement | null>(null)
let cleanupMotion: VoidFunction | undefined
let detailRequestToken = 0

const avatarPalette = [
  { bg: '#edf3ec', fg: '#346538' },
  { bg: '#f9ebdd', fg: '#9f5c38' },
  { bg: '#eeeaf7', fg: '#6c58a5' },
]

const presentedCircles = computed(() =>
  [...circles.value].sort((left, right) => right.weeklyUpdateCount - left.weeklyUpdateCount),
)

const categories = computed(() => {
  const names = Array.from(
    new Set(
      (activeDetail.value?.sharedMenus || []).map((menu) => menu.categoryName).filter(Boolean),
    ),
  )
  return ['全部', ...names]
})

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

const statusText = computed(() => (isLoading.value ? '正在加载搭子圈…' : '还没有搭子圈'))

onMounted(async () => {
  setupMotion()
  await loadData()
})

onUnmounted(() => {
  cleanupMotion?.()
})

watch(categories, (nextCategories) => {
  if (!nextCategories.includes(activeCategory.value)) {
    activeCategory.value = '全部'
  }
})

watch(activeCircleId, (circleId) => {
  if (!circleId) return
  void loadCircleDetail(circleId)
})

async function loadData() {
  isLoading.value = true
  try {
    const { data } = await socialService.getCircles()
    circles.value = data
    activeCircleId.value = presentedCircles.value[0]?.id || ''
    if (!activeCircleId.value) {
      activeDetail.value = null
      isLoading.value = false
    }
  } finally {
    if (!activeCircleId.value) {
      isLoading.value = false
    }
  }
}

async function loadCircleDetail(circleId: string) {
  const requestToken = ++detailRequestToken
  isLoading.value = true
  try {
    const { data } = await socialService.getCircleDetail(circleId)
    if (requestToken !== detailRequestToken) return
    activeDetail.value = data
  } finally {
    if (requestToken !== detailRequestToken) return
    isLoading.value = false
    void refreshMotion()
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
  router.push({ name: 'create-circle' })
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push({ name: 'home' })
}

function openMembers() {
  if (!activeCircleId.value) return
  router.push({ name: 'circle-members', params: { id: activeCircleId.value } })
}

function openDish(id: DishSummary['id']) {
  router.push({ name: 'dish-detail', params: { id } })
}

function getInitial(member: BuddyCircleMember) {
  return (member.nickname || member.account || '?').trim().slice(0, 1).toUpperCase()
}

function setupMotion() {
  if (!pageRef.value) return
  cleanupMotion = runScopedMotion(pageRef.value, ({ reducedMotion = false }) => {
    animateStagger(pageRef.value!.querySelectorAll('[data-motion]'), {
      reducedMotion,
      y: 18,
      stagger: 0.06,
      duration: 0.32,
    })

    return attachPressAnimations(
      pageRef.value!,
      '.nav-button, .circle-switch-button, .member-entry, .category-chip, .recipe-card, .create-button, .circle-option, .sheet-close',
      { activeScale: 0.97 },
    )
  })
}

async function refreshMotion() {
  await nextTick()
  cleanupMotion?.()
  setupMotion()
}
</script>

<style scoped>
.circles-page {
  min-height: 100vh;
  padding: 0 0 120px;
  background: #f7f6f3;
}

.top-nav,
.switch-panel-header,
.current-circle-summary,
.section-head,
.member-entry,
.member-avatars,
.recipe-grid,
.sheet-head,
.circle-option {
  display: flex;
}

.top-nav,
.switch-panel-header,
.current-circle-summary,
.section-head,
.member-entry,
.sheet-head,
.circle-option {
  align-items: center;
  justify-content: space-between;
}

.top-nav {
  height: 54px;
  padding: 14px 20px 0;
}

.nav-button {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 999px;
  background: #fff;
  color: #151515;
  display: grid;
  place-items: center;
  box-shadow: 0 4px 14px rgba(27, 58, 45, 0.05);
}

.icon-back,
.icon-plus,
.member-entry-arrow {
  line-height: 1;
}

.icon-back {
  font-size: 1.3rem;
}

.icon-plus {
  font-size: 1.15rem;
}

h1,
h2,
h3,
h4,
p {
  margin: 0;
}

h1 {
  color: #151515;
  font-size: 1rem;
  font-weight: 600;
}

.circle-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 12px 20px 24px;
}

.circle-switch-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  border-radius: 14px;
  background: #fff;
  padding: 12px;
}

.switch-panel-header {
  gap: 12px;
}

.switch-panel-header strong {
  color: #151515;
  font-size: 0.8125rem;
  font-weight: 600;
}

.switch-panel-header span {
  min-width: 0;
  overflow: hidden;
  color: #787774;
  font-size: 0.6875rem;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.current-circle-summary {
  min-height: 54px;
  border-radius: 12px;
  background: #edf3ec;
  padding: 0 12px;
  gap: 12px;
}

.current-circle-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

h2 {
  overflow: hidden;
  color: #151515;
  font-size: 0.9375rem;
  font-weight: 700;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.current-circle-copy p {
  overflow: hidden;
  color: #346538;
  font-size: 0.6875rem;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.circle-switch-button {
  height: 28px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: none;
  border-radius: 999px;
  background: #fff;
  color: #346538;
  padding: 0 10px;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}

.circle-switch-button:disabled {
  cursor: default;
  opacity: 0.68;
}

.section-block {
  width: 100%;
}

.section-head {
  align-items: flex-end;
  gap: 12px;
}

h3 {
  color: #1b3a2d;
  font-family: var(--font-serif);
  font-size: 1.18rem;
  font-weight: 600;
  line-height: 1.25;
}

.section-head p,
.recipe-category,
.status-card p,
.empty-card p {
  color: #8b8b8b;
  font-size: 0.75rem;
}

.section-head p {
  margin-top: 4px;
}

.section-link,
.recipe-owner {
  color: #9f5c38;
  font-size: 0.75rem;
  font-weight: 600;
}

.member-entry {
  width: 100%;
  margin-top: 12px;
  padding: 16px;
  border: none;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 10px 22px rgba(27, 58, 45, 0.07);
}

.member-avatars {
  align-items: center;
  gap: 8px;
}

.avatar-badge {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-size: 0.9375rem;
  font-weight: 600;
}

.member-entry-arrow {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: #f5efe7;
  color: #9f5c38;
  display: grid;
  place-items: center;
  font-size: 1rem;
}

.recipes-section {
  padding-top: 4px;
}

.category-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.category-chip {
  border: none;
  border-radius: 999px;
  background: #fff;
  color: #3d3d3d;
  padding: 8px 16px;
  font-size: 0.8125rem;
  font-weight: 600;
}

.category-chip.active {
  background: #1b3a2d;
  color: #fff;
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.recipe-card,
.empty-card,
.status-card {
  background: #fff;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 10px 22px rgba(27, 58, 45, 0.07);
}

.recipe-card {
  min-width: 0;
}

.recipe-image {
  width: 100%;
  height: 118px;
  object-fit: cover;
  display: block;
}

.recipe-copy {
  padding: 10px;
}

h4 {
  overflow: hidden;
  color: #1b3a2d;
  font-size: 0.9375rem;
  font-weight: 600;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recipe-category,
.recipe-owner {
  margin-top: 5px;
}

.empty-card,
.status-card {
  display: grid;
  place-items: center;
  min-height: 120px;
}

.status-card {
  margin: 12px 20px 0;
  padding: 18px;
  gap: 12px;
  text-align: center;
}

.create-button {
  border: none;
  border-radius: 999px;
  background: #9f5c38;
  color: #fff;
  padding: 10px 14px;
  font-size: 0.75rem;
  font-weight: 600;
}

.circle-switch-overlay {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(21, 21, 21, 0.38);
}

.circle-switch-sheet {
  width: min(390px, 100vw);
  max-height: min(76vh, 560px);
  overflow: hidden;
  border-radius: 24px 24px 0 0;
  background: #fff;
  padding: 16px 20px 22px;
  box-shadow: 0 -18px 40px rgba(27, 58, 45, 0.16);
}

.sheet-handle {
  width: 46px;
  height: 4px;
  margin: 0 auto 16px;
  border-radius: 999px;
  background: #ddd2c7;
}

.sheet-head {
  gap: 16px;
  margin-bottom: 14px;
}

.sheet-head small {
  display: block;
  color: #9f5c38;
  font-size: 0.6875rem;
  font-weight: 600;
}

.sheet-head h2 {
  margin-top: 4px;
  font-family: var(--font-serif);
  font-size: 1.18rem;
  font-weight: 600;
}

.sheet-close {
  width: 36px;
  height: 36px;
  flex: none;
  border: none;
  border-radius: 999px;
  background: #f5efe7;
  color: #151515;
  font-size: 1.125rem;
  line-height: 1;
}

.circle-option-list {
  max-height: calc(min(76vh, 560px) - 110px);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 2px;
}

.circle-option {
  width: 100%;
  gap: 12px;
  border: none;
  border-radius: 16px;
  background: #f8f3ec;
  padding: 12px;
  text-align: left;
}

.circle-option.active {
  background: #edf3ec;
}

.circle-option-mark {
  width: 42px;
  height: 42px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: #fff;
  color: #1b3a2d;
  font-size: 0.9375rem;
  font-weight: 700;
}

.circle-option-copy {
  flex: 1;
  min-width: 0;
}

.circle-option-copy strong,
.circle-option-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.circle-option-copy strong {
  color: #151515;
  font-size: 0.875rem;
  font-weight: 600;
}

.circle-option-copy small {
  margin-top: 3px;
  color: #787774;
  font-size: 0.75rem;
}

.circle-option-state {
  flex: none;
  color: #9f5c38;
  font-size: 0.75rem;
  font-weight: 600;
}

</style>
