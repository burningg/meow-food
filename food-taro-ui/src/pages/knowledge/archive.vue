<template>
  <view class="page-shell knowledge-archive-page">
    <view class="top-nav">
      <button class="nav-button" @tap="goBack('home')">‹</button>
      <text class="nav-title">知识分享</text>
      <view class="placeholder nav-button"></view>
    </view>

    <section class="archive-hero">
      <text class="archive-kicker">饮食知识</text>
      <text class="archive-title">本周知识档案</text>
    </section>

    <view v-if="loading && !articles.length" class="status-card">正在加载知识内容</view>

    <view v-else class="knowledge-list">
      <button
        v-for="article in articles"
        :key="article.id"
        class="knowledge-item-card"
        @tap="goToArticle(article.id)"
      >
        <image
          v-if="article.imageUrl"
          class="knowledge-item-image"
          :src="article.imageUrl"
          mode="aspectFill"
        />
        <view class="knowledge-item-head">
          <text class="knowledge-tag">{{ article.category }}</text>
          <text class="knowledge-time">{{ formatKnowledgeDate(article.publishedAt) }}</text>
        </view>
        <text class="knowledge-item-title">{{ article.title }}</text>
        <text class="knowledge-item-preview">{{ article.bodyPreview }}</text>
      </button>

      <view v-if="loadingMore" class="load-more-text">继续加载中</view>
      <view v-else-if="articles.length && !hasMore" class="load-more-text">已经到底啦</view>
      <view v-else-if="!articles.length" class="empty-card">暂无知识内容</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useReachBottom } from '@tarojs/taro'
import { Message } from '@/lib/feedback'
import { goBack, push } from '@/lib/navigation'
import { KnowledgeService, type KnowledgeArticleSummary } from '@/services/knowledge-service'

const PAGE_SIZE = 20
const knowledgeService = new KnowledgeService()

const articles = ref<KnowledgeArticleSummary[]>([])
const page = ref(1)
const hasMore = ref(true)
const loading = ref(false)
const loadingMore = ref(false)

onMounted(() => {
  void loadFirstPage()
})

useReachBottom(() => {
  void loadMore()
})

async function loadFirstPage() {
  loading.value = true
  page.value = 1
  hasMore.value = true
  try {
    const { data } = await knowledgeService.queryHistory({ page: page.value, size: PAGE_SIZE })
    articles.value = data.items
    hasMore.value = data.hasMore
    page.value = data.page + 1
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '知识加载失败')
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (loading.value || loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    const { data } = await knowledgeService.queryHistory({ page: page.value, size: PAGE_SIZE })
    articles.value = [...articles.value, ...data.items]
    hasMore.value = data.hasMore
    page.value = data.page + 1
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '更多知识加载失败')
  } finally {
    loadingMore.value = false
  }
}

function goToArticle(id: string) {
  push({ name: 'knowledge-detail', params: { id } })
}

function formatKnowledgeDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getMonth() + 1}-${date.getDate()}`
}
</script>

<style>
.knowledge-archive-page {
  padding-bottom: 44px;
}

.nav-title,
.archive-title,
.knowledge-item-title {
  color: var(--text-main);
  font-weight: 800;
}

.nav-title {
  font-size: var(--title-sm);
}

.archive-hero {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 18px;
  border-radius: 24px;
  background: #fffdf7;
  padding: 20px;
  box-shadow: var(--shadow);
}

.archive-kicker,
.knowledge-time,
.knowledge-item-preview,
.load-more-text {
  color: var(--text-muted);
  font-size: var(--text-sm);
}

.archive-title {
  font-size: 28px;
  line-height: 1.14;
}

.knowledge-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.knowledge-item-card {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 8px;
  border-radius: 20px;
  background: #fff;
  padding: 16px;
  text-align: left;
  box-shadow: inset 0 0 0 1px #edf0e8, var(--shadow);
}

.knowledge-item-image {
  width: 100%;
  height: 132px;
  border-radius: 16px;
  background: #e8e5e0;
}

.knowledge-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.knowledge-tag {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 26px;
  border-radius: 999px;
  background: #eef5ea;
  padding: 4px 10px;
  color: #2d6b3f;
  font-size: 12px;
  font-weight: 700;
}

.knowledge-item-title {
  font-size: var(--title-md);
  line-height: 1.35;
}

.knowledge-item-preview {
  display: -webkit-box;
  overflow: hidden;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.load-more-text {
  padding: 12px 0 4px;
  text-align: center;
}
</style>
