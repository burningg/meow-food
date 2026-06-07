<template>
  <view class="page-shell notifications-page">


    <section class="notification-guidance-card">
      <text class="guidance-title">还有 {{ unreadCount }} 条未读通知</text>
    </section>

    <section class="notification-list-section">
      <view class="notification-list-head">
        <text class="section-title">全部通知</text>
      </view>

      <view class="notification-list">
        <button
          v-for="item in notifications"
          :key="item.id"
          :class="['notification-item-card', { read: item.read, important: item.priority === 'important' }]"
          @tap="openNotification(item)"
        >
          <view class="notification-item-head">
            <view :class="['notification-item-dot', dotToneClass(item)]"></view>
            <text class="notification-item-time">{{ formatNotificationTime(item.publishedAt) }}</text>
          </view>
          <view class="notification-item-copy">
            <text class="notification-item-title">{{ item.title }}</text>
            <text class="notification-item-summary">{{ item.summary }}</text>
          </view>
          <view class="notification-item-footer">
            <text class="notification-item-prompt">展开看完整内容</text>
            <view :class="['notification-item-state', { read: item.read }]">
              <text>{{ item.read ? '已读' : '未读' }}</text>
            </view>
          </view>
        </button>

        <article v-if="!notifications.length" class="empty-card">
          <text>现在还没有新通知</text>
        </article>
      </view>
    </section>

    <NotificationModalCard
      :visible="Boolean(selectedNotification)"
      :title="selectedNotification?.title || ''"
      :body="selectedNotification?.body || ''"
      @confirm="closeNotification"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useDidShow } from '@tarojs/taro'
import NotificationModalCard from '@/components/NotificationModalCard.vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { goBack } from '@/lib/navigation'
import { NotificationService, type NotificationItem } from '@/services/notification-service'

const notificationService = new NotificationService()
const notifications = ref<NotificationItem[]>([])
const selectedNotification = ref<NotificationItem | null>(null)

const unreadCount = computed(() => notifications.value.filter((item) => !item.read).length)

onMounted(async () => {
  if (!(await requireAuth('notifications'))) return
  await loadNotifications()
})

useDidShow(async () => {
  if (!(await requireAuth('notifications'))) return
  await loadNotifications()
})

async function loadNotifications() {
  try {
    const { data } = await notificationService.getNotifications()
    notifications.value = data.items
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '通知加载失败')
  }
}

function openNotification(item: NotificationItem) {
  selectedNotification.value = item
  if (!item.read) {
    void markNotificationRead(item.id)
  }
}

function closeNotification() {
  selectedNotification.value = null
}

async function markNotificationRead(notificationId: string) {
  try {
    await notificationService.markRead(notificationId)
    notifications.value = sortNotifications(
      notifications.value.map((item) => (
        item.id === notificationId ? { ...item, read: true } : item
      )),
    )
    if (selectedNotification.value?.id === notificationId) {
      selectedNotification.value = { ...selectedNotification.value, read: true }
    }
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '通知状态更新失败')
  }
}

function dotToneClass(item: NotificationItem) {
  if (item.read) return 'muted'
  return item.priority === 'important' ? 'accent' : 'soft'
}

function sortNotifications(items: NotificationItem[]) {
  return [...items].sort((left, right) => {
    if (left.read !== right.read) return Number(left.read) - Number(right.read)
    return new Date(right.publishedAt).getTime() - new Date(left.publishedAt).getTime()
  })
}

function formatNotificationTime(value: string) {
  const time = new Date(value).getTime()
  if (Number.isNaN(time)) return value
  const diff = Date.now() - time
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < hour) return `${Math.max(1, Math.floor(diff / minute))} 分钟前`
  if (diff < day) return `${Math.max(1, Math.floor(diff / hour))} 小时前`
  if (diff < day * 7) return `${Math.max(1, Math.floor(diff / day))} 天前`
  const date = new Date(value)
  return `${date.getMonth() + 1}-${date.getDate()}`
}
</script>

<style>
.notifications-page {
  padding-bottom: 40px;
}

.title-block,
.notification-item-copy {
  display: flex;
  flex-direction: column;
}

.title-block {
  text-align: center;
}

.page-title,
.section-title,
.guidance-title,
.notification-item-title {
  color: var(--text-main);
  font-weight: 800;
}

.page-title {
  font-size: var(--title-sm);
}

.notification-guidance-card,
.notification-list-section {
  border-radius: 22px;
  background: #fff;
  padding: 18px;
  box-shadow: var(--shadow);
}

.notification-list-section {
  margin-top: 16px;
}

.guidance-title {
  display: block;
  font-size: var(--title-md);
}

.guidance-body,
.section-note,
.notification-item-time,
.notification-item-summary,
.notification-item-prompt {
  color: var(--text-muted);
  font-size: var(--text-sm);
}

.guidance-body {
  display: block;
  margin-top: 6px;
  line-height: 1.5;
}

.notification-list-head,
.notification-item-head,
.notification-item-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 14px;
}

.notification-item-card {
  width: 100%;
  border-radius: 18px;
  background: #fff;
  padding: 16px;
  text-align: left;
  box-shadow: inset 0 0 0 1px #efe9df;
}

.notification-item-card.read {
  background: #f8f5f0;
}

.notification-item-card.important {
  box-shadow: inset 0 0 0 1px rgba(196, 112, 75, 0.18);
}

.notification-item-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}

.notification-item-dot.accent {
  background: var(--accent);
}

.notification-item-dot.soft {
  background: #d19a63;
}

.notification-item-dot.muted {
  background: #d8d0c5;
}

.notification-item-copy {
  gap: 6px;
  margin-top: 12px;
}

.notification-item-title {
  font-size: var(--text-md);
}

.notification-item-summary {
  line-height: 1.55;
}

.notification-item-footer {
  margin-top: 12px;
}

.notification-item-state {
  border-radius: 999px;
  background: #f4e6de;
  color: var(--accent-dark);
  padding: 5px 10px;
  font-size: 11px;
  font-weight: 700;
}

.notification-item-state.read {
  background: #efe8df;
  color: #8b857d;
}
</style>
