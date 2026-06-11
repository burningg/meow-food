<template>
  <view class="login-page">
    <view class="login-content">
      <view class="auth-badge">
        <text class="badge-dot"></text>
        <text>meow食堂</text>
      </view>

      <view class="login-card">
        <text class="auth-title">登录你的美味空间</text>

        <view v-if="profileRequired" class="wechat-profile-card">
          <button
            class="avatar-picker"
            open-type="chooseAvatar"
            :disabled="loading || avatarUploading"
            @chooseavatar="handleChooseAvatar"
          >
            <image
              v-if="form.avatar"
              class="avatar-image"
              :src="form.avatar"
              mode="aspectFill"
            />
            <view v-else class="avatar-placeholder">
              <text class="avatar-plus">{{ avatarUploading ? "..." : "+" }}</text>
              <text>{{ avatarUploading ? "上传中" : "选择头像" }}</text>
            </view>
          </button>

          <label class="field">
            <text>微信昵称</text>
            <input
              v-model.trim="form.nickname"
              class="field-input"
              :type="isWeapp ? 'nickname' : 'text'"
              maxlength="20"
              placeholder="写个你常用的昵称"
            />
          </label>
        </view>

        <view class="submit-block">
          <button
            class="primary-button submit"
            :disabled="loading || avatarUploading"
            @tap="submitWechatLogin"
          >
            {{ submitText }}
          </button>
          <text class="submit-hint">{{ submitHint }}</text>
          <view v-if="profileRequired" class="agreement-row" @tap="toggleAgreement">
            <view :class="['agreement-checkbox', { checked: agreementChecked }]">
              <text v-if="agreementChecked">✓</text>
            </view>
            <text class="agreement-copy">我已阅读并同意</text>
            <button class="agreement-link" @tap.stop="openPrivacyAgreement">
              《用户隐私协议》
            </button>
          </view>
        </view>
      </view>

      <!-- <view class="footer-prompt">
        <text>还没有账号？</text>
        <button class="footer-link" @tap="goRegister">去注册</button>
      </view> -->
    </view>

    <view
      v-if="privacyAgreementVisible"
      class="privacy-overlay"
      @tap="closePrivacyAgreement"
    >
      <view class="privacy-card" @tap.stop>
        <view class="privacy-head">
          <view>
            <text class="privacy-title">用户隐私协议</text>
            <text class="privacy-subtitle">meow食堂</text>
          </view>
          <button class="privacy-close" @tap="closePrivacyAgreement">×</button>
        </view>

        <scroll-view :scroll-y="true" class="privacy-body">
          <view
            v-for="section in privacySections"
            :key="section.title"
            class="privacy-section"
          >
            <text class="privacy-section-title">{{ section.title }}</text>
            <text
              v-for="item in section.items"
              :key="item"
              class="privacy-paragraph"
            >
              {{ item }}
            </text>
          </view>
        </scroll-view>

        <button class="privacy-confirm primary-button" @tap="acceptPrivacyAgreement">
          已阅读并同意
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { Message } from "@/lib/feedback";
import {
  getRouteParams,
  navigateByLegacyPath,
  push,
  replace,
} from "@/lib/navigation";
import { FoodService } from "@/services/food-service";
import { isWechatNicknameReady, useAuthStore } from "@/stores/auth-store";

