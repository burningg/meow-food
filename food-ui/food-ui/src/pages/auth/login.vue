<template>
  <div ref="pageRef" class="login-page">
    

    <section class="login-content">
      <div class="auth-badge" data-motion="item">
        <span class="badge-dot"></span>
        <span>meow食堂 · 欢迎回来</span>
      </div>

      <section class="login-card" data-motion="card">
        <h1 data-motion="item">登录你的美味空间</h1>

        <label class="field" data-motion="item">
          <span>账号</span>
          <input v-model.trim="form.account" placeholder="输入账号" />
        </label>

        <label class="field" data-motion="item">
          <span>密码</span>
          <input v-model.trim="form.password" type="password" placeholder="输入密码" />
        </label>

        <div class="submit-block" data-motion="item">
          <button class="primary-button submit" type="button" :disabled="loading" @click="submit">
            {{ loading ? '登录中...' : '进入美味空间' }}
          </button>
          <p class="submit-hint">登录后可管理你的菜谱、收藏与好友动态。</p>
        </div>
      </section>

      <div class="footer-prompt" data-motion="item">
        <span>还没有账号？</span>
        <button type="button" class="footer-link" @click="goRegister">去注册</button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { animateStagger, attachPressAnimations, runScopedMotion } from '@/lib/motion'
import { useAuthStore } from '@/stores/auth-store'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const pageRef = ref<HTMLElement | null>(null)
let cleanupMotion: VoidFunction | undefined

const loading = ref(false)
const form = reactive({
  account: '',
  password: '',
})

onMounted(() => {
  if (!pageRef.value) return
  cleanupMotion = runScopedMotion(pageRef.value, ({ reducedMotion = false }) => {
    animateStagger(pageRef.value!.querySelectorAll('[data-motion="card"]'), {
      reducedMotion,
      y: 22,
      duration: 0.36,
    })
    animateStagger(pageRef.value!.querySelectorAll('[data-motion="item"]'), {
      reducedMotion,
      y: 16,
      stagger: 0.06,
      delay: 0.08,
      duration: 0.28,
    })

    return attachPressAnimations(pageRef.value!, '.submit', { activeScale: 0.97 })
  })
})

onUnmounted(() => {
  cleanupMotion?.()
})

async function submit() {
  if (!form.account || !form.password) {
    Message.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await authStore.login(form.account, form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
    router.replace(redirect)
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

function goRegister() {
  router.push({ name: 'register' })
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  padding: 14px 0 24px;
  background:
    radial-gradient(circle at 85% 12%, rgba(231, 165, 106, 0.3), transparent 24%),
    radial-gradient(circle at 10% 86%, rgba(140, 178, 122, 0.2), transparent 28%),
    linear-gradient(180deg, #fbf4ec 0%, #f7f6f3 44%, #eef4eb 100%);
}

.login-page::after {
  content: '';
  position: absolute;
  right: 78px;
  bottom: 116px;
  width: 16px;
  height: 16px;
  border-radius: 999px;
  background: #f1d8b8;
}

.status-bar,
.login-content {
  width: min(390px, 100%);
  margin: 0 auto;
}

.status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  min-height: 54px;
  color: #5a4333;
  font-size: 15px;
  font-weight: 600;
}

.status-icons {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.login-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 18px 20px 28px;
}

.auth-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  align-self: flex-start;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.65);
  color: #9f5c38;
  font-size: 12px;
  font-weight: 600;
}

.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: currentColor;
}

.login-card {
  background: rgba(255, 255, 255, 0.91);
  border-radius: 22px;
  padding: 22px 18px;
  box-shadow:
    0 18px 42px rgba(27, 58, 45, 0.07),
    0 0 0 1px rgba(255, 255, 255, 0.26) inset;
  backdrop-filter: blur(10px);
}

.field span,
.submit-hint,
.footer-prompt {
  color: #787774;
}

h1 {
  margin: 0 0 18px;
  color: #151515;
  font-size: 23px;
  line-height: 1.2;
  font-family: var(--font-serif);
  font-weight: 600;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.field:last-of-type {
  margin-bottom: 0;
}

.field span {
  font-size: 12px;
  font-weight: 600;
  color: #6b625b;
}

.field input {
  border: 1px solid #e8dccf;
  border-radius: 16px;
  min-height: 54px;
  padding: 0 16px;
  background: #fff8f2;
  font-size: 14px;
}

.field input::placeholder {
  color: #aaa19a;
}

.submit-block {
  margin-top: 18px;
}

.submit {
  width: 100%;
  border-radius: 18px;
  padding: 15px 18px;
  font-size: 15px;
  font-weight: 600;
  box-shadow: 0 12px 24px rgba(159, 92, 56, 0.19);
}

.submit:disabled {
  opacity: 0.7;
}

.submit-hint {
  margin: 10px 0 0;
  font-size: 11px;
  line-height: 1.5;
}

.footer-prompt {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding-top: 8px;
  font-size: 13px;
}

.footer-link {
  border: none;
  padding: 0;
  background: none;
  color: #9f5c38;
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 420px) {
  .login-page {
    padding-bottom: 20px;
  }

  .login-content {
    padding-inline: 16px;
  }
}
</style>
