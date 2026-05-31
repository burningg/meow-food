<template>
  <div ref="pageRef" class="page-shell edit-profile-page">
    <header class="top-nav" data-motion="section">
      <button class="icon-button" type="button" @click="router.back()">‹</button>
      <div class="title-block">
        <small>个人信息</small>
        <h1>编辑资料</h1>
      </div>
      <div class="nav-spacer" aria-hidden="true"></div>
    </header>

    <section class="profile-card" data-motion="section">
      <div class="avatar-panel">
        <img v-if="avatarUrl" class="avatar" :src="avatarUrl" :alt="form.nickname" />
        <div v-else class="avatar avatar-fallback" aria-hidden="true">{{ nicknameInitial }}</div>
        <div class="avatar-copy">
          <strong>{{ form.nickname || '未设置昵称' }}</strong>
          <span>{{ form.bio || '偏爱热汤和下饭菜，最近在认真收集一人食菜单。' }}</span>
          <small>{{ currentAvatarLabel }}</small>
        </div>
      </div>

      <button class="avatar-entry" type="button" @click="openEditAvatarPage">
        <strong>更换头像</strong>
      </button>
    </section>

    <section class="form-card" data-motion="section">
      <div class="section-head">
        <div>
          <small>基础资料</small>
          <h2>只保留最必要的信息</h2>
        </div>
      </div>

      <label class="field">
        <span>昵称</span>
        <input v-model.trim="form.nickname" type="text" maxlength="20" placeholder="例如 胖虎今天吃什么" />
      </label>

      <label class="field">
        <span>简介</span>
        <textarea
          v-model.trim="form.bio"
          rows="4"
          maxlength="80"
          placeholder="简单介绍一下你的口味、拿手菜或者最近想吃什么"
        />
      </label>
    </section>

    <section class="tips-card" data-motion="section">
      <small>展示预览</small>
      <p>资料卡会展示在你的个人主页顶部，保持简洁会更耐看。</p>
    </section>

    <div class="bottom-bar" data-motion="section">
      <button class="primary-button save-button" type="button" :disabled="saving" @click="submitProfile">
        {{ saving ? '保存中...' : '保存修改' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { animateStagger, attachPressAnimations, runScopedMotion } from '@/lib/motion'
import { defaultAvatarOptions } from '@/assets/default-avatars'
import { SocialService } from '@/services/social-service'
import { useAuthStore } from '@/stores/auth-store'

const router = useRouter()
const authStore = useAuthStore()
const socialService = new SocialService()
const pageRef = ref<HTMLElement | null>(null)
const saving = ref(false)
let cleanupMotion: VoidFunction | undefined

const form = reactive({
  nickname: authStore.user?.nickname || '',
  bio: authStore.user?.bio || '',
  account: authStore.user?.account || '',
  avatar: authStore.user?.avatar || '',
})

const defaultAvatarMap = new Map(defaultAvatarOptions.map((option) => [option.src, option.label]))
const avatarUrl = computed(() => form.avatar)
const nicknameInitial = computed(() => (form.nickname || form.account || '我').slice(0, 1))
const currentAvatarLabel = computed(() => {
  if (!form.avatar) return '尚未设置头像'
  return defaultAvatarMap.get(form.avatar) || '自定义头像'
})

onMounted(() => {
  if (!pageRef.value) return
  cleanupMotion = runScopedMotion(pageRef.value, ({ reducedMotion = false }) => {
    animateStagger(pageRef.value!.querySelectorAll('[data-motion="section"]'), {
      reducedMotion,
      y: 18,
      stagger: 0.07,
      duration: 0.3,
    })

    return attachPressAnimations(pageRef.value!, '.icon-button, .avatar-entry, .save-button', {
      activeScale: 0.98,
    })
  })
})

onUnmounted(() => {
  cleanupMotion?.()
})

function openEditAvatarPage() {
  router.push({ name: 'edit-avatar' })
}

async function submitProfile() {
  const nickname = form.nickname.trim()
  const bio = form.bio.trim()

  if (!nickname) {
    Message.error('请输入昵称')
    return
  }

  saving.value = true
  try {
    const { data } = await socialService.updateProfile({
      nickname,
      bio,
    })
    authStore.setSession(authStore.token, data)
    form.nickname = data.nickname || ''
    form.bio = data.bio || ''
    form.account = data.account || ''
    form.avatar = data.avatar || ''
    Message.success('个人信息已保存')
    router.back()
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.edit-profile-page {
  min-height: 100vh;
  padding-bottom: 120px;
  background: #f7f6f3;
}

.top-nav,
.profile-card,
.form-card,
.tips-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 12px 24px rgba(21, 21, 21, 0.05);
}

.top-nav,
.avatar-panel,
.section-head,
.avatar-entry {
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
.field span,
.field small,
.tips-card small,
.avatar-copy span,
.avatar-copy small,
.avatar-entry small {
  color: var(--text-muted);
}

.title-block small,
.section-head small,
.tips-card small {
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
.avatar-entry {
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
.form-card,
.tips-card {
  padding: 18px;
  margin-bottom: 16px;
}

.avatar-panel {
  gap: 14px;
  align-items: center;
  margin-bottom: 14px;
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
.tips-card p,
.field small,
.avatar-entry small {
  font-size: var(--text-sm);
  line-height: 1.6;
}

.avatar-entry {
  width: 100%;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 10px 16px;
  border-radius: 14px;
  background: #faf5ef;
  text-align: center;
}

.avatar-entry strong {
  color: #151515;
  font-size: 14px;
  font-weight: 600;
}

.section-head {
  margin-bottom: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.field:last-of-type {
  margin-bottom: 0;
}

.field input,
.field textarea {
  width: 100%;
  border: none;
  border-radius: 16px;
  padding: 14px 16px;
  background: #fffaf6;
  color: #151515;
  resize: none;
  outline: none;
}

.field input {
  min-height: 52px;
}

.tips-card p {
  margin: 10px 0 0;
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
</style>
