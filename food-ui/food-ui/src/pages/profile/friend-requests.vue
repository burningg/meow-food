<template>
  <div class="page-shell requests-page">
    <header class="top-nav">
      <button class="icon-button" type="button" @click="router.back()">‹</button>
      <div class="title-block">
        <h1>好友申请</h1>
      </div>
      <button class="icon-button" type="button" @click="loadData">↻</button>
    </header>
   
    <section class="section">
      <div class="section-head">
        <div>
          <h2>待处理申请</h2>
        </div>
        <span class="section-note">48 小时内处理更礼貌</span>
      </div>

      <div v-if="pendingIncoming.length" class="request-list">
        <article v-for="item in pendingIncoming" :key="item.id" class="request-card">
          <div class="request-head">
            <div class="user-block">
              <img class="avatar" :src="item.requesterAvatar" :alt="item.requesterNickname" />
              <div>
                <strong>{{ item.requesterNickname }}</strong>
                <p>{{ formatRequestMeta(item) }}</p>
              </div>
            </div>
            <span class="time-chip">{{ formatRelativeTime(item.createdAt) }}</span>
          </div>

          <div class="message-box">
            {{ item.message || '这个人很安静，暂时没有留言。' }}
          </div>

          <div class="action-row">
            <button class="secondary-button action-button" type="button" :disabled="loadingId === item.id" @click="handleReject(item.id)">
              暂不接受
            </button>
            <button class="primary-button action-button" type="button" :disabled="loadingId === item.id" @click="handleAccept(item.id)">
              {{ loadingId === item.id ? '处理中...' : '接受申请' }}
            </button>
          </div>
        </article>
      </div>

      <article v-else class="empty-card">
        <strong>暂时没有待处理申请</strong>
      </article>
    </section>

    <section class="section">
      <div class="section-head">
        <div>
          <h2>我发出的申请</h2>
        </div>
        <span class="section-note">对方确认后自动成为好友</span>
      </div>

      <div v-if="outgoing.length" class="request-list">
        <article v-for="item in outgoing" :key="item.id" class="request-card compact">
          <div class="request-head">
            <div class="user-block">
              <img class="avatar" :src="item.targetAvatar" :alt="item.targetNickname" />
              <div>
                <strong>{{ item.targetNickname }}</strong>
                <p>{{ formatOutgoingMeta(item) }}</p>
              </div>
            </div>
            <span :class="['status-tag', item.status]">{{ statusLabelMap[item.status] }}</span>
          </div>

          <div class="message-line">留言：{{ item.message || '未填写留言' }}</div>
        </article>
      </div>

      <article v-else class="empty-card">
        <strong>你还没有发出新的申请</strong>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { SocialService, type FriendRequestItem } from '@/services/social-service'

const router = useRouter()
const socialService = new SocialService()

const incoming = ref<FriendRequestItem[]>([])
const outgoing = ref<FriendRequestItem[]>([])
const loadingId = ref<string | null>(null)

const pendingIncoming = computed(() => incoming.value.filter((item) => item.status === 'pending'))
const acceptedIncoming = computed(() => incoming.value.filter((item) => item.status === 'accepted'))
const pendingOutgoing = computed(() => outgoing.value.filter((item) => item.status === 'pending'))

const statusLabelMap: Record<FriendRequestItem['status'], string> = {
  pending: '等待确认',
  accepted: '已通过',
  rejected: '已拒绝',
  cancelled: '已取消',
}

onMounted(loadData)

async function loadData() {
  try {
    const { data } = await socialService.getFriendRequests()
    incoming.value = data.incoming
    outgoing.value = data.outgoing
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '加载好友申请失败')
  }
}

async function handleAccept(requestId: string) {
  loadingId.value = requestId
  try {
    await socialService.acceptFriendRequest(requestId)
    Message.success('已成为好友')
    await loadData()
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '接受申请失败')
  } finally {
    loadingId.value = null
  }
}

async function handleReject(requestId: string) {
  loadingId.value = requestId
  try {
    await socialService.rejectFriendRequest(requestId)
    Message.success('已拒绝该申请')
    await loadData()
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '拒绝申请失败')
  } finally {
    loadingId.value = null
  }
}

function formatRequestMeta(item: FriendRequestItem) {
  return `向你发来好友申请 · @${item.requesterUserId}`
}

