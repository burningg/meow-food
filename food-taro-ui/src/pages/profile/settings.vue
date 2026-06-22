<template>
  <view class="page-shell settings-page">
    <section class="settings-card settings-card-spaced">
      <button class="settings-row settings-row-link" :disabled="Boolean(savingKey)" @tap="openEditProfilePage">
        <view class="settings-row-copy">
          <text class="settings-row-title">编辑资料</text>
          <text class="settings-row-desc">修改昵称、简介和头像展示</text>
        </view>
        <text class="settings-row-arrow">›</text>
      </button>
    </section>

    <section class="settings-card">
      <button
        v-for="item in preferenceItems"
        :key="item.key"
        class="settings-row"
        :disabled="Boolean(savingKey)"
        @tap="togglePreference(item.key)"
      >
        <view class="settings-row-copy">
          <text class="settings-row-title">{{ item.title }}</text>
          <text class="settings-row-desc">{{ item.desc }}</text>
        </view>
        <view :class="['settings-switch', { active: item.enabled, saving: savingKey === item.key }]">
          <view class="settings-switch-thumb"></view>
        </view>
      </button>
    </section>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useDidShow } from '@tarojs/taro'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { goBack, push } from '@/lib/navigation'
import { SocialService } from '@/services/social-service'
import { useAuthStore } from '@/stores/auth-store'

type PreferenceKey = 'knowledge' | 'pet'

const socialService = new SocialService()
const authStore = useAuthStore()
const showKnowledgeOnHome = ref(true)
const showPetOnHome = ref(true)
const savingKey = ref<PreferenceKey | ''>('')

const preferenceItems = computed(() => [
  {
    key: 'knowledge' as const,
    title: '饮食小知识',
    desc: '首页显示知识分享',
    enabled: showKnowledgeOnHome.value,
  },
  {
    key: 'pet' as const,
    title: '宠物管理',
    desc: '首页显示宠物',
    enabled: showPetOnHome.value,
  },
])

onMounted(async () => {
  if (!(await requireAuth('settings'))) return
  await loadPreferences()
})

useDidShow(async () => {
  if (!(await requireAuth('settings'))) return
  await loadPreferences()
})

async function loadPreferences() {
  try {
    const { data } = await socialService.getProfile()
    showKnowledgeOnHome.value = data.showKnowledgeOnHome !== false
    showPetOnHome.value = data.showPetOnHome !== false
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '设置加载失败')
  }
}

function openEditProfilePage() {
  push('edit-profile')
}

async function togglePreference(key: PreferenceKey) {
  if (savingKey.value) return

  const previousKnowledge = showKnowledgeOnHome.value
  const previousPet = showPetOnHome.value
  if (key === 'knowledge') {
    showKnowledgeOnHome.value = !showKnowledgeOnHome.value
  } else {
    showPetOnHome.value = !showPetOnHome.value
  }

  savingKey.value = key
  try {
    const { data } = await socialService.updateHomePreferences({
      showKnowledgeOnHome: showKnowledgeOnHome.value,
      showPetOnHome: showPetOnHome.value,
    })
    authStore.user = data
    Message.success('设置已保存')
  } catch (error: any) {
    showKnowledgeOnHome.value = previousKnowledge
    showPetOnHome.value = previousPet
    Message.error(error?.response?.data?.message || '设置保存失败，请稍后重试')
  } finally {
    savingKey.value = ''
  }
}
</script>

<style>
.settings-page {
  min-height: 100vh;
  padding-bottom: 120px;
}

.settings-top-nav {
  min-height: 42px;
}

.settings-nav-title {
  color: var(--text-main);
  font-size: var(--title-md);
  font-weight: 800;
}

.settings-card {
  overflow: hidden;
  border-radius: 22px;
  background: #fff;
  box-shadow: var(--shadow);
}

.settings-card-spaced {
  margin-bottom: 16px;
}

.settings-row {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  text-align: left;
}

.settings-row + .settings-row {
  border-top: 1px solid var(--line);
}

.settings-row[disabled] {
  opacity: 0.72;
}

.settings-row-link {
  min-height: 74px;
}

.settings-row-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 4px;
}

.settings-row-title {
  color: var(--text-main);
  font-size: 16px;
  font-weight: 800;
}

.settings-row-desc {
  color: var(--text-muted);
  font-size: var(--text-sm);
}

.settings-row-arrow {
  color: var(--text-muted);
  font-size: 20px;
  font-weight: 600;
  line-height: 1;
}

.settings-switch {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  width: 50px;
  height: 30px;
  border-radius: 999px;
  background: #dedbd5;
  padding: 3px;
  transition: background 0.2s ease;
}

.settings-switch.active {
  justify-content: flex-end;
  background: #7a9e7e;
}

.settings-switch.saving {
  background: #b7c8b5;
}

.settings-switch-thumb {
  width: 24px;
  height: 24px;
  border-radius: 999px;
  background: #fff;
  box-shadow: 0 4px 10px rgba(27, 58, 45, 0.14);
}
</style>
