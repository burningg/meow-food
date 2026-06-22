<template>
  <view class="page-shell default-visibility-page">
    <!-- <section class="visibility-intro-card">
      <text class="visibility-intro-title">决定新菜谱默认向谁开放</text>
      <text class="visibility-intro-body">
        这里的选择会作为你发布新菜谱时的默认权限。你也可以按圈子细分开放范围。
      </text>
    </section> -->

    <section class="visibility-card">
      <text class="visibility-card-desc">你可以决定哪些圈子能看到你的菜单。默认权限会作为新菜谱的初始权限。</text>

      <button
        v-for="option in visibilityOptions"
        :key="option.value"
        :class="['visibility-row', { active: draftVisibility === option.value }]"
        :disabled="savingVisibility"
        @tap="selectVisibility(option.value)"
      >
        <view class="visibility-row-copy">
          <text class="visibility-title">{{ option.label }}</text>
          <text class="visibility-desc">{{ option.desc }}</text>
        </view>
        <text class="visibility-dot"></text>
      </button>

      <view v-if="draftVisibility === 'circle'" class="circle-picker-card">
        <view class="circle-picker-head">
          <text class="visibility-title">默认开放圈子</text>
          <text class="visibility-desc">{{ draftCircleIds.length }} 个已选</text>
        </view>
        <view v-if="circles.length" class="circle-chip-list">
          <button
            v-for="circle in circles"
            :key="circle.id"
            :class="['circle-chip', { active: draftCircleIds.includes(circle.id) }]"
            :disabled="savingVisibility"
            @tap="toggleCircle(circle.id)"
          >
            {{ circle.name }}
          </button>
        </view>
        <text v-else class="visibility-desc">你还没有加入圈子，暂时不能把默认权限设为指定圈子。</text>
      </view>
    </section>

    <button class="visibility-save-button" :disabled="savingVisibility" @tap="saveVisibility">
      {{ savingVisibility ? '保存中...' : '保存默认权限' }}
    </button>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useDidShow } from '@tarojs/taro'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { SocialService, type BuddyCircleSummary, type ProfileResponse } from '@/services/social-service'
import { useAuthStore } from '@/stores/auth-store'
import type { MenuVisibility } from '@/services/auth-service'

const socialService = new SocialService()
const authStore = useAuthStore()
const profile = ref<ProfileResponse | null>(null)
const circles = ref<BuddyCircleSummary[]>([])
const draftVisibility = ref<Exclude<MenuVisibility, 'inherit'>>('public')
const draftCircleIds = ref<string[]>([])
const savingVisibility = ref(false)

const visibilityOptions: Array<{ value: Exclude<MenuVisibility, 'inherit'>; label: string; desc: string }> = [
  { value: 'private', label: '仅自己可见', desc: '只在你自己的菜单空间展示' },
  { value: 'public', label: '圈内公开', desc: '对你所在全部圈子的成员开放' },
  { value: 'circle', label: '指定圈子', desc: '仅对你选中的圈子成员开放' },
]

onMounted(async () => {
  if (!(await requireAuth('default-visibility'))) return
  await loadVisibilitySettings()
})

useDidShow(async () => {
  if (!(await requireAuth('default-visibility'))) return
  await loadVisibilitySettings()
})

async function loadVisibilitySettings() {
  try {
    const [profileResult, circlesResult] = await Promise.allSettled([socialService.getProfile(), socialService.getCircles()])
    if (profileResult.status === 'fulfilled') {
      profile.value = profileResult.value.data
      draftVisibility.value = profileResult.value.data.defaultMenuVisibility
      draftCircleIds.value = [...profileResult.value.data.defaultMenuCircleIds]
    } else {
      throw profileResult.reason
    }

    if (circlesResult.status === 'fulfilled') {
      circles.value = circlesResult.value.data
    } else {
      circles.value = []
    }
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '默认权限加载失败')
  }
}

function selectVisibility(value: Exclude<MenuVisibility, 'inherit'>) {
  if (savingVisibility.value) return
  draftVisibility.value = value
  if (value !== 'circle') {
    draftCircleIds.value = []
  }
}

function toggleCircle(circleId: string) {
  if (!circleId || savingVisibility.value) return
  if (draftCircleIds.value.includes(circleId)) {
    draftCircleIds.value = draftCircleIds.value.filter((id) => id !== circleId)
    return
  }
  draftCircleIds.value = [...draftCircleIds.value, circleId]
}

async function saveVisibility() {
  if (savingVisibility.value) return
  if (draftVisibility.value === 'circle' && !draftCircleIds.value.length) {
    Message.warning('请选择至少一个默认圈子')
    return
  }

  savingVisibility.value = true
  try {
    // 统一在这个独立页面里保存默认权限，避免个人页上频繁触发保存请求。
    const { data } = await socialService.updateVisibility(
      draftVisibility.value,
      draftVisibility.value === 'circle' ? draftCircleIds.value : [],
    )
    authStore.user = data
    Message.success('菜单默认可见范围已更新')
    await loadVisibilitySettings()
    await authStore.restore()
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '权限更新失败，请稍后重试')
    if (profile.value) {
      draftVisibility.value = profile.value.defaultMenuVisibility
      draftCircleIds.value = [...profile.value.defaultMenuCircleIds]
    }
  } finally {
    savingVisibility.value = false
  }
}
</script>

<style>
.default-visibility-page {
  min-height: 100vh;
  padding-bottom: 32px;
}

.visibility-intro-card,
.visibility-card {
  border-radius: 22px;
  background: #fff;
  box-shadow: var(--shadow);
}

.visibility-intro-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 18px;
  padding: 18px;
}

.visibility-intro-title {
  color: #151515;
  font-size: 22px;
  font-weight: 800;
}

.visibility-intro-body,
.visibility-desc,
.visibility-card-desc {
  color: #787774;
  font-size: 13px;
  line-height: 1.55;
}

.visibility-card {
  overflow: hidden;
  padding: 16px;
}

.visibility-card-desc {
  display: block;
  margin-bottom: 8px;
}

.visibility-row {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0;
  border-top: 1px solid var(--line);
  text-align: left;
}

.visibility-row:first-of-type {
  border-top: none;
}

.visibility-row[disabled] {
  opacity: 0.72;
}

.visibility-row-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 4px;
}

.visibility-title {
  color: var(--text-main);
  font-size: 14px;
  font-weight: 800;
}

.visibility-dot {
  width: 12px;
  height: 12px;
  flex: 0 0 auto;
  border: 2px solid #e0d7ca;
  border-radius: 999px;
}

.visibility-row.active .visibility-dot {
  border-color: #9f5c38;
  background: #9f5c38;
}

.circle-picker-card {
  margin-top: 14px;
  border-radius: 16px;
  background: #f7f4ef;
  padding: 14px;
}

.circle-picker-head,
.circle-chip-list {
  display: flex;
}

.circle-picker-head {
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.circle-chip-list {
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.circle-chip {
  border-radius: 999px;
  background: #ebe4d8;
  color: #6e6253;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 800;
  text-align: center;
}

.circle-chip.active {
  background: #9f5c38;
  color: #fff;
}

.visibility-save-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 50px;
  margin-top: 18px;
  border-radius: 14px;
  background: #9f5c38;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  text-align: center;
}

.visibility-save-button[disabled] {
  opacity: 0.72;
}
</style>
