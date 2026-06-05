<template>
  <view class="tab-bar">
    <button
      v-for="item in visibleItems"
      :key="item.name"
      :class="[
        'tab-item',
        `tab-item-${item.name}`,
        { active: item.name === active, add: item.add },
      ]"
      hover-class="pressable"
      @tap="navigate(item)"
    >
      <view
        :class="['tab-icon', { 'tab-icon-add': item.add }]"
        :style="{ '--tab-icon-mask': `url(${item.iconSrc})` }"
      ></view>
      <text v-if="item.name === active && !item.add" class="tab-label">{{
        item.label
      }}</text>
    </button>
  </view>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { openPrimaryRoute, push, type RouteName } from "@/lib/navigation";
import homeIcon from "@/assets/tab-bar-icons/home.svg";
import feedIcon from "@/assets/tab-bar-icons/feed.svg";
import addIcon from "@/assets/tab-bar-icons/add.svg";
import circlesIcon from "@/assets/tab-bar-icons/circles.svg";
import profileIcon from "@/assets/tab-bar-icons/profile.svg";

const props = withDefaults(
  defineProps<{
    active: "home" | "feed" | "profile" | "circles";
    showAdd?: boolean;
  }>(),
  {
    showAdd: true,
  },
);

type TabItem = {
  name: string;
  label: string;
  icon: "home" | "feed" | "add" | "circles" | "profile";
  iconSrc: string;
  route: RouteName;
  add?: boolean;
};

const items: TabItem[] = [
  {
    name: "home",
    label: "首页",
    icon: "home",
    iconSrc: homeIcon,
    route: "home",
  },
  {
    name: "feed",
    label: "动态",
    icon: "feed",
    iconSrc: feedIcon,
    route: "feed",
  },
  {
    name: "add",
    label: "添加",
    icon: "add",
    iconSrc: addIcon,
    route: "add-dish",
    add: true,
  },
  {
    name: "circles",
    label: "搭子圈",
    icon: "circles",
    iconSrc: circlesIcon,
    route: "circles",
  },
  {
    name: "profile",
    label: "我的",
    icon: "profile",
    iconSrc: profileIcon,
    route: "profile",
  },
];

const visibleItems = computed(() =>
  items.filter((item) => props.showAdd || !item.add),
);

function navigate(item: TabItem) {
  if (item.name === props.active) return;
  if (item.add) {
    push(item.route);
    return;
  }
  openPrimaryRoute(item.route);
}
</script>

<style>
.tab-bar {
  position: fixed;
  left: 50%;
  bottom: 25px;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-around;
  width: min(358px, calc(100vw - 32px));
  height: 56px;
  padding: 8px 12px;
  transform: translateX(-50%);
  border-radius: 999px;
  overflow: visible;
  isolation: isolate;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: rgba(255, 250, 242, 0.96);
  box-shadow:
    0 10px 28px rgba(129, 98, 68, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(20px) saturate(145%);
  -webkit-backdrop-filter: blur(20px) saturate(145%);
  box-sizing: border-box;
}

.tab-bar::before,
.tab-bar::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: inherit;
}

.tab-bar::before {
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.34) 0%,
    rgba(255, 255, 255, 0.14) 44%,
    rgba(255, 255, 255, 0.02) 100%
  );
}

.tab-bar::after {
  top: calc(100% - 6px);
  left: 4px;
  right: 4px;
  bottom: auto;
  height: calc(40px + env(safe-area-inset-bottom));
  border-radius: 0 0 28px 28px;
  background:
    linear-gradient(
      180deg,
      rgba(255, 248, 240, 0.08) 0%,
      rgba(255, 249, 243, 0.18) 20%,
      rgba(255, 251, 247, 0.36) 45%,
      rgba(255, 253, 251, 0.62) 72%,
      rgba(255, 255, 255, 0.88) 100%
    ),
    linear-gradient(
      180deg,
      rgba(255, 255, 255, 0) 0%,
      rgba(255, 255, 255, 0.16) 52%,
      rgba(255, 255, 255, 0.34) 100%
    );
  backdrop-filter: blur(30px) saturate(150%);
  -webkit-backdrop-filter: blur(30px) saturate(150%);
  box-shadow:
    inset 0 -12px 18px rgba(255, 255, 255, 0.18),
    0 14px 28px rgba(255, 252, 248, 0.38);
  opacity: 1;
}

.tab-item {
  position: relative;
  z-index: 1;
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
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    transform 0.2s ease;
}

.tab-item.active {
  background: rgba(207, 197, 186, 0.45);
  color: #151515;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.45),
    0 4px 10px rgba(120, 100, 83, 0.08);
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
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  background-color: currentColor;
  -webkit-mask-image: var(--tab-icon-mask);
  -webkit-mask-position: center;
  -webkit-mask-repeat: no-repeat;
  -webkit-mask-size: contain;
  mask-image: var(--tab-icon-mask);
  mask-position: center;
  mask-repeat: no-repeat;
  mask-size: contain;
}

.tab-icon-add {
  width: 22px;
  height: 22px;
}

.tab-label {
  color: #151515;
  font-family: "Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}
</style>