const authStore = useAuthStore();
const foodService = new FoodService();
const loading = ref(false);
const avatarUploading = ref(false);
const profileRequired = ref(false);
const agreementChecked = ref(false);
const privacyAgreementVisible = ref(false);
const params = getRouteParams() as { redirect?: string };
const isWeapp = process.env.TARO_ENV === "weapp";
const form = reactive({
  nickname: "",
  avatar: "",
});
const privacySections = [
  {
    title: "一、我们如何收集和使用信息",
    items: [
      "为了完成微信登录、创建账号并提供菜谱管理、菜单共享、好友互动等功能，我们会根据你的操作收集微信头像、昵称、账号标识、菜谱内容、菜单计划、好友关系及你主动上传的图片。",
      "当你使用图片上传、菜谱编辑、菜单共享或好友邀请功能时，我们会处理你主动提交的文字、图片和关联信息，仅用于展示、保存、同步和完成对应功能。",
    ],
  },
  {
    title: "二、设备与运行信息",
    items: [
      "为保障登录状态、基础安全和服务稳定，我们可能会记录必要的设备类型、系统版本、网络状态、登录时间、操作日志和异常日志。",
      "我们不会为了与本应用无关的目的读取你的通讯录、相册、定位或麦克风。涉及微信授权或图片选择时，以微信小程序弹出的授权范围为准。",
    ],
  },
  {
    title: "三、信息存储与保护",
    items: [
      "我们会采取合理的技术和管理措施保护你的个人信息，尽量避免未经授权的访问、泄露、篡改或丢失。",
      "我们只在实现本协议所述功能所必需的期限内保存相关信息。法律法规另有要求，或为处理争议、安全审计所必需的除外。",
    ],
  },
  {
    title: "四、信息共享",
    items: [
      "你主动将菜单、菜谱或圈子内容设置为共享时，相关内容会向你选择的好友、圈子成员或访问对象展示。",
      "除取得你的明确同意、履行法律义务、处理安全风险或完成你主动发起的共享外，我们不会向无关第三方出售或出租你的个人信息。",
    ],
  },
  {
    title: "五、你的权利",
    items: [
      "你可以在应用内查看、修改头像、昵称、菜谱、菜单计划和部分账号资料，也可以删除你主动创建的内容。",
      "如需注销账号、删除更多个人信息或撤回授权，可通过应用内反馈渠道联系开发者。我们会在核验身份后按法律法规要求处理。",
    ],
  },
  {
    title: "六、未成年人保护",
    items: [
      "未成年人使用本应用前，应当取得监护人同意。监护人发现未成年人个人信息被不当收集或使用的，可以联系开发者处理。",
    ],
  },
  {
    title: "七、协议更新",
    items: [
      "我们可能根据产品功能、法律法规或安全要求更新本协议。发生重要变更时，会在登录、使用或其他合理位置提示你再次阅读并确认。",
      "继续使用 meow食堂，即表示你理解并同意更新后的用户隐私协议。",
    ],
  },
];
const submitHint = computed(() =>
  profileRequired.value
    ? "首次登录必须勾选同意《用户隐私协议》，并补充头像和昵称。"
    : "登录后就能整理菜谱、共享菜单和查看好友近况。",
);
const submitText = computed(() => {
  if (loading.value) return "登录中...";
  return profileRequired.value ? "完成微信登录" : "微信一键登录";
});

async function submitWechatLogin() {
  // 登录前必须确认隐私协议，避免首次补资料流程绕过用户授权。
  if (profileRequired.value && !agreementChecked.value) {
    Message.warning("请先勾选同意《用户隐私协议》");
    return;
  }

  if (!isWeapp) {
    Message.warning("请在微信小程序中完成登录");
    return;
  }

  if (profileRequired.value && !form.avatar) {
    Message.warning("请先选择微信头像");
    return;
  }

  if (profileRequired.value && !isWechatNicknameReady(form.nickname)) {
    Message.warning("请先填写可用的微信昵称");
    return;
  }

  try {
    loading.value = true;
    await authStore.wechatLogin({
      nickname: profileRequired.value ? form.nickname.trim() : undefined,
      avatar: profileRequired.value ? form.avatar : undefined,
    });
    goAfterLogin();
  } catch (error: any) {
    if (error?.response?.data?.code === "WECHAT_PROFILE_REQUIRED") {
      profileRequired.value = true;
      Message.warning("首次微信登录请先选择头像并填写昵称");
      return;
    }

    const errMsg =
      error?.errMsg || error?.response?.data?.message || error?.message || "";
    Message.error(errMsg || "微信登录失败");
  } finally {
    loading.value = false;
  }
}

async function handleChooseAvatar(event: any) {
  const filePath = event?.detail?.avatarUrl;
  if (!filePath || avatarUploading.value) {
    return;
  }

  avatarUploading.value = true;
  try {
    const { data } = await foodService.uploadImage(filePath);
    form.avatar = data;
    Message.success("头像已上传");
  } catch (error: any) {
    Message.error(error?.response?.data?.message || error?.message || "头像上传失败");
  } finally {
    avatarUploading.value = false;
  }
}

function toggleAgreement() {
  agreementChecked.value = !agreementChecked.value;
}

function openPrivacyAgreement() {
  privacyAgreementVisible.value = true;
}

function closePrivacyAgreement() {
  privacyAgreementVisible.value = false;
}

function acceptPrivacyAgreement() {
  agreementChecked.value = true;
  closePrivacyAgreement();
}

function goAfterLogin() {
  if (params.redirect) {
    navigateByLegacyPath(decodeURIComponent(params.redirect), "home");
    return;
  }
  replace("home");
}

function goRegister() {
  push({
    name: "register",
    query: params.redirect ? { redirect: params.redirect } : undefined,
  });
}
</script>

