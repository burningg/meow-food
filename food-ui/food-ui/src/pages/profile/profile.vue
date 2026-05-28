<template>
  <div class="page-shell profile-page">
    <section class="hero-card">
      <div class="hero-top">
        <div class="user-row">
          <img class="avatar" :src="profile?.user.avatar" :alt="profile?.user.nickname" />
          <div>
            <h1>{{ profile?.user.nickname }}的美味空间</h1>
            <p>{{ profile?.user.bio }}</p>
          </div>
        </div>
        <button class="ghost-circle" type="button" @click="logout">⇥</button>
      </div>

      <div class="stats-row">
        <div>
          <strong>{{ profile?.stats.friendCount ?? 0 }}</strong>
          <span>好友</span>
        </div>
        <div>
          <strong>{{ profile?.stats.menuCount ?? 0 }}</strong>
          <span>菜单</span>
        </div>
        <div>
          <strong>{{ profile?.stats.circleCount ?? 0 }}</strong>
          <span>搭子圈</span>
        </div>
      </div>

      <div class="cta-row">
        <button class="primary-button flex-button" type="button" @click="addFriend">添加好友</button>
        <button class="secondary-button flex-button" type="button" @click="createCircle">创建搭子圈</button>
      </div>
    </section>

    <section class="section">
      <div class="section-head">
        <div>
          <small>social</small>
          <h2>我的好友</h2>
        </div>
        <button class="ghost-link" type="button" @click="openFriendInbox">管理邀请</button>
      </div>
      <div class="friend-grid">
        <FriendCard
          v-for="friend in profile?.friendPreview || []"
          :key="friend.id"
          :friend="friend"
          action-label="查看"
          @action="goToFriend(friend.id)"
        />
      </div>
    </section>

    <section class="section">
      <div class="section-head">
        <div>
          <small>social</small>
          <h2>好友动态</h2>
        </div>
        <button class="ghost-link" type="button" @click="router.push({ name: 'feed' })">查看全部</button>
      </div>
      <ActivityCard
        v-for="item in profile?.feedPreview || []"
        :key="item.id"
        :item="item"
        @open="openFeedItem(item)"
      />
    </section>

    <section class="section">
      <div class="section-head">
        <div>
          <small>social</small>
          <h2>菜单可见范围</h2>
        </div>
      </div>
      <article class="visibility-card">
        <p>你可以决定谁能看到自己的菜单，也能因此访问好友开放的菜单。</p>
        <button
          v-for="option in visibilityOptions"
          :key="option.value"
          :class="['visibility-row', { active: profile?.defaultMenuVisibility === option.value }]"
          type="button"
          @click="updateVisibility(option.value)"
        >
          <span>
            <strong>{{ option.label }}</strong>
            <small>{{ option.desc }}</small>
          </span>
          <i></i>
        </button>
      </article>
    </section>

    <AppTabBar active="profile" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import AppTabBar from '@/components/AppTabBar.vue'
import FriendCard from '@/components/FriendCard.vue'
import ActivityCard from '@/components/ActivityCard.vue'
import { SocialService, type FeedItem, type ProfileResponse } from '@/services/social-service'
import { useAuthStore } from '@/stores/auth-store'
import type { MenuVisibility } from '@/services/auth-service'

const router = useRouter()
const authStore = useAuthStore()
const socialService = new SocialService()

const profile = ref<ProfileResponse | null>(null)

const visibilityOptions: Array<{ value: Exclude<MenuVisibility, 'inherit'>; label: string; desc: string }> = [
  { value: 'friends', label: '好友可见', desc: '你的菜单会同步给好友和已加入的搭子圈。' },
  { value: 'public', label: '公开', desc: '任何用户都可以从动态和菜单页进入。' },
  { value: 'private', label: '仅自己可见', desc: '只在你自己的菜单空间展示。' },
]

onMounted(loadProfile)

async function loadProfile() {
  const { data } = await socialService.getProfile()
  profile.value = data
}

