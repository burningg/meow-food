<template>
  <div class="page-shell friends-page">
    <div class="friends-content">
      <section class="friends-top-card">
        <button class="nav-shell" type="button" @click="router.back()">‹</button>
        <div class="friends-title-stack">
          <small>好友关系</small>
          <h1>我的好友</h1>
        </div>
        <button class="nav-shell nav-shell-add" type="button" @click="openAddFriendModal">＋</button>
      </section>

      <button class="friends-request-entry" type="button" @click="openFriendRequests">
        <div class="friends-request-copy">
          <strong>好友申请</strong>
          <p>待处理 {{ pendingRequestCount }} · 我发出的 {{ outgoingRequestCount }}</p>
        </div>
        <span class="friends-request-arrow">›</span>
      </button>

      <section class="friends-list-section">
        <div class="friends-list-head">
          <div>
            <h3>好友列表</h3>
            <p>点击可查看对方菜单</p>
          </div>
        </div>

        <div class="friends-simple-card">
          <article v-for="(friend, index) in friends" :key="friend.id" class="simple-friend-row">
            <button class="simple-friend-main" type="button" @click="openFriend(friend.id)">
              <div :class="['friend-avatar-box', avatarToneClass(index)]">
                <span>{{ avatarInitial(friend.nickname) }}</span>
              </div>
              <div class="simple-friend-copy">
                <strong>{{ friend.nickname }}</strong>
                <p>{{ simpleFriendMeta(friend, index) }}</p>
              </div>
            </button>
          </article>

          <article v-if="!friends.length" class="friends-empty-card">
            <strong>还没有好友</strong>
            <p>先发送一条好友申请，再一起分享菜单。</p>
          </article>
        </div>
      </section>
    </div>

    <div
      v-if="addFriendVisible"
      class="friends-modal-overlay"
      @click.self="closeAddFriendModal"
    >
      <section class="friends-modal-card">
        <div class="friends-modal-head">
          <div class="friends-modal-title">
            <small>好友关系</small>
            <h2>添加好友</h2>
            <p>输入对方账号即可发起好友申请，对方确认后会出现在彼此的好友列表里。</p>
          </div>
          <button class="modal-close-shell" type="button" :disabled="addFriendSubmitting" @click="closeAddFriendModal">×</button>
        </div>

        <div class="friends-modal-form">
          <label class="modal-field">
            <div class="modal-label-row">
              <span>对方账号</span>
              <small>必填</small>
            </div>
            <input
              v-model="friendAccount"
              class="modal-input"
              type="text"
              placeholder="例如：ali"
              maxlength="32"
              :disabled="addFriendSubmitting"
            />
          </label>

          <label class="modal-field">
            <div class="modal-label-row">
              <span>申请留言</span>
              <small class="accent">{{ friendMessage.length }}/60</small>
            </div>
            <textarea
              v-model="friendMessage"
              class="modal-textarea"
              placeholder="一起分享美味吧"
              maxlength="60"
              :disabled="addFriendSubmitting"
            ></textarea>
          </label>
        </div>

        <div class="friends-modal-divider"></div>

        <div class="friends-modal-footer">
          <p>发送后可在“好友申请”里查看状态。</p>
          <div class="friends-modal-actions">
            <button class="modal-action ghost" type="button" :disabled="addFriendSubmitting" @click="closeAddFriendModal">
              取消
            </button>
            <button
              class="modal-action primary"
              type="button"
              :disabled="!friendAccount.trim() || addFriendSubmitting"
              @click="submitAddFriend"
            >
              {{ addFriendSubmitting ? '发送中...' : '发送申请' }}
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { SocialService, type FriendItem, type FriendRequestItem } from '@/services/social-service'

const route = useRoute()
const router = useRouter()
const socialService = new SocialService()

const friends = ref<FriendItem[]>([])
const incomingRequests = ref<FriendRequestItem[]>([])
const outgoingRequests = ref<FriendRequestItem[]>([])
const addFriendVisible = ref(false)
const addFriendSubmitting = ref(false)
const friendAccount = ref('')
const friendMessage = ref('一起分享美味吧')

const avatarTones = [
  { box: 'tone-sage' },
  { box: 'tone-apricot' },
  { box: 'tone-lavender' },
]

const pendingRequestCount = computed(() => incomingRequests.value.filter((item) => item.status === 'pending').length)
const outgoingRequestCount = computed(() => outgoingRequests.value.length)

onMounted(() => {
  loadFriendsPageData()
})

onMounted(handleInitialAction)

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
  if (route.query.action !== 'add') return
  openAddFriendModal()
  router.replace({ name: 'friends' })
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
  router.push({ name: 'user-menu', params: { id: userId } })
}

function openFriendRequests() {
  router.push({ name: 'friend-requests' })
}

function avatarInitial(name: string) {
  const text = (name || '?').trim()
  return text.slice(0, 1).toUpperCase()
}

function avatarToneClass(index: number) {
  return avatarTones[index % avatarTones.length].box
}

function simpleFriendMeta(friend: FriendItem, index: number) {
  if (friend.bio?.trim()) {
    return friend.bio.trim()
  }
  const fallback = [
    `好友可见 ${friend.visibleMenuCount} 份菜单`,
    `共同收藏 ${Math.max(friend.sharedMenuCount, 1)} 道菜`,
    `一起吃过 ${Math.max(friend.sharedMenuCount, 1)} 次`,
  ]
  return fallback[index % fallback.length]
}
</script>

<style scoped>
.friends-page {
  position: relative;
  min-height: 100vh;
  background: #f7f6f3;
}

.friends-top-card,
.friends-list-head,
.simple-friend-main,
.friends-modal-head,
.modal-label-row,
.friends-modal-actions {
  display: flex;
}

