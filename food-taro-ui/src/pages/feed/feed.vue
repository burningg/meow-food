<template>
  <view class="feed-page-root">
    <PullRefreshPage @refresh="loadData">
      <view class="page-shell feed-page">
        <header class="top-nav">
          <view class="nav-placeholder"></view>
          <text class="page-title">好友动态</text>
          <view class="nav-placeholder"></view>
        </header>

        <section class="list-section">
          <ActivityCard v-for="item in feeds" :key="item.id" :item="item" @open="openDish(item.dishId)" />
          <view v-if="!feeds.length" class="empty-card">暂时还没有好友动态</view>
        </section>
      </view>
    </PullRefreshPage>

    <AppTabBar active="feed" />
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppTabBar from '@/components/AppTabBar.vue'
import ActivityCard from '@/components/ActivityCard.vue'
import PullRefreshPage from '@/components/PullRefreshPage.vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { push } from '@/lib/navigation'
import { SocialService, type FeedItem } from '@/services/social-service'

const socialService = new SocialService()
const feeds = ref<FeedItem[]>([])

onMounted(async () => {
  if (!(await requireAuth('feed'))) return
  await loadData()
})

async function loadData() {
  try {
    const { data } = await socialService.getFeed('all')
    feeds.value = data
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '动态加载失败')
  }
}

function openDish(id: string) {
  push({ name: 'dish-detail', params: { id } })
}
</script>

<style>
.feed-page {
  padding-bottom: 120px;
}

.top-nav {
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

.list-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 18px 0;
}
</style>
