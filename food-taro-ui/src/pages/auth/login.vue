<template>
  <view class="login-page">
    <view class="login-content">
      <view class="auth-badge">
        <text class="badge-dot"></text>
        <text>meow食堂 · 欢迎回来</text>
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
              placeholder="点击填写昵称"
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
        </view>
      </view>

      <view class="footer-prompt">
        <text>还没有账号？</text>
        <button class="footer-link" @tap="goRegister">去注册</button>
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
const params = getRouteParams() as { redirect?: string };
const isWeapp = process.env.TARO_ENV === "weapp";
const form = reactive({
  nickname: "",
  avatar: "",
});
const submitHint = computed(() =>
  profileRequired.value
    ? "首次微信登录请先选择头像并填写昵称，后续登录无需重复填写。"
    : "登录后可管理你的菜谱、收藏与好友动态。",
);
const submitText = computed(() => {
  if (loading.value) return "登录中...";
  return profileRequired.value ? "完成微信登录" : "微信一键登录";
});

async function submitWechatLogin() {
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
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.avatar-picker {
  display: flex;
  width: 92px;
  height: 92px;
  align-items: center;
  justify-content: center;
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
