<template>
  <article class="menu-card" @click="$emit('open')">
    <div class="head">
      <div>
        <small>{{ menu.ownerNickname }}</small>
        <h3>{{ menu.name }}</h3>
      </div>
      <span class="badge">{{ badgeText }}</span>
    </div>
    <p>{{ menu.description }}</p>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DishSummary } from '@/services/food-service'

const props = defineProps<{
  menu: DishSummary
}>()

defineEmits<{
  open: []
}>()

const badgeText = computed(() => {
  if (props.menu.effectiveVisibility === 'public') return '公开同步'
  return '圈内共享'
})
</script>

<style scoped>
.menu-card {
  background: #fff;
  border-radius: 18px;
  padding: 16px;
  box-shadow: 0 10px 24px rgba(27, 58, 45, 0.06);
}

.head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

small,
p {
  color: var(--text-muted);
}

h3,
p {
  margin: 0;
}

h3 {
  color: var(--text-main);
  font-size: var(--text-lg);
  margin-top: 4px;
  font-weight: 600;
}

p {
  margin-top: 10px;
  font-size: var(--text-sm);
  line-height: 1.6;
}

.badge {
  align-self: flex-start;
  background: #edf3ec;
  color: #346538;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: var(--text-xs);
  font-weight: 600;
}
</style>
