<template>
  <div class="page-shell friends-page">
    <template v-if="selectionMode">
      <header class="top-nav">
        <button class="icon-button" type="button" @click="router.back()">‹</button>
        <div class="title-block">
          <small>圈子邀请</small>
          <h1>邀请好友加入搭子圈</h1>
        </div>
        <div class="top-actions">
          <button class="icon-button" type="button" @click="loadCircleInviteData">↻</button>
        </div>
      </header>

      <section class="summary-card">
        <div>
          <strong>{{ friends.length }}</strong>
          <span>位好友</span>
        </div>
        <div>
          <strong>{{ circleSummaryLabel }}</strong>
          <span>当前搭子圈</span>
        </div>
      </section>

      <section class="section">
        <div class="section-head">
          <div>
            <small>成员选择</small>
            <h2>从好友中挑选要加入的人</h2>
          </div>
          <span class="section-note">只能邀请已是好友的人</span>
        </div>

        <article
          v-for="friend in friends"
          :key="friend.id"
          :class="['friend-row', { disabled: currentMemberIds.has(friend.id) }]"
        >
          <button class="friend-main" type="button" @click="toggleSelect(friend)">
            <img class="avatar" :src="friend.avatar" :alt="friend.nickname" />
            <div class="copy">
              <div class="title-line">
                <strong>{{ friend.nickname }}</strong>
                <span v-if="currentMemberIds.has(friend.id)" class="state-chip muted">已在当前圈子</span>
                <span v-else class="state-chip">可邀请</span>
              </div>
              <p>@{{ friend.account }} · {{ friend.bio || '一起分享菜单的好友' }}</p>
              <div class="meta-line">
                <span>可访问 {{ friend.visibleMenuCount }} 份菜单</span>
                <span>共享 {{ friend.sharedMenuCount }} 份菜单</span>
              </div>
            </div>
          </button>

          <button
            class="select-button"
            type="button"
            :disabled="currentMemberIds.has(friend.id) || submitting"
            @click="toggleSelect(friend)"
          >
            {{ selectedFriendId === friend.id ? '已选中' : currentMemberIds.has(friend.id) ? '不可选' : '选择' }}
          </button>
        </article>

        <article v-if="!friends.length" class="empty-card">
          <strong>你还没有可邀请的好友</strong>
          <p>先去添加好友，再把他们邀请进搭子圈。</p>
        </article>
      </section>

      <div class="bottom-bar">
        <div>
          <strong>{{ selectedFriendName || '请选择一位好友' }}</strong>
          <p>{{ selectedFriendName ? '确认后会直接加入当前搭子圈。' : '从好友列表里选择一位成员加入。' }}</p>
        </div>
        <button class="primary-button invite-button" type="button" :disabled="!selectedFriendId || submitting" @click="submitInvite">
          {{ submitting ? '邀请中...' : '确认邀请' }}
        </button>
      </div>
    </template>

    <template v-else>
      <header class="top-nav">
        <button class="icon-button" type="button" @click="router.back()">‹</button>
        <div class="title-block">
          <small>好友关系</small>
          <h1>我的好友</h1>
        </div>
        <div class="top-actions">
          <button class="icon-button" type="button" @click="openAddFriendModal">＋</button>
          <button class="icon-button" type="button" @click="loadFriendsPageData">↻</button>
        </div>
      </header>

      <section class="summary-card">
        <div>
          <strong>{{ friends.length }}</strong>
          <span>位好友</span>
        </div>
        <div>
          <strong>已互相添加</strong>
          <span>当前状态</span>
        </div>
      </section>

      <section class="section">
        <div class="section-head">
          <div>
            <small>好友列表</small>
            <h2>一起吃饭的朋友们</h2>
          </div>
          <span class="section-note">点击可查看对方菜单</span>
        </div>

        <article
          v-for="friend in friends"
          :key="friend.id"
          class="friend-row"
        >
          <button class="friend-main" type="button" @click="openFriend(friend.id)">
            <img class="avatar" :src="friend.avatar" :alt="friend.nickname" />
            <div class="copy">
              <div class="title-line">
                <strong>{{ friend.nickname }}</strong>
              </div>
              <p>@{{ friend.account }} · {{ friend.bio || '一起分享菜单的好友' }}</p>
              <div class="meta-line">
                <span>可访问 {{ friend.visibleMenuCount }} 份菜单</span>
                <span>共享 {{ friend.sharedMenuCount }} 份菜单</span>
              </div>
            </div>
          </button>

          <button class="ghost-link" type="button" @click="openFriend(friend.id)">查看菜单</button>
        </article>

        <article v-if="!friends.length" class="empty-card">
          <strong>还没有好友</strong>
          <p>从用户菜单页发起好友申请后，就能在这里管理好友。</p>
        </article>
      </section>
    </template>

    <a-modal
      v-model:visible="addFriendVisible"
      title="添加好友"
      :ok-button-props="{ loading: addFriendSubmitting, disabled: !friendAccount.trim() }"
      @before-ok="submitAddFriend"
      @cancel="resetAddFriendForm"
    >
      <div class="modal-copy">
        <p>输入对方账号即可发起好友申请。</p>
        <span>对方确认后，你们就会出现在彼此的好友列表里。</span>
      </div>
      <a-input
        v-model="friendAccount"
        placeholder="例如 ali"
        allow-clear
        :max-length="32"
      />
      <a-textarea
        v-model="friendMessage"
        placeholder="一起分享美味吧"
        :max-length="60"
        allow-clear
        :auto-size="{ minRows: 3, maxRows: 5 }"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { SocialService, type BuddyCircleDetail, type FriendItem } from '@/services/social-service'

