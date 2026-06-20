<template>
  <view class="page-shell feedback-page">
 

    <main v-if="submitted" class="feedback-success-content">
      <section class="feedback-success-card">
        <view class="feedback-success-icon-shell">
          <text class="feedback-success-icon">✓</text>
        </view>
        <view class="feedback-success-copy">
          <text class="feedback-success-title">已收到反馈</text>
          <text class="feedback-success-body">
            谢谢你把真实体验告诉我们。我们会认真阅读每一条建议。
          </text>
        </view>
        <button class="feedback-secondary-button" @tap="goBack('profile')">返回个人页</button>
      </section>
    </main>

    <main v-else class="feedback-content">
     

      <section class="feedback-field-card">
        <view class="feedback-field-head">
          <text class="feedback-field-label">反馈内容</text>
          <text class="feedback-count">{{ content.length }}/500</text>
        </view>
        <textarea
          v-model="content"
          class="feedback-textarea"
          maxlength="500"
          placeholder="写下你的想法，或告诉我们哪里不够顺手。"
          placeholder-class="feedback-placeholder"
        />
        <text class="feedback-helper">提交后会在消息中心收到确认提醒。</text>
      </section>

      <button class="feedback-submit-button" :disabled="submitting" @tap="submitFeedback">
        {{ submitting ? '提交中...' : '提交反馈' }}
      </button>
    </main>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { goBack } from '@/lib/navigation'
import { FeedbackService } from '@/services/feedback-service'

const feedbackService = new FeedbackService()
const content = ref('')
const submitting = ref(false)
const submitted = ref(false)

onMounted(() => {
  void requireAuth('feedback')
})

async function submitFeedback() {
  const trimmedContent = content.value.trim()
  if (!trimmedContent) {
    Message.error('请填写反馈内容')
    return
  }
  if (trimmedContent.length > 500) {
    Message.error('反馈内容不能超过500字')
    return
  }
  if (submitting.value) return

  submitting.value = true
  try {
    await feedbackService.submitFeedback(trimmedContent)
    submitted.value = true
    content.value = ''
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '反馈提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style>
.feedback-page {
  min-height: 100vh;
  background: #f7f6f3;
}

.feedback-top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 60px;
  padding: 14px 20px 6px;
}

.feedback-back-button,
.feedback-top-spacer {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: #fff;
}

.feedback-back-button {
  color: #151515;
  font-size: 26px;
  line-height: 1;
}

.feedback-title-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}

.feedback-page-label {
  color: #9f5c38;
  font-size: 11px;
  font-weight: 600;
}

.feedback-page-title {
  color: #151515;
  font-size: 16px;
  font-weight: 600;
}

.feedback-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 12px 20px 24px;
}

.feedback-intro-card,
.feedback-field-card,
.feedback-success-card {
  background: #fff;
}

.feedback-intro-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-radius: 12px;
  padding: 18px;
}

.feedback-intro-title {
  color: #151515;
  font-family: serif;
  font-size: 22px;
}

.feedback-intro-body,
.feedback-helper,
.feedback-success-body {
  color: #787774;
  font-size: 13px;
  line-height: 1.55;
}

.feedback-field-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-radius: 12px;
  padding: 16px;
}

.feedback-field-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}

.feedback-field-label {
  color: #151515;
  font-size: 14px;
  font-weight: 600;
}

.feedback-count {
  color: #9f5c38;
  font-family: monospace;
  font-size: 12px;
}

.feedback-textarea {
  width: 100%;
  height: 260px;
  border: 1px solid #f0e7dc;
  border-radius: 14px;
  background: #fbf8f4;
  padding: 14px;
  color: #151515;
  font-size: 14px;
  line-height: 1.55;
}

.feedback-placeholder {
  color: #8b857d;
}

.feedback-submit-button,
.feedback-secondary-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 50px;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 600;
  text-align: center;
}

.feedback-submit-button {
  background: #9f5c38;
  color: #fff;
}

.feedback-submit-button[disabled] {
  opacity: 0.72;
}

.feedback-success-content {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: calc(100vh - 60px);
  padding: 16px 20px 32px;
}

.feedback-success-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  border: 1px solid #efe5db;
  border-radius: 16px;
  padding: 22px;
}

.feedback-success-icon-shell {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  border-radius: 18px;
  background: #edf3ec;
}

.feedback-success-icon {
  color: #346538;
  font-size: 28px;
  font-weight: 800;
}

.feedback-success-copy {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.feedback-success-title {
  color: #151515;
  font-family: serif;
  font-size: 26px;
  font-weight: 600;
}

.feedback-success-body {
  width: 100%;
  color: #544d45;
  text-align: center;
  line-height: 1.65;
}

.feedback-secondary-button {
  border: 1px solid #e9e2d8;
  background: #fff;
  color: #9f5c38;
}
</style>
