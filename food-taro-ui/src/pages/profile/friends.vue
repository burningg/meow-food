<template>
  <view class="page-shell friends-page">
    <view class="friends-content">
      <section class="friends-top-card">
        <button class="nav-shell" @tap="goBack()">‹</button>
        <view class="friends-title-stack">
          <text class="eyebrow">好友关系</text>
          <text class="title">我的好友</text>
        </view>
        <button class="nav-shell nav-shell-add" @tap="openAddFriendModal">＋</button>
      </section>

      <button class="friends-request-entry" @tap="openFriendRequests">
        <view class="friends-request-copy">
          <text class="entry-title">好友申请</text>
          <text class="muted">待处理 {{ pendingRequestCount }} · 我发出的 {{ outgoingRequestCount }}</text>
        </view>
        <text class="friends-request-arrow">›</text>
      </button>

      <section class="friends-list-section">
        <view class="friends-list-head">
          <view>
            <text class="list-title">好友列表</text>
            <text class="muted">点击可查看对方菜单</text>
          </view>
        </view>

        <view class="friends-simple-card">
          <article v-for="(friend, index) in friends" :key="friend.id" class="simple-friend-row">
            <button class="simple-friend-main" @tap="openFriend(friend.id)">
              <view :class="['friend-avatar-box', avatarToneClass(index)]">
                <text>{{ avatarInitial(friend.nickname) }}</text>
              </view>
              <view class="simple-friend-copy">
                <text class="friend-name">{{ friend.nickname }}</text>
                <text class="muted">{{ simpleFriendMeta(friend, index) }}</text>
              </view>
            </button>
          </article>

          <article v-if="!friends.length" class="friends-empty-card">
            <text class="entry-title">还没有好友</text>
            <text class="muted">先发送一条好友申请，再一起分享菜单。</text>
          </article>
        </view>
      </section>
    </view>

    <view v-if="addFriendVisible" class="friends-modal-overlay" @tap="closeAddFriendModal">
      <section class="friends-modal-card" @tap.stop>
        <view class="friends-modal-head">
          <view class="friends-modal-title">
            <text class="eyebrow">好友关系</text>
            <text class="modal-title">添加好友</text>
            <text class="muted">输入对方账号即可发起好友申请，对方确认后会出现在彼此的好友列表里。</text>
          </view>
          <button class="modal-close-shell" :disabled="addFriendSubmitting" @tap="closeAddFriendModal">×</button>
        </view>

        <view class="friends-modal-form">
          <label class="modal-field">
            <view class="modal-label-row">
              <text>对方账号</text>
              <text class="muted">必填</text>
            </view>
            <input v-model="friendAccount" class="modal-input" maxlength="32" placeholder="例如：ali" :disabled="addFriendSubmitting" />
          </label>

          <label class="modal-field">
            <view class="modal-label-row">
              <text>申请留言</text>
              <text class="accent">{{ friendMessage.length }}/60</text>
            </view>
            <textarea v-model="friendMessage" class="modal-textarea" maxlength="60" placeholder="一起分享美味吧" :disabled="addFriendSubmitting" />
          </label>
        </view>

        <view class="friends-modal-divider"></view>

        <view class="friends-modal-footer">
          <text class="muted">发送后可在“好友申请”里查看状态。</text>
          <view class="friends-modal-actions">
            <button class="modal-action ghost" :disabled="addFriendSubmitting" @tap="closeAddFriendModal">取消</button>
            <button class="modal-action primary" :disabled="!friendAccount.trim() || addFriendSubmitting" @tap="submitAddFriend">
              {{ addFriendSubmitting ? '发送中...' : '发送申请' }}
            </button>
          </view>
        </view>
      </section>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { getRouteParams, goBack, push, replace } from '@/lib/navigation'
import { SocialService, type FriendItem, type FriendRequestItem } from '@/services/social-service'

const params = getRouteParams() as { action?: string }
const socialService = new SocialService()
const friends = ref<FriendItem[]>([])
const incomingRequests = ref<FriendRequestItem[]>([])
const outgoingRequests = ref<FriendRequestItem[]>([])
const addFriendVisible = ref(false)
const addFriendSubmitting = ref(false)
const friendAccount = ref('')
const friendMessage = ref('一起分享美味吧')

const avatarTones = ['tone-sage', 'tone-apricot', 'tone-lavender']
const pendingRequestCount = computed(() => incomingRequests.value.filter((item) => item.status === 'pending').length)
const outgoingRequestCount = computed(() => outgoingRequests.value.length)

onMounted(async () => {
  if (!(await requireAuth('friends'))) return
  await loadFriendsPageData()
  handleInitialAction()
})

