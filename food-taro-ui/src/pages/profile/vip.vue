<template>
  <view class="page-shell vip-page">
    <header class="vip-top-nav">
      <view class="vip-title-block">
        <text class="vip-eyebrow">{{ isVipActive ? 'VIP ACTIVE' : 'MEOW VIP' }}</text>
        <text class="vip-page-title">{{ isVipActive ? '我的会员' : '会员权益' }}</text>
      </view>
      <view class="vip-nav-spacer"></view>
    </header>

    <main class="vip-content">
      <section class="vip-poster">
        <view class="poster-glow poster-glow-top"></view>
        <view class="poster-glow poster-glow-sage"></view>
        <view class="poster-spark poster-spark-large"></view>
        <view class="poster-spark poster-spark-small"></view>
        <view class="poster-paper poster-paper-shadow"></view>
        <view class="poster-paper poster-paper-core"></view>

        <view class="vip-crown-badge">
          <text class="vip-crown-mark">♕</text>
        </view>

        <view class="poster-copy">
          <text class="poster-eyebrow">{{ isVipActive ? 'BENEFITS UNLOCKED' : 'FIRST MONTH FREE' }}</text>
          <text class="poster-title">{{ isVipActive ? '权益已点亮\n欢迎回到食堂' : '把食堂开成\n你的专属社区' }}</text>
          <text class="poster-desc">
            {{ isVipActive ? '你的圈子、菜谱容量、AI 整理和专属标识已经生效，继续把喜欢的饭菜留下来。' : '更大的圈子、更多菜谱、AI 协助整理步骤食材，还有属于你的会员标识。' }}
          </text>
        </view>

        <view class="avatar-frame-preview">
          <view class="framed-avatar">
            <text>喵</text>
          </view>
          <view class="avatar-preview-copy">
            <text class="avatar-preview-title">专属头像框</text>
            <text class="avatar-preview-desc">尊贵标识同步展示</text>
          </view>
        </view>
      </section>

      <section class="benefits-section">
        <view class="section-heading">
          <view>
            <text class="vip-eyebrow">权益一览</text>
          </view>
        </view>

        <view class="benefit-list">
          <view v-for="benefit in benefits" :key="benefit.title" class="benefit-row">
            <view :class="['benefit-icon', { gold: benefit.gold }]">
              <text>{{ benefit.icon }}</text>
            </view>
            <view class="benefit-copy">
              <text class="benefit-title">{{ benefit.title }}</text>
              <text class="benefit-desc">{{ benefit.desc }}</text>
            </view>
            <view v-if="benefit.before && benefit.after" class="benefit-limit">
              <text class="old-limit">{{ benefit.before }}</text>
              <text class="limit-arrow">→</text>
              <text class="new-limit">{{ benefit.after }}</text>
            </view>
          </view>
        </view>
      </section>

      <section class="price-ticket">
        <view class="ticket-top">
          <view class="ticket-copy">
            <text class="vip-eyebrow">{{ isVipActive ? '会员状态' : '限时礼遇' }}</text>
            <text class="ticket-title">{{ isVipActive ? '已开通 VIP' : '首月免费领取' }}</text>
          </view>
          <view class="ticket-price">
            <text class="ticket-price-main">{{ isVipActive ? '生效中' : '¥0' }}</text>
            <text class="ticket-price-sub">{{ isVipActive ? expiresText : '原价 9.9' }}</text>
          </view>
        </view>
        <view class="ticket-divider"></view>
        <text class="ticket-desc">
          {{ isVipActive ? '权益已生效，到期后自动取消，不会自动续费。感谢你支持 meow 食堂继续运营。' : '无需自动续费订阅，到期自动取消。首月免费后，第一年 2.9 元。' }}
        </text>
      </section>

      <section class="thanks-note">
        <text class="vip-eyebrow">写给会员的话</text>
        <text class="thanks-body">感谢您的充值，可以为meow食堂的运营提供助力，我们将倾听您的意见，尽力打造我们共同的社区</text>
      </section>

      <section v-if="isVipActive" class="active-status-card">
        <view class="active-status-panel">
          <view class="active-status-icon">♕</view>
          <view class="active-status-copy">
            <text class="active-status-title">VIP 权益生效中</text>
            <text class="active-status-desc">首月权益结束后自动取消，无需手动管理。</text>
          </view>
          <view class="active-status-pill">
            <text>已开通</text>
          </view>
        </view>
      </section>

      <section v-else class="vip-action-card">
        <button class="claim-button" :disabled="loading || claiming" @tap="claimVip">
          <text>{{ claiming ? '领取中...' : '首月免费领取' }}</text>
          <text>→</text>
        </button>
        <text class="claim-note">领取后不会自动续费，会员到期自动取消。</text>
      </section>
    </main>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useDidShow } from '@tarojs/taro'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { goBack } from '@/lib/navigation'