function formatOutgoingMeta(item: FriendRequestItem) {
  return `发送给 @${item.targetUserId} · ${formatRelativeTime(item.createdAt)}`
}

function formatRelativeTime(value: string) {
  const time = new Date(value).getTime()
  if (Number.isNaN(time)) return value
  const diff = Date.now() - time
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < hour) {
    return `${Math.max(1, Math.floor(diff / minute))} 分钟前`
  }
  if (diff < day) {
    return `${Math.max(1, Math.floor(diff / hour))} 小时前`
  }
  return `${Math.max(1, Math.floor(diff / day))} 天前`
}
</script>

<style scoped>
.requests-page {
  padding-bottom: 40px;
}

.top-nav,
.summary-head,
.stats-row,
.section-head,
.request-head,
.user-block,
.action-row {
  display: flex;
}

.top-nav,
.summary-head,
.section-head,
.request-head {
  justify-content: space-between;
}

.top-nav,
.user-block,
.request-head,
.summary-head,
.section-head {
  align-items: center;
}

.top-nav {
  margin-bottom: 18px;
}

.title-block {
  text-align: center;
}

.title-block small,
.section-head small,
.section-note,
.summary-card p,
.user-block p,
.message-line,
.empty-card p {
  color: var(--text-muted);
}

.title-block small,
.section-head small {
  display: block;
  margin-bottom: 4px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--accent);
}

.icon-button {
  border: none;
  background: #fff;
  width: 36px;
  height: 36px;
  border-radius: 999px;
}

h1,
h2,
.user-block strong,
.empty-card strong {
  margin: 0;
  color: var(--text-main);
}

h1 {
  font-size: var(--title-sm);
  font-weight: 600;
}

h2 {
  font-size: var(--title-md);
  font-family: var(--font-serif);
  font-weight: 600;
  line-height: 1.25;
}

.summary-card,
.section,
.request-card,
.empty-card {
  background: #fff;
  border-radius: 22px;
  box-shadow: 0 12px 26px rgba(27, 58, 45, 0.06);
}

.summary-card,
.section {
  padding: 18px;
}

.section {
  margin-top: 16px;
}

.summary-head {
  gap: 12px;
}

.summary-head h2 {
  margin-bottom: 8px;
}

.status-chip,
.time-chip,
.status-tag {
  border-radius: 999px;
  padding: 7px 10px;
  font-size: var(--text-xs);
  font-weight: 600;
  white-space: nowrap;
}

.status-chip {
  background: var(--accent-soft);
  color: var(--text-main);
}

.stats-row {
  gap: 10px;
  margin-top: 16px;
}

.stat-card {
  flex: 1;
  background: #f7f3ee;
  border-radius: 16px;
  padding: 14px;
}

.stat-card strong {
  display: block;
  font-size: var(--title-lg);
  color: var(--text-main);
  font-variant-numeric: tabular-nums;
}

.stat-card span {
  font-size: var(--text-sm);
  color: #8b857d;
}

.section-head {
  margin-bottom: 14px;
}

.section-note {
  font-size: var(--text-sm);
}

.request-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.request-card {
  padding: 16px;
}

.request-card.compact {
  box-shadow: none;
  border: 1px solid var(--line);
}

.user-block {
  gap: 12px;
}

.avatar {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  object-fit: cover;
  background: #edf3ec;
}

.user-block strong {
  display: block;
  margin-bottom: 4px;
}

.user-block p,
.message-line {
  margin: 0;
  font-size: var(--text-sm);
}

.time-chip {
  background: #f7f3ee;
  color: #8b857d;
}

.message-box {
  margin: 14px 0;
  padding: 14px;
  border-radius: 16px;
  background: #fbf8f3;
  color: #4b4741;
  line-height: 1.6;
}

.action-row {
  gap: 10px;
}

.action-button {
  flex: 1;
}

.secondary-button {
  border: none;
  border-radius: 14px;
  padding: 14px 18px;
  font-weight: 700;
  background: #f1ece6;
  color: #6f675d;
}

.status-tag.pending {
  background: #f7f3ee;
  color: #8b857d;
}

.status-tag.accepted {
  background: #edf3ec;
  color: #346538;
}

.status-tag.rejected,
.status-tag.cancelled {
  background: #f3e8e3;
  color: #9a5f48;
}

.empty-card {
  padding: 18px;
}

.empty-card strong {
  display: block;
  margin-bottom: 8px;
}
</style>
