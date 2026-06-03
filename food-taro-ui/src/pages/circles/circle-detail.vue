<template>
  <view class="page-shell circle-detail-page">
    <header class="top-nav circle-nav">
      <button class="icon-shell" @tap="goBack('circles')">‹</button>
      <text class="page-title">美食搭子</text>
      <view class="circle-nav-actions">
        <button class="icon-shell share-shell" :disabled="sharingCircle" @tap="shareCircleInvite">↗</button>
        <button class="icon-shell" @tap="openInvitePicker">＋</button>
      </view>
    </header>

    <template v-if="detail">
      <section class="overview-card">
        <view class="overview-head">
          <view class="overview-copy">
            <text class="eyebrow">当前圈子</text>
            <text class="circle-title">{{ detail.circle.name }}</text>
          </view>
          <text class="state-pill">{{ detail.stats.memberCount }}人 · {{ detail.stats.sharedMenuCount }}菜谱</text>
        </view>

        <scroll-view v-if="switcherCircles.length" class="circle-switch-list" scroll-x>
          <button
            v-for="circle in switcherCircles"
            :key="circle.id"
            :class="['circle-switch-row', { active: circle.id === activeCircleId }]"
            @tap="switchCircle(circle.id)"
          >
            <text>{{ circle.name }}</text>
            <text>{{ circle.memberCount }}人 · {{ circle.sharedMenuCount }}菜谱</text>
          </button>
        </scroll-view>
      </section>

      <section class="section-block">
        <view class="section-head">
          <view>
            <text class="section-title">搭子圈成员</text>
            <text class="muted">点击进入成员页，查看圈成员关系</text>
          </view>
          <text class="section-link">共 {{ detail.stats.memberCount }} 人</text>
        </view>
        <button class="member-entry" @tap="openMembers">
          <view class="member-avatars">
            <view
              v-for="member in previewMembers"
              :key="member.id"
              class="avatar-badge"
              :style="{ background: avatarPalette[member.avatarTone].bg, color: avatarPalette[member.avatarTone].fg }"
            >
              {{ member.initial }}
            </view>
          </view>
          <text class="member-entry-arrow">›</text>
        </button>
      </section>

      <section class="section-block recipes-section">
        <view class="section-head">
          <view>
            <text class="section-title">圈内菜谱</text>
            <text class="muted">按分类快速看圈内最近共享的菜</text>
          </view>
          <text class="section-link">最近更新</text>
        </view>

        <scroll-view class="category-group" scroll-x>
          <button
            v-for="category in categories"
            :key="category"
            :class="['category-chip', { active: category === activeCategory }]"
            @tap="activeCategory = category"
          >
            {{ category }}
          </button>
        </scroll-view>

        <view v-if="visibleMenus.length" class="recipe-grid">
          <button v-for="menu in visibleMenus" :key="menu.id" class="recipe-card" @tap="openDish(menu.id)">
            <image class="recipe-image" :src="menu.image" mode="aspectFill" />
            <view class="recipe-copy">
              <text class="recipe-name">{{ menu.name }}</text>
              <text class="muted">{{ menu.categoryName }}</text>
              <text class="recipe-owner">{{ menu.ownerNickname }}创建</text>
            </view>
          </button>
        </view>
        <view v-else class="empty-card">这个分类还没有共享菜谱</view>
      </section>
    </template>

    <section v-else class="status-card">{{ statusText }}</section>

    <view v-if="inviteModalVisible" class="invite-modal-overlay" @tap="closeInvitePicker">
      <section class="invite-modal-card" @tap.stop>
        <view class="invite-modal-handle"></view>
        <view class="invite-modal-head">
          <view class="invite-modal-title">
            <text class="eyebrow">搭子圈邀请</text>
          </view>
          <button class="modal-close-shell" :disabled="inviteSubmitting" @tap="closeInvitePicker">×</button>
        </view>

        <view v-if="inviteCandidates.length" class="invite-friends-card">
          <article v-for="(friend, index) in inviteCandidates" :key="friend.id" class="invite-friend-row">
            <button class="invite-friend-main" :disabled="inviteSubmitting" @tap="selectedFriendId = friend.id">
              <view :class="['invite-avatar-box', inviteAvatarToneClass(index)]">
                <text>{{ avatarInitial(friend.nickname) }}</text>
              </view>
              <view class="invite-friend-copy">
                <text class="strong">{{ friend.nickname }}</text>
                <text class="muted">{{ inviteFriendMeta(friend, index) }}</text>
              </view>
              <text :class="['invite-check', { active: selectedFriendId === friend.id }]">
                {{ selectedFriendId === friend.id ? '✓' : '' }}
              </text>
            </button>
          </article>
        </view>

        <view v-else class="invite-empty-card">
          <text class="strong">{{ inviteEmptyTitle }}</text>
          <text class="muted">{{ inviteEmptyText }}</text>
        </view>

        <view class="invite-modal-footer">
          <text class="muted">一次选择 1 位好友发出邀请。</text>
          <view class="invite-modal-actions">
            <button class="modal-action ghost" :disabled="inviteSubmitting" @tap="closeInvitePicker">取消</button>
            <button class="modal-action primary" :disabled="!selectedFriendId || inviteSubmitting || inviteLoading" @tap="submitInvite">
              {{ inviteSubmitting ? '邀请中...' : inviteButtonText }}
            </button>
          </view>
        </view>
      </section>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { getRouteParams, goBack, push } from '@/lib/navigation'
