<template>
  <view class="page-shell circle-members-page">
    <view class="circle-members-content">
      <section class="circle-members-top-card">
        <view class="circle-members-title-stack">
          <text class="eyebrow">搭子圈成员</text>
          <text class="title">圈内成员</text>
        </view>
        <view class="circle-members-actions">
          <button class="nav-shell share-shell" open-type="share">↗</button>
          <button class="nav-shell" @tap="openInvitePicker">＋</button>
        </view>
      </section>

      <section v-if="detail" class="circle-members-intro-card">
        <text class="intro-title">{{ detail.circle.name }}</text>
        <text class="muted">{{ detail.stats.memberCount }} 位成员 · {{ detail.stats.sharedMenuCount }} 份共享菜谱 · 发起人 {{ detail.circle.ownerNickname }}</text>
        <text class="intro-desc">{{ detail.circle.description || '只显示当前搭子圈里的成员，点击可查看对方菜单。' }}</text>
      </section>

      <section class="circle-members-list-section">
        <view class="circle-members-list-head">
          <text class="list-title">成员列表</text>
          <text v-if="detail" class="circle-members-count">{{ detail.stats.memberCount }} 人</text>
        </view>

        <view v-if="detail?.members.length" class="circle-members-list-card">
          <article v-for="(member, index) in orderedMembers" :key="member.id" class="circle-member-row">
            <button class="circle-member-main" @tap="openMember(member.id)">
              <view :class="['member-avatar-box', avatarToneClass(index)]">
                <text>{{ avatarInitial(member.nickname || member.account) }}</text>
              </view>
              <view class="circle-member-copy">
                <view class="circle-member-name-row">
                  <text class="member-name">{{ member.nickname }}</text>
                  <text :class="['role-pill', member.role === 'owner' ? 'role-pill-owner' : 'role-pill-member']">
                    {{ member.role === 'owner' ? '发起人' : '成员' }}
                  </text>
                </view>
                <text class="muted">{{ memberMeta(member, index) }}</text>
              </view>
            </button>
          </article>
        </view>

        <section v-else-if="!isLoading" class="circle-members-empty-card">
          <text>等有人加入后，就会出现在这里。</text>
        </section>
        <section v-else class="circle-members-empty-card">
          <text class="member-name">正在加载成员</text>
        </section>
      </section>
    </view>

    <view v-if="inviteModalVisible" class="invite-modal-overlay" @tap="closeInvitePicker">
      <section class="invite-modal-card" @tap.stop>
        <view class="invite-modal-handle"></view>
        <view class="invite-modal-head">
          <view class="invite-modal-title">
            <text class="eyebrow">搭子圈邀请</text>
            <text class="invite-title">邀请好友加入</text>
          </view>
          <button class="modal-close-shell" :disabled="inviteSubmitting" @tap="closeInvitePicker">×</button>
        </view>

        <view v-if="inviteCandidates.length" class="invite-friends-card">
          <article v-for="(friend, index) in inviteCandidates" :key="friend.id" class="invite-friend-row">
            <button class="invite-friend-main" :disabled="inviteSubmitting" @tap="selectedFriendId = friend.id">
              <view :class="['invite-avatar-box', avatarToneClass(index)]">
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
import { useShareAppMessage } from '@tarojs/taro'
import { computed, onMounted, ref } from 'vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { getRouteParams, goBack, push, resolveSharePath } from '@/lib/navigation'
import { SocialService, type BuddyCircleDetail, type BuddyCircleMember, type FriendItem } from '@/services/social-service'
import { useAuthStore } from '@/stores/auth-store'

const params = getRouteParams() as { id?: string }
const socialService = new SocialService()
const authStore = useAuthStore()
const detail = ref<BuddyCircleDetail | null>(null)
const isLoading = ref(true)
const inviteModalVisible = ref(false)
const inviteLoading = ref(false)
const inviteSubmitting = ref(false)
const inviteLoadFailed = ref(false)
const friends = ref<FriendItem[]>([])
const selectedFriendId = ref('')
const avatarTones = ['tone-sage', 'tone-apricot', 'tone-lavender']
const circleId = computed(() => String(params.id || ''))
const orderedMembers = computed(() => {
  const members = detail.value?.members || []
  return [...members].sort((left, right) => {
    if (left.role === right.role) return 0
    return left.role === 'owner' ? -1 : 1
  })
})
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

useShareAppMessage(() => {
  const circle = detail.value?.circle
  const inviterId = authStore.user?.id || ''
  const sharedCircleId = circle?.id || circleId.value

  return {
    title: circle ? `邀请你加入「${circle.name}」搭子圈` : '邀请你加入 meow食堂搭子圈',
    path: resolveSharePath({
      name: 'circle-share-invite',
      params: { circleId: sharedCircleId, inviterId },
    }),
  }
})

