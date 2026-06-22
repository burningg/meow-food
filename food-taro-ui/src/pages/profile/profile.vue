<template>
  <view class="profile-page-root">
    <PullRefreshPage @refresh="refreshProfile">
      <view class="page-shell profile-page">
        <section class="hero-card">
          <view class="hero-top">
            <view class="user-row">
              <image v-if="displayAvatar" :class="['avatar', { 'vip-avatar-frame': isVipActive }]" :src="displayAvatar" mode="aspectFill" />
              <view v-else :class="['avatar', 'avatar-fallback', { 'vip-avatar-frame': isVipActive }]">{{ displayName.slice(0, 1) }}</view>
              <view class="user-copy">
                <view class="hero-title">
                  <text class="hero-title-name">{{ displayName }}</text>
                  <text class="hero-title-suffix"> 的食堂</text>
                  <view :class="['vip-chip', { 'vip-chip-muted': !isVipActive }]" @tap="openVipPage">
                    <text class="vip-chip-label">{{ vipChipLabel }}</text>
                  </view>
                </view>
                <text class="bio">{{ displayBio }}</text>
              </view>
            </view>
            <view class="hero-actions">
              <view class="ghost-circle hero-settings-button" @tap="openSettingsPage">
                <image class="ghost-circle-image" :src="slidersHorizontalIcon" mode="aspectFit" />
              </view>
              <view class="ghost-circle notification-button" @tap="openNotificationsPage">
                <image class="ghost-circle-image" :src="bellIcon" mode="aspectFit" />
                <view v-if="hasUnreadNotification" class="notification-dot"></view>
              </view>
            </view>
          </view>

          <view class="stats-row">
            <view class="stat-item stat-item-clickable" @tap="openHomePage">
              <text class="stat-number">{{ profile?.stats.menuCount ?? 0 }}</text>
              <text>菜单</text>
            </view>
            <view class="stat-item stat-item-clickable" @tap="openCirclesPage">
              <text class="stat-number">{{ profile?.stats.circleCount ?? 0 }}</text>
              <text>搭子圈</text>
            </view>
          </view>
        </section>

        <section class="quick-entry-block">
          <button class="entry-card pressable" @tap="openPetPage">
            <view class="entry-left">
              <view class="pet-icon-shell">
                <view class="pet-paw-icon">
                  <view class="paw-toe paw-toe-1"></view>
                  <view class="paw-toe paw-toe-2"></view>
                  <view class="paw-toe paw-toe-3"></view>
                  <view class="paw-pad-main"></view>
                </view>
              </view>
              <view class="entry-copy">
                <text class="entry-title">我的宠物</text>
                <text class="entry-desc">{{ hasPet ? '查看宠物状态和成长详情' : '领取专属宠物，开启饭点陪伴' }}</text>
              </view>
            </view>
            <view class="entry-arrow-shell">
              <text class="entry-arrow">›</text>
            </view>
          </button>

          <button class="entry-card pressable" @tap="openDefaultVisibilityPage">
            <view class="entry-left">
              <view class="default-visibility-icon-shell">
                <image class="entry-icon-image" :src="slidersHorizontalIcon" mode="aspectFit" />
              </view>
              <view class="entry-copy">
                <text class="entry-title">默认权限</text>
                <text class="entry-desc">设置新菜谱默认向谁开放，也可以按圈子细分</text>
              </view>
            </view>
            <view class="entry-arrow-shell">
              <text class="entry-arrow">›</text>
            </view>
          </button>

          <button class="entry-card pressable" @tap="openFeedbackPage">
            <view class="entry-left">
              <view class="feedback-icon-shell">
                <image class="entry-icon-image" :src="chatCircleTextIcon" mode="aspectFit" />
              </view>
              <view class="entry-copy">
                <text class="entry-title">意见反馈</text>
                <text class="entry-desc">遇到问题，或有想让我们改进的地方，都可以写给我们</text>
              </view>
            </view>
            <view class="entry-arrow-shell">
              <text class="entry-arrow">›</text>
            </view>
          </button>
        </section>

        <!-- <section class="invite-section">
          <button class="invite-card pressable" open-type="share">
            <view class="invite-mark">邀</view>
            <view class="invite-copy">
              <text class="invite-title">邀请好友加入 meoi食堂</text>
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
import chatCircleTextIcon from '@/assets/action-icons/chat-circle-text.svg'
import slidersHorizontalIcon from '@/assets/action-icons/sliders-horizontal.svg'
import AppTabBar from '@/components/AppTabBar.vue'
import PullRefreshPage from '@/components/PullRefreshPage.vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { openPrimaryRoute, push, replace } from '@/lib/navigation'
import { createHomeShareMessage } from '@/lib/share'
import { NotificationService } from '@/services/notification-service'
import { PetService, type PetResponse } from '@/services/pet-service'
import { SocialService, type ProfileResponse } from '@/services/social-service'
import { useAuthStore } from '@/stores/auth-store'

const authStore = useAuthStore()
const socialService = new SocialService()
const notificationService = new NotificationService()
const petService = new PetService()
const profile = ref<ProfileResponse | null>(null)
const pet = ref<PetResponse | null>(null)
const hasUnreadNotification = ref(false)

const displayName = computed(() => profile.value?.user.nickname || authStore.user?.nickname || 'meoi')
const displayAvatar = computed(() => profile.value?.user.avatar || authStore.user?.avatar || '')
const displayBio = computed(() => profile.value?.user.bio?.trim() || authStore.user?.bio?.trim() || '还没有留下简介')
const isVipActive = computed(() => Boolean(profile.value?.vipInfo?.vip || authStore.user?.vip))
const hasPet = computed(() => Boolean(pet.value?.claimed))
const vipChipLabel = computed(() => {
  if (!isVipActive.value) return '开通VIP'
  return formatVipLabel(profile.value?.vipInfo?.vipLevel || authStore.user?.vipLevel)
})

