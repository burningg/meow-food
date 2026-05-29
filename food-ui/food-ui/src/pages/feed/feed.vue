<template>
  <div ref="pageRef" class="page-shell feed-page">
    <header class="top-nav" data-motion="top-nav">
      <h1>好友动态</h1>
      <button class="back" type="button" @click="loadData">↻</button>
    </header>

    <section class="list-section">
      <ActivityCard v-for="item in feeds" :key="item.id" :item="item" @open="openDish(item.dishId)" />
    </section>

    <AppTabBar active="feed" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppTabBar from '@/components/AppTabBar.vue'
import ActivityCard from '@/components/ActivityCard.vue'
import { animateStagger, attachPressAnimations, attachRevealOnScroll, runScopedMotion } from '@/lib/motion'
import { SocialService, type FeedItem } from '@/services/social-service'

const router = useRouter()
const socialService = new SocialService()

const feeds = ref<FeedItem[]>([])
const pageRef = ref<HTMLElement | null>(null)
let cleanupMotion: VoidFunction | undefined
let cleanupListMotion: VoidFunction | undefined

onMounted(async () => {
  setupMotion()
  await loadData()
})

onUnmounted(() => {
  cleanupListMotion?.()
  cleanupMotion?.()
})

async function loadData() {
  const { data: feedData } = await socialService.getFeed('all')
  feeds.value = feedData
}


function openDish(id: string) {
  router.push({ name: 'dish-detail', params: { id } })
}

function setupMotion() {
  if (!pageRef.value) return
  cleanupMotion = runScopedMotion(pageRef.value, ({ reducedMotion = false }) => {
    animateStagger(pageRef.value!.querySelectorAll('[data-motion="top-nav"]'), {
      reducedMotion,
      y: 18,
      duration: 0.3,
    })

    return attachPressAnimations(pageRef.value!, '.back, .activity-card', { activeScale: 0.97 })
  })
}

async function refreshFeedMotion() {
  await nextTick()
  cleanupListMotion?.()
  if (!pageRef.value) return

  cleanupListMotion = runScopedMotion(pageRef.value, ({ reducedMotion = false }) => {
    const cards = pageRef.value!.querySelectorAll('.activity-card')
    if (!cards.length) return

    const firstBatch = Array.from(cards).slice(0, 4)
    const rest = Array.from(cards).slice(4)

    animateStagger(firstBatch, {
      reducedMotion,
      y: 22,
      stagger: 0.07,
      duration: 0.34,
      delay: 0.06,
    })

    if (!rest.length) return
    return attachRevealOnScroll(rest, {
      reducedMotion,
      y: 24,
      stagger: 0.08,
    })
  })
}

watch(feeds, () => {
  void refreshFeedMotion()
})
</script>

<style scoped>
.feed-page {
  padding-bottom: 120px;
}

.top-nav {
  display: flex;
}

.top-nav {
  align-items: center;
  justify-content: space-between;
}

.top-nav {
  margin-bottom: 16px;
}

.back {
  border: none;
  background: #fff;
  width: 36px;
  height: 36px;
  border-radius: 999px;
}

h1,
h2 {
  margin: 0;
  color: var(--text-main);
}

.list-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 18px 0;
}
</style>
