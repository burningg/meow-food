<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import { animatePageIn, type PageTransition } from '@/lib/motion'

const route = useRoute()
const pageStage = ref<HTMLElement | null>(null)

function animateCurrentPage() {
  if (!pageStage.value) return
  const transition = (route.meta.transition as PageTransition | undefined) ?? 'forward'
  animatePageIn(pageStage.value, transition)
}

watch(
  () => route.fullPath,
  async () => {
    await nextTick()
    animateCurrentPage()
  },
  { immediate: true },
)
</script>

<template>
  <RouterView v-slot="{ Component }">
    <div ref="pageStage" :key="route.fullPath" class="route-stage">
      <component :is="Component" />
    </div>
  </RouterView>
</template>
