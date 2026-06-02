<template>
  <view class="tab-bar">
    <button
      v-for="item in items"
      :key="item.name"
      :class="['tab-item', `tab-item-${item.name}`, { active: item.name === active, add: item.add }]"
      hover-class="pressable"
      @tap="navigate(item)"
    >
      <view :class="['tab-icon', `tab-icon-${item.icon}`, { 'tab-icon-add': item.add }]">
        <template v-if="item.icon === 'home'">
          <view class="home-roof"></view>
          <view class="home-body"></view>
          <view class="home-door"></view>
        </template>
        <template v-else-if="item.icon === 'feed'">
          <view class="feed-bubble">
            <text class="feed-dot"></text>
            <text class="feed-dot"></text>
            <text class="feed-dot"></text>
          </view>
          <view class="feed-tail"></view>
        </template>
        <template v-else-if="item.icon === 'add'">
          <view class="plus-line"></view>
          <view class="plus-line plus-line-vertical"></view>
        </template>
        <template v-else-if="item.icon === 'circles'">
          <view class="circle-node circle-node-main"></view>
          <view class="circle-node circle-node-side"></view>
          <view class="circle-arc circle-arc-main"></view>
          <view class="circle-arc circle-arc-side"></view>
        </template>
        <template v-else>
          <view class="profile-head"></view>
          <view class="profile-shoulders"></view>
        </template>
      </view>
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
  icon: 'home' | 'feed' | 'add' | 'circles' | 'profile'
  route: RouteName
  add?: boolean
}

const items: TabItem[] = [
  { name: 'home', label: '首页', icon: 'home', route: 'home' },
  { name: 'feed', label: '动态', icon: 'feed', route: 'feed' },
  { name: 'add', label: '添加', icon: 'add', route: 'add-dish', add: true },
  { name: 'circles', label: '搭子圈', icon: 'circles', route: 'circles' },
  { name: 'profile', label: '我的', icon: 'profile', route: 'profile' },
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
  box-sizing: border-box;
}

.tab-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 999px;
  background: transparent;
  color: #787774;
  flex-shrink: 0;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.tab-item.active {
  background: #edf3ec;
  color: #151515;
}

.tab-item.active .tab-icon {
  color: #346538;
}

.tab-item.add {
  min-width: 56px;
  min-height: 40px;
  padding: 8px 16px;
  background: #9f5c38;
  color: #fff;
}

.tab-icon {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  color: currentColor;
  line-height: 1;
}

.tab-icon-add {
  width: 22px;
  height: 22px;
}

.home-roof,
.home-body,
.home-door,
.feed-bubble,
.feed-tail,
.plus-line,
.circle-node,
.circle-arc,
.profile-head,
.profile-shoulders {
  position: absolute;
  box-sizing: border-box;
}

.home-roof {
  top: 2px;
  left: 5px;
  width: 11px;
  height: 11px;
  border-left: 2px solid currentColor;
  border-top: 2px solid currentColor;
  border-radius: 2px 0 0;
  transform: rotate(45deg);
}

.home-body {
  left: 4px;
  bottom: 2px;
  width: 12px;
  height: 10px;
  border: 2px solid currentColor;
  border-top: none;
  border-radius: 0 0 3px 3px;
}

.home-door {
  left: 9px;
  bottom: 2px;
  width: 4px;
  height: 7px;
  border-left: 2px solid currentColor;
}

.feed-bubble {
  left: 2px;
  top: 3px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  width: 17px;
  height: 14px;
  border: 2px solid currentColor;
  border-radius: 999px;
}

.feed-tail {
  left: 4px;
  bottom: 2px;
  width: 6px;
  height: 6px;
  border-left: 2px solid currentColor;
  border-bottom: 2px solid currentColor;
  transform: skew(-18deg);
}

.feed-dot {
  width: 2px;
  height: 2px;
  border-radius: 999px;
  background: currentColor;
}

.plus-line {
  left: 50%;
  top: 50%;
  width: 14px;
  height: 2px;
  border-radius: 999px;
  background: currentColor;
  transform: translate(-50%, -50%);
}

.plus-line-vertical {
  transform: translate(-50%, -50%) rotate(90deg);
}

.circle-node {
  border: 2px solid currentColor;
  border-radius: 999px;
}

.circle-node-main {
  left: 2px;
  top: 3px;
  width: 9px;
  height: 9px;
}

.circle-node-side {
  right: 2px;
  top: 4px;
  width: 7px;
  height: 7px;
}

.circle-arc {
  border-color: currentColor;
  border-style: solid;
  border-radius: 999px 999px 0 0;
  border-bottom: none;
}

.circle-arc-main {
  left: 1px;
  bottom: 2px;
  width: 14px;
  height: 8px;
  border-width: 2px 2px 0;
}

.circle-arc-side {
  right: 1px;
  bottom: 3px;
  width: 10px;
  height: 6px;
  border-width: 2px 2px 0;
}

.profile-head {
  left: 6px;
  top: 2px;
  width: 8px;
  height: 8px;
  border: 2px solid currentColor;
  border-radius: 999px;
}

.profile-shoulders {
  left: 3px;
  bottom: 2px;
  width: 14px;
  height: 8px;
  border: 2px solid currentColor;
  border-bottom: none;
  border-radius: 999px 999px 0 0;
}

.tab-label {
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}
</style>