onMounted(async () => {
  if (!(await requireAuth('circle-members'))) return
  await loadCircleDetail()
})

async function loadCircleDetail() {
  if (!circleId.value) {
    isLoading.value = false
    return
  }
  isLoading.value = true
  try {
    const { data } = await socialService.getCircleDetail(circleId.value)
    detail.value = data
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '加载圈成员失败')
  } finally {
    isLoading.value = false
  }
}

function openMember(userId: string) {
  push({ name: 'user-menu', params: { id: userId }, query: { circleId: circleId.value } })
}

function openInvitePicker() {
  if (!detail.value) return
  inviteModalVisible.value = true
  void ensureInviteCandidates()
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
  if (!circleId.value || !selectedFriendId.value) return
  inviteSubmitting.value = true
  try {
    const { data } = await socialService.inviteToCircle(circleId.value, {
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

function avatarInitial(name: string) {
  return (name || '?').trim().slice(0, 1).toUpperCase()
}

function avatarToneClass(index: number) {
  return avatarTones[index % avatarTones.length]
}

function memberMeta(member: BuddyCircleMember, index: number) {
  if (member.role === 'owner') return `发起了这个搭子圈 · 共享 ${member.sharedMenuCount} 份菜单`
  const fallback = [
    `共享了 ${member.sharedMenuCount} 份菜单`,
    `通过搭子圈可见 ${Math.max(member.sharedMenuCount, 1)} 份内容`,
    `圈内一起收藏了不少想吃的菜`,
  ]
  return fallback[index % fallback.length]
}

function inviteFriendMeta(friend: FriendItem, index: number) {
  if (friend.bio?.trim()) return friend.bio.trim()
  const fallback = [
    `你当前可见 ${friend.visibleMenuCount} 份菜单`,
    `共同收藏 ${Math.max(friend.sharedMenuCount, 1)} 道菜`,
    `一起吃过 ${Math.max(friend.sharedMenuCount, 1)} 次`,
  ]
  return fallback[index % fallback.length]
}
</script>

<style>
.circle-members-page {
  min-height: 100vh;
  background: #f7f6f3;
}

.circle-members-content {
  padding: 10px 20px 28px;
}

.circle-members-top-card,
.circle-members-intro-card,
.circle-members-list-card,
.circle-members-empty-card {
  background: #fff;
}

.circle-members-top-card,
.circle-members-list-head,
.circle-member-main,
.circle-member-name-row,
.circle-members-actions,
.invite-modal-head,
.invite-friend-main,
.invite-modal-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.circle-members-top-card {
  border-radius: 12px;
  padding: 16px;
}

.circle-members-actions {
  gap: 8px;
}

.share-shell {
  color: #1b3a2d;
}

.circle-members-title-stack {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.eyebrow {
  color: #9f5c38;
  font-size: 11px;
  font-weight: 800;
}

.title,
.intro-title,
.list-title,
.member-name {
  color: #151515;
  font-weight: 800;
}

.title {
  font-size: 16px;
}

.circle-members-intro-card {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin-top: 16px;
  border-radius: 14px;
  padding: 16px;
}

.intro-title {
  font-size: 22px;
}

.muted,
.intro-desc,
.circle-members-count {
  color: #787774;
  font-size: 12px;
}

.intro-desc {
  line-height: 1.5;
}

.circle-members-list-section {
  margin-top: 24px;
}

.circle-members-list-card,
.circle-members-empty-card {
  margin-top: 12px;
  border-radius: 14px;
  overflow: hidden;
}

.circle-member-main {
  width: 100%;
  justify-content: flex-start;
  gap: 12px;
  padding: 14px;
  border-bottom: 1px solid #f0ece6;
  text-align: left;
}

.member-avatar-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
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

.circle-member-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
}

.role-pill {
  border-radius: 999px;
  padding: 3px 8px;
  font-size: 11px;
  font-weight: 800;
}

.role-pill-owner {
  background: #f9ebdd;
  color: #9f5c38;
}

.role-pill-member {
  background: #edf3ec;
  color: #346538;
}

.circle-members-empty-card {
  padding: 18px;
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

.invite-modal-title,
.invite-friend-copy,
.invite-modal-footer {
  display: flex;
  flex-direction: column;
}

.invite-title {
  color: #151515;
  font-size: 16px;
  font-weight: 800;
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

.invite-friend-copy {
  flex: 1;
}

.invite-check {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: 1px solid #d8d0c6;
  border-radius: 999px;
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
