<template>
  <view class="page-shell knowledge-detail-page">
    <view v-if="loading" class="status-card">正在加载知识内容</view>

    <article v-else-if="article" class="article-content">
      <image
        v-if="article.imageUrl"
        class="article-hero-image"
        :src="article.imageUrl"
        mode="aspectFill"
      />
      <view class="article-meta-row">
        <text class="article-category">{{ article.category }}</text>
        <text class="article-time">{{ formatKnowledgeDate(article.publishedAt) }}</text>
        <button class="article-share-button" open-type="share">分享</button>
      </view>
      <text class="article-title">{{ article.title }}</text>
      <rich-text class="article-rich-text" :nodes="article.bodyNodes" />
    </article>

    <view v-else class="empty-card">这条知识不存在或已下架</view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useShareAppMessage } from '@tarojs/taro'
import { Message } from '@/lib/feedback'
import { getRouteParams, goBack, resolveSharePath } from '@/lib/navigation'
import { KnowledgeService, type KnowledgeArticleDetail } from '@/services/knowledge-service'

const knowledgeService = new KnowledgeService()
const article = ref<KnowledgeArticleDetail | null>(null)
const loading = ref(true)

onMounted(() => {
  void loadArticle()
})

useShareAppMessage(() => ({
  title: article.value?.title || '饮食知识',
  path: article.value
    ? resolveSharePath({ name: 'knowledge-detail', params: { id: article.value.id } })
    : resolveSharePath('knowledge-archive'),
}))

async function loadArticle() {
  const params = getRouteParams<{ id?: string }>()
  if (!params.id) {
    loading.value = false
    Message.error('缺少知识信息')
    return
  }
  try {
    const { data } = await knowledgeService.getArticle(params.id)
    article.value = data
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '知识加载失败')
  } finally {
    loading.value = false
  }
}

function formatKnowledgeDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getMonth() + 1}-${date.getDate()}`
}
</script>

<style>
.knowledge-detail-page {
  padding-bottom: 44px;
  font-family: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

.nav-title,
.article-title {
  color: var(--text-main);
  font-weight: 800;
}

.nav-title {
  font-size: var(--title-sm);
}

.article-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  border-radius: 26px;
  background: #fffdf7;
  padding: 16px 16px 22px;
  box-shadow: 0 18px 36px rgba(27, 58, 45, 0.08);
}

.article-hero-image {
  width: 100%;
  height: 180px;
  border-radius: 20px;
  background: #e8e5e0;
}

.article-meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.article-share-button {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 0 0 auto;
  min-width: 64px;
  height: 30px;
  border-radius: 999px;
  background: #18291d;
  padding: 0 14px;
  color: #fffdf7;
  font-size: 12px;
  font-weight: 800;
  line-height: 30px;
  letter-spacing: 0;
}

.article-share-button::after {
  border: none;
}

.article-category {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 26px;
  border-radius: 999px;
  background: #eaf3e5;
  padding: 4px 11px;
  color: #2d6b3f;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0;
}

.article-time,
.article-lead {
  color: #758070;
  font-size: 13px;
}

.article-title {
  color: #18291d;
  font-family: 'Songti SC', 'Noto Serif SC', 'STSong', 'PingFang SC', serif;
  font-size: 31px;
  font-weight: 700;
  line-height: 1.24;
  letter-spacing: 0;
}

.article-lead {
  border-left: 3px solid #d9c9a8;
  padding-left: 12px;
  color: #5c6659;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.82;
}

.article-rich-text {
  display: block;
  padding-top: 4px;
  color: #303b31;
  font-family: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  font-size: 16px;
  font-weight: 400;
  line-height: 1.92;
}

.article-rich-text p {
  margin: 0 0 16px;
}

.article-rich-text ul {
  margin: 2px 0 16px;
  padding-left: 20px;
}

.article-rich-text li {
  margin-bottom: 10px;
  color: #354034;
}
</style>
