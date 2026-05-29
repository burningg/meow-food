<template>
  <div class="page-shell user-menu-page">
    <header class="top-nav">
      <button class="back" type="button" @click="router.back()">‹</button>
      <h1>访问 {{ access?.user.nickname }} 的菜单</h1>
      <div class="back placeholder"></div>
    </header>

    <section class="hero-card">
      <img class="avatar" :src="access?.user.avatar" :alt="access?.user.nickname" />
      <div>
        <h2>{{ access?.user.nickname }} 的私房菜单</h2>
        <p>{{ access?.description }}</p>
      </div>
    </section>

    <section class="rules-card">
      <div v-for="rule in access?.accessRules || []" :key="rule.label" class="rule-row">
        <div>
          <strong>{{ rule.label }}</strong>
          <p>{{ rule.description }}</p>
        </div>
        <span>{{ rule.state }}</span>
      </div>
    </section>

    <section class="menus">
      <div class="section-head">
        <div>
          <h2>可访问菜单</h2>
        </div>
        <span>{{ access?.accessibleCount ?? 0 }} 份</span>
      </div>
      <article v-for="menu in access?.menus || []" :key="menu.id" class="menu-row" @click="openDish(menu.id)">
        <img :src="menu.image" :alt="menu.name" />
        <div>
          <strong>{{ menu.name }}</strong>
          <p>{{ menu.effectiveVisibility === 'friends' ? '好友可见' : '公开菜单' }}</p>
        </div>
      </article>
    </section>

    <button class="primary-button action" type="button" @click="handleAction">
      {{ access?.actionType === 'invite-circle' ? '邀请加入搭子圈' : '发起好友申请' }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { SocialService, type UserMenuAccessResponse } from '@/services/social-service'

const route = useRoute()
const router = useRouter()
const socialService = new SocialService()
const access = ref<UserMenuAccessResponse | null>(null)

onMounted(loadData)

async function loadData() {
  const { data } = await socialService.getUserMenuAccess(String(route.params.id))
  access.value = data
}

function openDish(id: string) {
  router.push({ name: 'dish-detail', params: { id } })
}

async function handleAction() {
  if (!access.value) return
  if (access.value.actionType === 'friend-request') {
    await socialService.createFriendRequest({ targetUserId: access.value.user.id, message: '想看看你的菜单，也想一起分享美味。' })
    Message.success('好友申请已发送')
    return
  }
  router.push({ name: 'circles' })
}
</script>

<style scoped>
.user-menu-page {
  padding-bottom: 48px;
}

.top-nav,
.hero-card,
.rule-row,
.menu-row,
.section-head {
  display: flex;
}

.top-nav,
.rule-row,
.section-head {
  justify-content: space-between;
  align-items: center;
}

.back {
  border: none;
  background: #fff;
  width: 36px;
  height: 36px;
  border-radius: 999px;
}

.placeholder {
  visibility: hidden;
}

.hero-card,
.rules-card,
.menus {
  background: #fff;
  border-radius: 22px;
  padding: 18px;
  box-shadow: 0 12px 26px rgba(27, 58, 45, 0.06);
  margin-top: 16px;
}

.hero-card {
  gap: 14px;
  align-items: center;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  object-fit: cover;
}

h1,
h2 {
  margin: 0;
  color: var(--text-main);
}

h2 {
  font-family: 'Playfair Display', serif;
}

p,
small,
.rule-row span,
.menu-row p {
  color: var(--text-muted);
}

.rule-row {
  padding: 14px 0;
  border-top: 1px solid var(--line);
}

.rule-row:first-child {
  padding-top: 0;
  border-top: none;
}

.menus .section-head {
  margin-bottom: 14px;
}

.menu-row {
  gap: 12px;
  align-items: center;
  padding: 10px 0;
}

.menu-row img {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 16px;
}

.menu-row strong,
.menu-row p {
  display: block;
  margin: 0;
}

.action {
  width: 100%;
  margin-top: 18px;
}
</style>
