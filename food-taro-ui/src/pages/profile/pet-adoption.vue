<template>
  <view class="page-shell pet-adoption-page">
    <section class="adoption-hero">
      <view class="adoption-glow adoption-glow-green"></view>
      <view class="adoption-glow adoption-glow-gold"></view>
      <view class="adoption-hero-copy">
        <text class="adoption-eyebrow">PET PARTNER</text>
        <text class="adoption-title">今天开始，饭点有人陪</text>
        <text class="adoption-body">选一只合眼缘的小伙伴，再给 TA 起个名字。</text>
      </view>
      <text class="adoption-mark adoption-mark-one">✦</text>
    </section>

    <section class="pet-choice-section">
      <view class="pet-choice-head">
        <text class="pet-section-title">选择你的饭搭子</text>
        <text class="pet-choice-hint">可切换</text>
      </view>
      <view class="pet-choice-row">
        <button
          v-for="option in petOptions"
          :key="option.type"
          :class="['pet-choice-card', { active: selectedPetType === option.type }]"
          @tap="selectPet(option.type)"
        >
          <view class="pet-choice-image-wrap">
            <image class="pet-choice-image" :src="option.assets.normal" :svg="true" mode="aspectFit" />
          </view>
          <view class="pet-choice-copy">
            <text class="pet-choice-title">{{ option.label }}</text>
            <text class="pet-choice-desc">{{ option.description }}</text>
          </view>
        </button>
      </view>
    </section>

    <section class="pet-name-card">
      <view class="pet-name-head">
        <text class="pet-name-label">给 TA 起个名字</text>
      </view>
      <view class="pet-name-input-wrap">
        <input
          v-model.trim="petName"
          class="pet-name-input"
          maxlength="8"
          :placeholder="selectedDefinition.defaultName"
          placeholder-class="pet-name-placeholder"
        />
        <text class="pet-name-hint">最多 8 个字</text>
      </view>
    </section>

    <section class="adoption-note">
      <text class="adoption-note-icon">锅</text>
      <text class="adoption-note-text">领取后可以在宠物详情页查看心情、饱腹和成长。</text>
    </section>

    <button class="confirm-adoption-button" :disabled="saving" @tap="claimPet">
      <text class="confirm-adoption-text">{{ saving ? '领取中...' : '确认领取' }}</text>
      <text class="confirm-adoption-arrow">→</text>
    </button>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { getPetDefinition, PET_TYPE_OPTIONS, type PetType } from '@/lib/pet'
import { replace } from '@/lib/navigation'
import { PetService } from '@/services/pet-service'

const petService = new PetService()
const petOptions = PET_TYPE_OPTIONS
const selectedPetType = ref<PetType>('tabby_cat')
const petName = ref(getPetDefinition('tabby_cat').defaultName)
const saving = ref(false)

const selectedDefinition = computed(() => getPetDefinition(selectedPetType.value))

onMounted(async () => {
  if (!(await requireAuth('pet-adoption'))) return
  await redirectIfClaimed()
})

async function redirectIfClaimed() {
  try {
    const { data } = await petService.getMyPet()
    if (data.claimed) {
      await replace('pet-detail')
    }
  } catch {
    Message.error('宠物状态加载失败')
  }
}

function selectPet(type: PetType) {
  const previousDefaultName = selectedDefinition.value.defaultName
  selectedPetType.value = type
  if (!petName.value.trim() || petName.value.trim() === previousDefaultName) {
    petName.value = getPetDefinition(type).defaultName
  }
}

async function claimPet() {
  const name = petName.value.trim()
  if (!name) {
    Message.error('请给宠物起个名字')
    return
  }
  if (saving.value) return

  saving.value = true
  try {
    await petService.claimPet({
      petType: selectedPetType.value,
      name,
    })
    Message.success('宠物领取成功')
    await replace('pet-detail')
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '宠物领取失败，请稍后重试')
  } finally {
    saving.value = false
  }
}
</script>

