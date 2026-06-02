<template>
  <div ref="pageRef" class="page-shell home-page">
    <section class="hero" data-motion="hero">
      <div>
        <p class="eyebrow">下午好，{{ displayName }}</p>
      </div>
      <div class="avatar" @click="goToProfile">{{ displayName.slice(0, 1) }}</div>
    </section>

    <section class="search-bar" data-motion="search">
      <span class="search-icon">⌕</span>
      <span>搜索菜谱...</span>
    </section>

    <section class="category-strip" data-motion="categories">
      <button
        :class="['category-pill', { active: !selectedCategoryId }]"
        type="button"
        @click="selectCategory('')"
      >
        全部
      </button>
      <button
        v-for="category in homeData.categories"
        :key="category.id"
        :class="['category-pill', { active: category.id === selectedCategoryId }]"
        type="button"
        @click="selectCategory(category.id)"
      >
        {{ category.name }}
      </button>
    </section>

    <section class="section-block" data-motion="section">
      <div class="section-head" data-motion="section-head">
        <div>
          <h2>我的菜品</h2>
        </div>
      </div>

      <div v-if="filteredDishes.length" class="recent-row">
        <article
          v-for="dish in filteredDishes"
          :key="dish.id"
          class="recent-card"
          data-reveal-card
          @click="goToDetail(dish.id)"
        >
          <img :src="dish.image" :alt="dish.name" />
          <div class="recent-copy">
            <h3>{{ dish.name }}</h3>
            <p>{{ dish.categoryName }}</p>
          </div>
        </article>
      </div>

      <div v-else class="empty-state" data-motion="empty">
        <p>你还没有添加自己的菜品，先从第一道开始吧。</p>
        <button class="primary-button small" type="button" @click="goToAdd">添加菜谱</button>
      </div>
    </section>

    <button ref="floatingAddRef" class="floating-add" type="button" @click="goToAdd">＋</button>

    <AppTabBar active="home" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import AppTabBar from '@/components/AppTabBar.vue'
import { animatePulse, animateStagger, attachPressAnimations, runScopedMotion } from '@/lib/motion'
import { FoodService, type HomeResponse, type DishSummary } from '@/services/food-service'
import { useAuthStore } from '@/stores/auth-store'

const router = useRouter()
const foodService = new FoodService()
const authStore = useAuthStore()
const pageRef = ref<HTMLElement | null>(null)
const floatingAddRef = ref<HTMLElement | null>(null)
let cleanupMotion: VoidFunction | undefined
let cleanupCards: VoidFunction | undefined

const homeData = ref<HomeResponse>({
  categories: [],
  featuredDishes: [],
  recentDishes: [],
  featuredByCategory: [],
})
const selectedCategoryId = ref('')
const displayName = computed(() => authStore.user?.nickname ?? 'meow')
const filteredDishes = computed(() => {
  if (!selectedCategoryId.value) {
    return homeData.value.recentDishes
  }
  return homeData.value.recentDishes.filter((dish: DishSummary) => dish.categoryId === selectedCategoryId.value)
})

onMounted(async () => {
  setupMotion()
  await loadHome()
})

onUnmounted(() => {
  cleanupCards?.()
  cleanupMotion?.()
})

async function loadHome() {
  try {
    const { data } = await foodService.getHomeData()
    homeData.value = data
  } catch (error) {
    Message.error('首页数据加载失败')
  }
}

function goToDetail(id: string) {
  router.push({ name: 'dish-detail', params: { id } })
}

function goToAdd() {
  router.push({ name: 'add-dish' })
}

function goToProfile() {
  router.push({ name: 'profile' })
}

function selectCategory(categoryId: string) {
  selectedCategoryId.value = categoryId
}

