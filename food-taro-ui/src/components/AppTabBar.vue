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
import { openPrimaryRoute, push, type RouteName } from '@/lib/navigation'

const props = defineProps<{
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
  if (item.name === props.active) return
  if (item.add) {
    push(item.route)
    return
  }
  openPrimaryRoute(item.route)
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
  background: #fff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  box-sizing: border-box;
}

.tab-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 44px;
  height: 40px;
  padding: 8px 12px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #787774;
  flex-shrink: 0;
  line-height: 1;
  transition: background-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.tab-item.active {
  background: #edf3ec;
  color: #151515;
}

.tab-item.active .tab-icon {
  color: #346538;
}

.tab-item.add {
  min-width: 54px;
  width: 54px;
  height: 40px;
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
  left: 4px;
  width: 12px;
  height: 12px;
  border-top: 2px solid currentColor;
  border-left: 2px solid currentColor;
  border-radius: 3px 0 0 0;
  transform: rotate(45deg);
}

.home-body {
  left: 3px;
  bottom: 2px;
  width: 14px;
  height: 10px;
  border: 2px solid currentColor;
  border-top: none;
  border-radius: 0 0 4px 4px;
}

.home-door {
  left: 9px;
  bottom: 2px;
  width: 0;
  height: 6px;
  border-left: 2px solid currentColor;
}

.tab-item.active .home-roof {
  top: 3px;
  left: 4px;
  width: 12px;
  height: 12px;
  border: none;
  border-radius: 3px 3px 0 0;
  background: currentColor;
  transform: rotate(45deg);
}

.tab-item.active .home-body {
  left: 4px;
  width: 12px;
  height: 10px;
  border: none;
  border-radius: 0 0 4px 4px;
  background: currentColor;
}

.tab-item.active .home-door {
  left: 9px;
  width: 4px;
  height: 5px;
  border: none;
  border-radius: 3px 3px 0 0;
  background: #edf3ec;
}

.feed-bubble {
  left: 2px;
  top: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  width: 16px;
  height: 14px;
  border: 2px solid currentColor;
  border-radius: 8px;
}

.feed-tail {
  left: 5px;
  bottom: 2px;
  width: 5px;
  height: 5px;
  border-left: 2px solid currentColor;
  border-bottom: 2px solid currentColor;
  border-bottom-left-radius: 2px;
  transform: skew(-18deg);
}

.feed-dot {
  width: 2.5px;
  height: 2.5px;
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
  left: 1px;
  top: 3px;
  width: 6px;
  height: 6px;
}

.circle-node-side {
  right: 1px;
  top: 2px;
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
  left: 0;
  bottom: 2px;
  width: 10px;
  height: 7px;
  border-width: 2px 2px 0;
}

.circle-arc-side {
  right: 0;
  bottom: 2px;
  width: 10px;
  height: 6px;
  border-width: 2px 2px 0;
}

.profile-head {
  left: 6px;
  top: 2px;
  width: 7px;
  height: 7px;
  border: 2px solid currentColor;
  border-radius: 999px;
}

.profile-shoulders {
  left: 3px;
  bottom: 2px;
  width: 13px;
  height: 7px;
  border: 2px solid currentColor;
  border-bottom: none;
  border-radius: 999px 999px 0 0;
}

.tab-label {
  color: #151515;
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}
</style>