<style>
.login-page {
  min-height: 100vh;
  overflow: hidden;
  padding: 14px 0 24px;
  background:
    radial-gradient(
      circle at 85% 12%,
      rgba(231, 165, 106, 0.3),
      transparent 24%
    ),
    radial-gradient(
      circle at 10% 86%,
      rgba(140, 178, 122, 0.2),
      transparent 28%
    ),
    linear-gradient(180deg, #fbf4ec 0%, #f7f6f3 44%, #eef4eb 100%);
}

.login-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
  width: min(390px, 100%);
  margin: 0 auto;
  padding: 72px 20px 28px;
}

.auth-badge {
  display: flex;
  align-items: center;
  align-self: flex-start;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.65);
  color: #9f5c38;
  font-size: 12px;
  font-weight: 700;
}

.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: currentColor;
}

.login-card {
  display: flex;
  flex-direction: column;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.91);
  padding: 22px 18px;
  box-shadow: 0 18px 42px rgba(27, 58, 45, 0.07);
}

.auth-title {
  margin-bottom: 18px;
  color: #151515;
  font-size: 23px;
  line-height: 1.2;
  font-weight: 700;
}

.wechat-profile-card {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 14px;
  margin-bottom: 18px;
}

.avatar-picker {
  display: flex;
  width: 92px;
  height: 92px;
  align-items: center;
  justify-content: center;
  align-self: center;
  padding: 0;
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid #eadbcb;
  border-radius: 24px;
  background: #fff8f2;
}

.avatar-image {
  width: 100%;
  height: 100%;
}

.avatar-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #8c7662;
  font-size: 12px;
  font-weight: 700;
}

.avatar-plus {
  font-size: 28px;
  line-height: 1;
}

.field {
  display: flex;
  flex: 1;
  flex-direction: column;
}

.field text {
  color: #6b625b;
  font-size: 12px;
  font-weight: 700;
}

.field-input {
  min-height: 54px;
  margin-top: 8px;
  border: 1px solid #e8dccf;
  border-radius: 16px;
  background: #fff8f2;
  padding: 0 16px;
  font-size: 14px;
}

.submit-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 4px;
}

.submit {
  width: 100%;
  min-height: 54px;
  border-radius: 18px;
  font-size: 15px;
}

.submit-hint {
  color: #787774;
  font-size: 11px;
  line-height: 1.5;
}

.agreement-row {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #6b625b;
  font-size: 12px;
  line-height: 1.4;
}

.agreement-checkbox {
  display: flex;
  width: 16px;
  height: 16px;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px solid #d9c6b1;
  border-radius: 5px;
  background: #fff8f2;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
}

.agreement-checkbox.checked {
  border-color: #9f5c38;
  background: #9f5c38;
}

.agreement-copy {
  flex-shrink: 0;
}

.agreement-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: auto;
  margin: 0;
  padding: 0;
  background: transparent;
  color: #9f5c38;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.4;
  text-align: center;
}

.agreement-link::after,
.privacy-close::after,
.privacy-confirm::after {
  border: none;
}

.privacy-overlay {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 18px;
  background: rgba(21, 21, 21, 0.42);
}

.privacy-card {
  display: flex;
  width: min(390px, 100%);
  max-height: 78vh;
  flex-direction: column;
  border-radius: 24px;
  background: #fffdf9;
  box-shadow: 0 20px 48px rgba(27, 58, 45, 0.18);
}

.privacy-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 18px 12px;
  border-bottom: 1px solid #f0e4d8;
}

.privacy-title,
.privacy-subtitle,
.privacy-section-title,
.privacy-paragraph {
  display: block;
}

.privacy-title {
  color: #151515;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.25;
}

.privacy-subtitle {
  margin-top: 4px;
  color: #9f5c38;
  font-size: 12px;
  font-weight: 700;
}

.privacy-close {
  display: flex;
  width: 34px;
  height: 34px;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin: 0;
  padding: 0;
  border-radius: 999px;
  background: #f6ede4;
  color: #8c7662;
  font-size: 22px;
  line-height: 1;
  text-align: center;
}

.privacy-body {
  height: 46vh;
  padding: 14px 18px;
  box-sizing: border-box;
}

.privacy-section {
  margin-bottom: 16px;
}

.privacy-section-title {
  margin-bottom: 8px;
  color: #2f2a25;
  font-size: 14px;
  font-weight: 800;
  line-height: 1.45;
}

.privacy-paragraph {
  margin-bottom: 8px;
  color: #61564f;
  font-size: 12px;
  line-height: 1.7;
}

.privacy-confirm {
  width: calc(100% - 36px);
  min-height: 48px;
  margin: 0 18px 18px;
  border-radius: 16px;
  font-size: 14px;
  text-align: center;
}

.footer-prompt {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #787774;
  font-size: 13px;
}

.footer-link {
  color: #9f5c38;
  font-weight: 800;
}
</style>