.friends-top-card,
.friends-list-head,
.friends-modal-head,
.modal-label-row {
  justify-content: space-between;
}

.friends-top-card,
.simple-friend-main,
.modal-label-row,
.friends-modal-actions {
  align-items: center;
}

.friends-content {
  padding: 20px 20px 24px;
}

.friends-top-card,
.friends-request-entry,
.friends-simple-card,
.friends-modal-card {
  background: #ffffff;
}

.friends-top-card {
  padding: 16px;
  border-radius: 12px;
}

.nav-shell,
.modal-close-shell {
  border: none;
  border-radius: 999px;
  background: #f5efe7;
  color: #151515;
}

.nav-shell {
  width: 36px;
  height: 36px;
  font-size: 24px;
  line-height: 1;
}

.nav-shell-add {
  color: #9f5c38;
}

.friends-title-stack {
  text-align: center;
}

.friends-title-stack small,
.friends-modal-title small {
  display: block;
  margin-bottom: 3px;
  font-size: 11px;
  font-weight: 600;
  color: #9f5c38;
}

.friends-title-stack h1,
.friends-modal-title h2,
.friends-list-head h3 {
  margin: 0;
  color: #151515;
}

.friends-title-stack h1 {
  font-size: 16px;
  font-weight: 600;
}

.friends-request-entry {
  width: 100%;
  margin-top: 16px;
  padding: 14px 16px;
  border: none;
  border-radius: 12px;
  text-align: left;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.friends-request-copy strong {
  display: block;
  margin-bottom: 4px;
  font-size: 15px;
  font-weight: 600;
  color: #151515;
}

.friends-request-copy p,
.friends-list-head p,
.simple-friend-copy p,
.friends-empty-card p,
.friends-modal-title p,
.friends-modal-footer p {
  margin: 0;
  color: #787774;
}

.friends-request-copy p,
.simple-friend-copy p {
  font-size: 12px;
}

.friends-request-arrow {
  font-size: 20px;
  color: #b3aca3;
}

.friends-list-section {
  margin-top: 26px;
}

.friends-list-head {
  margin-bottom: 12px;
}

.friends-list-head h3 {
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 600;
}

.friends-list-head p {
  margin-top: 4px;
  font-size: 11px;
}

.friends-simple-card {
  border-radius: 12px;
  padding: 18px;
}

.simple-friend-row + .simple-friend-row {
  border-top: 1px solid #ece5dc;
}

.simple-friend-main {
  width: 100%;
  gap: 18px;
  padding: 0;
  min-height: 112px;
  border: none;
  background: transparent;
  text-align: left;
}

.simple-friend-row:first-child .simple-friend-main {
  padding-top: 0;
}

.simple-friend-copy strong {
  display: block;
  margin-bottom: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #151515;
}

.friend-avatar-box {
  width: 54px;
  height: 54px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.friend-avatar-box span {
  font-size: 18px;
  font-weight: 600;
}

.tone-sage {
  background: #edf3ec;
}

.tone-sage span,
.status-sage {
  color: #346538;
}

.tone-apricot {
  background: #f9ebdd;
}

.tone-apricot span,
.status-apricot {
  color: #9f5c38;
}

.tone-lavender {
  background: #eeeaf7;
}

.tone-lavender span,
.status-lavender {
  color: #6c58a5;
}

.friends-empty-card {
  padding: 8px 0;
}

.friends-empty-card strong {
  display: block;
  margin-bottom: 6px;
  color: #151515;
}

.friends-modal-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 208px 20px 32px;
  background: rgba(21, 21, 21, 0.45);
}

.friends-modal-card {
  width: 100%;
  max-width: 350px;
  border-radius: 20px;
  padding: 18px;
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.12);
}

.friends-modal-title {
  max-width: 236px;
}

.friends-modal-title h2 {
  font-family: var(--font-serif);
  font-size: 24px;
  font-weight: 600;
}

.friends-modal-title p {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
}

.modal-close-shell {
  width: 34px;
  height: 34px;
  font-size: 24px;
  line-height: 1;
  color: #787774;
}

.friends-modal-form {
  margin-top: 16px;
  display: grid;
  gap: 14px;
}

.modal-field {
  display: grid;
  gap: 8px;
}

.modal-label-row span,
.modal-label-row small {
  font-size: 12px;
}

.modal-label-row span {
  font-weight: 600;
  color: #151515;
}

.modal-label-row small {
  color: #787774;
}

.modal-label-row small.accent {
  color: #9f5c38;
}

.modal-input,
.modal-textarea {
  width: 100%;
  border: 1px solid #efe5db;
  border-radius: 16px;
  background: #fbf8f4;
  color: #2f3437;
  resize: none;
}

.modal-input::placeholder,
.modal-textarea::placeholder {
  color: #9b948b;
}

.modal-input {
  height: 52px;
  padding: 0 16px;
}

.modal-textarea {
  min-height: 98px;
  padding: 14px 16px;
}

.friends-modal-divider {
  height: 1px;
  margin: 16px 0 12px;
  background: #ece5dc;
}

.friends-modal-footer p {
  font-size: 11px;
}

.friends-modal-actions {
  gap: 10px;
  margin-top: 12px;
}

.modal-action {
  flex: 1;
  height: 46px;
  border: none;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}

.modal-action.ghost {
  background: #f3eee8;
  color: #151515;
}

.modal-action.primary {
  background: #9f5c38;
  color: #ffffff;
}

.modal-action:disabled,
.modal-close-shell:disabled,
.nav-shell:disabled {
  opacity: 0.55;
}

@media (max-width: 420px) {
  .friends-content {
    padding-left: 16px;
    padding-right: 16px;
  }

  .friends-modal-overlay {
    padding-left: 12px;
    padding-right: 12px;
  }
}
</style>