import { shareAppMessageToGroup } from '@/lib/share'
import type { DishSummary } from '@/services/food-service'
import {
  SocialService,
  type BuddyCircleDetail,
  type BuddyCircleMember,
  type BuddyCircleSummary,
  type FriendItem,
} from '@/services/social-service'
import { useAuthStore } from '@/stores/auth-store'

type PreviewMember = { id: string; initial: string; avatarTone: number }

const params = getRouteParams() as { id?: string }
const socialService = new SocialService()
const authStore = useAuthStore()
const detail = ref<BuddyCircleDetail | null>(null)
const circles = ref<BuddyCircleSummary[]>([])
const activeCategory = ref('全部')
const isLoading = ref(true)
const sharingCircle = ref(false)
const inviteModalVisible = ref(false)
const inviteLoading = ref(false)
const inviteSubmitting = ref(false)
const inviteLoadFailed = ref(false)
const friends = ref<FriendItem[]>([])
const selectedFriendId = ref('')
let detailRequestToken = 0

const avatarPalette = [
  { bg: '#edf3ec', fg: '#346538' },
  { bg: '#f9ebdd', fg: '#9f5c38' },
  { bg: '#eeeaf7', fg: '#6c58a5' },
]

const circleId = computed(() => String(params.id || ''))
const activeCircleId = computed(() => detail.value?.circle.id || circleId.value)
const switcherCircles = computed(() => (circles.value.length ? circles.value : detail.value ? [detail.value.circle] : []))
const categories = computed(() => ['全部', ...Array.from(new Set((detail.value?.sharedMenus || []).map((menu) => menu.categoryName).filter(Boolean)))])
const visibleMenus = computed(() => {
  const menus = detail.value?.sharedMenus || []
  if (activeCategory.value === '全部') return menus.slice(0, 4)
  return menus.filter((menu) => menu.categoryName === activeCategory.value).slice(0, 4)
})
const previewMembers = computed<PreviewMember[]>(() =>
  (detail.value?.members || []).slice(0, 3).map((member, index) => ({
    id: member.id,
    initial: getInitial(member),
    avatarTone: index % avatarPalette.length,
  })),
)
const statusText = computed(() => (isLoading.value ? '正在加载搭子圈...' : '没有找到这个搭子圈'))
const circleMemberIds = computed(() => new Set((detail.value?.members || []).map((member) => member.id)))
const inviteCandidates = computed(() => friends.value.filter((friend) => !circleMemberIds.value.has(friend.id)))
const inviteButtonText = computed(() => {
  const target = inviteCandidates.value.find((friend) => friend.id === selectedFriendId.value)
  return target ? `邀请${target.nickname}加入搭子圈` : '选择好友后邀请'
})
const inviteEmptyTitle = computed(() => {
  if (inviteLoading.value) return '正在加载好友'
  if (inviteLoadFailed.value) return '好友加载失败'
  return '暂无可邀请好友'
})
const inviteEmptyText = computed(() => {
  if (inviteLoading.value) return '稍等一下，马上就好。'
  if (inviteLoadFailed.value) return '请稍后重试。'
  return '你的好友已经都在这个搭子圈里了。'
})

onMounted(async () => {
  if (!(await requireAuth('circle-detail'))) return
  await loadData()
})