const route = useRoute()
const router = useRouter()
const socialService = new SocialService()

const friends = ref<FriendItem[]>([])
const circleDetail = ref<BuddyCircleDetail | null>(null)
const selectedFriendId = ref<string | null>(null)
const submitting = ref(false)
const addFriendVisible = ref(false)
const addFriendSubmitting = ref(false)
const friendAccount = ref('')
const friendMessage = ref('一起分享美味吧')

const selectionMode = computed(() => typeof route.query.circleId === 'string' && route.query.circleId.length > 0)
const circleId = computed(() => (typeof route.query.circleId === 'string' ? route.query.circleId : ''))
const circleSummaryLabel = computed(() => {
  return typeof route.query.circleName === 'string' && route.query.circleName ? route.query.circleName : '当前圈子'
})
const currentMemberIds = computed(() => new Set((circleDetail.value?.members || []).map((member) => member.id)))
const selectedFriendName = computed(() => friends.value.find((friend) => friend.id === selectedFriendId.value)?.nickname || '')

onMounted(() => {
  if (selectionMode.value) {
    loadCircleInviteData()
    return
  }
  loadFriendsPageData()
})
onMounted(handleInitialAction)

async function loadFriendsPageData() {
  try {
    const { data } = await socialService.getFriends()
    friends.value = data
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '加载好友列表失败')
  }
}

async function loadCircleInviteData() {
  if (!circleId.value) return
  try {
    const { data } = await socialService.getFriends()
    friends.value = data
    const { data: detailData } = await socialService.getCircleDetail(circleId.value)
    circleDetail.value = detailData
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '加载好友列表失败')
  }
}

function handleInitialAction() {
  if (selectionMode.value || route.query.action !== 'add') return
  openAddFriendModal()
  router.replace({ name: 'friends' })
}

