<template>
  <view class="login-page">
    <view class="login-content">
      <view class="auth-badge">
        <text class="badge-dot"></text>
        <text>meow食堂 · 欢迎回来</text>
      </view>

      <view class="login-card">
        <text class="auth-title">登录你的美味空间</text>

        <label class="field">
          <text>账号</text>
          <input v-model.trim="form.account" placeholder="输入账号" />
        </label>

        <label class="field">
          <text>密码</text>
          <input v-model.trim="form.password" password placeholder="输入密码" />
        </label>

        <view class="submit-block">
          <button class="primary-button submit" :disabled="loading" @tap="submit">
            {{ loading ? '登录中...' : '进入美味空间' }}
          </button>
          <text class="submit-hint">登录后可管理你的菜谱、收藏与好友动态。</text>
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
import { onMounted, reactive, ref } from 'vue'
import { Message } from '@/lib/feedback'
import { getRouteParams, navigateByLegacyPath, push, replace } from '@/lib/navigation'
import { useAuthStore } from '@/stores/auth-store'

const authStore = useAuthStore()
const loading = ref(false)
const params = getRouteParams() as { redirect?: string }

const form = reactive({
  account: '',
  password: '',
})

async function submit() {
  if (!form.account || !form.password) {
    Message.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await authStore.login(form.account, form.password)
    goAfterLogin()
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function autoWechatLogin() {
  loading.value = true
  try {
    await authStore.wechatLogin()
    goAfterLogin()
  } catch (error: any) {
    Message.error(error?.response?.data?.message || error?.message || '微信登录失败')
  } finally {
    loading.value = false
  }
}

function goAfterLogin() {
  if (params.redirect) {
    navigateByLegacyPath(decodeURIComponent(params.redirect), 'home')
    return
  }
  replace('home')
}

function goRegister() {
  push('register')
}

onMounted(() => {
  if (process.env.TARO_ENV === 'weapp') {
    autoWechatLogin()
  }
})
</script>

<style>
.login-page {
  min-height: 100vh;
  overflow: hidden;
  padding: 14px 0 24px;
  background:
    radial-gradient(circle at 85% 12%, rgba(231, 165, 106, 0.3), transparent 24%),
    radial-gradient(circle at 10% 86%, rgba(140, 178, 122, 0.2), transparent 28%),
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

.field {
  margin-bottom: 14px;
}

.field text {
  color: #6b625b;
  font-size: 12px;
  font-weight: 700;
}

.field input {
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