watch(categories, (nextCategories) => {
  if (!nextCategories.includes(activeCategory.value)) activeCategory.value = '全部'
})

async function loadData() {
  try {
    const [{ data: circleList }] = await Promise.all([socialService.getCircles(), loadDetail(circleId.value)])
    circles.value = circleList
  } finally {
    if (!circles.value.length) isLoading.value = false
  }
}

async function loadDetail(targetCircleId: string) {
  if (!targetCircleId) {
    detail.value = null
    isLoading.value = false
    return
  }
  const requestToken = ++detailRequestToken
  isLoading.value = true
  try {
    const { data } = await socialService.getCircleDetail(targetCircleId)
    if (requestToken !== detailRequestToken) return
    detail.value = data
  } finally {
    if (requestToken === detailRequestToken) isLoading.value = false
  }
}

function getInitial(member: BuddyCircleMember) {
  return (member.nickname || member.account || '?').trim().slice(0, 1).toUpperCase()
}

function openInvitePicker() {
  if (!detail.value) return
  inviteModalVisible.value = true
  void ensureInviteCandidates()
}

async function shareCircleInvite() {
  const circle = detail.value?.circle
  const inviterId = authStore.user?.id
  if (!circle || !inviterId || sharingCircle.value) return
  sharingCircle.value = true
  try {
    await shareAppMessageToGroup({
      title: `邀请你加入「${circle.name}」搭子圈`,
      path: `/pages/circles/circle-share-invite?circleId=${encodeURIComponent(circle.id)}&inviterId=${encodeURIComponent(inviterId)}`,
    })
  } catch (error: any) {
    Message.error(error?.message || '当前环境暂不支持分享到聊天')
  } finally {
    sharingCircle.value = false
  }
}

function closeInvitePicker() {
  if (inviteSubmitting.value) return
  inviteModalVisible.value = false
  selectedFriendId.value = ''
}

async function ensureInviteCandidates() {
  if (friends.value.length) {
    inviteLoadFailed.value = false
    syncSelectedFriend()
    return
  }
  inviteLoading.value = true
  inviteLoadFailed.value = false
  try {
    const { data } = await socialService.getFriends()
    friends.value = data
    syncSelectedFriend()
  } catch (error: any) {
    inviteLoadFailed.value = true
    Message.error(error?.response?.data?.message || '加载好友列表失败')
  } finally {
    inviteLoading.value = false
  }
}

function syncSelectedFriend() {
  if (inviteCandidates.value.some((friend) => friend.id === selectedFriendId.value)) return
  selectedFriendId.value = inviteCandidates.value[0]?.id || ''
}

async function submitInvite() {
  if (!activeCircleId.value || !selectedFriendId.value) return
  inviteSubmitting.value = true
  try {
    const { data } = await socialService.inviteToCircle(activeCircleId.value, {
      inviteeUserId: selectedFriendId.value,
    })
    detail.value = data
    Message.success('邀请已发送')
    closeInvitePicker()
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '邀请失败')
  } finally {
    inviteSubmitting.value = false
  }
}

function openMembers() {
  if (!detail.value) return
  push({ name: 'circle-members', params: { id: activeCircleId.value } })
}

function openDish(id: DishSummary['id']) {
  push({ name: 'dish-detail', params: { id } })
}

function switchCircle(id: string) {
  if (!id || id === activeCircleId.value) return
  closeInvitePicker()
  push({ name: 'circle-detail', params: { id } })
}

function avatarInitial(name: string) {
  return (name || '?').trim().slice(0, 1).toUpperCase()
}

function inviteAvatarToneClass(index: number) {
  return ['tone-sage', 'tone-apricot', 'tone-lavender'][index % 3]
}

function inviteFriendMeta(friend: FriendItem, index: number) {
  if (friend.bio?.trim()) return friend.bio.trim()
  const fallback = [
    `好友可见 ${friend.visibleMenuCount} 份菜单`,
    `共同收藏 ${Math.max(friend.sharedMenuCount, 1)} 道菜`,
    `一起吃过 ${Math.max(friend.sharedMenuCount, 1)} 次`,
  ]
  return fallback[index % fallback.length]
}
</script>

<style>
.circle-detail-page {
  padding: 0 0 28px;
  background: #f7f6f3;
}

.circle-nav {
  height: 54px;
  padding: 14px 20px 12px;
}

.circle-nav-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.share-shell {
  color: #1b3a2d;
}

