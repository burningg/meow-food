<template>
  <view :class="wrapperClasses">
    <image
      :class="innerClasses"
      :src="src"
      :mode="mode"
      @load="handleLoad"
      @error="handleError"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    src?: string
    mode?: string
    variant?: 'card' | 'hero'
    className?: string
    imageClass?: string
  }>(),
  {
    src: '',
    mode: 'aspectFill',
    variant: 'card',
    className: '',
    imageClass: '',
  },
)

const loaded = ref(false)
const failed = ref(false)

watch(
  () => props.src,
  (nextSrc) => {
    loaded.value = false
    failed.value = !nextSrc
  },
  { immediate: true },
)

const wrapperClasses = computed(() => [
  'smart-image',
  `smart-image-${props.variant}`,
  props.className,
  {
    'is-loaded': loaded.value,
    'is-failed': failed.value,
  },
])

const innerClasses = computed(() => ['smart-image__inner', props.imageClass])

function handleLoad() {
  loaded.value = true
  failed.value = false
}

function handleError() {
  loaded.value = false
  failed.value = true
}
</script>

<style>
.smart-image {
  position: relative;
  display: block;
  overflow: hidden;
  background: #ece7e0;
}

.smart-image__inner {
  width: 100%;
  height: 100%;
  opacity: 0;
  transition-property: opacity, transform, filter;
  transition-timing-function: ease-out;
  will-change: opacity, transform, filter;
}

.smart-image-card .smart-image__inner {
  transform: scale(1.04);
  transition-duration: 240ms;
}

.smart-image-hero .smart-image__inner {
  opacity: 0.84;
  filter: blur(8px);
  transform: scale(1.03);
  transition-duration: 300ms;
}

.smart-image.is-loaded .smart-image__inner {
  opacity: 1;
  filter: blur(0);
  transform: scale(1);
}

.smart-image.is-failed .smart-image__inner {
  opacity: 0;
  filter: none;
  transform: none;
}
</style>
