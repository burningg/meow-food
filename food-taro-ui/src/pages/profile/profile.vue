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
          </article>
        </section>

        <section class="feedback-block">
          <button class="pet-entry-card pressable" @tap="openPetPage">
            <view class="pet-entry-left">
              <view class="pet-icon-shell">
                <view class="pet-paw-icon">
                  <view class="paw-toe paw-toe-1"></view>
                  <view class="paw-toe paw-toe-2"></view>
                  <view class="paw-toe paw-toe-3"></view>
                  <view class="paw-pad-main"></view>
                </view>
              </view>
              <view class="pet-entry-copy">
                <text class="pet-entry-title">我的宠物</text>
              </view>
            </view>
            <view class="feedback-arrow-shell">
              <text class="feedback-arrow">›</text>
            </view>
          </button>

          <button class="feedback-entry-card pressable" @tap="openFeedbackPage">
            <view class="feedback-entry-left">
              <view class="feedback-icon-shell">
                <image class="feedback-icon-image" :src="chatCircleTextIcon" mode="aspectFit" />
              </view>
              <view class="feedback-entry-copy">
                <text class="feedback-entry-title">意见反馈</text>
              </view>
            </view>
            <view class="feedback-arrow-shell">
              <text class="feedback-arrow">›</text>
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
import { SocialService, type BuddyCircleSummary, type ProfileResponse } from '@/services/social-service'
import { useAuthStore } from '@/stores/auth-store'
import type { MenuVisibility } from '@/services/auth-service'

const authStore = useAuthStore()
const socialService = new SocialService()
const notificationService = new NotificationService()
const petService = new PetService()
const profile = ref<ProfileResponse | null>(null)
const circles = ref<BuddyCircleSummary[]>([])
const pet = ref<PetResponse | null>(null)
const draftVisibility = ref<Exclude<MenuVisibility, 'inherit'>>('public')
const draftCircleIds = ref<string[]>([])
const savingVisibility = ref(false)
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

const visibilityOptions: Array<{ value: Exclude<MenuVisibility, 'inherit'>; label: string; desc: string }> = [
  { value: 'private', label: '仅自己可见', desc: '只在你自己的菜单空间展示' },
  { value: 'public', label: '圈内公开', desc: '对你所在全部圈子的成员开放' },
  { value: 'circle', label: '指定圈子', desc: '仅对你选中的圈子成员开放' },
]

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
    const [profileResult, circlesResult, notificationResult, petResult] = await Promise.allSettled([
      socialService.getProfile(),
      socialService.getCircles(),
      notificationService.getBootstrap(),
      petService.getMyPet(),
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

async function selectVisibility(value: Exclude<MenuVisibility, 'inherit'>) {
  if (savingVisibility.value) return
  draftVisibility.value = value
  await saveVisibility()
}

async function toggleCircle(circleId: string) {
  if (!circleId || savingVisibility.value) return
  if (draftCircleIds.value.includes(circleId)) {
    draftCircleIds.value = draftCircleIds.value.filter((id) => id !== circleId)
  } else {
    draftCircleIds.value = [...draftCircleIds.value, circleId]
  }
  await saveVisibility()
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
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '权限更新失败，请稍后重试')
    if (profile.value) {
      draftVisibility.value = profile.value.defaultMenuVisibility
      draftCircleIds.value = [...profile.value.defaultMenuCircleIds]
    }
  } finally {
    savingVisibility.value = false
  }
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

.section-card {
  padding: 18px;
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

.feedback-block {
  display: flex;
  flex-direction: row;
  gap: 12px;
  margin-top: 18px;
}

.feedback-entry-card,
.pet-entry-card {
  display: flex;
  flex: 1 1 0;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  gap: 8px;
  border-radius: 12px;
  background: #fff;
  padding: 12px;
  text-align: left;
}

.feedback-entry-left,
.pet-entry-left {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.feedback-icon-shell,
.pet-icon-shell {
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

.feedback-entry-copy,
.pet-entry-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.feedback-entry-title,
.pet-entry-title {
  color: #151515;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.3;
  white-space: nowrap;
}

.pet-paw-icon {
  position: relative;
  width: 18px;
  height: 18px;
}

.feedback-icon-image {
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

.feedback-arrow-shell {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 999px;
  background: #f7f4ef;
}

.feedback-arrow {
  color: #9f5c38;
  font-size: 20px;
  line-height: 1;
}

@media screen and (max-width: 350px) {
  .feedback-block {
    gap: 8px;
  }

  .feedback-entry-card,
  .pet-entry-card {
    gap: 6px;
    padding: 10px;
  }

  .feedback-entry-left,
  .pet-entry-left {
    gap: 8px;
  }

  .feedback-icon-shell,
  .pet-icon-shell {
    width: 32px;
    height: 32px;
  }

  .feedback-arrow-shell {
    width: 22px;
    height: 22px;
  }

  .feedback-entry-title,
  .pet-entry-title {
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

.logout-section {
  margin-top: 18px;
}
</style>
