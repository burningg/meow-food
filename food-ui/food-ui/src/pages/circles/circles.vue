<template>
  <div ref="pageRef" class="page-shell circles-page">
    <header class="top-nav" data-motion="top-nav">
      <h1>我的搭子圈</h1>
      <button class="nav-button" type="button" @click="createCircle">＋</button>
    </header>

    <section class="create-hint" data-motion="create-hint">
      <h2>还想再建一个新圈子？</h2>
      <div class="create-hint-action">
        <span>菜单共享会更happy</span>
        <button class="create-button" type="button" @click="createCircle">新建圈子</button>
      </div>
    </section>

    <section class="circle-section">
      <div class="section-head" data-motion="section-head">
        <div>
          <small>全部圈子</small>
        </div>
      </div>

      <article
        v-for="circle in presentedCircles"
        :key="circle.id"
        class="circle-card"
        data-reveal-circle
        @click="openCircle(circle.id)"
      >
        <div class="card-head">
          <div class="card-main">
            <div class="avatar-shell" :style="{ background: circle.accent }">
              <span>众</span>
            </div>
            <div class="card-copy">
              <h3>{{ circle.name }}</h3>
              <p class="card-meta">{{ circle.memberCount }} 人 · {{ circle.sharedMenuCount }} 份菜单</p>
            </div>
          </div>
        </div>

        <p class="card-description">{{ circle.description }}</p>

        <div class="card-footer">
          <span>{{ circle.updateLabel }}</span>
          <span class="arrow-shell" :style="{ background: circle.arrowAccent }">↗</span>
        </div>
      </article>
    </section>

    <AppTabBar active="circles" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppTabBar from '@/components/AppTabBar.vue'
import { animateStagger, attachPressAnimations, attachRevealOnScroll, runScopedMotion } from '@/lib/motion'
import { SocialService, type BuddyCircleSummary } from '@/services/social-service'

type CircleCardView = BuddyCircleSummary & {
  accent: string
  arrowAccent: string
  updateLabel: string
}

const router = useRouter()
const socialService = new SocialService()
const circles = ref<BuddyCircleSummary[]>([])
const pageRef = ref<HTMLElement | null>(null)
let cleanupMotion: VoidFunction | undefined
let cleanupCards: VoidFunction | undefined

const accentPairs = [
  { accent: '#E8D8C8', arrowAccent: '#F7EFE6' },
  { accent: '#DDEBDD', arrowAccent: '#EEF3EC' },
  { accent: '#F0D7D0', arrowAccent: '#F7ECEA' },
]

const presentedCircles = computed<CircleCardView[]>(() =>
  [...circles.value]
    .sort((left, right) => right.weeklyUpdateCount - left.weeklyUpdateCount)
    .map((circle, index) => ({
      ...circle,
      ...accentPairs[index % accentPairs.length],
      updateLabel: getUpdateLabel(circle.weeklyUpdateCount, index),
    })),
)

onMounted(async () => {
  setupMotion()
  await loadData()
})

onUnmounted(() => {
  cleanupCards?.()
  cleanupMotion?.()
})

async function loadData() {
  const { data } = await socialService.getCircles()
  circles.value = data
}

async function createCircle() {
  router.push({ name: 'create-circle' })
}

function openCircle(id: string) {
  router.push({ name: 'circle-detail', params: { id } })
}

function getUpdateLabel(weeklyUpdateCount: number, index: number) {
  if (weeklyUpdateCount >= 4) return '今天刚更新'
  if (weeklyUpdateCount >= 2) return '昨天 21:10'
  if (weeklyUpdateCount >= 1) return '周三 19:42'
  return index === 0 ? '最近刚整理' : '本周还没有更新'
}

function setupMotion() {
  if (!pageRef.value) return
  cleanupMotion = runScopedMotion(pageRef.value, ({ reducedMotion = false }) => {
    animateStagger(
      pageRef.value!.querySelectorAll('[data-motion="top-nav"], [data-motion="create-hint"], [data-motion="section-head"]'),
      {
        reducedMotion,
        y: 20,
        stagger: 0.07,
        duration: 0.34,
      },
    )

    return attachPressAnimations(pageRef.value!, '.nav-button, .create-button, .circle-card', { activeScale: 0.97 })
  })
}

async function refreshCircleMotion() {
  await nextTick()
  cleanupCards?.()
  if (!pageRef.value) return

  cleanupCards = runScopedMotion(pageRef.value, ({ reducedMotion = false }) => {
    const cards = pageRef.value!.querySelectorAll('[data-reveal-circle]')
    if (!cards.length) return

    const firstBatch = Array.from(cards).slice(0, 3)
    const rest = Array.from(cards).slice(3)

    animateStagger(firstBatch, {
      reducedMotion,
      y: 22,
      stagger: 0.08,
      duration: 0.34,
      delay: 0.08,
    })

    if (!rest.length) return
    return attachRevealOnScroll(rest, {
      reducedMotion,
      y: 24,
      stagger: 0.09,
    })
  })
}

watch(presentedCircles, () => {
  void refreshCircleMotion()
})
</script>

<style scoped>
.circles-page {
  padding: 12px 16px 120px;
  background: #f7f6f3;
}

.top-nav,
.create-hint-action,
.section-head,
.card-head,
.card-main,
.card-footer {
  display: flex;
}

.top-nav,
.create-hint-action,
.section-head,
.card-footer {
  align-items: center;
  justify-content: space-between;
}

.top-nav {
  margin: 2px 0 12px;
}

.nav-button {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 999px;
  background: #fff;
  color: #151515;
  font-size: 1.2rem;
  line-height: 1;
}

h1,
h2,
h3,
p {
  margin: 0;
}

h1 {
  color: #151515;
  font-size: var(--title-sm);
  font-weight: 600;
}

.create-hint {
  background: #fbf8f4;
  border-radius: 12px;
  padding: 18px;
  margin-bottom: 18px;
}

.create-hint h2,
.circle-card h3 {
  color: #151515;
  font-size: var(--text-md);
  font-weight: 600;
}

.create-hint p,
.card-description {
  color: #2f3437;
  font-size: var(--text-sm);
  line-height: 1.5;
}

.create-hint p {
  margin-top: 10px;
}

.create-hint-action {
  margin-top: 10px;
  gap: 12px;
}

.create-hint-action span,
.section-head small,
.card-meta,
.card-footer span:first-child {
  color: #787774;
  font-size: var(--text-xs);
  font-weight: 500;
}

.create-button,
.filter-chip {
  border: none;
  border-radius: 999px;
  font-size: var(--text-xs);
  font-weight: 600;
}

.create-button {
  background: #f5ede7;
  color: #9f5c38;
  padding: 10px 14px;
  flex-shrink: 0;
}

.circle-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-head {
  margin-bottom: 2px;
}

.section-head h2 {
  color: #151515;
  font-family: var(--font-serif);
  font-size: var(--title-lg);
  font-weight: 600;
  line-height: 1.2;
  margin-top: 4px;
}

.filter-chip {
  background: #f3eee7;
  color: #9f5c38;
  padding: 6px 10px;
}

.circle-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
}

.card-main {
  align-items: center;
  gap: 12px;
}

.avatar-shell,
.arrow-shell {
  display: grid;
  place-items: center;
  color: #151515;
}

.avatar-shell {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  font-size: 1rem;
  font-weight: 600;
}

.card-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-description {
  margin: 14px 0;
}

.arrow-shell {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  color: #9f5c38;
  font-size: var(--text-md);
}
</style>
