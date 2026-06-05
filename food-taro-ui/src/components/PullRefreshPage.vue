<template>
  <scroll-view
    :class="['pull-refresh-scroll', pageClass]"
    :scroll-y="true"
    :enhanced="true"
    :show-scrollbar="false"
    :enable-back-to-top="true"
    :refresher-enabled="true"
    :refresher-threshold="threshold"
    refresher-default-style="none"
    refresher-background="transparent"
    :refresher-triggered="refreshing"
    @refresherpulling="handlePulling"
    @refresherrefresh="handleRefresh"
    @refresherrestore="handleRestore"
    @refresherabort="handleAbort"
  >
    <view class="pull-refresh-indicator-layer">
      <view :class="['pull-refresh-indicator', { visible: indicatorVisible, refreshing }]">
        <view class="paw-mark paw-mark-main" :style="mainPawStyle">
          <view class="paw-pad paw-pad-core"></view>
          <view class="paw-pad paw-pad-toe paw-pad-toe-1"></view>
          <view class="paw-pad paw-pad-toe paw-pad-toe-2"></view>
          <view class="paw-pad paw-pad-toe paw-pad-toe-3"></view>
        </view>
        <view :class="['paw-mark paw-mark-side paw-mark-side-left', { stepping: refreshing }]"></view>
        <view :class="['paw-mark paw-mark-side paw-mark-side-right', { stepping: refreshing }]"></view>
      </view>
    </view>

    <view :class="['pull-refresh-content', contentClass]">
      <slot />
    </view>
  </scroll-view>
</template>

<script setup lang="ts">
defineOptions({
  inheritAttrs: false,
})

import { computed, ref, useAttrs, withDefaults } from 'vue'

type RefreshListener = () => void | Promise<void>
type RefresherPullingEvent = {
  detail?: {
    dy?: number
  }
}

const props = withDefaults(defineProps<{
  threshold?: number
  pageClass?: string
  contentClass?: string
}>(), {
  threshold: 96,
  pageClass: '',
  contentClass: '',
})

const attrs = useAttrs() as {
  onRefresh?: RefreshListener | RefreshListener[]
}

const pullDistance = ref(0)
const refreshing = ref(false)

const pullProgress = computed(() => Math.min(pullDistance.value / props.threshold, 1.15))
const indicatorVisible = computed(() => pullDistance.value > 0 || refreshing.value)
const indicatorTranslateY = computed(() => {
  const baseOffset = refreshing.value ? 10 : 18
  return `${Math.max(-24 + pullDistance.value * 0.42 + baseOffset, -8)}px`
})
const indicatorScale = computed(() => {
  if (refreshing.value) return 1
  return 0.82 + Math.min(pullProgress.value, 1) * 0.2
})
const indicatorOpacity = computed(() => {
  if (refreshing.value) return 1
  return Math.min(0.16 + pullProgress.value * 0.95, 1)
})

const mainPawStyle = computed(() => ({
  transform: `translateY(${refreshing.value ? 0 : Math.max(0, (1 - Math.min(pullProgress.value, 1)) * 8)}px) scale(${0.96 + Math.min(pullProgress.value, 1) * 0.08})`,
}))

function handlePulling(event: RefresherPullingEvent) {
  if (refreshing.value) return
  pullDistance.value = Math.max(event.detail?.dy || 0, 0)
}

async function handleRefresh() {
  if (refreshing.value) return
  refreshing.value = true
  pullDistance.value = props.threshold

  try {
    const listeners = normalizeListeners(attrs.onRefresh)
    await Promise.all(listeners.map((listener) => listener()))
  } finally {
    refreshing.value = false
  }
}

function handleRestore() {
  if (refreshing.value) return
  pullDistance.value = 0
}

function handleAbort() {
  if (refreshing.value) return
  pullDistance.value = 0
}

function normalizeListeners(listener: typeof attrs.onRefresh) {
  if (!listener) return []
  return Array.isArray(listener) ? listener : [listener]
}
</script>

<style>
.pull-refresh-scroll {
  position: relative;
  height: 100vh;
}

.pull-refresh-indicator-layer {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 12;
  height: 0;
  overflow: visible;
  pointer-events: none;
}

.pull-refresh-indicator {
  position: absolute;
  top: 0;
  left: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 108px;
  height: 44px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 999px;
  background:
    radial-gradient(circle at 24% 30%, rgba(255, 255, 255, 0.88), transparent 32%),
    linear-gradient(180deg, #fffdf9 0%, #fff7ef 100%);
  box-shadow:
    0 10px 24px rgba(27, 58, 45, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
  transform: translateX(-50%) translateY(-24px) scale(0.82);
  opacity: 0;
  transition:
    opacity 0.22s ease,
    transform 0.22s ease,
    box-shadow 0.22s ease;
}

.pull-refresh-indicator.visible {
  opacity: v-bind(indicatorOpacity);
  transform: translateX(-50%) translateY(v-bind(indicatorTranslateY)) scale(v-bind(indicatorScale));
}

.pull-refresh-indicator.refreshing {
  box-shadow:
    0 12px 28px rgba(27, 58, 45, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.78);
}

.pull-refresh-content {
  min-height: 100vh;
}

.paw-mark {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.paw-mark-main {
  width: 26px;
  height: 22px;
  transition: transform 0.18s ease;
}

.paw-mark-side {
  width: 10px;
  height: 10px;
  border-radius: 999px 999px 70% 70%;
  background: rgba(122, 158, 126, 0.18);
  opacity: 0.7;
  transform: translateY(4px) scale(0.88);
}

.paw-mark-side-left {
  animation-delay: 0.12s;
}

.paw-mark-side-right {
  animation-delay: 0.24s;
}

.paw-mark-side.stepping {
  animation: paw-step 1.05s ease-in-out infinite;
}

.paw-pad {
  position: absolute;
  background: #c4704b;
  box-shadow: inset 0 -1px 0 rgba(159, 92, 56, 0.18);
}

.paw-pad-core {
  bottom: 0;
  left: 50%;
  width: 16px;
  height: 11px;
  border-radius: 12px 12px 10px 10px;
  transform: translateX(-50%);
}

.paw-pad-toe {
  top: 1px;
  width: 7px;
  height: 8px;
  border-radius: 999px 999px 70% 70%;
}

.paw-pad-toe-1 {
  left: 1px;
  transform: rotate(-18deg);
}

.paw-pad-toe-2 {
  left: 50%;
  transform: translateX(-50%);
}

.paw-pad-toe-3 {
  right: 1px;
  transform: rotate(18deg);
}

@keyframes paw-step {
  0%,
  100% {
    opacity: 0.52;
    transform: translateY(4px) scale(0.86);
  }

  50% {
    opacity: 0.95;
    transform: translateY(-2px) scale(1);
  }
}
</style>
