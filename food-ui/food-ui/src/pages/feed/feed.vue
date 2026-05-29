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

    <AppTabBar active="feed" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppTabBar from '@/components/AppTabBar.vue'
import ActivityCard from '@/components/ActivityCard.vue'
import { SocialService, type FeedItem } from '@/services/social-service'

const router = useRouter()
const socialService = new SocialService()

const feeds = ref<FeedItem[]>([])

onMounted(loadData)

async function loadData() {
  const { data: feedData } = await socialService.getFeed('all')
  feeds.value = feedData
}


function openDish(id: string) {
  router.push({ name: 'dish-detail', params: { id } })
}
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
