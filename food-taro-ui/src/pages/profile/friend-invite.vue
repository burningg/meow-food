<template>
  <view class="invite-page">
    <view class="invite-bg invite-bg-top"></view>
    <view class="invite-bg invite-bg-bottom"></view>

    <view class="invite-content">
      <view class="invite-badge">
        <text class="badge-dot"></text>
        <text>meow食堂 · 好友邀请</text>
      </view>

      <section v-if="invitation" class="invite-hero-card">
        <view class="plate-ring">
          <image v-if="invitation.inviter.avatar" class="inviter-avatar" :src="invitation.inviter.avatar" mode="aspectFill" />
          <text v-else class="avatar-fallback">{{ inviterInitial }}</text>
        </view>

        <view class="invite-copy">
          <text class="invite-kicker">来自 {{ invitation.inviter.nickname || '好友' }} 的邀请</text>
          <text class="invite-title">一起交换私房菜单和美味灵感</text>
          <text class="invite-desc">接受后，你们会自动成为好友，也能看到彼此开放给好友的菜单。</text>
        </view>

        <view class="status-strip">
          <text>{{ statusText }}</text>
        </view>

        <view class="action-row">
          <button class="secondary-button action-button" :disabled="submitting" @tap="rejectInvite">
            暂不接受
          </button>
          <button class="primary-button action-button" :disabled="submitting || invitation.status === 'already_friend'" @tap="acceptInvite">
            {{ acceptButtonText }}
          </button>
        </view>
      </section>

      <section v-else class="invite-hero-card loading-card">
        <view class="plate-ring skeleton"></view>
        <text class="invite-title">正在打开邀请</text>
        <text class="invite-desc">我们正在确认这张好友卡片。</text>
      </section>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { getRouteParams, replace } from '@/lib/navigation'
import { SocialService, type FriendInvitationResponse } from '@/services/social-service'

const params = getRouteParams() as { inviterId?: string }
const socialService = new SocialService()
const invitation = ref<FriendInvitationResponse | null>(null)
const submitting = ref(false)

const inviterInitial = computed(() => (invitation.value?.inviter.nickname || '?').trim().slice(0, 1).toUpperCase())
const acceptButtonText = computed(() => {
  if (submitting.value) return '处理中...'
  if (invitation.value?.status === 'already_friend') return '已经是好友'
  return '接受邀请'
})
const statusText = computed(() => {
  if (!invitation.value) return '邀请确认中'
  if (invitation.value.status === 'already_friend') return '你们已经是好友，可以直接查看彼此菜单。'
  if (invitation.value.status === 'accepted') return '邀请已接受，好友关系已建立。'
  if (invitation.value.status === 'rejected') return '你已拒绝这次邀请。'
  return '点接受后就会直接成为好友。'
})

onMounted(async () => {
  if (!(await requireAuth('friend-invite'))) return
  if (!params.inviterId) {
    Message.error('邀请卡片缺少邀请人信息')
    replace('profile')
    return
  }
  await loadInvitation()
})

async function loadInvitation() {
  try {
    const { data } = await socialService.getFriendInvitation(params.inviterId!)
    invitation.value = data
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '邀请不存在或已失效')
    replace('profile')
  }
}

async function acceptInvite() {
  if (!params.inviterId || invitation.value?.status === 'already_friend') {
    replace('profile')
    return
  }
  submitting.value = true
  try {
    await socialService.acceptFriendInvitation(params.inviterId)
    Message.success('已成为好友')
    replace('profile')
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '接受邀请失败')
  } finally {
    submitting.value = false
  }
}

async function rejectInvite() {
  if (!params.inviterId) {
    replace('profile')
    return
  }
  submitting.value = true
  try {
    await socialService.rejectFriendInvitation(params.inviterId)
    Message.success('已拒绝邀请')
    replace('profile')
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '拒绝邀请失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style>
.invite-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: linear-gradient(180deg, #fbf4ec 0%, #f7f6f3 48%, #edf4eb 100%);
}

.invite-bg {
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
}

.invite-bg-top {
  top: -88px;
  right: -78px;
  width: 210px;
  height: 210px;
  background: rgba(196, 112, 75, 0.18);
}

.invite-bg-bottom {
  bottom: -70px;
  left: -64px;
  width: 180px;
  height: 180px;
  background: rgba(122, 158, 126, 0.2);
}

.invite-content {
  position: relative;
  z-index: 1;
  width: min(390px, 100%);
  margin: 0 auto;
  padding: 74px 20px 30px;
}

.invite-badge {
  display: flex;
  width: fit-content;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--accent-dark);
  padding: 8px 14px;
  font-size: 12px;
  font-weight: 800;
}

.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: currentColor;
}

.invite-hero-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 20px;
  border-radius: 30px;
  background:
    radial-gradient(circle at 50% -10%, rgba(255, 248, 242, 0.95), transparent 44%),
    #fff;
  padding: 24px 18px 18px;
  box-shadow: 0 22px 54px rgba(27, 58, 45, 0.1);
}

.plate-ring {
  display: flex;
  width: 112px;
  height: 112px;
  align-items: center;
  justify-content: center;
  border: 10px solid #fff8f2;
  border-radius: 999px;
  background: #eef4eb;
  box-shadow: inset 0 0 0 1px #eadbcb, 0 18px 30px rgba(196, 112, 75, 0.16);
}

.inviter-avatar,
.avatar-fallback {
  width: 82px;
  height: 82px;
  border-radius: 999px;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #7a9e7e, #c4704b);
  color: #fff;
  font-size: 30px;
  font-weight: 900;
}

.invite-copy {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-top: 22px;
  text-align: center;
}

.invite-kicker {
  color: var(--accent-dark);
  font-size: 13px;
  font-weight: 800;
}

.invite-title {
  color: var(--text-main);
  font-size: 27px;
  line-height: 1.14;
  font-weight: 900;
}

.invite-desc {
  color: #6d776d;
  font-size: 14px;
  line-height: 1.7;
}

.status-strip {
  width: 100%;
  margin-top: 20px;
  border-radius: 18px;
  background: #f8f5f0;
  color: #6b625b;
  padding: 12px;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
}

.action-row {
  display: flex;
  width: 100%;
  gap: 10px;
  margin-top: 14px;
}

.action-button {
  flex: 1;
}

.loading-card {
  gap: 14px;
}

.skeleton {
  background: linear-gradient(90deg, #f1eee9, #fff8f2, #f1eee9);
}
</style>
