<template>
  <view class="loading-page">
    <view class="loading-card">
      <view class="emblem">
        <image v-if="logoSrc" class="logo" :src="logoSrc" mode="aspectFit" />
        <text v-else>meow</text>
      </view>
      <view class="title">
        <text class="title-main">meow食堂</text>
        <text class="title-sub">正在打开你的美味空间</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { replace } from '@/lib/navigation'
import { useAuthStore } from '@/stores/auth-store'

const authStore = useAuthStore()
const logoSrc = ''

onMounted(async () => {
  await authStore.restore()
  setTimeout(() => {
    replace('home')
  }, 900)
})
</script>

<style>
.loading-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background:
    radial-gradient(circle at 85% 12%, rgba(231, 165, 106, 0.28), transparent 24%),
    radial-gradient(circle at 10% 86%, rgba(140, 178, 122, 0.2), transparent 28%),
    linear-gradient(180deg, #fbf4ec 0%, #f7f6f3 44%, #eef4eb 100%);
}

.loading-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
}

.emblem {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 86px;
  height: 86px;
  border-radius: 28px;
  background: #fff;
  color: #9f5c38;
  font-weight: 800;
  box-shadow: 0 18px 42px rgba(27, 58, 45, 0.08);
}

.logo {
  width: 60px;
  height: 60px;
}

.title {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.title-main {
  color: #151515;
  font-size: 28px;
  font-weight: 800;
}

.title-sub {
  color: #787774;
  font-size: 13px;
}
</style>
