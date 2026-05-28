<template>
  <article class="friend-card">
    <div class="friend-top">
      <img class="avatar" :src="friend.avatar" :alt="friend.nickname" />
      <span class="tag">{{ friend.friend ? '好友' : '推荐' }}</span>
    </div>
    <div class="copy">
      <h3>{{ friend.nickname }}</h3>
      <p>{{ metaText }}</p>
    </div>
    <div class="footer">
      <span>菜单访问</span>
      <button class="link" type="button" @click="$emit('action')">
        {{ actionLabel }}
      </button>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FriendItem } from '@/services/social-service'

const props = defineProps<{
  friend: FriendItem
  actionLabel?: string
}>()

defineEmits<{
  action: []
}>()

const metaText = computed(() => {
  if (props.friend.sharedMenuCount) {
    return `共享 ${props.friend.sharedMenuCount} 份菜单`
  }
  return `好友可见 ${props.friend.visibleMenuCount} 份菜单`
})
</script>

<style scoped>
.friend-card {
  flex: 1;
  background: #fff;
  border-radius: 18px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: 0 10px 24px rgba(27, 58, 45, 0.06);
}

.friend-top,
.footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.avatar {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  object-fit: cover;
}

.tag {
  background: #edf3ec;
  color: #346538;
  border-radius: 999px;
  font-size: 0.72rem;
  padding: 4px 10px;
}

.copy h3,
.copy p {
  margin: 0;
}

.copy h3 {
  font-size: 1rem;
  color: var(--text-main);
}

.copy p,
.footer span {
  color: var(--text-muted);
  font-size: 0.78rem;
  line-height: 1.5;
}

.link {
  border: none;
  background: transparent;
  color: var(--accent);
  font-weight: 700;
}
</style>
