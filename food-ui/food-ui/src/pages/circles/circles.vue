<template>
  <div class="page-shell circles-page">
    <header class="top-nav">
      <button class="back" type="button" @click="router.push({ name: 'home' })">‹</button>
      <h1>美食搭子</h1>
      <button class="back" type="button" @click="createCircle">＋</button>
    </header>

    <article v-for="circle in circles" :key="circle.id" class="circle-card" @click="openCircle(circle.id)">
      <small>搭子小圈</small>
      <h2>{{ circle.name }}</h2>
      <p>{{ circle.description }}</p>
      <div class="stats">
        <span>{{ circle.memberCount }} 成员</span>
        <span>{{ circle.sharedMenuCount }} 共享菜单</span>
        <span>{{ circle.weeklyUpdateCount }} 本周更新</span>
      </div>
    </article>

    <AppTabBar active="circles" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import AppTabBar from '@/components/AppTabBar.vue'
import { SocialService, type BuddyCircleSummary } from '@/services/social-service'

const router = useRouter()
const socialService = new SocialService()
const circles = ref<BuddyCircleSummary[]>([])

onMounted(loadData)

async function loadData() {
  const { data } = await socialService.getCircles()
  circles.value = data
}

async function createCircle() {
  const name = window.prompt('输入搭子圈名称', '周末探店局')
  if (!name) return
  const description = window.prompt('补一句圈子介绍', '和好友一起建圈、邀人、共享菜单。') || ''
  const { data } = await socialService.createCircle({ name, description })
  Message.success('搭子圈已创建')
  router.push({ name: 'circle-detail', params: { id: data.circle.id } })
}

function openCircle(id: number) {
  router.push({ name: 'circle-detail', params: { id } })
}
</script>

<style scoped>
.circles-page {
  padding-bottom: 120px;
}

.top-nav,
.stats {
  display: flex;
}

.top-nav {
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.back {
  border: none;
  background: #fff;
  width: 36px;
  height: 36px;
  border-radius: 999px;
}

.circle-card {
  background: #fff;
  border-radius: 22px;
  padding: 18px;
  box-shadow: 0 12px 26px rgba(27, 58, 45, 0.06);
  margin-bottom: 16px;
}

.circle-card small,
.circle-card p {
  color: var(--text-muted);
}

.circle-card h2,
.circle-card p {
  margin: 0;
}

.circle-card h2 {
  color: var(--text-main);
  font-family: 'Playfair Display', serif;
  margin: 8px 0 10px;
}

.stats {
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.stats span {
  background: #f6f0ea;
  color: var(--accent);
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 0.76rem;
}
</style>
