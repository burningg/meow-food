<template>
  <view class="page-shell edit-profile-page">
    <header class="top-nav edit-top-nav">
      <button class="icon-button" @tap="goBack()">‹</button>
      <view class="title-block">
        <text class="eyebrow">个人信息</text>
        <text class="page-title">编辑资料</text>
      </view>
      <view class="nav-spacer"></view>
    </header>

    <section class="profile-card">
      <view class="avatar-panel">
        <image v-if="avatarUrl" class="avatar" :src="avatarUrl" mode="aspectFill" />
        <view v-else class="avatar avatar-fallback">{{ nicknameInitial }}</view>
        <view class="avatar-copy">
          <text class="avatar-name">{{ form.nickname || '未设置昵称' }}</text>
          <text class="muted">{{ form.bio || '偏爱热汤和下饭菜，最近在认真收集一人食菜单。' }}</text>
          <text class="muted">{{ currentAvatarLabel }}</text>
        </view>
      </view>

      <button class="avatar-entry" @tap="openEditAvatarPage">
        <text>更换头像</text>
      </button>
    </section>

    <section class="form-card">
      <view class="section-head">
        <view>
          <text class="eyebrow">基础资料</text>
          <text class="section-title">只保留最必要的信息</text>
        </view>
      </view>

      <label class="field">
        <text>昵称</text>
        <input v-model.trim="form.nickname" maxlength="20" placeholder="例如 meow今天吃什么" />
      </label>

      <label class="field">
        <text>简介</text>
        <textarea v-model.trim="form.bio" maxlength="80" placeholder="简单介绍一下你的口味、拿手菜或者最近想吃什么" />
      </label>
    </section>

    <section class="tips-card">
      <text class="eyebrow">展示预览</text>
      <text class="muted">资料卡会展示在你的个人主页顶部，保持简洁会更耐看。</text>
    </section>

    <view class="bottom-bar">
      <button class="primary-button save-button" :disabled="saving" @tap="submitProfile">
        {{ saving ? '保存中...' : '保存修改' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { goBack, push } from '@/lib/navigation'
import { defaultAvatarOptions } from '@/assets/default-avatars'
import { SocialService } from '@/services/social-service'
import { useAuthStore } from '@/stores/auth-store'

const authStore = useAuthStore()
const socialService = new SocialService()
const saving = ref(false)

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
  void requireAuth('edit-profile')
})

function openEditAvatarPage() {
  push('edit-avatar')
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
    goBack('profile')
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}
</script>

<style>
.edit-profile-page {
  min-height: 100vh;
  padding-bottom: 120px;
  background: #f7f6f3;
}

.edit-top-nav,
.profile-card,
.form-card,
.tips-card {
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 12px 24px rgba(21, 21, 21, 0.05);
}

.edit-top-nav {
  gap: 12px;
  padding: 14px 16px;
}

.title-block {
  display: flex;
  flex: 1;
  flex-direction: column;
}

.nav-spacer {
  width: 36px;
  height: 36px;
}

.eyebrow,
.muted,
.field text {
  color: var(--text-muted);
  font-size: var(--text-sm);
}

.page-title,
.section-title {
  color: var(--text-main);
  font-weight: 800;
}

.page-title {
  font-size: var(--title-lg);
}

.section-title {
  display: block;
  margin-top: 4px;
  font-size: var(--title-md);
}

.profile-card,
.form-card,
.tips-card {
  padding: 18px;
  margin-bottom: 16px;
}

.avatar-panel {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 18px;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #edf3ec;
  color: #346538;
  font-size: 22px;
  font-weight: 800;
}

.avatar-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.avatar-name {
  color: #151515;
  font-weight: 800;
}

.avatar-entry {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 44px;
  padding: 10px 16px;
  border-radius: 14px;
  background: #faf5ef;
  color: #151515;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
  text-align: center;
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
  border-radius: 16px;
  background: #fffaf6;
  color: #151515;
  padding: 0 16px;
}

.field input {
  min-height: 52px;
}

.field textarea {
  min-height: 104px;
  padding-top: 14px;
}

.edit-profile-page .bottom-bar {
  position: fixed;
  left: 50%;
  bottom: calc(24px + env(safe-area-inset-bottom));
  z-index: 18;
  width: min(358px, calc(100vw - 32px));
  transform: translateX(-50%);
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 32px rgba(21, 21, 21, 0.08);
  backdrop-filter: blur(14px);
}

.save-button {
  width: 100%;
  min-height: 50px;
  background: #9f5c38;
  box-shadow: none;
}

.save-button[disabled] {
  opacity: 0.7;
}
</style>
