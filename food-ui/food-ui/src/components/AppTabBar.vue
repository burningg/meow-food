<template>
  <nav ref="tabBarRef" class="tab-bar">
    <button
      v-for="item in items"
      :key="item.name"
      :class="['tab-item', `tab-item-${item.name}`, { active: item.name === active, add: item.add }]"
      :data-tab-name="item.name"
      type="button"
      :aria-label="item.label"
      @click="navigate(item)"
    >
      <span class="tab-icon" :class="{ 'tab-icon-add': item.add }" aria-hidden="true">
        <svg v-if="item.icon === 'home'" viewBox="0 0 24 24">
          <path
            d="M3.75 10.5 12 4l8.25 6.5v8.25a1.5 1.5 0 0 1-1.5 1.5h-4.5v-6h-4.5v6h-4.5a1.5 1.5 0 0 1-1.5-1.5z"
          />
        </svg>
        <svg v-else-if="item.icon === 'feed'" viewBox="0 0 24 24">
          <path
            d="M20.25 11.25a7.5 7.5 0 0 1-7.5 7.5H7.5L3.75 21v-5.25a7.5 7.5 0 1 1 16.5-4.5Z"
          />
          <path d="M8.75 11.25h.5" />
          <path d="M11.75 11.25h.5" />
          <path d="M14.75 11.25h.5" />
        </svg>
        <svg v-else-if="item.icon === 'add'" viewBox="0 0 24 24">
          <path d="M12 5.5v13" />
          <path d="M5.5 12h13" />
        </svg>
        <svg v-else-if="item.icon === 'circles'" viewBox="0 0 24 24">
          <path d="M7.5 12.75a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z" />
          <path d="M16.5 11.25a2.25 2.25 0 1 0 0-4.5 2.25 2.25 0 0 0 0 4.5Z" />
          <path d="M4.75 18.25a4.75 4.75 0 0 1 8.5-2.9" />
          <path d="M14 18.25a4 4 0 0 1 5.25-3.8" />
        </svg>
        <svg v-else viewBox="0 0 24 24">
          <path d="M12 12.75a3.75 3.75 0 1 0 0-7.5 3.75 3.75 0 0 0 0 7.5Z" />
          <path d="M5 19.25a7 7 0 0 1 14 0" />
        </svg>
      </span>
      <small v-if="item.name === active && !item.add">{{ item.label }}</small>
    </button>
  </nav>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { gsap } from 'gsap'
import { attachPressAnimations, runScopedMotion } from '@/lib/motion'

const props = defineProps<{
  active: 'home' | 'feed' | 'profile' | 'circles'
}>()

const router = useRouter()
const tabBarRef = ref<HTMLElement | null>(null)
let cleanupMotion: VoidFunction | undefined

type TabItem = {
  name: string
  label: string
  icon: 'home' | 'feed' | 'add' | 'circles' | 'profile'
  route: { name: string }
  add?: boolean
}

const items: TabItem[] = [
  { name: 'home', label: '首页', icon: 'home', route: { name: 'home' } },
  { name: 'feed', label: '动态', icon: 'feed', route: { name: 'feed' } },
  { name: 'add', label: '添加', icon: 'add', route: { name: 'add-dish' }, add: true },
  { name: 'circles', label: '搭子圈', icon: 'circles', route: { name: 'circles' } },
  { name: 'profile', label: '我的', icon: 'profile', route: { name: 'profile' } },
]

function navigate(item: TabItem) {
  if (item.name === props.active) return
  router.push(item.route)
}

function animateActiveTab() {
  if (!tabBarRef.value) return
  const activeItem = tabBarRef.value.querySelector<HTMLElement>(`[data-tab-name="${props.active}"]`)
  if (activeItem) {
    gsap.fromTo(
      activeItem,
      { y: 5, scale: 0.96 },
      {
        y: 0,
        scale: 1,
        duration: 0.34,
        ease: 'back.out(1.6)',
        overwrite: 'auto',
      },
    )
    const icon = activeItem.querySelector('.tab-icon')
    if (icon) {
      gsap.fromTo(
        icon,
        { scale: 0.88, rotate: -8 },
        {
          scale: 1,
          rotate: 0,
          duration: 0.3,
          ease: 'power2.out',
          overwrite: 'auto',
        },
      )
    }
  }

  const addItem = tabBarRef.value.querySelector<HTMLElement>('[data-tab-name="add"]')
  if (addItem) {
    gsap.fromTo(
      addItem,
      { scale: 0.94 },
      {
        scale: 1,
        duration: 0.38,
        ease: 'back.out(1.7)',
        overwrite: 'auto',
      },
    )
  }
}

onMounted(() => {
  if (!tabBarRef.value) return
  cleanupMotion = runScopedMotion(tabBarRef.value, () => {
    const releaseTabs = attachPressAnimations(tabBarRef.value!, '.tab-item', { activeScale: 0.94 })
    return () => {
      releaseTabs()
    }
  })
  animateActiveTab()
})

watch(
  () => props.active,
  async () => {
    await nextTick()
    animateActiveTab()
  },
)

onUnmounted(() => {
  cleanupMotion?.()
})
</script>

<style scoped>
.tab-bar {
  position: fixed;
  left: 50%;
  bottom: calc(16px + env(safe-area-inset-bottom));
  transform: translateX(-50%);
  width: min(358px, calc(100vw - 32px));
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 56px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(16px);
  z-index: 20;
  box-sizing: border-box;
}

.tab-item {
  border: none;
  background: transparent;
  color: #787774;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 999px;
  flex-shrink: 0;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.tab-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
}

.tab-icon svg {
  width: 20px;
  height: 20px;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
  fill: none;
}

.tab-item.active {
  color: #151515;
  background: #edf3ec;
}

.tab-item.active .tab-icon {
  color: #346538;
}

.tab-item.add {
  background: #9f5c38;
  color: #fff;
  min-width: 56px;
  min-height: 40px;
  padding: 8px 16px;
}

.tab-icon-add,
.tab-item.add .tab-icon svg {
  width: 22px;
  height: 22px;
}

.tab-item small {
  color: #151515;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}
</style>
