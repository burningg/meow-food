<template>
  <view v-if="visible" class="notification-modal-overlay">
    <section class="notification-modal-card" @tap.stop>
      <text class="notification-modal-title">{{ title }}</text>
      <view class="notification-modal-body-shell">
        <text class="notification-modal-body">{{ body }}</text>
      </view>
      <button class="notification-modal-action" :disabled="loading" @tap="emit('confirm')">
        {{ loading ? '处理中...' : confirmText }}
      </button>
    </section>
  </view>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    visible: boolean
    title: string
    body: string
    confirmText?: string
    loading?: boolean
  }>(),
  {
    confirmText: '我知道了',
    loading: false,
  },
)

const emit = defineEmits<{
  (e: 'confirm'): void
}>()
</script>

<style>
.notification-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(21, 21, 21, 0.45);
  padding: 20px;
}

.notification-modal-card {
  width: 100%;
  max-width: 350px;
  border: 1px solid #efe5db;
  border-radius: 22px;
  background: #fff;
  padding: 20px;
  box-shadow: 0 20px 44px rgba(0, 0, 0, 0.08);
}

.notification-modal-title,
.notification-modal-body,
.notification-modal-action {
  display: block;
}

.notification-modal-title {
  color: #151515;
  font-family: 'Times New Roman', 'Noto Serif SC', serif;
  font-size: 27px;
  line-height: 1.14;
  font-weight: 700;
}

.notification-modal-body-shell {
  margin-top: 16px;
  border: 1px solid #f0e7dc;
  border-radius: 16px;
  background: #fbf8f4;
  padding: 16px;
}

.notification-modal-body {
  color: #544d45;
  font-size: 13px;
  line-height: 1.65;
}

.notification-modal-action {
  margin-top: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  border-radius: 999px;
  background: linear-gradient(135deg, #a15f38 0%, #bf764a 100%);
  color: #fff;
  font-weight: 700;
  line-height: 1;
  text-align: center;
  box-shadow: 0 8px 18px rgba(164, 106, 31, 0.1);
}
</style>
