<template>
  <div ref="pageRef" class="page-shell edit-avatar-page">
    <header class="top-nav" data-motion="section">
      <button class="icon-button" type="button" @click="router.back()">‹</button>
      <div class="title-block">
        <small>个人信息</small>
        <h1>更换头像</h1>
      </div>
      <div class="nav-spacer" aria-hidden="true"></div>
    </header>

    <section class="profile-card" data-motion="section">
      <div class="avatar-panel">
        <img v-if="avatarUrl" class="avatar" :src="avatarUrl" :alt="displayName" />
        <div v-else class="avatar avatar-fallback" aria-hidden="true">{{ nicknameInitial }}</div>
        <div class="avatar-copy">
          <strong>{{ displayName }}</strong>
          <span>{{ authStore.user?.bio || '系统头像和自定义头像都会先预览，保存后生效。' }}</span>
          <small>{{ currentAvatarLabel }}</small>
        </div>
      </div>
    </section>

    <section class="avatar-card" data-motion="section">
      <div class="section-head">
        <div>
          <small>系统头像</small>
          <h2>挑一个更像你的头像</h2>
        </div>
        <span class="section-tip">12 款可选</span>
      </div>

      <div class="avatar-grid">
        <button
          v-for="option in defaultAvatarOptions"
          :key="option.id"
          :class="['avatar-option', { active: avatar === option.src }]"
          type="button"
          @click="selectDefaultAvatar(option.src)"
        >
          <img :src="option.src" :alt="option.label" />
          <span>{{ option.label }}</span>
        </button>
      </div>
    </section>

    <section class="upload-card" data-motion="section">
      <div class="section-head">
        <div>
          <small>自定义头像</small>
          <h2>也可以换成自己的照片</h2>
        </div>
      </div>

      <div class="upload-layout">
        <AUploadComponent :fileData="customAvatarPreview" tip="支持 JPG / PNG，系统会自动压缩" @success="handleAvatarUpload" />
        <div class="upload-copy">
          <p>上传成功后会立即预览。想回到系统头像，也可以一键切回默认头像。</p>
          <button v-if="customAvatarPreview && isCustomAvatar" class="ghost-button" type="button" @click="restoreDefaultAvatar">
            改回系统头像
          </button>
        </div>
      </div>
    </section>

    <div class="bottom-bar" data-motion="section">
      <button class="primary-button save-button" type="button" :disabled="saving" @click="submitAvatar">
        {{ saving ? '保存中...' : '保存头像' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import AUploadComponent from '@/components/AUploadComponent.vue'
import { defaultAvatarOptions } from '@/assets/default-avatars'
import { animateStagger, attachPressAnimations, runScopedMotion } from '@/lib/motion'
import { SocialService } from '@/services/social-service'
import { useAuthStore } from '@/stores/auth-store'

const router = useRouter()
const authStore = useAuthStore()
const socialService = new SocialService()
const pageRef = ref<HTMLElement | null>(null)
const saving = ref(false)
const avatar = ref(authStore.user?.avatar || '')
let cleanupMotion: VoidFunction | undefined

const defaultAvatarMap = new Map(defaultAvatarOptions.map((option) => [option.src, option.label]))
const avatarUrl = computed(() => avatar.value)
const displayName = computed(() => authStore.user?.nickname || 'meow')
const nicknameInitial = computed(() => (authStore.user?.nickname || authStore.user?.account || '我').slice(0, 1))
const isCustomAvatar = computed(() => Boolean(avatar.value) && !defaultAvatarMap.has(avatar.value))
const fallbackDefaultAvatar = computed(() => {
  const currentAvatar = authStore.user?.avatar || ''
  if (defaultAvatarMap.has(currentAvatar)) {
    return currentAvatar
  }
  return defaultAvatarOptions[0]?.src || ''
})
const currentAvatarLabel = computed(() => {
  if (!avatar.value) return '尚未设置头像'
  return defaultAvatarMap.get(avatar.value) || '自定义头像'
})
const customAvatarPreview = computed(() => (isCustomAvatar.value ? avatar.value : ''))

onMounted(() => {
  if (!pageRef.value) return
  cleanupMotion = runScopedMotion(pageRef.value, ({ reducedMotion = false }) => {
    animateStagger(pageRef.value!.querySelectorAll('[data-motion="section"]'), {
      reducedMotion,
      y: 18,
      stagger: 0.07,
      duration: 0.3,
    })

    return attachPressAnimations(pageRef.value!, '.icon-button, .ghost-button, .save-button, .avatar-option', {
      activeScale: 0.98,
    })
  })
})

onUnmounted(() => {
  cleanupMotion?.()
})

function selectDefaultAvatar(src: string) {
  avatar.value = src
}

function handleAvatarUpload(url: string) {
  avatar.value = url
  Message.success('自定义头像已上传，保存后即可生效')
}

function restoreDefaultAvatar() {
  avatar.value = fallbackDefaultAvatar.value
}

async function submitAvatar() {
  const nickname = authStore.user?.nickname?.trim() || ''
  const bio = authStore.user?.bio || ''

  if (!nickname) {
    Message.error('当前用户昵称为空，请先返回个人信息页补充昵称')
    return
  }

  saving.value = true
  try {
    const { data } = await socialService.updateProfile({
      nickname,
      bio,
      avatar: avatar.value,
    })
    authStore.setSession(authStore.token, data)
    Message.success('头像已保存')
    router.back()
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.edit-avatar-page {
  min-height: 100vh;
  padding-bottom: 120px;
  background: #f7f6f3;
}

.top-nav,
.profile-card,
.avatar-card,
.upload-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 12px 24px rgba(21, 21, 21, 0.05);
}

.top-nav,
.avatar-panel,
.section-head {
  display: flex;
}

.top-nav {
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 16px;
}

.title-block {
  flex: 1;
}

.nav-spacer {
  width: 36px;
  height: 36px;
}

.title-block small,
.section-head small,
.avatar-copy span,
.section-tip {
  color: var(--text-muted);
}

.title-block small,
.section-head small {
  display: block;
  letter-spacing: 0.08em;
  font-size: var(--text-xs);
}

.title-block h1,
.section-head h2 {
  margin: 4px 0 0;
  color: var(--text-main);
}

.title-block h1 {
  font-size: var(--title-lg);
  font-family: var(--font-serif);
  font-weight: 500;
}

.section-head h2 {
  font-size: var(--title-md);
  font-family: var(--font-serif);
  font-weight: 500;
}

.icon-button,
.ghost-button,
.avatar-option {
  border: none;
}

.icon-button {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: #f5efe7;
  color: #151515;
  font-size: 1.2rem;
}

.profile-card,
.avatar-card,
.upload-card {
  padding: 18px;
  margin-bottom: 16px;
}

.avatar-panel {
  gap: 14px;
  align-items: center;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  object-fit: cover;
}

.avatar-fallback {
  display: grid;
  place-items: center;
  background: #edf3ec;
  color: #346538;
  font-size: 1.35rem;
  font-weight: 600;
}

.avatar-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.avatar-copy strong {
  color: #151515;
  font-size: 1rem;
  font-weight: 600;
}

.avatar-copy span,
.avatar-copy small,
.upload-copy p {
  font-size: var(--text-sm);
  line-height: 1.6;
}

.section-head {
  align-items: end;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-tip {
  font-size: var(--text-xs);
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.avatar-option {
  border-radius: 16px;
  padding: 10px 8px;
  background: #fffaf6;
  border: 1px solid transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.avatar-option img {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  object-fit: cover;
}

.avatar-option span {
  font-size: 11px;
  color: var(--text-main);
  line-height: 1.3;
}

.avatar-option.active {
  border-color: #9f5c38;
  box-shadow: 0 10px 20px rgba(159, 92, 56, 0.14);
  transform: translateY(-2px);
}

.upload-layout {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
}

.upload-copy {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.upload-copy p {
  margin: 0;
  color: var(--text-muted);
}

.ghost-button {
  min-height: 44px;
  border-radius: 14px;
  background: #f8f4ef;
  color: var(--text-main);
  padding: 0 16px;
}

.bottom-bar {
  position: fixed;
  left: 50%;
  bottom: 24px;
  width: min(358px, calc(100vw - 32px));
  transform: translateX(-50%);
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 32px rgba(21, 21, 21, 0.08);
}

.save-button {
  width: 100%;
  min-height: 50px;
  background: #9f5c38;
  box-shadow: none;
}

.save-button:disabled {
  opacity: 0.7;
}

@media (max-width: 640px) {
  .avatar-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .upload-layout {
    grid-template-columns: 1fr;
  }
}
</style>
