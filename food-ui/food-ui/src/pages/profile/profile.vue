<template>
  <div ref="pageRef" class="page-shell profile-page">
    <section class="hero-card" data-motion="hero-card">
      <div class="hero-top">
        <div class="user-row">
          <img v-if="displayAvatar" class="avatar" :src="displayAvatar" :alt="displayName" />
          <div v-else class="avatar avatar-fallback" aria-hidden="true">{{ displayName.slice(0, 1) }}</div>
          <div>
            <h1 class="hero-title">
              <span class="hero-title-name">{{ displayName }}</span>
              <span class="hero-title-suffix">的美味空间</span>
            </h1>
            <p>{{ displayBio }}</p>
          </div>
        </div>
        <div class="hero-actions">
          <button class="ghost-circle" type="button" @click="openEditProfilePage">✎</button>
        </div>
      </div>

      <div class="stats-row">
        <button class="stat-button" type="button" @click="openFriendsPage">
          <strong>{{ profile?.stats.friendCount ?? 0 }}</strong>
          <span>好友</span>
        </button>
        <div>
          <strong>{{ profile?.stats.menuCount ?? 0 }}</strong>
          <span>菜单</span>
        </div>
        <div>
          <strong>{{ profile?.stats.circleCount ?? 0 }}</strong>
          <span>搭子圈</span>
        </div>
      </div>
    </section>

    <section class="section" data-motion="section">
      <div class="section-head">
        <div>
          <small>权限</small>
        </div>
      </div>
      <article class="visibility-card">
        <button
          v-for="option in visibilityOptions"
          :key="option.value"
          :class="['visibility-row', { active: profile?.defaultMenuVisibility === option.value }]"
          data-motion-row
          type="button"
          @click="updateVisibility(option.value)"
        >
          <span>
            <strong>{{ option.label }}</strong>
            <small>{{ option.desc }}</small>
          </span>
          <i></i>
        </button>
      </article>
    </section>

    <section class="section logout-section" data-motion="section">
      <button class="primary-button logout-button" type="button" @click="logout">退出登录</button>
    </section>

    <AppTabBar active="profile" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import AppTabBar from '@/components/AppTabBar.vue'
import { animateStagger, attachPressAnimations, runScopedMotion } from '@/lib/motion'
import { SocialService, type ProfileResponse } from '@/services/social-service'
import { useAuthStore } from '@/stores/auth-store'
import type { MenuVisibility } from '@/services/auth-service'

const router = useRouter()
const authStore = useAuthStore()
const socialService = new SocialService()

const profile = ref<ProfileResponse | null>(null)
const pageRef = ref<HTMLElement | null>(null)
let cleanupMotion: VoidFunction | undefined
let cleanupProfileRows: VoidFunction | undefined

const displayName = computed(() => profile.value?.user.nickname || authStore.user?.nickname || 'meow')
const displayBio = computed(() => profile.value?.user.bio || '菜单、好友和搭子圈都在这里慢慢展开。')
const displayAvatar = computed(() => profile.value?.user.avatar || authStore.user?.avatar || '')

const visibilityOptions: Array<{ value: Exclude<MenuVisibility, 'inherit'>; label: string; desc: string }> = [
  { value: 'friends', label: '好友可见', desc: '你的菜单会同步给好友和已加入的搭子圈。' },
  { value: 'public', label: '公开', desc: '任何用户都可以从动态和菜单页进入。' },
  { value: 'private', label: '仅自己可见', desc: '只在你自己的菜单空间展示。' },
]

onMounted(async () => {
  setupMotion()
  await loadProfile()
})

onUnmounted(() => {
  cleanupProfileRows?.()
  cleanupMotion?.()
})

async function loadProfile() {
  const { data } = await socialService.getProfile()
  profile.value = data
}

async function updateVisibility(value: Exclude<MenuVisibility, 'inherit'>) {
  await socialService.updateVisibility(value)
  Message.success('菜单默认可见范围已更新')
  await loadProfile()
  await authStore.restore()
}

function openFriendsPage() {
  router.push({ name: 'friends' })
}

function openEditProfilePage() {
  router.push({ name: 'edit-profile' })
}

function logout() {
  authStore.logout()
  router.replace({ name: 'login' })
}

