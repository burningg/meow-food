<template>
  <view class="profile-page-root">
    <PullRefreshPage @refresh="refreshProfile">
      <view class="page-shell profile-page">
        <section class="hero-card">
          <view class="hero-top">
            <view class="user-row">
              <image v-if="displayAvatar" class="avatar" :src="displayAvatar" mode="aspectFill" />
              <view v-else class="avatar avatar-fallback">{{ displayName.slice(0, 1) }}</view>
              <view class="user-copy">
                <view class="hero-title">
                  <text class="hero-title-name">{{ displayName }}</text>
                  <text class="hero-title-suffix"> 的美味空间</text>
                </view>
                <text class="bio">{{ displayBio }}</text>
                <view v-if="vipChipLabel" class="vip-chip">
                  <text class="vip-chip-label">{{ vipChipLabel }}</text>
                </view>
              </view>
            </view>
            <view class="hero-actions">
              <button class="ghost-circle notification-button" @tap="openNotificationsPage">
                <image class="ghost-circle-image" :src="bellIcon" mode="aspectFit" />
                <view v-if="hasUnreadNotification" class="notification-dot"></view>
              </button>
              <button class="ghost-circle" @tap="openEditProfilePage">✎</button>
            </view>
          </view>

          <view class="stats-row">
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
              :class="['visibility-row', { active: draftVisibility === option.value }]"
              @tap="selectVisibility(option.value)"
            >
              <view>
                <text class="visibility-title">{{ option.label }}</text>
                <text class="visibility-desc">{{ option.desc }}</text>
              </view>
              <text class="visibility-dot"></text>
            </button>
            <view v-if="draftVisibility === 'circle'" class="circle-picker-card">
              <view class="circle-picker-head">
                <text class="visibility-title">默认开放圈子</text>
                <text class="visibility-desc">{{ draftCircleIds.length }} 个已选</text>
              </view>
              <view v-if="circles.length" class="circle-chip-list">
                <button
                  v-for="circle in circles"
                  :key="circle.id"
                  :class="['circle-chip', { active: draftCircleIds.includes(circle.id) }]"
                  @tap="toggleCircle(circle.id)"
                >
                  {{ circle.name }}
                </button>
              </view>
              <text v-else class="visibility-desc">你还没有加入圈子，暂时不能把默认权限设为指定圈子。</text>
            </view>
            <button class="primary-button visibility-save" :disabled="savingVisibility" @tap="saveVisibility">
              {{ savingVisibility ? '保存中' : '保存默认权限' }}
            </button>
          </article>
        </section>

        <!-- <section class="invite-section">
          <button class="invite-card pressable" open-type="share">
            <view class="invite-mark">邀</view>
            <view class="invite-copy">
              <text class="invite-title">邀请好友加入 meow食堂</text>
              <text class="invite-desc">发送一张小程序卡片，对方接受后会自动成为好友。</text>
            </view>
            <text class="invite-action">分享好友</text>
          </button>
        </section> -->

        <section class="logout-section">
          <button class="primary-button logout-button" @tap="logout">退出登录</button>
        </section>
      </view>
    </PullRefreshPage>

    <AppTabBar active="profile" />
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useDidShow, useShareAppMessage } from '@tarojs/taro'
import bellIcon from '@/assets/action-icons/bell.svg'
import AppTabBar from '@/components/AppTabBar.vue'
import PullRefreshPage from '@/components/PullRefreshPage.vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { push, replace, resolveSharePath } from '@/lib/navigation'
import { NotificationService } from '@/services/notification-service'
import { SocialService, type BuddyCircleSummary, type ProfileResponse } from '@/services/social-service'
import { useAuthStore } from '@/stores/auth-store'
import type { MenuVisibility } from '@/services/auth-service'

const authStore = useAuthStore()
const socialService = new SocialService()
const notificationService = new NotificationService()
const profile = ref<ProfileResponse | null>(null)
const circles = ref<BuddyCircleSummary[]>([])
const draftVisibility = ref<Exclude<MenuVisibility, 'inherit'>>('public')
const draftCircleIds = ref<string[]>([])
const savingVisibility = ref(false)
const hasUnreadNotification = ref(false)

const displayName = computed(() => profile.value?.user.nickname || authStore.user?.nickname || 'meow')
const displayAvatar = computed(() => profile.value?.user.avatar || authStore.user?.avatar || '')
const displayBio = computed(() => profile.value?.user.bio?.trim() || authStore.user?.bio?.trim() || '还没有留下简介')
const inviterId = computed(() => profile.value?.user.id || authStore.user?.id || '')
const vipChipLabel = computed(() => formatVipLabel(profile.value?.vipInfo?.vip ? profile.value?.vipInfo?.vipLevel : undefined))

const visibilityOptions: Array<{ value: Exclude<MenuVisibility, 'inherit'>; label: string; desc: string }> = [
  { value: 'public', label: '圈内公开', desc: '对你所在全部圈子的成员开放' },
  { value: 'circle', label: '指定圈子', desc: '仅对你选中的圈子成员开放' },
  { value: 'private', label: '仅自己可见', desc: '只在你自己的菜单空间展示' },
]

onMounted(async () => {
  if (!(await requireAuth('profile'))) return
  await loadProfilePageData()
})

useDidShow(async () => {
  if (!(await requireAuth('profile'))) return
  await loadProfilePageData()
})

useShareAppMessage(() => ({
  title: '我在 meow食堂邀请你成为好友',
  path: resolveSharePath({ name: 'friend-invite', params: { inviterId: inviterId.value } }),
}))

