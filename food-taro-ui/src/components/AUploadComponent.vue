<template>
  <button class="upload-box" :disabled="disabled || uploading" @tap="chooseImage">
    <image v-if="fileData" class="upload-image" :src="fileData" mode="aspectFill" />
    <view v-else class="upload-placeholder">
      <text class="upload-plus">＋</text>
      <text>上传图片</text>
    </view>
    <view v-if="uploading" class="upload-mask">
      <text>上传中...</text>
    </view>
  </button>
</template>

<script setup lang="ts">
import Taro from '@tarojs/taro'
import { ref } from 'vue'
import { Message } from '@/lib/feedback'
import { FoodService } from '@/services/food-service'

const props = defineProps<{
  tip?: string
  fileData?: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  success: [url: string]
}>()

const foodService = new FoodService()
const uploading = ref(false)

async function chooseImage() {
  if (props.disabled || uploading.value) return
  try {
    const result = await Taro.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
    })
    const filePath = result.tempFilePaths[0]
    if (!filePath) return
    uploading.value = true
    const { data } = await foodService.uploadImage(filePath)
    emit('success', data)
    Message.success('图片已上传')
  } catch (error: any) {
    if (error?.errMsg?.includes('cancel')) return
    Message.error(error?.response?.data?.message || error?.message || props.tip || '上传失败')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.upload-box {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 150px;
  height: 150px;
  margin: 0 auto;
  overflow: hidden;
  border-radius: 18px;
  background: #f4f0ea;
}

.upload-image {
  width: 100%;
  height: 100%;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #8b8b8b;
  font-size: 13px;
  font-weight: 700;
}

.upload-plus {
  color: #9f5c38;
  font-size: 28px;
}

.upload-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(21, 21, 21, 0.45);
  color: #fff;
  font-size: 13px;
}
</style>
