<template>
  <nav class="tab-bar">
    <button
      v-for="item in items"
      :key="item.name"
      :class="['tab-item', { active: item.name === active, add: item.add }]"
      type="button"
      @click="navigate(item)"
    >
      <span>{{ item.icon }}</span>
      <small v-if="item.label">{{ item.label }}</small>
    </button>
  </nav>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const props = defineProps<{
  active: 'home' | 'feed' | 'profile' | 'circles'
}>()

const router = useRouter()

type TabItem = {
  name: string
  label: string
  icon: string
  route: { name: string }
  add?: boolean
}

const items: TabItem[] = [
  { name: 'home', label: '首页', icon: '⌂', route: { name: 'home' } },
  { name: 'feed', label: '动态', icon: '◌', route: { name: 'feed' } },
  { name: 'add', label: '', icon: '＋', route: { name: 'add-dish' }, add: true },
  { name: 'circles', label: '搭子圈', icon: '☰', route: { name: 'circles' } },
  { name: 'profile', label: '我的', icon: '☺', route: { name: 'profile' } },
] 

function navigate(item: TabItem) {
  router.push(item.route)
}
</script>

<style scoped>
.tab-bar {
  position: fixed;
  left: 50%;
  bottom: 16px;
  transform: translateX(-50%);
  width: min(358px, calc(100vw - 32px));
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 30px rgba(27, 58, 45, 0.12);
  backdrop-filter: blur(16px);
  z-index: 20;
}

.tab-item {
  border: none;
  background: transparent;
  color: var(--text-muted);
  display: grid;
  place-items: center;
  gap: 4px;
  padding: 8px 12px;
  border-radius: 999px;
}

.tab-item.active {
  color: var(--text-main);
  background: var(--accent-soft);
}

.tab-item.add {
  background: var(--accent);
  color: #fff;
  min-width: 56px;
  min-height: 40px;
}

.tab-item small {
  font-size: 0.72rem;
}
</style>