async function updateVisibility(value: Exclude<MenuVisibility, 'inherit'>) {
  await socialService.updateVisibility(value)
  Message.success('菜单默认可见范围已更新')
  await loadProfile()
  await authStore.restore()
}

async function addFriend() {
  const account = window.prompt('输入对方账号，例如 ali')
  if (!account) return
  try {
    await socialService.createFriendRequest({ targetAccount: account, message: '一起分享美味吧' })
    Message.success('好友申请已发送')
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '发送失败')
  }
}

async function createCircle() {
  const name = window.prompt('输入搭子圈名称', '周末探店局')
  if (!name) return
  const description = window.prompt('补一句圈子介绍', '和好友一起建圈、邀人、共享菜单。') || ''
  const { data } = await socialService.createCircle({ name, description })
  Message.success('搭子圈已创建')
  router.push({ name: 'circle-detail', params: { id: data.circle.id } })
}

function goToFriend(userId: number) {
  router.push({ name: 'user-menu', params: { id: userId } })
}

function openFeedItem(item: FeedItem) {
  router.push({ name: 'dish-detail', params: { id: item.dishId } })
}

async function openFriendInbox() {
  const { data } = await socialService.getFriendRequests()
  const pending = data.incoming.filter((item) => item.status === 'pending')
  if (!pending.length) {
    Message.info('暂时没有待处理的好友申请')
    return
  }
  const first = pending[0]
  if (window.confirm(`接受 ${first.requesterNickname} 的好友申请？`)) {
    await socialService.acceptFriendRequest(first.id)
    Message.success('已成为好友')
    await loadProfile()
  }
}

function logout() {
  authStore.logout()
  router.replace({ name: 'login' })
}
</script>

<style scoped>
.profile-page {
  padding-bottom: 120px;
}

.hero-card,
.visibility-card,
.section {
  background: #fff;
  border-radius: 22px;
  box-shadow: 0 12px 26px rgba(27, 58, 45, 0.06);
}

.hero-card {
  padding: 18px;
  margin-bottom: 18px;
}

.hero-top,
.user-row,
.stats-row,
.cta-row,
.section-head {
  display: flex;
}

.hero-top,
.section-head {
  justify-content: space-between;
}

.user-row {
  gap: 14px;
  align-items: center;
}

.avatar {
  width: 58px;
  height: 58px;
  border-radius: 16px;
  object-fit: cover;
}

h1,
h2 {
  margin: 0;
  color: var(--text-main);
}

h1 {
  font-size: 1.6rem;
  font-family: 'Playfair Display', serif;
}

.hero-top p,
small,
.visibility-card p,
.visibility-row small {
  color: var(--text-muted);
}

.hero-top p {
  margin: 8px 0 0;
  line-height: 1.6;
}

.ghost-circle {
  border: none;
  background: #f5f2ed;
  border-radius: 999px;
  width: 36px;
  height: 36px;
}

.stats-row {
  justify-content: space-between;
  margin: 18px 0;
  padding: 18px 0;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}

.stats-row div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stats-row strong {
  font-size: 1.4rem;
}

.cta-row {
  gap: 10px;
}

.flex-button {
  flex: 1;
}

.secondary-button {
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
  color: var(--text-main);
}

.section {
  padding: 18px;
  margin-bottom: 18px;
}

.section-head {
  align-items: end;
  margin-bottom: 14px;
}

.section-head small {
  display: block;
  margin-bottom: 2px;
  letter-spacing: 0.08em;
}

.ghost-link {
  border: none;
  background: transparent;
  color: var(--accent);
}

.friend-grid {
  display: flex;
  gap: 12px;
}

.visibility-card {
  padding: 16px;
}

.visibility-row {
  width: 100%;
  border: none;
  background: transparent;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-top: 1px solid var(--line);
}

.visibility-row span {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.visibility-row i {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #d8d2cb;
}

.visibility-row.active i {
  background: var(--text-main);
}
</style>
