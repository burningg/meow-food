<template>
  <div class="page-shell friends-page">
    <header class="top-nav">
      <button class="icon-button" type="button" @click="router.back()">‹</button>
      <div class="title-block">
        <small>{{ selectionMode ? '圈子邀请' : '好友关系' }}</small>
        <h1>{{ selectionMode ? '邀请好友加入搭子圈' : '我的好友' }}</h1>
      </div>
      <button class="icon-button" type="button" @click="loadData">↻</button>
    </header>

    <section class="summary-card">
      <div>
        <strong>{{ friends.length }}</strong>
        <span>位好友</span>
      </div>
      <div>
        <strong>{{ availableInviteCount }}</strong>
        <span>位可邀请</span>
      </div>
      <div>
        <strong>{{ summaryLabel }}</strong>
        <span>{{ selectionMode ? '当前搭子圈' : '当前状态' }}</span>
      </div>
    </section>

    <section class="section">
      <div class="section-head">
        <div>
          <small>{{ selectionMode ? '成员选择' : '好友列表' }}</small>
          <h2>{{ selectionMode ? '从好友中挑选要加入的人' : '一起吃饭的朋友们' }}</h2>
        </div>
        <span class="section-note">{{ selectionMode ? '只能邀请已是好友的人' : '点击可查看对方菜单' }}</span>
      </div>

      <article
        v-for="friend in friends"
        :key="friend.id"
        :class="['friend-row', { disabled: selectionMode && currentMemberIds.has(friend.id) }]"
      >
        <button class="friend-main" type="button" @click="handleRowClick(friend)">
          <img class="avatar" :src="friend.avatar" :alt="friend.nickname" />
          <div class="copy">
            <div class="title-line">
              <strong>{{ friend.nickname }}</strong>
              <span v-if="selectionMode && currentMemberIds.has(friend.id)" class="state-chip muted">已在当前圈子</span>
              <span v-else-if="selectionMode" class="state-chip">可邀请</span>
            </div>
            <p>@{{ friend.account }} · {{ friend.bio || '一起分享菜单的好友' }}</p>
            <div class="meta-line">
              <span>可访问 {{ friend.visibleMenuCount }} 份菜单</span>
              <span>共享 {{ friend.sharedMenuCount }} 份菜单</span>
            </div>
          </div>
        </button>

        <button
          v-if="selectionMode"
          class="select-button"
          type="button"
          :disabled="currentMemberIds.has(friend.id) || submitting"
          @click="toggleSelect(friend)"
        >
          {{ selectedFriendId === friend.id ? '已选中' : currentMemberIds.has(friend.id) ? '不可选' : '选择' }}
        </button>

        <button v-else class="ghost-link" type="button" @click="openFriend(friend.id)">查看菜单</button>
      </article>

      <article v-if="!friends.length" class="empty-card">
        <strong>{{ selectionMode ? '你还没有可邀请的好友' : '还没有好友' }}</strong>
        <p>{{ selectionMode ? '先去添加好友，再把他们邀请进搭子圈。' : '从用户菜单页发起好友申请后，就能在这里管理好友。' }}</p>
      </article>
    </section>

    <div v-if="selectionMode" class="bottom-bar">
      <div>
        <strong>{{ selectedFriendName || '请选择一位好友' }}</strong>
        <p>{{ selectedFriendName ? '确认后会直接加入当前搭子圈。' : '从好友列表里选择一位成员加入。' }}</p>
      </div>
      <button class="primary-button invite-button" type="button" :disabled="!selectedFriendId || submitting" @click="submitInvite">
        {{ submitting ? '邀请中...' : '确认邀请' }}
      </button>
    </div>
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

const selectionMode = computed(() => typeof route.query.circleId === 'string' && route.query.circleId.length > 0)
const circleId = computed(() => (typeof route.query.circleId === 'string' ? route.query.circleId : ''))
const summaryLabel = computed(() => {
  if (!selectionMode.value) return '已互相添加'
  return typeof route.query.circleName === 'string' && route.query.circleName ? route.query.circleName : '当前圈子'
})
const currentMemberIds = computed(() => new Set((circleDetail.value?.members || []).map((member) => member.id)))
const availableInviteCount = computed(() => friends.value.filter((friend) => !currentMemberIds.value.has(friend.id)).length)
const selectedFriendName = computed(() => friends.value.find((friend) => friend.id === selectedFriendId.value)?.nickname || '')

onMounted(loadData)

async function loadData() {
  try {
    const { data } = await socialService.getFriends()
    friends.value = data
    if (selectionMode.value && circleId.value) {
      const { data: detailData } = await socialService.getCircleDetail(circleId.value)
      circleDetail.value = detailData
    }
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '加载好友列表失败')
  }
}

function openFriend(userId: string) {
  router.push({ name: 'user-menu', params: { id: userId } })
}

function handleRowClick(friend: FriendItem) {
  if (selectionMode.value) {
    toggleSelect(friend)
    return
  }
  openFriend(friend.id)
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
.bottom-bar {
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
.bottom-bar {
  align-items: center;
}

.top-nav {
  margin-bottom: 18px;
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
.empty-card p {
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
  font-family: 'Playfair Display', serif;
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
  font-size: 0.82rem;
  color: var(--text-muted);
}

.section {
  padding: 18px;
}

.section-head {
  align-items: end;
  gap: 12px;
  margin-bottom: 14px;
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
