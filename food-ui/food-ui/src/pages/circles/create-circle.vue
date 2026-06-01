<template>
  <div class="page-shell create-circle-page">
    <header class="top-nav">
      <button class="nav-shell" type="button" aria-label="返回" @click="goBack">
        <span>‹</span>
      </button>
      <h1>新建圈子</h1>
      <button class="publish-chip" type="button" :disabled="submitDisabled" @click="submit">
        {{ saving ? '提交中' : '完成' }}
      </button>
    </header>

    <section class="page-content">
      <section class="field-section">
        <span class="section-label">圈子封面</span>
      </section>

      <section class="info-section">
        <div class="info-card">
          <div class="info-head">
            <div class="info-title-wrap">
              <h2>基础信息</h2>
            </div>
          </div>

          <label class="field-group">
            <span class="field-label">圈子名称</span>
            <div class="line-input-shell">
              <input
                v-model.trim="form.name"
                class="line-input"
                type="text"
                maxlength="24"
                placeholder="例如 一起减肥吧"
              />
            </div>
          </label>

          <label class="field-group">
            <span class="field-label">一句话介绍</span>
            <div class="intro-input-shell">
              <textarea
                v-model.trim="form.description"
                class="intro-input"
                rows="3"
                maxlength="90"
                placeholder="meow"
              />
            </div>
          </label>
        </div>
      </section>

      <section class="submit-section">
        <button class="submit-button" type="button" :disabled="submitDisabled" @click="submit">
          {{ saving ? '创建中...' : '创建圈子并发送邀请' }}
        </button>
        <p>创建后你仍然可以编辑圈子介绍、增减成员和调整加入方式。</p>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { SocialService } from '@/services/social-service'

const router = useRouter()
const socialService = new SocialService()
const saving = ref(false)

const form = reactive({
  name: '',
  description: '',
})

const submitDisabled = computed(() => saving.value || !form.name.trim() || !form.description.trim())

async function submit() {
  if (submitDisabled.value) {
    Message.warning(!form.name.trim() ? '请先填写圈子名称' : '请补一句圈子介绍')
    return
  }

  saving.value = true
  try {
    const { data } = await socialService.createCircle({
      name: form.name.trim(),
      description: form.description.trim(),
    })
    Message.success('搭子圈已创建')
    router.push({ name: 'circle-detail', params: { id: data.circle.id } })
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '创建圈子失败')
  } finally {
    saving.value = false
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push({ name: 'circles' })
}
</script>

<style scoped>
.create-circle-page {
  padding: 0 0 28px;
  background: #f7f6f3;
}

.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.top-nav {
  height: 54px;
  padding: 14px 20px 0;
}

.top-nav h1,
.info-title-wrap h2,
.field-label,
.submit-section p {
  margin: 0;
}

.top-nav h1 {
  color: #151515;
  font-size: 1rem;
  font-weight: 600;
  line-height: 1.2;
}

.nav-shell,
.publish-chip,
.submit-button {
  border: none;
}

.nav-shell,
.publish-chip {
  border-radius: 999px;
}

.nav-shell {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  padding: 0;
  background: #ffffff;
  color: #151515;
  font-size: 1.1rem;
  line-height: 1;
}

.publish-chip {
  min-width: 54px;
  padding: 8px 14px;
  background: #9f5c38;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
}

.publish-chip:disabled,
.submit-button:disabled {
  opacity: 0.58;
}

.page-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 12px 20px 24px;
}

.field-section,
.info-section,
.submit-section,
.info-card,
.field-group,
.info-title-wrap {
  display: flex;
  flex-direction: column;
}

.field-section {
  gap: 10px;
}

.info-section {
  gap: 12px;
}

.info-card {
  gap: 14px;
  padding: 18px;
  border-radius: 16px;
  background: #ffffff;
}

.section-label,
.field-label {
  color: #787774;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.35;
}
.info-title-wrap h2 {
  color: #151515;
  font-family: var(--font-serif);
  font-size: 1.125rem;
  font-weight: 600;
  line-height: 1.28;
}

.submit-section p {
  margin: 0;
  color: #787774;
  font-size: 11px;
  line-height: 1.45;
}

.info-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.info-title-wrap {
  gap: 4px;
}

.field-group {
  gap: 8px;
}

.line-input-shell {
  height: 48px;
  display: flex;
  align-items: center;
  padding: 0 14px;
  border-bottom: 1px solid #e9e2d8;
}

.intro-input-shell {
  display: flex;
  padding: 14px;
  border-radius: 12px;
  background: #f6f2ec;
}

.line-input,
.intro-input {
  width: 100%;
  border: none;
  padding: 0;
  background: transparent;
  color: #151515;
}

.line-input {
  font-size: 14px;
  line-height: 48px;
}

.intro-input {
  min-height: 68px;
  resize: none;
  font-size: 13px;
  line-height: 1.45;
  color: #2f3437;
}

.line-input::placeholder,
.intro-input::placeholder {
  color: #8a817a;
}

.line-input:focus,
.intro-input:focus {
  outline: none;
}

.submit-section {
  gap: 8px;
}

.submit-button {
  width: 100%;
  min-height: 48px;
  padding: 14px 18px;
  border-radius: 16px;
  background: #9f5c38;
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.2;
}

@media (max-width: 360px) {
  .page-content {
    padding-left: 16px;
    padding-right: 16px;
  }

  .top-nav {
    padding-left: 16px;
    padding-right: 16px;
  }
}
</style>