function setupMotion() {
  if (!pageRef.value) return
  cleanupMotion = runScopedMotion(pageRef.value, ({ reducedMotion = false }) => {
    animateStagger(pageRef.value!.querySelectorAll('[data-motion="hero"], [data-motion="search"], [data-motion="categories"], [data-motion="section-head"]'), {
      reducedMotion,
      y: 20,
      stagger: 0.07,
      duration: 0.34,
    })

    const releaseButtons = attachPressAnimations(
      pageRef.value!,
      '.category-pill, .avatar, .recent-card, .primary-button.small',
      { activeScale: 0.97 },
    )
    const releaseFloat = floatingAddRef.value
      ? attachPressAnimations(pageRef.value!, '.floating-add', { activeScale: 0.92 })
      : () => undefined
    const stopPulse = floatingAddRef.value ? animatePulse(floatingAddRef.value, { reducedMotion }) : () => undefined

    return () => {
      releaseButtons()
      releaseFloat()
      stopPulse()
    }
  })
}

async function refreshDishMotion() {
  await nextTick()
  cleanupCards?.()
  if (!pageRef.value) return

  cleanupCards = runScopedMotion(pageRef.value, ({ reducedMotion = false }) => {
    const cards = pageRef.value!.querySelectorAll('[data-reveal-card]')
    if (cards.length) {
      animateStagger(cards, {
        reducedMotion,
        y: 24,
        stagger: 0.06,
        duration: 0.34,
      })
      return
    }

    const emptyState = pageRef.value!.querySelectorAll('[data-motion="empty"]')
    animateStagger(emptyState, {
      reducedMotion,
      y: 16,
      duration: 0.28,
    })
  })
}

watch(filteredDishes, () => {
  void refreshDishMotion()
})
</script>

<style scoped>
.home-page {
  padding-bottom: 120px;
}

.hero,
.section-head,
.recent-copy,
.search-bar,
.empty-state {
  display: flex;
}

.hero,
.section-head {
  align-items: center;
  justify-content: space-between;
}

.hero {
  margin-bottom: 18px;
}

.eyebrow {
  color: #8b8b8b;
  font-size: var(--text-sm);
  margin-bottom: 2px;
}

h1,
h2,
h3 {
  color: #1b3a2d;
  margin: 0;
}

.avatar {
  width: 42px;
  height: 42px;
  border-radius: 999px;
  background: #7a9e7e;
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 700;
}

.search-bar {
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #eeebe6;
  color: #8b8b8b;
  margin-bottom: 16px;
}

.search-icon {
  font-size: 1rem;
}

.category-strip {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 8px;
  margin: 0 -4px 18px;
}

.category-strip::-webkit-scrollbar {
  display: none;
}

.category-pill {
  border: none;
  border-radius: 999px;
  background: #fff;
  color: #3d3d3d;
  padding: 10px 16px;
  white-space: nowrap;
  cursor: pointer;
  box-shadow: 0 8px 22px rgba(27, 58, 45, 0.08);
  font-size: var(--text-sm);
  font-weight: 600;
}

.category-pill.active {
  background: #1b3a2d;
  color: #fff;
}

.section-block {
  margin-bottom: 24px;
}

.section-head {
  margin-bottom: 12px;
}

.section-head h2 {
  font-family: var(--font-serif);
  font-size: var(--title-md);
  font-weight: 600;
  line-height: 1.25;
}

.recent-card {
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.recent-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.recent-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.recent-card {
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(27, 58, 45, 0.08);
}

.recent-card img {
  height: 126px;
}

.recent-copy {
  flex-direction: column;
  padding: 10px;
}

.recent-copy h3 {
  font-size: var(--text-md);
  font-weight: 600;
  line-height: 1.4;
}

.recent-copy p {
  color: #8b8b8b;
  font-size: var(--text-xs);
  margin-top: 4px;
}

.empty-state {
  flex-direction: column;
  gap: 12px;
  align-items: flex-start;
  padding: 18px;
  border-radius: 18px;
  background: #fff;
}

.empty-state p {
  color: #6c746f;
  font-size: var(--text-md);
}

.primary-button.small {
  padding: 10px 14px;
}

.floating-add {
  position: fixed;
  right: calc(50% - min(195px, 50vw) + 12px);
  bottom: 112px;
  width: 54px;
  height: 54px;
  border: none;
  border-radius: 999px;
  background: #c4704b;
  color: #fff;
  font-size: 2rem;
  line-height: 1;
  box-shadow: 0 18px 30px rgba(196, 112, 75, 0.3);
}
</style>
