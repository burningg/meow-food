<template>
  <article class="activity-card" @click="$emit('open')">
    <img class="avatar" :src="item.actorAvatar" :alt="item.actorNickname" />
    <div class="content">
      <div class="top-line">
        <strong>{{ item.actorNickname }}</strong>
        <span class="badge">{{ badgeText }}</span>
      </div>
      <p class="action">{{ item.actionText }}</p>
      <h3>{{ item.dishName }}</h3>
      <p class="meta">{{ item.dishDescription }}</p>
    </div>
  </article>
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
  background: #fff;
  border-radius: 18px;
  padding: 14px;
  display: flex;
  gap: 12px;
  box-shadow: 0 10px 24px rgba(27, 58, 45, 0.06);
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  object-fit: cover;
}

.content {
  flex: 1;
}

.top-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.action,
.meta {
  margin: 0;
  color: var(--text-muted);
  font-size: var(--text-sm);
}

h3 {
  margin: 8px 0 6px;
  color: var(--text-main);
  font-size: var(--text-md);
  font-weight: 600;
}

.badge {
  background: #f4ece6;
  color: var(--accent);
  border-radius: 999px;
  font-size: var(--text-xs);
  padding: 4px 10px;
  font-weight: 600;
}
</style>