<style>
.pet-adoption-page {
  min-height: 100vh;
  padding-bottom: 28px;
  background: linear-gradient(180deg, #fff7ea 0%, #f7f6f3 48%, #edf3ec 100%);
}

.adoption-hero {
  position: relative;
  overflow: hidden;
  min-height: 122px;
  border: 1px solid #ead9c4;
  border-radius: 24px;
  background: #fff8ef;
  box-shadow: 0 14px 34px rgba(138, 78, 41, 0.1);
}

.adoption-glow {
  position: absolute;
  border-radius: 999px;
}

.adoption-glow-green {
  left: 8px;
  bottom: 6px;
  width: 54px;
  height: 54px;
  background: radial-gradient(circle, rgba(141, 176, 125, 0.28), rgba(141, 176, 125, 0));
}

.adoption-glow-gold {
  top: 4px;
  right: 18px;
  width: 108px;
  height: 108px;
  background: radial-gradient(circle, rgba(255, 229, 168, 0.5), rgba(255, 229, 168, 0));
}

.adoption-hero-copy {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
  width: 235px;
  padding: 18px;
}

.adoption-eyebrow {
  color: #346538;
  font-family: monospace;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1px;
}

.adoption-title,
.pet-section-title {
  color: #151515;
  font-size: 22px;
  font-weight: 800;
}

.adoption-body {
  color: #5b4a39;
  font-size: var(--text-sm);
  line-height: 1.5;
}

.adoption-mark {
  position: absolute;
  color: #c8873b;
  font-size: 22px;
  font-weight: 800;
}

.adoption-mark-one {
  top: 22px;
  right: 38px;
}

.adoption-mark-two {
  right: 18px;
  bottom: 28px;
  color: #346538;
}

.pet-choice-section,
.pet-name-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}

.pet-choice-head,
.pet-name-head,
.pet-name-input-wrap,
.adoption-note,
.confirm-adoption-button {
  display: flex;
  align-items: center;
}

.pet-choice-head,
.pet-name-head,
.pet-name-input-wrap {
  justify-content: space-between;
}

.pet-choice-hint {
  color: #9f5c38;
  font-size: 12px;
  font-weight: 700;
}

.pet-choice-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.pet-choice-card {
  display: flex;
  flex-direction: column;
  min-height: 210px;
  border: 1px solid #efe3d1;
  border-radius: 20px;
  background: #fff;
  padding: 12px;
  text-align: left;
  box-shadow: 0 12px 26px rgba(27, 58, 45, 0.06);
}

.pet-choice-card.active {
  border-color: #346538;
  border-width: 2px;
  background: #edf3ec;
}

.pet-choice-image-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 108px;
}

.pet-choice-image {
  width: 112px;
  height: 112px;
  image-rendering: pixelated;
}

.pet-choice-copy {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.pet-choice-title {
  color: #151515;
  font-size: 16px;
  font-weight: 800;
}

.pet-choice-desc,
.pet-name-hint,
.adoption-note-text {
  color: #787774;
  font-size: 12px;
  line-height: 1.45;
}

.pet-name-card {
  border: 1px solid #efe3d1;
  border-radius: 20px;
  background: #fff;
  padding: 16px;
  box-shadow: 0 12px 26px rgba(138, 78, 41, 0.08);
}

.pet-name-label {
  color: #5b4a39;
  font-size: 13px;
  font-weight: 700;
}

.pet-name-icon {
  color: #9f5c38;
  font-size: 18px;
  font-weight: 800;
}

.pet-name-input-wrap {
  height: 52px;
  border: 1px solid #f0e7dc;
  border-radius: 16px;
  background: #fbf8f4;
  padding: 0 14px;
}

.pet-name-input {
  flex: 1;
  min-width: 0;
  height: 52px;
  color: #151515;
  font-size: 23px;
  font-weight: 800;
}

.pet-name-placeholder {
  color: #b7afa5;
}

.adoption-note {
  gap: 10px;
  margin-top: 16px;
  border: 1px solid #dce7da;
  border-radius: 18px;
  background: #edf3ec;
  padding: 14px;
}

.adoption-note-icon {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 10px;
  background: #346538;
  color: #fff;
  font-size: 13px;
  font-weight: 800;
}

.adoption-note-text {
  flex: 1;
  min-width: 0;
  color: #2f3437;
}

.confirm-adoption-button {
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 52px;
  margin-top: 16px;
  border-radius: 999px;
  background: linear-gradient(135deg, #9f5c38, #c8873b);
  box-shadow: 0 10px 24px rgba(164, 106, 31, 0.2);
}

.confirm-adoption-button[disabled] {
  opacity: 0.72;
}

.confirm-adoption-text,
.confirm-adoption-arrow {
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  line-height: 1;
  text-align: center;
}
</style>
