<template>
  <a-space direction="vertical" :style="{ width: '100%' }">
    <a-upload
      :disabled="disabled"
      :tip="tip"
      action=""
      :fileList="file ? [file] : []"
      :show-file-list="false"
      :image-preview="true"
      :custom-request="customRequest"
      @success="success"
      @progress="onProgress"
    >
      <template #upload-button>
        <div
          :class="`arco-upload-list-item${
            file && file.status === 'error' ? ' arco-upload-list-item-error' : ''
          }`"
        >
          <div
            style="width: 150px; height: 150px"
            class="arco-upload-list-picture custom-upload-avatar"
            v-if="file && file.url"
          >
            <img :src="file.url" />
            <div class="arco-upload-list-picture-mask">
              <IconEdit />
            </div>
            <a-progress
              v-if="file.status === 'uploading' && file.percent < 100"
              :percent="file.percent"
              type="circle"
              size="mini"
              :style="{
                position: 'absolute',
                left: '50%',
                top: '50%',
                transform: 'translateX(-50%) translateY(-50%)',
              }"
            />
          </div>
          <div style="width: 120px; height: 120px" class="arco-upload-picture-card" v-else>
            <div class="arco-upload-picture-card-text">
              <IconPlus />
              <div style="width: 120px; height: 120px; margin-top: 90px; font-weight: 600">
                上传图片
              </div>
            </div>
          </div>
        </div>
      </template>
    </a-upload>
  </a-space>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue'
import { FoodService } from '@/services/food-service'
import imageCompression from 'browser-image-compression'
import { Message } from '@arco-design/web-vue'
const foodService = new FoodService()
const props = defineProps({
  tip: {
    type: String,
  },
  fileData: {
    type: String,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})
const emit = defineEmits<{
  (e: 'success', url: string): void
}>()
const file = ref()

watch(
  () => props.fileData,
  (newVal) => {
    if (newVal) {
      file.value = {
        url: newVal,
      }
    } else {
      file.value = undefined
    }
  },
  { immediate: true },
)

const success = (currentFile: any) => {
  file.value = currentFile
  emit('success', file.value.url)
}

const customRequest = async (option: any) => {
  const { onProgress, onError, onSuccess, fileItem, name } = option

  const fileName = fileItem.name
  const fileExtension = fileItem.name.split('.').pop()
  const formData = new FormData()

  // 1. 加入 JSON 信息
  //formData.append(fileName, JSON.stringify(meta))
  if (fileItem.file) {
    // 使用 Compressor.js 压缩图片
    const options = {
      maxSizeMB: 0.2, // 最大文件大小，单位 MB
      maxWidthOrHeight: 800, // 最大宽高
      useWebWorker: true, // 是否使用 Web Worker 来异步压缩
    }
    const compressedFile = await imageCompression(fileItem.file, options)

    formData.append(
      'file',
      new File([compressedFile], fileName || `image.${fileExtension || 'jpg'}`, {
        type: compressedFile.type,
      }),
    )
    try {
      const response: any = await foodService.uploadImage(formData)
      var url = response.data
      fileItem.url = url
      onSuccess(fileItem)
    } catch (err: any) {
      Message.error(err.message)
      onError(err)
    }
  }
}

const onProgress = (currentFile: any) => {
  file.value = currentFile
}
</script>
<style scoped lang="scss"></style>
