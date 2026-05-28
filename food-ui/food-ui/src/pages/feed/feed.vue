<template>
  <div class="page-shell feed-page">
    <header class="top-nav">
      <button class="back" type="button" @click="router.back()">‹</button>
      <h1>好友动态</h1>
      <button class="back" type="button" @click="loadData">↻</button>
    </header>



    <section class="list-section">
      <ActivityCard v-for="item in feeds" :key="item.id" :item="item" @open="openDish(item.dishId)" />
    </section>

    <section class="section">
      <div class="section-head">
        <div>
          <h2>可访问菜单</h2>
        </div>
      </div>
      <div class="accessible-row">
        <article v-for="menu in accessibleMenus" :key="menu.id" class="accessible-card" @click="openDish(menu.id)">
          <img :src="menu.image" :alt="menu.name" />
          <div>
            <strong>{{ menu.name }}</strong>
            <p>{{ menu.ownerNickname }}</p>
          </div>
        </article>
      </div>
    </section>

    <AppTabBar active="feed" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppTabBar from '@/components/AppTabBar.vue'
import ActivityCard from '@/components/ActivityCard.vue'
import { SocialService, type FeedItem } from '@/services/social-service'
import type { DishSummary } from '@/services/food-service'

const router = useRouter()
const socialService = new SocialService()

const filter = ref<'all' | 'new' | 'circle'>('all')
const feeds = ref<FeedItem[]>([])
const accessibleMenus = ref<DishSummary[]>([])

const filters = [
  { value: 'all', label: '全部' },
  { value: 'new', label: '上新' },
  { value: 'circle', label: '圈内分享' },
] as const

onMounted(loadData)

async function loadData() {
  const [{ data: feedData }, { data: menuData }] = await Promise.all([
    socialService.getFeed(filter.value),
    socialService.getAccessibleMenus(),
  ])
  feeds.value = feedData
  accessibleMenus.value = menuData.menus
}


function openDish(id: string) {
  router.push({ name: 'dish-detail', params: { id } })
}
</script>

<style scoped>
.feed-page {
  padding-bottom: 120px;
}

.top-nav,
.section-head,
.accessible-card {
  display: flex;
}

.top-nav,
.section-head {
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

.intro-card,
.section {
  background: #fff;
  border-radius: 22px;
  padding: 18px;
  box-shadow: 0 12px 26px rgba(27, 58, 45, 0.06);
}

.intro-card small,
.intro-card p,
.section small,
.accessible-card p {
  color: var(--text-muted);
}

.intro-card h2,
.section h2 {
  font-family: 'Playfair Display', serif;
  margin-top: 8px;
}

.filters,
.accessible-row {
  display: flex;
  gap: 10px;
}

.filters {
  margin-top: 16px;
}

.filter {
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 999px;
  padding: 10px 16px;
}

.filter.active {
  background: var(--text-main);
  color: #fff;
  border-color: var(--text-main);
}

.list-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 18px 0;
}

.section {
  margin-top: 12px;
}

.section-head {
  margin-bottom: 14px;
}

.accessible-row {
  overflow-x: auto;
}

.accessible-card {
  min-width: 172px;
  background: #fbfbfa;
  border-radius: 16px;
  padding: 10px;
  gap: 10px;
  align-items: center;
}

.accessible-card img {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  object-fit: cover;
}

.accessible-card strong,
.accessible-card p {
  display: block;
  margin: 0;
}
</style>