function openAddFriendModal() {
  resetAddFriendForm()
  addFriendVisible.value = true
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
    resetAddFriendForm()
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

function toggleSelect(friend: FriendItem) {
  if (!selectionMode.value || currentMemberIds.value.has(friend.id)) return
  selectedFriendId.value = selectedFriendId.value === friend.id ? null : friend.id
}

async function submitInvite() {
  if (!selectionMode.value || !selectedFriendId.value || !circleId.value) return
  submitting.value = true
  try {
    await socialService.inviteToCircle(circleId.value, { inviteeUserId: selectedFriendId.value })
    Message.success('好友已加入搭子圈')
    router.replace({ name: 'circle-detail', params: { id: circleId.value } })
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '邀请失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.friends-page {
  padding-bottom: 128px;
}

.top-nav,
.summary-card,
.section-head,
.friend-main,
.title-line,
.meta-line,
.bottom-bar,
.top-actions {
  display: flex;
}

.top-nav,
.section-head,
.title-line,
.bottom-bar {
  justify-content: space-between;
}

.top-nav,
.summary-card,
.friend-main,
.bottom-bar,
.top-actions {
  align-items: center;
}

.top-nav {
  margin-bottom: 18px;
}

.top-actions {
  gap: 8px;
}

.icon-button {
  border: none;
  background: #fff;
  width: 38px;
  height: 38px;
  border-radius: 999px;
  box-shadow: 0 10px 24px rgba(27, 58, 45, 0.08);
}

.title-block {
  text-align: center;
}

.title-block small,
.section-note,
.copy p,
.meta-line,
.bottom-bar p,
.empty-card p,
.modal-copy p,
.modal-copy span {
  color: var(--text-muted);
}

.title-block h1,
.section-head h2,
.summary-card strong,
.bottom-bar strong {
  margin: 0;
  color: var(--text-main);
}

.title-block h1,
.section-head h2 {
  font-family: var(--font-serif);
  font-weight: 600;
  line-height: 1.25;
}

.title-block h1 {
  font-size: var(--title-sm);
}

.section-head h2 {
  font-size: var(--title-lg);
}

.summary-card,
.section,
.bottom-bar {
  background: rgba(255, 255, 255, 0.94);
  border-radius: 24px;
  box-shadow: 0 18px 36px rgba(27, 58, 45, 0.08);
}

.summary-card {
  gap: 12px;
  justify-content: space-between;
  padding: 18px;
  margin-bottom: 16px;
}

.summary-card div {
  flex: 1;
  min-width: 0;
}

.summary-card strong,
.summary-card span {
  display: block;
}

.summary-card span {
  margin-top: 6px;
  font-size: var(--text-sm);
  color: var(--text-muted);
}

.section {
  padding: 18px;
}

.modal-copy {
  display: grid;
  gap: 4px;
  margin-bottom: 14px;
}

.modal-copy p,
.modal-copy span {
  margin: 0;
}

.section-head {
  align-items: end;
  gap: 12px;
  margin-bottom: 14px;
}

.section-note,
.copy p,
.meta-line,
.bottom-bar p,
.empty-card p,
.modal-copy p,
.modal-copy span {
  font-size: var(--text-sm);
}

.friend-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  border-top: 1px solid var(--line);
}

.friend-row:first-of-type {
  border-top: none;
  padding-top: 0;
}

.friend-row.disabled {
  opacity: 0.72;
}

.friend-main {
  flex: 1;
  gap: 12px;
  min-width: 0;
  border: none;
  background: transparent;
  padding: 0;
  text-align: left;
}

.avatar {
  width: 54px;
  height: 54px;
  border-radius: 16px;
  object-fit: cover;
  background: #f3eee8;
}

.copy {
  min-width: 0;
}

.copy strong,
.copy p,
.meta-line span {
  display: block;
}

.copy p,
.meta-line {
  margin: 6px 0 0;
  font-size: 0.82rem;
  line-height: 1.5;
}

.title-line {
  align-items: center;
  gap: 8px;
}

.meta-line {
  gap: 12px;
  flex-wrap: wrap;
}

.state-chip,
.select-button {
  border-radius: 999px;
  padding: 7px 12px;
  font-size: 0.76rem;
}

.state-chip {
  background: #edf5ee;
  color: #346538;
}

.state-chip.muted {
  background: #f3efe9;
  color: #8b8b8b;
}

.select-button {
  border: none;
  background: #f5ede7;
  color: var(--accent);
  font-weight: 700;
}

.select-button:disabled {
  color: #aaa29a;
  background: #f3f0eb;
}

.empty-card {
  border-radius: 18px;
  background: #fbf8f4;
  padding: 18px;
}

.empty-card strong,
.empty-card p {
  display: block;
}

.empty-card p {
  margin: 8px 0 0;
}

.bottom-bar {
  position: fixed;
  left: 16px;
  right: 16px;
  bottom: 16px;
  padding: 16px;
  gap: 14px;
}

.invite-button {
  min-width: 120px;
  white-space: nowrap;
}
</style>