async function loadProfilePageData() {
  try {
    const [profileResult, circlesResult, notificationResult] = await Promise.allSettled([
      socialService.getProfile(),
      socialService.getCircles(),
      notificationService.getBootstrap(),
    ])
    if (profileResult.status === 'fulfilled') {
      profile.value = profileResult.value.data
      draftVisibility.value = profileResult.value.data.defaultMenuVisibility
      draftCircleIds.value = [...profileResult.value.data.defaultMenuCircleIds]
    } else {
      throw profileResult.reason
    }
    if (circlesResult.status === 'fulfilled') {
      circles.value = circlesResult.value.data
    } else {
      circles.value = []
    }
    if (notificationResult.status === 'fulfilled') {
      hasUnreadNotification.value = notificationResult.value.data.hasUnread
    } else {
      hasUnreadNotification.value = false
    }
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '个人资料加载失败')
  }
}

async function refreshProfile() {
  await loadProfilePageData()
  await authStore.restore()
}

function selectVisibility(value: Exclude<MenuVisibility, 'inherit'>) {
  draftVisibility.value = value
}

function toggleCircle(circleId: string) {
  if (!circleId) return
  if (draftCircleIds.value.includes(circleId)) {
    draftCircleIds.value = draftCircleIds.value.filter((id) => id !== circleId)
    return
  }
  draftCircleIds.value = [...draftCircleIds.value, circleId]
}

async function saveVisibility() {
  if (draftVisibility.value === 'circle' && !draftCircleIds.value.length) {
    Message.warning('请选择至少一个默认圈子')
    return
  }
  savingVisibility.value = true
  try {
    await socialService.updateVisibility(
      draftVisibility.value,
      draftVisibility.value === 'circle' ? draftCircleIds.value : [],
    )
    Message.success('菜单默认可见范围已更新')
    await loadProfilePageData()
    await authStore.restore()
  } finally {
    savingVisibility.value = false
  }
}

function openEditProfilePage() {
  push('edit-profile')
}

function openNotificationsPage() {
  push('notifications')
}

function logout() {
  authStore.logout()
  hasUnreadNotification.value = false
  replace('login')
}

function formatVipLabel(level?: string) {
  if (!level) return ''
  const normalized = level.trim().replace(/\s+/g, ' ')
  if (/^vip\b/i.test(normalized)) {
    return normalized.replace(/\s+/g, '·')
  }
  // return `VIP·${normalized}`
  return `VIP`
}
</script>

<style>
.profile-page {
  padding-bottom: 120px;
}

.hero-card,
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
.hero-actions,
.visibility-row {
  display: flex;
}

.hero-top,
.visibility-row {
  justify-content: space-between;
}

.user-row {
  gap: 14px;
  align-items: flex-start;
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
  gap: 2px;
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
  margin-left: 3px;
}

.user-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.bio {
  display: block;
  font-size: var(--text-sm);
  line-height: 1.5;
}

.ghost-circle {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: #f5f2ed;
  line-height: 1;
}

.hero-actions {
  gap: 10px;
}

.ghost-circle-image {
  width: 18px;
  height: 18px;
}

.notification-dot {
  position: absolute;
  top: 7px;
  right: 7px;
  width: 8px;
  height: 8px;
  border: 2px solid #fff;
  border-radius: 999px;
  background: #d84f3c;
}

.stats-row {
  justify-content: space-around;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
}

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

.vip-chip {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #6e4317 0%, #b97a2a 100%);
  box-shadow: 0 6px 14px rgba(164, 111, 31, 0.12);
  padding: 5px 9px;
}

.vip-chip-icon,
.vip-chip-label {
  color: #fff3d6;
}

.vip-chip-icon {
  font-size: 10px;
  line-height: 1;
}

.vip-chip-label {
  font-weight: 800;
  font-size: 10px;
  line-height: 1;
}

.invite-section {
  margin-top: 18px;
}

.invite-card {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 12px;
  border-radius: 22px;
  background:
    radial-gradient(circle at 88% 18%, rgba(196, 112, 75, 0.18), transparent 26%),
    linear-gradient(135deg, #fffaf4 0%, #ffffff 60%, #eef4eb 100%);
  padding: 16px;
  text-align: left;
  box-shadow: var(--shadow);
}

.invite-mark {
  display: flex;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: var(--text-main);
  color: #fff;
  font-size: 18px;
  font-weight: 900;
}

.invite-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 4px;
}

.invite-title {
  color: var(--text-main);
  font-weight: 800;
}

.invite-desc {
  color: var(--text-muted);
  font-size: var(--text-sm);
}

.invite-action {
  flex-shrink: 0;
  border-radius: 999px;
  background: rgba(196, 112, 75, 0.12);
  color: var(--accent-dark);
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 800;
}

.visibility-card {
  margin-top: 12px;
  border-radius: 18px;
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

.circle-picker-card {
  margin-top: 14px;
  border-radius: 16px;
  background: #f7f4ef;
  padding: 14px;
}

.circle-picker-head,
.circle-chip-list {
  display: flex;
}

.circle-picker-head {
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.circle-chip-list {
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.circle-chip {
  border-radius: 999px;
  background: #ebe4d8;
  color: #6e6253;
  padding: 8px 12px;
  font-size: var(--text-xs);
  font-weight: 800;
}

.circle-chip.active {
  background: #9f5c38;
  color: #fff;
}

.visibility-save {
  margin-top: 14px;
}

.logout-section {
  margin-top: 18px;
}
</style>
