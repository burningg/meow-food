<template>
  <button class="activity-card" @tap="$emit('open')">
    <image class="avatar" :src="item.actorAvatar" mode="aspectFill" />
    <view class="content">
      <view class="top-line">
        <text class="nickname">{{ item.actorNickname }}</text>
        <text class="badge">{{ badgeText }}</text>
      </view>
      <text class="action">{{ item.actionText }}</text>
      <text class="dish-name">{{ item.dishName }}</text>
      <text class="meta">{{ item.dishDescription }}</text>
    </view>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FeedItem } from '@/services/social-service'

const props = defineProps<{
  item: FeedItem
}>()

defineEmits<{
  open: []
}>()

const badgeText = computed(() => {
  if (props.item.visibilityScope === 'circle') return '圈内共享'
  if (props.item.visibilityScope === 'friends') return '好友可见'
  return '公开'
})
</script>

<style scoped>
.activity-card {
  display: flex;
  gap: 12px;
  width: 100%;
  padding: 14px;
  border-radius: 18px;
  background: #fff;
  text-align: left;
  box-shadow: 0 10px 24px rgba(27, 58, 45, 0.06);
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  flex: 0 0 auto;
}

.content {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.top-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.nickname,
.dish-name {
  color: var(--text-main);
  font-weight: 700;
}

.dish-name {
  margin: 8px 0 6px;
  font-size: var(--text-md);
}

.action,
.meta {
  color: var(--text-muted);
  font-size: var(--text-sm);
}

.badge {
  flex: 0 0 auto;
  border-radius: 999px;
  background: #f4ece6;
  color: var(--accent);
  font-size: var(--text-xs);
  padding: 4px 10px;
  font-weight: 700;
}
</style>
