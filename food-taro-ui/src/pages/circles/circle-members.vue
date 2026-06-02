<template>
  <view class="page-shell circle-members-page">
    <view class="circle-members-content">
      <section class="circle-members-top-card">
        <button class="nav-shell" @tap="goBack({ name: 'circle-detail', params: { id: circleId } })">‹</button>
        <view class="circle-members-title-stack">
          <text class="eyebrow">搭子圈成员</text>
          <text class="title">圈内成员</text>
        </view>
        <view class="nav-shell nav-shell-placeholder"></view>
      </section>

      <section v-if="detail" class="circle-members-intro-card">
        <text class="intro-title">{{ detail.circle.name }}</text>
        <text class="muted">{{ detail.stats.memberCount }} 位成员 · {{ detail.stats.sharedMenuCount }} 份共享菜谱 · 发起人 {{ detail.circle.ownerNickname }}</text>
        <text class="intro-desc">{{ detail.circle.description || '只显示当前搭子圈里的成员，点击可查看对方菜单。' }}</text>
      </section>

      <section class="circle-members-list-section">
        <view class="circle-members-list-head">
          <text class="list-title">成员列表</text>
          <text v-if="detail" class="circle-members-count">{{ detail.stats.memberCount }} 人</text>
        </view>

        <view v-if="detail?.members.length" class="circle-members-list-card">
          <article v-for="(member, index) in orderedMembers" :key="member.id" class="circle-member-row">
            <button class="circle-member-main" @tap="openMember(member.id)">
              <view :class="['member-avatar-box', avatarToneClass(index)]">
                <text>{{ avatarInitial(member.nickname || member.account) }}</text>
              </view>
              <view class="circle-member-copy">
                <view class="circle-member-name-row">
                  <text class="member-name">{{ member.nickname }}</text>
                  <text :class="['role-pill', member.role === 'owner' ? 'role-pill-owner' : 'role-pill-member']">
                    {{ member.role === 'owner' ? '发起人' : '成员' }}
                  </text>
                </view>
                <text class="muted">{{ memberMeta(member, index) }}</text>
              </view>
            </button>
          </article>
        </view>

        <section v-else-if="!isLoading" class="circle-members-empty-card">
          <text>等有人加入后，就会出现在这里。</text>
        </section>
        <section v-else class="circle-members-empty-card">
          <text class="member-name">正在加载成员</text>
        </section>
      </section>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { getRouteParams, goBack, push } from '@/lib/navigation'
import { SocialService, type BuddyCircleDetail, type BuddyCircleMember } from '@/services/social-service'

const params = getRouteParams<{ id?: string }>()
const socialService = new SocialService()
const detail = ref<BuddyCircleDetail | null>(null)
const isLoading = ref(true)
const avatarTones = ['tone-sage', 'tone-apricot', 'tone-lavender']
const circleId = computed(() => String(params.id || ''))
const orderedMembers = computed(() => {
  const members = detail.value?.members || []
  return [...members].sort((left, right) => {
    if (left.role === right.role) return 0
    return left.role === 'owner' ? -1 : 1
  })
})

onMounted(async () => {
  if (!(await requireAuth('circle-members'))) return
  await loadCircleDetail()
})

async function loadCircleDetail() {
  if (!circleId.value) {
    isLoading.value = false
    return
  }
  isLoading.value = true
  try {
    const { data } = await socialService.getCircleDetail(circleId.value)
    detail.value = data
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '加载圈成员失败')
  } finally {
    isLoading.value = false
  }
}

function openMember(userId: string) {
  push({ name: 'user-menu', params: { id: userId } })
}

function avatarInitial(name: string) {
  return (name || '?').trim().slice(0, 1).toUpperCase()
}

function avatarToneClass(index: number) {
  return avatarTones[index % avatarTones.length]
}

function memberMeta(member: BuddyCircleMember, index: number) {
  if (member.role === 'owner') return `发起了这个搭子圈 · 共享 ${member.sharedMenuCount} 份菜单`
  const fallback = [
    `共享了 ${member.sharedMenuCount} 份菜单`,
    `通过搭子圈可见 ${Math.max(member.sharedMenuCount, 1)} 份内容`,
    `圈内一起收藏了不少想吃的菜`,
  ]
  return fallback[index % fallback.length]
}
</script>

<style scoped>
.circle-members-page {
  min-height: 100vh;
  background: #f7f6f3;
}

.circle-members-content {
  padding: 10px 20px 28px;
}

.circle-members-top-card,
.circle-members-intro-card,
.circle-members-list-card,
.circle-members-empty-card {
  background: #fff;
}

.circle-members-top-card,
.circle-members-list-head,
.circle-member-main,
.circle-member-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.circle-members-top-card {
  border-radius: 12px;
  padding: 16px;
}

.nav-shell-placeholder {
  visibility: hidden;
}

.circle-members-title-stack {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.eyebrow {
  color: #9f5c38;
  font-size: 11px;
  font-weight: 800;
}

.title,
.intro-title,
.list-title,
.member-name {
  color: #151515;
  font-weight: 800;
}

.title {
  font-size: 16px;
}

.circle-members-intro-card {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin-top: 16px;
  border-radius: 14px;
  padding: 16px;
}

.intro-title {
  font-size: 22px;
}

.muted,
.intro-desc,
.circle-members-count {
  color: #787774;
  font-size: 12px;
}

.intro-desc {
  line-height: 1.5;
}

.circle-members-list-section {
  margin-top: 24px;
}

.circle-members-list-card,
.circle-members-empty-card {
  margin-top: 12px;
  border-radius: 14px;
  overflow: hidden;
}

.circle-member-main {
  width: 100%;
  justify-content: flex-start;
  gap: 12px;
  padding: 14px;
  border-bottom: 1px solid #f0ece6;
  text-align: left;
}

.member-avatar-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  font-weight: 800;
}

.tone-sage {
  background: #edf3ec;
  color: #346538;
}

.tone-apricot {
  background: #f9ebdd;
  color: #9f5c38;
}

.tone-lavender {
  background: #eeeaf7;
  color: #6c58a5;
}

.circle-member-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
}

.role-pill {
  border-radius: 999px;
  padding: 3px 8px;
  font-size: 11px;
  font-weight: 800;
}

.role-pill-owner {
  background: #f9ebdd;
  color: #9f5c38;
}

.role-pill-member {
  background: #edf3ec;
  color: #346538;
}

.circle-members-empty-card {
  padding: 18px;
}
</style>
