<template>
  <view class="tab-bar">
    <button
      v-for="item in visibleItems"
      :key="item.key"
      :class="[
        'tab-item',
        `tab-item-${item.key}`,
        { active: item.key === active, add: item.add },
      ]"
      hover-class="pressable"
      @tap="navigate(item)"
    >
      <view
        :class="['tab-icon', { 'tab-icon-add': item.add }]"
        :style="{ '--tab-icon-mask': `url(${item.iconSrc})` }"
      ></view>
      <text v-if="item.key === active && !item.add" class="tab-label">{{
        item.label
      }}</text>
    </button>
  </view>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { openPrimaryRoute, push, type RouteName } from "@/lib/navigation";
import homeIcon from "@/assets/tab-bar-icons/home.svg";
import planIcon from "@/assets/tab-bar-icons/plan.svg";
import addIcon from "@/assets/tab-bar-icons/add.svg";
import circlesIcon from "@/assets/tab-bar-icons/circles.svg";
import profileIcon from "@/assets/tab-bar-icons/profile.svg";

const props = withDefaults(
  defineProps<{
    active: "home" | "plan" | "profile" | "circles";
    showAdd?: boolean;
  }>(),
  {
    showAdd: true,
  },
);

type TabItem = {
  key: "home" | "plan" | "add" | "circles" | "profile";
  label: string;
  iconSrc: string;
  route: RouteName;
  add?: boolean;
};

const items: TabItem[] = [
  {
    key: "home",
    label: "首页",
    iconSrc: homeIcon,
    route: "home",
  },
  {
    key: "plan",
    label: "计划",
    iconSrc: planIcon,
    route: "plan",
  },
  {
    key: "add",
    label: "添加",
    iconSrc: addIcon,
    route: "add-dish",
    add: true,
  },
  {
    key: "circles",
    label: "搭子圈",
    iconSrc: circlesIcon,
    route: "circles",
  },
  {
    key: "profile",
    label: "我的",
    iconSrc: profileIcon,
    route: "profile",
  },
];

const visibleItems = computed(() =>
  items.filter((item) => props.showAdd || !item.add),
);

function navigate(item: TabItem) {
  if (item.key === props.active) return;
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
  bottom: 16px;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-around;
  width: min(358px, calc(100vw - 32px));
  height: 56px;
  padding: 8px 12px;
  transform: translateX(-50%);
  border-radius: 999px;
  background: #ffffff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  box-sizing: border-box;
}

.tab-item {
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
