<template>
  <view class="page-shell requests-page">
    <header class="top-nav">
      <button class="icon-button" @tap="goBack()">‹</button>
      <view class="title-block">
        <text class="page-title">好友申请</text>
      </view>
      <button class="icon-button" @tap="loadData">↻</button>
    </header>

    <section class="section">
      <view class="section-head">
        <text class="section-title">待处理申请</text>
        <text class="section-note">48 小时内处理更礼貌</text>
      </view>

      <view v-if="pendingIncoming.length" class="request-list">
        <article v-for="item in pendingIncoming" :key="item.id" class="request-card">
          <view class="request-head">
            <view class="user-block">
              <image class="avatar" :src="item.requesterAvatar" mode="aspectFill" />
              <view>
                <text class="strong">{{ item.requesterNickname }}</text>
                <text class="muted">{{ formatRequestMeta(item) }}</text>
              </view>
            </view>
            <text class="time-chip">{{ formatRelativeTime(item.createdAt) }}</text>
          </view>

          <view class="message-box">{{ item.message || '这个人很安静，暂时没有留言。' }}</view>

          <view class="action-row">
            <button class="secondary-button action-button" :disabled="loadingId === item.id" @tap="handleReject(item.id)">
              暂不接受
            </button>
            <button class="primary-button action-button" :disabled="loadingId === item.id" @tap="handleAccept(item.id)">
              {{ loadingId === item.id ? '处理中...' : '接受申请' }}
            </button>
          </view>
        </article>
      </view>

      <article v-else class="empty-card">
        <text class="strong">暂时没有待处理申请</text>
      </article>
    </section>

    <section class="section">
      <view class="section-head">
        <text class="section-title">我发出的申请</text>
        <text class="section-note">对方确认后自动成为好友</text>
      </view>

      <view v-if="outgoing.length" class="request-list">
        <article v-for="item in outgoing" :key="item.id" class="request-card compact">
          <view class="request-head">
            <view class="user-block">
              <image class="avatar" :src="item.targetAvatar" mode="aspectFill" />
              <view>
                <text class="strong">{{ item.targetNickname }}</text>
                <text class="muted">{{ formatOutgoingMeta(item) }}</text>
              </view>
            </view>
            <text :class="['status-tag', item.status]">{{ statusLabelMap[item.status] }}</text>
          </view>
          <text class="message-line">留言：{{ item.message || '未填写留言' }}</text>
        </article>
      </view>

      <article v-else class="empty-card">
        <text class="strong">你还没有发出新的申请</text>
      </article>
    </section>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { goBack } from '@/lib/navigation'
import { SocialService, type FriendRequestItem } from '@/services/social-service'

const socialService = new SocialService()
const incoming = ref<FriendRequestItem[]>([])
const outgoing = ref<FriendRequestItem[]>([])
const loadingId = ref<string | null>(null)
const pendingIncoming = computed(() => incoming.value.filter((item) => item.status === 'pending'))

const statusLabelMap: Record<FriendRequestItem['status'], string> = {
  pending: '等待确认',
  accepted: '已通过',
  rejected: '已拒绝',
  cancelled: '已取消',
}

onMounted(async () => {
  if (!(await requireAuth('friend-requests'))) return
  await loadData()
})

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
  if (diff < hour) return `${Math.max(1, Math.floor(diff / minute))} 分钟前`
  if (diff < day) return `${Math.max(1, Math.floor(diff / hour))} 小时前`
  return `${Math.max(1, Math.floor(diff / day))} 天前`
}
</script>

<style scoped>
.requests-page {
  padding-bottom: 40px;
}

.title-block {
  text-align: center;
}

.page-title,
.section-title,
.strong {
  color: var(--text-main);
  font-weight: 800;
}

.page-title {
  font-size: var(--title-sm);
}

.section {
  margin-top: 16px;
  border-radius: 22px;
  background: #fff;
  padding: 18px;
  box-shadow: var(--shadow);
}

.section-head,
.request-head,
.user-block,
.action-row {
  display: flex;
  align-items: center;
}

.section-head,
.request-head {
  justify-content: space-between;
}

.section-note,
.muted,
.message-line {
  color: var(--text-muted);
  font-size: var(--text-sm);
}

.request-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 14px;
}

.request-card {
  border-radius: 18px;
  background: #f8f5f0;
  padding: 14px;
}

.user-block {
  gap: 10px;
}

.user-block view {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 14px;
}

.time-chip,
.status-tag {
  border-radius: 999px;
  background: #fff;
  color: var(--accent);
  padding: 4px 9px;
  font-size: 11px;
  font-weight: 800;
}

.message-box {
  margin-top: 12px;
  border-radius: 14px;
  background: #fff;
  padding: 12px;
  color: #4f4a45;
  font-size: var(--text-sm);
}

.action-row {
  gap: 10px;
  margin-top: 12px;
}

.action-button {
  flex: 1;
}

.message-line {
  display: block;
  margin-top: 10px;
}
</style>
