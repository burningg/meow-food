<template>
  <view v-if="visible" class="ai-import-overlay" @tap="close">
    <section class="ai-import-card" @tap.stop>
      <view class="ai-import-head">
        <text class="ai-import-title">AI导入菜谱</text>
        <button class="ai-import-close" :disabled="isBusy" @tap="close">×</button>
      </view>

      <text class="ai-import-desc">粘贴其他软件里的菜谱文字，或上传截图，AI会整理食材和步骤。</text>

      <section class="ai-import-section">
        <view class="ai-import-section-head">
          <text class="ai-import-section-title">图片</text>
          <text class="ai-import-limit">最多3张</text>
        </view>
        <view class="ai-import-images">
          <view v-for="(image, index) in images" :key="image" class="ai-import-image-tile">
            <image class="ai-import-image" :src="image" mode="aspectFill" />
            <button class="ai-import-image-delete" :disabled="isBusy" @tap="removeImage(index)">×</button>
          </view>
          <button
            v-if="images.length < maxImageCount"
            class="ai-import-upload-tile"
            :disabled="isBusy"
            @tap="chooseImages"
          >
            <text class="ai-import-upload-plus">＋</text>
            <text class="ai-import-upload-text">{{ uploading ? '上传中' : '上传' }}</text>
          </button>
        </view>
      </section>

      <section class="ai-import-section">
        <text class="ai-import-section-title">菜谱文字</text>
        <textarea
          v-model.trim="text"
          class="ai-import-textarea"
          :disabled="isBusy"
          maxlength="2000"
          placeholder="可直接粘贴菜谱链接～或文字、食材清单或做法步骤..."
        />
      </section>

      <button class="ai-import-action" :disabled="isBusy" @tap="submit">
        {{ loading ? '导入中...' : 'AI导入' }}
      </button>
    </section>
  </view>
</template>

<script setup lang="ts">
import Taro from '@tarojs/taro'
import { computed, ref, watch } from 'vue'
import { Message } from '@/lib/feedback'
import { FoodService } from '@/services/food-service'

const props = defineProps<{
  visible: boolean
  loading?: boolean
}>()

const emit = defineEmits<{
  close: []
  import: [payload: { text?: string; images: string[] }]
}>()

const foodService = new FoodService()
const maxImageCount = 3
const text = ref('')
const images = ref<string[]>([])
const uploading = ref(false)
const isBusy = computed(() => Boolean(props.loading) || uploading.value)

watch(
  () => props.visible,
  (visible) => {
    if (visible) return
    text.value = ''
    images.value = []
    uploading.value = false
  },
)

function close() {
  if (isBusy.value) return
  emit('close')
}

async function chooseImages() {
  if (isBusy.value) return
  const remainingCount = maxImageCount - images.value.length
  if (remainingCount <= 0) {
    Message.warning('最多上传3张图片')
    return
  }

  try {
    const result = await Taro.chooseImage({
      count: remainingCount,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
    })
    const filePaths = result.tempFilePaths.slice(0, remainingCount)
    if (!filePaths.length) return

    uploading.value = true
    const uploadedImages: string[] = []
    // 逐张上传，确保上传结果顺序和用户选择顺序一致。
    for (const filePath of filePaths) {
      const { data } = await foodService.uploadImage(filePath)
      uploadedImages.push(data)
    }
    images.value = [...images.value, ...uploadedImages].slice(0, maxImageCount)
    Message.success('图片已上传')
  } catch (error: any) {
    if (error?.errMsg?.includes('cancel')) return
    Message.error(error?.response?.data?.message || error?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

function removeImage(index: number) {
  if (isBusy.value) return
  images.value = images.value.filter((_, currentIndex) => currentIndex !== index)
}

function submit() {
  const normalizedText = text.value.trim()
  // AI导入至少需要一段文字或一张截图，否则没有可识别来源。
  if (!normalizedText && !images.value.length) {
    Message.warning('请先粘贴菜谱文字或上传截图')
    return
  }
  emit('import', {
    text: normalizedText || undefined,
    images: [...images.value],
  })
}
</script>

<style>
.ai-import-overlay {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(21, 21, 21, 0.45);
  padding: 20px;
}

.ai-import-card {
  width: 100%;
  max-width: 350px;
  border: 1px solid #efe5db;
  border-radius: 22px;
  background: #fff;
  padding: 20px;
  box-shadow: 0 20px 44px rgba(0, 0, 0, 0.08);
}

.ai-import-head,
.ai-import-section-head,
.ai-import-images {
  display: flex;
}

.ai-import-head {
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.ai-import-title {
  color: #151515;
  font-family: 'Times New Roman', 'Noto Serif SC', serif;
  font-size: 27px;
  font-weight: 700;
  line-height: 1.15;
}

.ai-import-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 999px;
  background: #f7f4ef;
  color: #787774;
  font-size: 20px;
  line-height: 1;
  flex-shrink: 0;
}

.ai-import-desc {
  display: block;
  margin-top: 14px;
  color: #787774;
  font-size: 12px;
  line-height: 1.55;
}

.ai-import-section {
  margin-top: 18px;
}

.ai-import-section-head {
  align-items: center;
  justify-content: space-between;
}

.ai-import-section-title {
  display: block;
  color: #151515;
  font-size: 14px;
  font-weight: 800;
}

.ai-import-limit {
  color: #9f5c38;
  font-size: 11px;
  font-weight: 800;
}

.ai-import-images {
  gap: 10px;
  margin-top: 10px;
}

.ai-import-image-tile,
.ai-import-upload-tile {
  position: relative;
  width: 96px;
  height: 96px;
  overflow: hidden;
  border: 1px solid #ead9c4;
  border-radius: 14px;
  background: #fbf8f4;
  flex-shrink: 0;
}

.ai-import-image {
  width: 100%;
  height: 100%;
}

.ai-import-image-delete {
  position: absolute;
  top: 6px;
  right: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: rgba(21, 21, 21, 0.56);
  color: #fff;
  font-size: 16px;
  line-height: 1;
}

.ai-import-upload-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #9f5c38;
}

.ai-import-upload-plus {
  font-size: 26px;
  font-weight: 600;
  line-height: 1;
}

.ai-import-upload-text {
  font-size: 11px;
  font-weight: 800;
}

.ai-import-textarea {
  width: 100%;
  min-height: 150px;
  margin-top: 10px;
  box-sizing: border-box;
  border: 1px solid #f0e7dc;
  border-radius: 16px;
  background: #fbf8f4;
  color: #544d45;
  font-size: 13px;
  line-height: 1.6;
  padding: 14px;
}

.ai-import-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 48px;
  margin-top: 18px;
  border-radius: 999px;
  background: linear-gradient(135deg, #a15f38 0%, #bf764a 100%);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  line-height: 1;
  box-shadow: 0 8px 18px rgba(164, 106, 31, 0.1);
}

.ai-import-action[disabled],
.ai-import-close[disabled],
.ai-import-upload-tile[disabled],
.ai-import-image-delete[disabled] {
  opacity: 0.5;
}
</style>