async function loadFriendsPageData() {
  const [friendsResult, requestsResult] = await Promise.allSettled([
    socialService.getFriends(),
    socialService.getFriendRequests(),
  ])

  if (friendsResult.status === 'fulfilled') {
    friends.value = friendsResult.value.data
  } else {
    const error: any = friendsResult.reason
    Message.error(error?.response?.data?.message || '加载好友列表失败')
  }

  if (requestsResult.status === 'fulfilled') {
    incomingRequests.value = requestsResult.value.data.incoming
    outgoingRequests.value = requestsResult.value.data.outgoing
  } else {
    incomingRequests.value = []
    outgoingRequests.value = []
  }
}

function handleInitialAction() {
  if (params.action !== 'add') return
  openAddFriendModal()
  replace('friends')
}

function openAddFriendModal() {
  resetAddFriendForm()
  addFriendVisible.value = true
}

function closeAddFriendModal() {
  if (addFriendSubmitting.value) return
  addFriendVisible.value = false
  resetAddFriendForm()
}

function resetAddFriendForm() {
  friendAccount.value = ''
  friendMessage.value = '一起分享美味吧'
}

async function submitAddFriend() {
  const targetAccount = friendAccount.value.trim()
  if (!targetAccount) return false
  addFriendSubmitting.value = true
  try {
    await socialService.createFriendRequest({
      targetAccount,
      message: friendMessage.value.trim() || '一起分享美味吧',
    })
    Message.success('好友申请已发送')
    closeAddFriendModal()
    await loadFriendsPageData()
    return true
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '发送失败')
    return false
  } finally {
    addFriendSubmitting.value = false
  }
}

function openFriend(userId: string) {
  push({ name: 'user-menu', params: { id: userId } })
}

function openFriendRequests() {
  push('friend-requests')
}

function avatarInitial(name: string) {
  return (name || '?').trim().slice(0, 1).toUpperCase()
}

function avatarToneClass(index: number) {
  return avatarTones[index % avatarTones.length]
}

function simpleFriendMeta(friend: FriendItem, index: number) {
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
.friends-page {
  position: relative;
  min-height: 100vh;
  background: #f7f6f3;
}

.friends-content {
  padding: 20px 20px 24px;
}

.friends-top-card,
.friends-request-entry,
.friends-simple-card,
.friends-modal-card {
  background: #fff;
}

.friends-top-card,
.friends-request-entry,
.friends-list-head,
.simple-friend-main,
.friends-modal-head,
.modal-label-row,
.friends-modal-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.friends-top-card,
.friends-request-entry,
.friends-simple-card {
  border-radius: 12px;
}

.friends-top-card {
  padding: 16px;
}

.friends-title-stack {
  display: flex;
  flex-direction: column;
  text-align: center;
}

.eyebrow {
  color: #9f5c38;
  font-size: 11px;
  font-weight: 800;
}

.title,
.entry-title,
.list-title,
.modal-title,
.friend-name {
  color: #151515;
  font-weight: 800;
}

.title {
  font-size: 16px;
}

.muted {
  color: #787774;
  font-size: 12px;
  line-height: 1.5;
}

.friends-request-entry {
  width: 100%;
  margin-top: 16px;
  padding: 14px 16px;
  text-align: left;
}

.friends-request-copy,
.friends-list-head view,
.simple-friend-copy,
.friends-modal-title,
.friends-modal-footer {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.friends-list-section {
  margin-top: 24px;
}

.friends-simple-card {
  margin-top: 12px;
  overflow: hidden;
}

.simple-friend-main {
  width: 100%;
  justify-content: flex-start;
  gap: 12px;
  padding: 14px;
  border-bottom: 1px solid #f0ece6;
  text-align: left;
}

.friend-avatar-box {
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

.friends-empty-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 18px;
}

.friends-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: flex-end;
  background: rgba(0, 0, 0, 0.35);
}

.friends-modal-card {
  width: 100%;
  border-radius: 24px 24px 0 0;
  padding: 20px;
}

.modal-close-shell {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: #f5efe7;
  color: #151515;
  font-size: 22px;
}

.friends-modal-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 18px;
}

.modal-field text:first-child {
  color: #151515;
  font-weight: 800;
}

.modal-input,
.modal-textarea {
  border-radius: 14px;
  background: #f6f2ec;
  padding: 0 14px;
}

.modal-input {
  min-height: 48px;
}

.modal-textarea {
  min-height: 92px;
  padding-top: 12px;
}

.accent {
  color: var(--accent);
  font-size: 12px;
}

.friends-modal-divider {
  height: 1px;
  margin: 18px 0;
  background: var(--line);
}

.friends-modal-actions {
  gap: 10px;
  margin-top: 12px;
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
