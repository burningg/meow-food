<template>
  <view class="page-shell user-menu-page">
    <header class="top-nav">
      <button class="back" @tap="goBack()">‹</button>
      <text class="page-title">访问 {{ access?.user.nickname }} 的菜单</text>
      <view class="back placeholder"></view>
    </header>

    <section class="hero-card">
      <image v-if="access?.user.avatar" class="avatar" :src="access.user.avatar" mode="aspectFill" />
      <view v-else class="avatar avatar-fallback">{{ access?.user.nickname?.slice(0, 1) || '?' }}</view>
      <view class="hero-copy">
        <text class="hero-title">{{ access?.user.nickname }} 的私房菜单</text>
        <text class="muted">{{ access?.description }}</text>
      </view>
    </section>

    <section class="menus">
      <view class="section-head">
      <view>
        <text class="section-title">可访问菜单</text>
      </view>
      <text class="muted">{{ access?.accessibleCount ?? 0 }} 份</text>
    </view>
    <button v-for="menu in access?.menus || []" :key="menu.id" class="menu-row" @tap="openDish(menu.id)">
        <SmartImage :src="menu.image" class-name="menu-image" />
        <view>
          <text class="menu-name">{{ menu.name }}</text>
          <text class="muted">{{ menu.description }}</text>
        </view>
      </button>
      <view v-if="access && !access.menus.length" class="empty-card">暂无可访问菜单</view>
    </section>

    <button class="primary-button action" @tap="handleAction">
      {{ access?.actionType === 'invite-circle' ? '邀请加入搭子圈' : '发起好友申请' }}
    </button>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import SmartImage from '@/components/SmartImage.vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { getRouteParams, goBack, push } from '@/lib/navigation'
import { SocialService, type UserMenuAccessResponse } from '@/services/social-service'

const socialService = new SocialService()
const access = ref<UserMenuAccessResponse | null>(null)
const params = getRouteParams() as { id?: string }

onMounted(async () => {
  if (!(await requireAuth('user-menu'))) return
  await loadData()
})

async function loadData() {
  try {
    const { data } = await socialService.getUserMenuAccess(String(params.id || ''))
    access.value = data
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '菜单访问加载失败')
  }
}

function openDish(id: string) {
  push({ name: 'dish-detail', params: { id } })
}

async function handleAction() {
  if (!access.value) return
  if (access.value.actionType === 'friend-request') {
    await socialService.createFriendRequest({
      targetUserId: access.value.user.id,
      message: '想看看你的菜单，也想一起分享美味。',
    })
    Message.success('好友申请已发送')
    return
  }
  push('circles')
}
</script>

<style>
.user-menu-page {
  padding-bottom: 48px;
}

.page-title {
  color: var(--text-main);
  font-size: var(--title-sm);
  font-weight: 700;
}

.hero-card,
.menus {
  display: flex;
  margin-top: 16px;
  border-radius: 22px;
  background: #fff;
  padding: 18px;
  box-shadow: var(--shadow);
}

.hero-card {
  gap: 14px;
  align-items: center;
}

.hero-copy,
.menus,
.menu-row view {
  display: flex;
  flex-direction: column;
}

.hero-copy {
  gap: 5px;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  flex: 0 0 auto;
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

.hero-title,
.section-title {
  color: var(--text-main);
  font-size: var(--title-md);
  font-weight: 800;
}

.muted {
  color: var(--text-muted);
  font-size: var(--text-sm);
  line-height: 1.55;
}

.section-head,
.menu-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.menu-row {
  justify-content: flex-start;
  gap: 12px;
  width: 100%;
  padding: 12px 0;
  border-top: 1px solid var(--line);
  text-align: left;
}

.menu-image {
  width: 62px;
  height: 62px;
  border-radius: 16px;
  flex: 0 0 auto;
}

.menu-name {
  color: var(--text-main);
  font-weight: 800;
}

.action {
  margin-top: 18px;
}
</style>