onMounted(async () => {
  if (!(await requireAuth('profile'))) return
  await loadProfilePageData()
})

useDidShow(async () => {
  if (!(await requireAuth('profile'))) return
  await loadProfilePageData()
})

useShareAppMessage(() =>
  createHomeShareMessage({
    title: '我在 meoi食堂记录了不少下厨灵感，来首页看看吧',
  }),
)

async function loadProfilePageData() {
  try {
    const [profileResult, notificationResult, petResult] = await Promise.allSettled([
      socialService.getProfile(),
      notificationService.getBootstrap(),
      petService.getMyPet(),
    ])
    if (profileResult.status === 'fulfilled') {
      profile.value = profileResult.value.data
    } else {
      throw profileResult.reason
    }
    if (notificationResult.status === 'fulfilled') {
      hasUnreadNotification.value = notificationResult.value.data.hasUnread
    } else {
      hasUnreadNotification.value = false
    }
    if (petResult.status === 'fulfilled') {
      pet.value = petResult.value.data
    } else {
      pet.value = null
    }
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '个人资料加载失败')
  }
}

async function refreshProfile() {
  await loadProfilePageData()
  await authStore.restore()
}

function openNotificationsPage() {
  push('notifications')
}

function openSettingsPage() {
  push('settings')
}

function openFeedbackPage() {
  push('feedback')
}

function openDefaultVisibilityPage() {
  push('default-visibility')
}

function openPetPage() {
  push(hasPet.value ? 'pet-detail' : 'pet-adoption')
}

function openVipPage() {
  push('vip')
}

function openHomePage() {
  openPrimaryRoute('home')
}

function openCirclesPage() {
  openPrimaryRoute('circles')
}

function logout() {
  authStore.logout()
  hasUnreadNotification.value = false
  replace('login')
}

function formatVipLabel(level?: string) {
  if (!level) return 'VIP'
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
.entry-card {
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
.hero-actions {
  display: flex;
}

.hero-top {
  justify-content: space-between;
}

.user-row {
  gap: 14px;
  align-items: flex-start;
}

.avatar {
  display: flex;
  box-sizing: border-box;
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

.vip-avatar-frame {
  border: 3px solid #b97825;
  box-shadow:
    0 0 0 3px #fff4db,
    0 8px 18px rgba(164, 106, 31, 0.22);
}

.hero-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px 6px;
}

.hero-title-name {
  color: var(--text-main);
  font-size: var(--title-lg);
  font-weight: 800;
}

.hero-title-suffix,
.bio,
.stats-row text {
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
  align-items: center;
}

.hero-settings-button {
  background: #fff;
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

.stat-item-clickable {
  cursor: pointer;
}

.stat-number {
  color: var(--text-main) !important;
  font-size: var(--title-md);
  font-weight: 800;
}

.vip-chip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  flex-shrink: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #6e4317 0%, #b97a2a 100%);
  box-shadow: 0 6px 14px rgba(164, 111, 31, 0.12);
  padding: 5px 9px;
}

.vip-chip-muted {
  background: #e8e5df;
  box-shadow: none;
}

.vip-chip-icon,
.vip-chip-label {
  color: #fff3d6;
}

.vip-chip-muted .vip-chip-label {
  color: #89847c;
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

.quick-entry-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}

.entry-card {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  gap: 8px;
  padding: 12px;
  text-align: left;
}

.entry-left {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.feedback-icon-shell,
.pet-icon-shell,
.default-visibility-icon-shell {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #edf3ec;
}

.pet-icon-shell {
  background: #fff0de;
}

.default-visibility-icon-shell {
  background: #fff0de;
}

.entry-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.entry-title {
  color: #151515;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.3;
}

.entry-desc {
  color: #787774;
  font-size: 12px;
  line-height: 1.45;
}

.pet-paw-icon {
  position: relative;
  width: 18px;
  height: 18px;
}

.entry-icon-image {
  width: 18px;
  height: 18px;
}

.paw-toe,
.paw-pad-main {
  position: absolute;
  background: #9f5c38;
}

.paw-toe {
  width: 4px;
  height: 5px;
  border-radius: 999px;
}

.paw-toe-1 {
  left: 2px;
  top: 3px;
  transform: rotate(-24deg);
}

.paw-toe-2 {
  left: 7px;
  top: 1px;
}

.paw-toe-3 {
  right: 2px;
  top: 3px;
  transform: rotate(24deg);
}

.paw-pad-main {
  left: 5px;
  bottom: 3px;
  width: 8px;
  height: 8px;
  border-radius: 6px 6px 7px 7px;
}

.entry-arrow-shell {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 999px;
  background: #f7f4ef;
}

.entry-arrow {
  color: #9f5c38;
  font-size: 20px;
  line-height: 1;
}

@media screen and (max-width: 350px) {
  .entry-card {
    gap: 6px;
    padding: 10px;
  }

  .entry-left {
    gap: 8px;
  }

  .feedback-icon-shell,
  .pet-icon-shell,
  .default-visibility-icon-shell {
    width: 32px;
    height: 32px;
  }

  .entry-arrow-shell {
    width: 22px;
    height: 22px;
  }

  .entry-title {
    font-size: 13px;
  }
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

.logout-section {
  margin-top: 18px;
}
</style>