import { SocialService, type ProfileResponse } from '@/services/social-service'
import { useAuthStore } from '@/stores/auth-store'

const socialService = new SocialService()
const authStore = useAuthStore()
const profile = ref<ProfileResponse | null>(null)
const loading = ref(false)
const claiming = ref(false)

const benefits = [
  { icon: '圈', title: '圈子上限', desc: '和更多朋友共享菜单', before: '3', after: '10' },
  { icon: '谱', title: '菜谱上限', desc: '安心收藏每一道拿手菜', before: '50', after: '500' },
  { icon: 'AI', title: 'AI生成步骤食材', desc: '上传灵感后自动整理结构', gold: true },
  { icon: '♕', title: '尊贵标识', desc: '在个人页与圈子中亮起' },
  { icon: '框', title: '专属头像框', desc: '让你的头像更有辨识度' },
]

const vipInfo = computed(() => profile.value?.vipInfo)
const isVipActive = computed(() => Boolean(vipInfo.value?.vip))
const expiresText = computed(() => {
  if (!vipInfo.value?.expiresAt) return '到期自动取消'
  return `${formatDate(vipInfo.value.expiresAt)} 到期`
})

onMounted(async () => {
  if (!(await requireAuth('vip'))) return
  await loadVipPage()
})

useDidShow(async () => {
  if (!(await requireAuth('vip'))) return
  await loadVipPage()
})

async function loadVipPage() {
  loading.value = true
  try {
    const { data } = await socialService.getProfile()
    profile.value = data
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '会员信息加载失败')
  } finally {
    loading.value = false
  }
}

async function claimVip() {
  if (claiming.value || isVipActive.value) return
  claiming.value = true
  try {
    const { data } = await socialService.claimVipFreeTrial()
    if (profile.value) {
      profile.value = {
        ...profile.value,
        vipInfo: data,
      }
    }
    await authStore.restore()
    Message.success('VIP 已开通')
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '领取失败，请稍后重试')
  } finally {
    claiming.value = false
  }
}

function formatDate(value: string) {
  return value.slice(0, 10).replace(/-/g, '.')
}
</script>

