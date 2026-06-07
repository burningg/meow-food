<template>
  <view class="page-shell edit-avatar-page">
    <header class="top-nav edit-top-nav">
      <button class="icon-button" @tap="goBack()">‹</button>
      <view class="title-block">
        <text class="eyebrow">个人信息</text>
        <text class="page-title">更换头像</text>
      </view>
      <view class="nav-spacer"></view>
    </header>

    <section class="profile-card">
      <view class="avatar-panel">
        <image v-if="avatarUrl" class="avatar" :src="avatarUrl" mode="aspectFill" />
        <view v-else class="avatar avatar-fallback">{{ nicknameInitial }}</view>
        <view class="avatar-copy">
          <text class="avatar-name">{{ displayName }}</text>
          <text class="muted">{{ authStore.user?.bio || '系统头像和自定义头像都会先预览，保存后生效。' }}</text>
          <text class="muted">{{ currentAvatarLabel }}</text>
        </view>
      </view>
    </section>

    <section class="avatar-card">
      <view class="section-head">
        <view>
          <text class="eyebrow">系统头像</text>
          <text class="section-title">挑一个更像你的头像</text>
        </view>
        <text class="muted">12 款可选</text>
      </view>

      <view class="avatar-grid">
        <button
          v-for="option in defaultAvatarOptions"
          :key="option.id"
          :class="['avatar-option', { active: avatar === option.src }]"
          @tap="selectDefaultAvatar(option.src)"
        >
          <image :src="option.src" mode="aspectFit" />
          <text>{{ option.label }}</text>
        </button>
      </view>
    </section>

    <section class="upload-card">
      <view class="section-head">
        <view>
          <text class="eyebrow">自定义头像</text>
          <text class="section-title">也可以换成自己的照片</text>
        </view>
      </view>

      <view class="upload-layout">
        <AUploadComponent :file-data="customAvatarPreview" tip="支持 JPG / PNG，系统会自动压缩" @success="handleAvatarUpload" />
        <view class="upload-copy">
          <text class="muted">新头像会先显示在这里，确认保存后才会换到个人主页。</text>
          <button v-if="customAvatarPreview && isCustomAvatar" class="ghost-button" @tap="restoreDefaultAvatar">
            改回系统头像
          </button>
        </view>
      </view>
    </section>

    <view class="bottom-bar">
      <button class="primary-button save-button" :disabled="saving" @tap="submitAvatar">
        {{ saving ? '保存中...' : '保存头像' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AUploadComponent from '@/components/AUploadComponent.vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { goBack } from '@/lib/navigation'
import { defaultAvatarOptions } from '@/assets/default-avatars'
import { SocialService } from '@/services/social-service'
import { useAuthStore } from '@/stores/auth-store'

const authStore = useAuthStore()
const socialService = new SocialService()
const saving = ref(false)
const avatar = ref(authStore.user?.avatar || '')

const defaultAvatarMap = new Map(defaultAvatarOptions.map((option) => [option.src, option.label]))
const avatarUrl = computed(() => avatar.value)
const displayName = computed(() => authStore.user?.nickname || 'meow')
const nicknameInitial = computed(() => (authStore.user?.nickname || authStore.user?.account || '我').slice(0, 1))
const isCustomAvatar = computed(() => Boolean(avatar.value) && !defaultAvatarMap.has(avatar.value))
const fallbackDefaultAvatar = computed(() => {
  const currentAvatar = authStore.user?.avatar || ''
  if (defaultAvatarMap.has(currentAvatar)) return currentAvatar
  return defaultAvatarOptions[0]?.src || ''
})
const currentAvatarLabel = computed(() => {
  if (!avatar.value) return '尚未设置头像'
  return defaultAvatarMap.get(avatar.value) || '自定义头像'
})
const customAvatarPreview = computed(() => (isCustomAvatar.value ? avatar.value : ''))

onMounted(() => {
  void requireAuth('edit-avatar')
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
    goBack('profile')
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}
</script>

<style>
.edit-avatar-page {
  min-height: 100vh;
  padding-bottom: 120px;
  background: #f7f6f3;
}

.edit-top-nav,
.profile-card,
.avatar-card,
.upload-card {
  border-radius: 12px;
  background: #fff;
  padding: 18px;
  margin-bottom: 16px;
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
.muted {
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

.profile-card {
  padding: 18px;
}

.avatar-panel {
  display: flex;
  align-items: center;
  gap: 14px;
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

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.avatar-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  border-radius: 14px;
  background: #f7f4ef;
  padding: 10px 6px;
  color: #787774;
  font-size: 11px;
}

.avatar-option.active {
  background: #edf3ec;
  color: #346538;
  font-weight: 800;
}

.avatar-option image {
  width: 52px;
  height: 52px;
}

.upload-layout {
  display: flex;
  gap: 14px;
  align-items: center;
}

.upload-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
}

.ghost-button {
  min-height: 40px;
  border-radius: 14px;
  background: #f5efe7;
  color: #151515;
  font-size: 13px;
  font-weight: 800;
}
</style>
