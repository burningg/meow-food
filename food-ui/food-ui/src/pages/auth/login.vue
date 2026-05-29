<template>
  <div class="login-page">
    <section class="login-card">
      <p class="eyebrow">食光小馆</p>
      <h1>登录你的美味空间</h1>
      <p class="intro">测试账号：`panghu` / `ali` / `zhouzhou` / `ahao`，密码统一为 `123456`。</p>

      <label class="field">
        <span>账号</span>
        <input v-model.trim="form.account" placeholder="输入账号" />
      </label>

      <label class="field">
        <span>密码</span>
        <input v-model.trim="form.password" type="password" placeholder="输入密码" />
      </label>

      <button class="primary-button submit" type="button" :disabled="loading" @click="submit">
        {{ loading ? '登录中...' : '进入美味空间' }}
      </button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { useAuthStore } from '@/stores/auth-store'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const form = reactive({
  account: 'panghu',
  password: '123456',
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
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(159, 92, 56, 0.16), transparent 28%),
    radial-gradient(circle at bottom right, rgba(52, 101, 56, 0.12), transparent 24%),
    #f5f2ed;
}

.login-card {
  width: min(390px, 100%);
  background: rgba(255, 255, 255, 0.92);
  border-radius: 28px;
  padding: 28px 22px;
  box-shadow: 0 30px 60px rgba(27, 58, 45, 0.12);
}

.eyebrow,
.intro,
.field span {
  color: var(--text-muted);
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 0.72rem;
}

h1 {
  margin: 8px 0 10px;
  color: var(--text-main);
  font-size: 2rem;
  line-height: 1.05;
  font-family: 'Playfair Display', serif;
}

.intro {
  margin-bottom: 24px;
  line-height: 1.6;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.field input {
  border: 1px solid #eadfd4;
  border-radius: 16px;
  min-height: 52px;
  padding: 0 16px;
  background: #fffaf6;
}

.submit {
  width: 100%;
  margin-top: 8px;
}
</style>