<style>
.vip-page {
  min-height: 100vh;
  padding-bottom: calc(28px + env(safe-area-inset-bottom));
  background:
    radial-gradient(circle at 88% 4%, rgba(245, 179, 106, 0.3), transparent 28%),
    radial-gradient(circle at 5% 72%, rgba(141, 176, 125, 0.26), transparent 34%),
    linear-gradient(180deg, #fff7ea 0%, #f7f6f3 43%, #eef5ea 100%);
}

.vip-top-nav,
.vip-content,
.poster-copy,
.benefits-section,
.benefit-list,
.ticket-copy,
.thanks-note,
.vip-action-card,
.active-status-card,
.active-status-copy {
  display: flex;
  flex-direction: column;
}

.vip-top-nav {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.vip-back-button,
.vip-nav-spacer {
  width: 38px;
  height: 38px;
}

.vip-back-button {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  color: #5a4333;
  font-size: 27px;
  line-height: 1;
  box-shadow: 0 8px 22px rgba(200, 135, 59, 0.13);
}

.vip-title-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.vip-eyebrow {
  display: block;
  color: #8e5c19;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1px;
}

.vip-page-title {
  color: #151515;
  font-family: 'Noto Serif SC', serif;
  font-size: 20px;
  font-weight: 500;
}

.vip-content {
  gap: 18px;
}

.vip-poster {
  position: relative;
  height: 428px;
  overflow: hidden;
  border: 1px solid #e7c98a;
  border-radius: 26px;
  background: linear-gradient(180deg, #402a18 0%, #8a4e29 56%, #f0c987 100%);
  box-shadow: 0 22px 54px rgba(138, 78, 41, 0.2);
}

.poster-glow,
.poster-spark,
.poster-paper,
.vip-crown-badge,
.poster-copy,
.avatar-frame-preview {
  position: absolute;
}

.poster-glow {
  border-radius: 999px;
}

.poster-glow-top {
  top: -58px;
  right: -90px;
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(255, 229, 168, 0.42), rgba(255, 229, 168, 0));
}

.poster-glow-sage {
  left: -88px;
  bottom: -56px;
  width: 220px;
  height: 220px;
  background: radial-gradient(circle, rgba(141, 176, 125, 0.28), rgba(141, 176, 125, 0));
}

.poster-spark {
  border-radius: 999px;
  background: rgba(246, 215, 154, 0.58);
}

.poster-spark-large {
  top: 64px;
  right: 26px;
  width: 18px;
  height: 18px;
}

.poster-spark-small {
  left: 28px;
  bottom: 166px;
  width: 10px;
  height: 10px;
}

.poster-paper {
  width: 302px;
  height: 300px;
  border-radius: 22px;
}

.poster-paper-shadow {
  left: 18px;
  top: 76px;
  width: 314px;
  transform: rotate(2.4deg);
  border: 1px solid rgba(255, 244, 219, 0.33);
  background: rgba(255, 244, 219, 0.2);
}

.poster-paper-core {
  left: 24px;
  top: 72px;
  transform: rotate(-1.6deg);
  border: 1px solid #e7c98a;
  background: #fff8ef;
  box-shadow: 0 16px 30px rgba(42, 22, 10, 0.15);
}

.vip-crown-badge {
  left: 126px;
  top: 28px;
  display: flex;
  width: 98px;
  height: 98px;
  align-items: center;
  justify-content: center;
  border: 2px solid #ffe8ad;
  border-radius: 36px;
  background: linear-gradient(180deg, #fff4db 0%, #e9b75f 100%);
  box-shadow: 0 14px 30px rgba(208, 139, 46, 0.27);
}

.vip-crown-mark {
  color: #7a401d;
  font-size: 46px;
  line-height: 1;
}

.poster-copy {
  left: 42px;
  top: 134px;
  width: 266px;
  align-items: center;
  gap: 10px;
  text-align: center;
}

.poster-eyebrow {
  color: #a46a1f;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1px;
}

.poster-title {
  color: #25160d;
  font-family: 'Noto Serif SC', serif;
  font-size: 30px;
  font-weight: 800;
  line-height: 1.12;
  white-space: pre-line;
}

.poster-desc {
  color: #5b4a39;
  font-size: 13px;
  line-height: 1.55;
}

.avatar-frame-preview {
  left: 86px;
  top: 288px;
  display: flex;
  width: 178px;
  height: 74px;
  align-items: center;
  justify-content: center;
  gap: 12px;
  border: 1px solid #e7c98a;
  border-radius: 24px;
  background: #fff4db;
}

.framed-avatar {
  display: flex;
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border: 3px solid #b97825;
  border-radius: 18px;
  background: #edf3ec;
  color: #346538;
  font-family: 'Noto Serif SC', serif;
  font-weight: 800;
}

.avatar-preview-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.avatar-preview-title {
  color: #8e5c19;
  font-size: 12px;
  font-weight: 800;
}

.avatar-preview-desc {
  color: #7a6854;
  font-size: 10px;
}

.benefits-section {
  gap: 12px;
}

.section-heading,
.benefit-row,
.ticket-top,
.active-status-panel {
  display: flex;
  flex-direction: row;
}

.section-heading,
.ticket-top {
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}

.section-title {
  display: block;
  margin-top: 3px;
  color: #151515;
  font-family: 'Noto Serif SC', serif;
  font-size: 21px;
  font-weight: 500;
}

.vip-mini-chip {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 5px;
  border: 1px solid #e7c98a;
  border-radius: 999px;
  background: #fff4db;
  color: #8e5c19;
  padding: 6px 10px;
  font-size: 11px;
  font-weight: 900;
}

.benefit-list {
  gap: 8px;
}

.benefit-row {
  align-items: center;
  gap: 12px;
  border: 1px solid #efe3d1;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
  padding: 12px 14px;
  box-shadow: 0 8px 20px rgba(138, 78, 41, 0.06);
}

.benefit-icon {
  display: flex;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: #edf3ec;
  color: #346538;
  font-size: 12px;
  font-weight: 900;
}

.benefit-icon.gold {
  background: #fff4db;
  color: #a46a1f;
}

.benefit-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.benefit-title {
  color: #151515;
  font-size: 14px;
  font-weight: 800;
}

.benefit-desc {
  color: #787774;
  font-size: 11px;
  line-height: 1.35;
}

.benefit-limit {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 4px;
}

.old-limit {
  color: #b8a897;
  font-size: 13px;
  font-weight: 800;
  text-decoration: line-through;
}

.limit-arrow {
  color: #a46a1f;
  font-size: 13px;
  font-weight: 800;
}

.new-limit {
  color: #a46a1f;
  font-size: 18px;
  font-weight: 900;
}

.price-ticket,
.thanks-note,
.vip-action-card,
.active-status-card {
  border-radius: 22px;
  box-shadow: 0 14px 34px rgba(164, 106, 31, 0.12);
}

.price-ticket {
  border: 1px solid #e7c98a;
  background: #fff8ef;
  padding: 18px;
}

.ticket-copy {
  gap: 4px;
}

.ticket-title {
  color: #25160d;
  font-family: 'Noto Serif SC', serif;
  font-size: 24px;
  font-weight: 800;
}

.ticket-price {
  display: flex;
  flex-shrink: 0;
  flex-direction: column;
  align-items: flex-end;
}

.ticket-price-main {
  color: #9f5c38;
  font-family: 'Noto Serif SC', serif;
  font-size: 36px;
  font-weight: 800;
  line-height: 1.05;
}

.ticket-price-sub {
  color: #a99785;
  font-size: 11px;
  text-decoration: line-through;
}

.ticket-divider {
  height: 1px;
  margin: 12px 0;
  background: #ead9c4;
}

.ticket-desc {
  display: block;
  color: #5b4a39;
  font-size: 13px;
  line-height: 1.55;
}

.thanks-note {
  gap: 8px;
  border: 1px solid #dce7da;
  background: #edf3ec;
  padding: 16px;
}

.thanks-body {
  color: #2f3437;
  font-size: 13px;
  line-height: 1.65;
}

.vip-action-card,
.active-status-card {
  gap: 9px;
  border: 1px solid #efe3d1;
  background: rgba(255, 255, 255, 0.86);
  padding: 14px;
}

.claim-button {
  display: flex;
  min-height: 52px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, #9f5c38 0%, #c8873b 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 900;
  box-shadow: 0 10px 24px rgba(164, 106, 31, 0.2);
}

.claim-button[disabled] {
  opacity: 0.72;
}

.claim-note,
.active-support-note {
  display: block;
  color: #8b857d;
  font-size: 11px;
  line-height: 1.45;
  text-align: center;
}

.active-status-panel {
  min-height: 66px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-radius: 18px;
  background: #edf3ec;
  padding: 12px 14px;
}

.active-status-icon {
  display: flex;
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: #fff4db;
  color: #a46a1f;
  font-size: 24px;
}

.active-status-copy {
  min-width: 0;
  flex: 1;
  gap: 3px;
}

.active-status-title {
  color: #151515;
  font-size: 14px;
  font-weight: 900;
}

.active-status-desc {
  color: #5b4a39;
  font-size: 11px;
  line-height: 1.35;
}

.active-status-pill {
  flex-shrink: 0;
  border-radius: 999px;
  background: #346538;
  color: #fff;
  padding: 6px 10px;
  font-size: 11px;
  font-weight: 900;
}

.active-support-note {
  color: #346538;
}
</style>