.page-title,
.strong,
.section-title,
.recipe-name {
  color: #151515;
  font-weight: 800;
}

.page-title {
  font-size: 16px;
}

.muted,
.eyebrow {
  color: #787774;
  font-size: 12px;
}

.section-block {
  margin: 16px 20px 0;
  border-radius: 14px;
  background: #fff;
  padding: 14px;
}

.section-head,
.member-entry,
.member-avatars {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-head view,
.recipe-copy {
  display: flex;
  flex-direction: column;
}

.section-link,
.recipe-owner,
.eyebrow {
  color: #9f5c38;
  font-size: 12px;
  font-weight: 800;
}

.member-entry {
  width: 100%;
  min-height: 58px;
  margin-top: 12px;
  border-radius: 14px;
  background: #f7f4ef;
  padding: 0 12px;
}

.avatar-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  margin-right: -8px;
  border: 2px solid #fff;
  border-radius: 999px;
  font-weight: 800;
}

.category-group {
  white-space: nowrap;
  margin: 14px 0;
}

.category-chip {
  display: inline-flex;
  margin-right: 8px;
  border-radius: 999px;
  background: #f5f2ed;
  color: #787774;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 800;
}

.category-chip.active {
  background: #1b3a2d;
  color: #fff;
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.recipe-card {
  overflow: hidden;
  border-radius: 16px;
  background: #f7f4ef;
  text-align: left;
}

.recipe-image {
  width: 100%;
  height: 112px;
}

.recipe-copy {
  gap: 3px;
  padding: 10px;
}

.overview-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0 20px;
  border-radius: 14px;
  background: #fff;
  padding: 14px;
}

.overview-head,
.circle-switch-list,
.invite-modal-head,
.invite-friend-main,
.invite-modal-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.overview-copy,
.invite-friend-copy,
.invite-modal-footer {
  display: flex;
  flex-direction: column;
}

.circle-title {
  color: #151515;
  font-size: 22px;
  font-weight: 800;
}

.state-pill {
  border-radius: 999px;
  background: #edf3ec;
  color: #346538;
  padding: 5px 9px;
  font-size: 11px;
  font-weight: 800;
}

.circle-switch-list {
  white-space: nowrap;
}

.circle-switch-row {
  display: inline-flex;
  flex-direction: column;
  gap: 3px;
  margin-right: 8px;
  border-radius: 12px;
  background: #f5f2ed;
  padding: 8px 10px;
  text-align: left;
}

.circle-switch-row.active {
  background: #1b3a2d;
  color: #fff;
}

.invite-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: flex-end;
  background: rgba(0, 0, 0, 0.35);
}

.invite-modal-card {
  width: 100%;
  border-radius: 24px 24px 0 0;
  background: #fff;
  padding: 12px 20px 24px;
}

.invite-modal-handle {
  width: 42px;
  height: 4px;
  margin: 0 auto 14px;
  border-radius: 999px;
  background: #ded8d0;
}

.modal-close-shell {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: #f5efe7;
  color: #151515;
  font-size: 22px;
}

.invite-friends-card,
.invite-empty-card {
  margin-top: 14px;
  border-radius: 16px;
  background: #f8f5f0;
  overflow: hidden;
}

.invite-empty-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px;
}

.invite-friend-main {
  width: 100%;
  justify-content: flex-start;
  gap: 12px;
  padding: 14px;
  border-bottom: 1px solid #ebe4dc;
  text-align: left;
}

.invite-avatar-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  font-weight: 800;
}

.tone-sage {
  background: #edf3ec;
  color: #346538;
}

.tone-apricot {
  background: #f9ebdd;
  color: #9f5c38;
}

.tone-lavender {
  background: #eeeaf7;
  color: #6c58a5;
}

.invite-friend-copy {
  flex: 1;
}

.invite-check {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 999px;
  border: 1px solid #d8d0c6;
}

.invite-check.active {
  border-color: #9f5c38;
  background: #9f5c38;
  color: #fff;
}

.invite-modal-footer {
  gap: 12px;
  margin-top: 16px;
}

.invite-modal-actions {
  gap: 10px;
}

.modal-action {
  flex: 1;
  min-height: 44px;
  border-radius: 14px;
  font-weight: 800;
}

.modal-action.ghost {
  background: #f5efe7;
  color: #151515;
}

.modal-action.primary {
  background: #9f5c38;
  color: #fff;
}
</style>
