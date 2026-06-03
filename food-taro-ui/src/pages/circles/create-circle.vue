<template>
  <view class="page-shell create-circle-page">
    <header class="top-nav circle-nav">
      <button class="nav-shell" @tap="goBack('circles')">‹</button>
      <text class="page-title">新建圈子</text>
    </header>

    <section class="page-content">
      <section class="field-section">
        <text class="section-label">圈子封面</text>
      </section>

      <section class="info-card">
        <text class="info-title">基础信息</text>
        <label class="field-group">
          <text class="field-label">圈子名称</text>
          <input v-model.trim="form.name" class="line-input" maxlength="24" placeholder="例如 一起减肥吧" />
        </label>
        <label class="field-group">
          <text class="field-label">一句话介绍</text>
          <textarea v-model.trim="form.description" class="intro-input" maxlength="90" placeholder="meow" />
        </label>
      </section>

      <section class="submit-section">
        <button class="submit-button" :disabled="submitDisabled" @tap="submit">
          {{ saving ? '创建中...' : '创建圈子并发送邀请' }}
        </button>
      </section>
    </section>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { goBack, push } from '@/lib/navigation'
import { SocialService } from '@/services/social-service'

const socialService = new SocialService()
const saving = ref(false)
const form = reactive({ name: '', description: '' })
const submitDisabled = computed(() => saving.value || !form.name.trim() || !form.description.trim())

onMounted(() => requireAuth('create-circle'))

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
    push({ name: 'circles', params: { id: data.circle.id } })
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '创建圈子失败')
  } finally {
    saving.value = false
  }
}
</script>

<style>
.create-circle-page {
  padding: 0 0 28px;
  background: #f7f6f3;
}

.circle-nav {
  height: 54px;
  padding: 14px 20px 0;
}

.page-title,
.info-title {
  color: #151515;
  font-weight: 800;
}

.publish-chip,
.submit-button {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #9f5c38;
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  line-height: 1;
  text-align: center;
}

.publish-chip {
  min-width: 54px;
  min-height: 32px;
  padding: 0 14px;
}

.page-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 12px 20px 24px;
}

.section-label,
.field-label,
.submit-section text {
  color: #787774;
  font-size: 12px;
}

.info-card,
.field-group,
.submit-section {
  display: flex;
  flex-direction: column;
}

.info-card {
  gap: 14px;
  border-radius: 16px;
  background: #fff;
  padding: 18px;
}

.info-title {
  font-size: 18px;
}

.field-group {
  gap: 8px;
}

.line-input,
.intro-input {
  background: #f6f2ec;
  border-radius: 12px;
  padding: 0 14px;
}

.line-input {
  min-height: 48px;
}

.intro-input {
  min-height: 92px;
  padding-top: 12px;
}

.submit-section {
  gap: 10px;
}

.submit-button {
  min-height: 48px;
  padding: 0 18px;
}
</style>
