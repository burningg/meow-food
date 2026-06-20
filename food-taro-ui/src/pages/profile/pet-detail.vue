<template>
  <view class="page-shell pet-detail-page">
    <view v-if="loading" class="status-card">
      <text>宠物状态加载中...</text>
    </view>

    <template v-else-if="pet?.claimed">
      <section class="pet-detail-hero">
        <view class="detail-glow detail-glow-green"></view>
        <view class="detail-glow detail-glow-gold"></view>
        <view class="pet-soft-shadow"></view>
        <image class="detail-pet-image" :src="petDefinition.assets.lounge" :svg="true" mode="aspectFit" />
        <view class="pet-name-panel">
          <text class="detail-pet-name">{{ pet.name }}</text>
          <view class="breed-pill">
            <text class="breed-text">{{ pet.petTypeName || petDefinition.label }}</text>
          </view>
        </view>
        <view class="pet-whisper-bubble">
          <text class="pet-whisper-star">✦</text>
          <text class="pet-whisper-text">{{ whisperText }}</text>
        </view>
      </section>

      <section class="pet-stats-row">
        <view class="pet-stat-card">
          <text class="pet-stat-value">{{ companionLabel }}</text>
          <text class="pet-stat-label">陪伴</text>
        </view>
        <view class="pet-stat-card">
          <text class="pet-stat-value mood">{{ pet.moodLabel || '开心' }}</text>
          <text class="pet-stat-label">心情</text>
        </view>
        <view class="pet-stat-card">
          <text class="pet-stat-value fullness">{{ fullnessLabel }}</text>
          <text class="pet-stat-label">饱腹</text>
        </view>
      </section>

      <section class="today-story-card">
        <view class="today-story-head">
          <text class="today-story-title">今日小事</text>
          <text class="today-story-time">刚刚</text>
        </view>
        <text class="today-story-body">{{ pet.todayStory }}</text>
      </section>

      <button class="go-recipe-button" @tap="goRecipes">
        <text class="go-recipe-text">让{{ pet.name }}去帮你计划今天的菜谱</text>
      </button>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useDidShow } from '@tarojs/taro'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { getPetDefinition } from '@/lib/pet'
import { openPrimaryRoute, replace } from '@/lib/navigation'
import { PetService, type PetResponse } from '@/services/pet-service'

const petService = new PetService()
const pet = ref<PetResponse | null>(null)
const loading = ref(true)

const petDefinition = computed(() => getPetDefinition(pet.value?.petType))
const companionLabel = computed(() => `${pet.value?.companionDays ?? 0} 天`)
const fullnessLabel = computed(() => `${pet.value?.fullnessPercent ?? 0}%`)
const whisperText = computed(() => {
  if (pet.value?.moodCode === 'bored') return '有点想你，带我看看新菜单吧。'
  return '今天想趴在菜谱边，等你开饭。'
})

onMounted(async () => {
  if (!(await requireAuth('pet-detail'))) return
  await loadPet()
})

useDidShow(async () => {
  if (!(await requireAuth('pet-detail'))) return
  await loadPet()
})

async function loadPet() {
  loading.value = true
  try {
    const { data } = await petService.getMyPet()
    pet.value = data
    if (!data.claimed) {
      await replace('pet-adoption')
    }
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '宠物状态加载失败')
  } finally {
    loading.value = false
  }
}

function goRecipes() {
  openPrimaryRoute('home')
}
</script>

<style>
.pet-detail-page {
  min-height: 100vh;
  padding-bottom: 32px;
  background: linear-gradient(180deg, #fff7ea 0%, #f7f6f3 44%, #edf3ec 100%);
}

.pet-detail-hero {
  position: relative;
  overflow: hidden;
  height: 252px;
  border: 1px solid #ead9c4;
  border-radius: 28px;
  background: #fff8ef;
  box-shadow: 0 18px 42px rgba(138, 78, 41, 0.12);
}

.detail-glow,
.pet-soft-shadow {
  position: absolute;
  border-radius: 999px;
}

.detail-glow-green {
  left: 0;
  bottom: 0;
  width: 126px;
  height: 126px;
  background: radial-gradient(circle, rgba(141, 176, 125, 0.28), rgba(141, 176, 125, 0));
}

.detail-glow-gold {
  top: 0;
  right: 20px;
  width: 168px;
  height: 168px;
  background: radial-gradient(circle, rgba(255, 229, 168, 0.5), rgba(255, 229, 168, 0));
}

.pet-soft-shadow {
  left: 50%;
  bottom: 42px;
  width: 198px;
  height: 38px;
  transform: translateX(-50%);
  background: #dce7da;
  filter: blur(1px);
}

.detail-pet-image {
  position: absolute;
  left: 50%;
  top: 42px;
  width: 164px;
  height: 164px;
  transform: translateX(-42%);
  image-rendering: pixelated;
}

.pet-name-panel {
  position: absolute;
  left: 18px;
  top: 18px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.detail-pet-name,
.today-story-title {
  color: #1b3a2d;
  font-size: 30px;
  font-weight: 800;
}

.breed-pill,
.pet-whisper-bubble,
.pet-stats-row,
.today-story-head,
.go-recipe-button {
  display: flex;
  align-items: center;
}

.breed-pill {
  gap: 6px;
  border-radius: 999px;
  background: #edf3ec;
  padding: 6px 10px;
}

.breed-dot {
  color: #346538;
  font-size: 11px;
  font-weight: 800;
}

.breed-text {
  color: #346538;
  font-size: 12px;
  font-weight: 700;
}

.pet-whisper-bubble {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 14px;
  justify-content: center;
  gap: 8px;
  height: 34px;
  border: 1px solid #efe3d1;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  padding: 0 12px;
}

.pet-whisper-star {
  flex: 0 0 auto;
  color: #c8873b;
  font-size: 15px;
  font-weight: 800;
}

.pet-whisper-text {
  min-width: 0;
  color: #5b4a39;
  font-size: 12px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pet-stats-row {
  gap: 10px;
  margin-top: 18px;
}

.pet-stat-card {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 76px;
  gap: 6px;
  border: 1px solid #efe3d1;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 10px 20px rgba(27, 58, 45, 0.06);
}

.pet-stat-value {
  color: #1b3a2d;
  font-size: 21px;
  font-weight: 800;
}

.pet-stat-value.mood {
  color: #9f5c38;
}

.pet-stat-value.fullness {
  color: #346538;
}

.pet-stat-label {
  color: #787774;
  font-size: 11px;
  font-weight: 700;
}

.today-story-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 28px;
  border: 1px solid #efe3d1;
  border-radius: 22px;
  background: #fff;
  padding: 16px;
  box-shadow: 0 12px 26px rgba(138, 78, 41, 0.08);
}

.today-story-head {
  justify-content: space-between;
}

.today-story-title {
  color: #151515;
  font-size: 21px;
}

.today-story-time {
  color: #9f5c38;
  font-size: 12px;
  font-weight: 700;
}

.today-story-body {
  color: #2f3437;
  font-size: 13px;
  line-height: 1.55;
}

.go-recipe-button {
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 50px;
  margin-top: 28px;
  border-radius: 999px;
  background: linear-gradient(135deg, #9f5c38, #c8873b);
  box-shadow: 0 10px 24px rgba(164, 106, 31, 0.2);
}

.go-recipe-text,
.go-recipe-arrow {
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  line-height: 1;
  text-align: center;
}
</style>
