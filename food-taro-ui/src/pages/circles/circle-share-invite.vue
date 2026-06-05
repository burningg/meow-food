<template>
  <view class="circle-invite-page">
    <view class="invite-bg invite-bg-top"></view>
    <view class="invite-bg invite-bg-bottom"></view>

    <view class="invite-content">
      <view class="invite-badge">
        <text class="badge-dot"></text>
        <text>meow食堂 · 搭子圈邀请</text>
      </view>

      <section v-if="invitation" class="invite-hero-card">
        <view class="circle-visual">
          <view class="plate-ring">
            <text class="circle-initial">{{ circleInitial }}</text>
          </view>
          <view class="inviter-chip">
            <image
              v-if="invitation.inviter.avatar"
              class="inviter-avatar"
              :src="invitation.inviter.avatar"
              mode="aspectFill"
            />
            <text v-else class="inviter-initial">{{ inviterInitial }}</text>
          </view>
        </view>

        <view class="invite-copy">
          <text class="invite-kicker"
            >{{ invitation.inviter.nickname || "好友" }}邀请你加入</text
          >
          <text class="invite-title">{{ invitation.circle.name }}</text>
        </view>

        <view class="status-strip">
          <text>{{ statusText }}</text>
        </view>

        <view class="action-row">
          <button
            class="secondary-button action-button"
            :disabled="submitting"
            @tap="skipInvite"
          >
            稍后再说
          </button>
          <button
            class="primary-button action-button"
            :disabled="submitting"
            @tap="acceptInvite"
          >
            {{ acceptButtonText }}
          </button>
        </view>
      </section>

      <section v-else class="invite-hero-card loading-card">
        <view class="plate-ring skeleton"></view>
        <text class="invite-title">正在打开搭子圈邀请</text>
        <text class="invite-desc">我们正在确认这张分享卡片。</text>
      </section>

      <text class="login-note">没有账号会先进入微信小程序登录/注册流程。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { requireAuth } from "@/lib/auth";
import { Message } from "@/lib/feedback";
import { getRouteParams, replace } from "@/lib/navigation";
import {
  SocialService,
  type BuddyCircleShareInvitation,
} from "@/services/social-service";

const params = getRouteParams() as { circleId?: string; inviterId?: string };
const socialService = new SocialService();
const invitation = ref<BuddyCircleShareInvitation | null>(null);
const submitting = ref(false);

const inviterInitial = computed(() =>
  (invitation.value?.inviter.nickname || "?").trim().slice(0, 1).toUpperCase(),
);
const circleInitial = computed(() =>
  (invitation.value?.circle.name || "圈").trim().slice(0, 1).toUpperCase(),
);

const acceptButtonText = computed(() => {
  if (submitting.value) return "处理中...";
  if (invitation.value?.member) return "进入搭子圈";
  return "接受并加入";
});
const statusText = computed(() => {
  if (!invitation.value) return "邀请确认中";
  if (invitation.value.member) return "你已经在这个搭子圈里，可以直接进入。";

  const circleName = invitation.value?.circle.name || "这个搭子圈";
  return `点击接受后，会直接加入「${circleName}」，一起共享菜单和约饭灵感。`;
});

onMounted(async () => {
  if (!(await requireAuth("circle-share-invite"))) return;
  if (!params.circleId || !params.inviterId) {
    Message.error("邀请卡片缺少搭子圈信息");
    replace("circles");
    return;
  }
  await loadInvitation();
});

async function loadInvitation() {
  try {
    const { data } = await socialService.getCircleShareInvitation(
      params.circleId!,
      params.inviterId!,
    );
    invitation.value = data;
  } catch (error: any) {
    Message.error(error?.response?.data?.message || "邀请不存在或已失效");
    replace("circles");
  }
}

async function acceptInvite() {
  if (!params.circleId || !params.inviterId) {
    replace("circles");
    return;
  }
  if (invitation.value?.member) {
    replace({ name: "circles", params: { id: params.circleId } });
    return;
  }
  submitting.value = true;
  try {
    const { data } = await socialService.acceptCircleShareInvitation(
      params.circleId,
      params.inviterId,
    );
    Message.success("已加入搭子圈");
    replace({ name: "circles", params: { id: data.circle.id } });
  } catch (error: any) {
    Message.error(error?.response?.data?.message || "加入搭子圈失败");
  } finally {
    submitting.value = false;
  }
}

function skipInvite() {
  replace("circles");
}
</script>

<style>
.circle-invite-page {
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
    radial-gradient(
      circle at 50% -10%,
      rgba(255, 248, 242, 0.95),
      transparent 44%
    ),
    #fff;
  padding: 24px 18px 18px;
  box-shadow: 0 22px 54px rgba(27, 58, 45, 0.1);
}

.circle-visual {
  position: relative;
  width: 128px;
  height: 112px;
}

.plate-ring {
  display: flex;
  width: 112px;
  height: 112px;
  align-items: center;
  justify-content: center;
  border: 10px solid #edf3ec;
  border-radius: 999px;
  background: linear-gradient(135deg, #1b3a2d, #7a9e7e);
  box-shadow:
    inset 0 0 0 1px #dce8d9,
    0 18px 30px rgba(27, 58, 45, 0.16);
}

.circle-initial {
  color: #fff;
  font-size: 34px;
  font-weight: 900;
}

.inviter-chip {
  position: absolute;
  right: 0;
  bottom: 6px;
  display: flex;
  width: 48px;
  height: 48px;
  align-items: center;
  justify-content: center;
  border: 4px solid #fff;
  border-radius: 999px;
  background: #fff8f2;
  color: #9f5c38;
  font-size: 18px;
  font-weight: 900;
}

.inviter-avatar,
.inviter-initial {
  width: 40px;
  height: 40px;
  border-radius: 999px;
}

.inviter-initial {
  display: flex;
  align-items: center;
  justify-content: center;
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
  font-family: "Noto Serif SC";
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
  padding: 12px;
  color: #6b625b;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.action-row {
  display: flex;
  width: 100%;
  gap: 10px;
  margin-top: 18px;
}

.action-button {
  flex: 1;
  height: 48px;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 800;
}

.secondary-button {
  background: #f1eee9;
  color: #151515;
}

.primary-button {
  background: #1b3a2d;
  color: #fff;
  box-shadow: 0 14px 26px rgba(27, 58, 45, 0.2);
}

.loading-card {
  min-height: 360px;
  justify-content: center;
}

.skeleton {
  opacity: 0.62;
}

.login-note {
  display: block;
  margin-top: 18px;
  color: #787774;
  font-size: 12px;
  text-align: center;
}
</style>