function setupMotion() {
  if (!pageRef.value) return
  cleanupMotion = runScopedMotion(pageRef.value, ({ reducedMotion = false }) => {
    animateStagger(pageRef.value!.querySelectorAll('[data-motion="hero-card"], [data-motion="section"]'), {
      reducedMotion,
      y: 22,
      stagger: 0.08,
      duration: 0.34,
    })

    return attachPressAnimations(
      pageRef.value!,
      '.ghost-circle, .stat-button, .visibility-row, .logout-button',
      { activeScale: 0.97 },
    )
  })
}

async function refreshProfileMotion() {
  await nextTick()
  cleanupProfileRows?.()
  if (!pageRef.value) return

  cleanupProfileRows = runScopedMotion(pageRef.value, ({ reducedMotion = false }) => {
    animateStagger(pageRef.value!.querySelectorAll('[data-motion-row]'), {
      reducedMotion,
      y: 16,
      stagger: 0.06,
      duration: 0.28,
      delay: 0.06,
    })
  })
}

watch(profile, () => {
  void refreshProfileMotion()
})
</script>

<style scoped>
.profile-page {
  padding-bottom: 120px;
}

.hero-card,
.visibility-card,
.section {
  background: #fff;
  border-radius: 22px;
  box-shadow: 0 12px 26px rgba(27, 58, 45, 0.06);
}

.hero-card {
  padding: 18px;
  margin-bottom: 18px;
}

.hero-top,
.user-row,
.stats-row,
.section-head,
.hero-actions {
  display: flex;
}

.hero-top,
.section-head {
  justify-content: space-between;
}

.user-row {
  gap: 14px;
  align-items: center;
}

.hero-actions {
  gap: 8px;
  align-items: flex-start;
}

.avatar {
  width: 58px;
  height: 58px;
  border-radius: 16px;
  object-fit: cover;
}

.avatar-fallback {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #7a9e7e, #a7be8f);
  color: #fff;
  font-weight: 700;
}

h1,
h2 {
  margin: 0;
  color: var(--text-main);
}

h1 {
  font-size: var(--title-lg);
  font-family: var(--font-serif);
  font-weight: 600;
  line-height: 1.25;
}

.hero-title {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  column-gap: 2px;
  row-gap: 0;
}

.hero-title-name {
  font-size: var(--title-lg);
  color: var(--text-main);
}

.hero-title-suffix {
  font-size: var(--text-md);
  color: var(--text-muted);
}

.hero-top p,
small,
.visibility-card p,
.visibility-row small {
  color: var(--text-muted);
}

.hero-top p {
  margin: 8px 0 0;
  line-height: 1.6;
  font-size: var(--text-sm);
}

.ghost-circle {
  border: none;
  background: #f5f2ed;
  border-radius: 999px;
  width: 36px;
  height: 36px;
}

.stats-row {
  justify-content: space-between;
  margin: 18px 0;
  padding: 18px 0;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}

.stats-row div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-button {
  border: none;
  background: transparent;
  padding: 0;
  color: inherit;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.stats-row strong {
  font-size: var(--title-lg);
  font-variant-numeric: tabular-nums;
}


.flex-button {
  flex: 1;
}

.secondary-button {
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
  color: var(--text-main);
}

.section {
  padding: 18px;
  margin-bottom: 18px;
}

.section-head {
  align-items: end;
  margin-bottom: 14px;
}

.section-head small {
  display: block;
  margin-bottom: 2px;
  letter-spacing: 0.08em;
  font-size: var(--text-xs);
}

.ghost-link {
  border: none;
  background: transparent;
  color: var(--accent);
}

.visibility-card {
  padding: 16px;
}

.visibility-row {
  width: 100%;
  border: none;
  background: transparent;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-top: 1px solid var(--line);
}

.visibility-row span {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.visibility-row strong {
  font-size: var(--text-md);
  font-weight: 600;
}

.visibility-row small {
  font-size: var(--text-sm);
  line-height: 1.5;
}

.visibility-row i {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #d8d2cb;
}

.visibility-row.active i {
  background: var(--text-main);
}

.logout-section {
  padding: 0;
  background: transparent;
  box-shadow: none;
}

.logout-button {
  width: 100%;
  background: #fff;
  color: #b14d3a;
  box-shadow: 0 12px 26px rgba(27, 58, 45, 0.06);
}
</style>
