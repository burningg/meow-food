<template>
  <view class="tab-bar">
    <button
      v-for="item in items"
      :key="item.name"
      :class="['tab-item', { active: item.name === active, add: item.add }]"
      hover-class="pressable"
      @tap="navigate(item)"
    >
      <text class="tab-icon">{{ item.symbol }}</text>
      <text v-if="item.name === active && !item.add" class="tab-label">{{ item.label }}</text>
    </button>
  </view>
</template>

<script setup lang="ts">
import { push, type RouteName } from '@/lib/navigation'

defineProps<{
  active: 'home' | 'feed' | 'profile' | 'circles'
}>()

type TabItem = {
  name: string
  label: string
  symbol: string
  route: RouteName
  add?: boolean
}

const items: TabItem[] = [
  { name: 'home', label: '首页', symbol: '⌂', route: 'home' },
  { name: 'feed', label: '动态', symbol: '◌', route: 'feed' },
  { name: 'add', label: '添加', symbol: '＋', route: 'add-dish', add: true },
  { name: 'circles', label: '搭子圈', symbol: '◎', route: 'circles' },
  { name: 'profile', label: '我的', symbol: '☻', route: 'profile' },
]

function navigate(item: TabItem) {
  push(item.route)
}
</script>

<style>
.tab-bar {
  position: fixed;
  left: 50%;
  bottom: calc(16px + env(safe-area-inset-bottom));
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-around;
  width: min(358px, calc(100vw - 32px));
  height: 56px;
  padding: 8px 12px;
  transform: translateX(-50%);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(16px);
}

.tab-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 40px;
  min-height: 40px;
  padding: 8px 12px;
  border-radius: 999px;
  color: #787774;
}

.tab-item.active {
  background: #edf3ec;
  color: #151515;
}

.tab-item.add {
  min-width: 56px;
  background: #9f5c38;
  color: #fff;
}

.tab-icon {
  font-size: 20px;
  line-height: 1;
}

.tab-label {
  font-size: 12px;
  font-weight: 700;
}
</style>
