<template>
  <view class="page-shell profile-page">
    <section class="hero-card">
      <view class="hero-top">
        <view class="user-row">
          <image v-if="displayAvatar" class="avatar" :src="displayAvatar" mode="aspectFill" />
          <view v-else class="avatar avatar-fallback">{{ displayName.slice(0, 1) }}</view>
          <view class="user-copy">
            <view class="hero-title">
              <text class="hero-title-name">{{ displayName }}</text>
              <text class="hero-title-suffix">的美味空间</text>
            </view>
            <text class="bio">{{ displayBio }}</text>
          </view>
        </view>
        <button class="ghost-circle" @tap="openEditProfilePage">✎</button>
      </view>

      <view class="stats-row">
        <button class="stat-button" @tap="openFriendsPage">
          <text class="stat-number">{{ profile?.stats.friendCount ?? 0 }}</text>
          <text>好友</text>
        </button>
        <view class="stat-item">
          <text class="stat-number">{{ profile?.stats.menuCount ?? 0 }}</text>
          <text>菜单</text>
        </view>
        <view class="stat-item">
          <text class="stat-number">{{ profile?.stats.circleCount ?? 0 }}</text>
          <text>搭子圈</text>
        </view>
      </view>
    </section>

    <section class="section-card">
      <view class="section-head">
        <text class="eyebrow">权限</text>
      </view>
      <article class="visibility-card">
        <button
          v-for="option in visibilityOptions"
          :key="option.value"
          :class="['visibility-row', { active: profile?.defaultMenuVisibility === option.value }]"
          @tap="updateVisibility(option.value)"
        >
          <view>
            <text class="visibility-title">{{ option.label }}</text>
            <text class="visibility-desc">{{ option.desc }}</text>
          </view>
          <text class="visibility-dot"></text>
        </button>
      </article>
    </section>

    <section class="logout-section">
      <button class="primary-button logout-button" @tap="logout">退出登录</button>
    </section>

    <AppTabBar active="profile" />
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppTabBar from '@/components/AppTabBar.vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { push, replace } from '@/lib/navigation'
import { SocialService, type ProfileResponse } from '@/services/social-service'
import { useAuthStore } from '@/stores/auth-store'
import type { MenuVisibility } from '@/services/auth-service'

const authStore = useAuthStore()
const socialService = new SocialService()
const profile = ref<ProfileResponse | null>(null)

const displayName = computed(() => profile.value?.user.nickname || authStore.user?.nickname || 'meow')
const displayBio = computed(() => profile.value?.user.bio || '菜单、好友和搭子圈都在这里慢慢展开。')
const displayAvatar = computed(() => profile.value?.user.avatar || authStore.user?.avatar || '')

const visibilityOptions: Array<{ value: Exclude<MenuVisibility, 'inherit'>; label: string; desc: string }> = [
  { value: 'friends', label: '好友可见', desc: '你的菜单会同步给好友和已加入的搭子圈。' },
  { value: 'public', label: '公开', desc: '任何用户都可以从动态和菜单页进入。' },
  { value: 'private', label: '仅自己可见', desc: '只在你自己的菜单空间展示。' },
]

onMounted(async () => {
  if (!(await requireAuth('profile'))) return
  await loadProfile()
})

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

function openFriendsPage() {
  push('friends')
}

function openEditProfilePage() {
  push('edit-profile')
}

function logout() {
  authStore.logout()
  replace('login')
}
</script>

<style>
.profile-page {
  padding-bottom: 120px;
}

.hero-card,
.visibility-card,
.section-card {
  border-radius: 22px;
  background: #fff;
  box-shadow: var(--shadow);
}

.hero-card {
  padding: 18px;
  margin-bottom: 18px;
}

.hero-top,
.user-row,
.stats-row,
.visibility-row {
  display: flex;
}

.hero-top,
.visibility-row {
  justify-content: space-between;
}

.user-row {
  gap: 14px;
  align-items: center;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  border-radius: 16px;
}

.avatar-fallback {
  background: linear-gradient(135deg, #7a9e7e, #a7be8f);
  color: #fff;
  font-weight: 800;
}

.hero-title {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
}

.hero-title-name {
  color: var(--text-main);
  font-size: var(--title-lg);
  font-weight: 800;
}

.hero-title-suffix,
.bio,
.stats-row text,
.visibility-desc,
.eyebrow {
  color: var(--text-muted);
}

.bio {
  display: block;
  margin-top: 8px;
  font-size: var(--text-sm);
}

.ghost-circle {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: #f5f2ed;
}

.stats-row {
  justify-content: space-around;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
}

.stat-button,
.stat-item {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-number {
  color: var(--text-main) !important;
  font-size: var(--title-md);
  font-weight: 800;
}

.section-card {
  padding: 18px;
}

.visibility-card {
  margin-top: 12px;
  overflow: hidden;
}

.visibility-row {
  width: 100%;
  align-items: center;
  padding: 14px 0;
  border-top: 1px solid var(--line);
  text-align: left;
}

.visibility-row:first-child {
  border-top: none;
}

.visibility-title,
.visibility-desc {
  display: block;
}

.visibility-title {
  color: var(--text-main);
  font-weight: 800;
}

.visibility-desc {
  margin-top: 4px;
  font-size: var(--text-sm);
}

.visibility-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  border: 2px solid #e0d7ca;
}

.visibility-row.active .visibility-dot {
  border-color: var(--accent);
  background: var(--accent);
}

.logout-section {
  margin-top: 18px;
}
</style>
