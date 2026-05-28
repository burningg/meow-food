<template>
  <div class="modal-mask" @click.self="handleBackdropClick">
    <div :class="['modal-container', modalClass, sizeClass, titleClass]">
      <!-- 顶部右上角 X 按钮 -->
      <div class="modal-title">{{ title }}</div>
      <button class="modal-close-btn" @click="emitClose">x</button>
      <div class="container">
        <!-- 动态组件 -->
        <div class="container-component">
          <component :is="component" v-bind="props.props" @close="emitClose" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeMount } from 'vue'
import { defineProps, defineEmits, computed, onMounted } from 'vue'

const props = defineProps<{
  component: any
  props?: any
  modalClass?: string
  size?: 'xxl' | 'xl' | 'l' | 'small'
  ignoreBackdropClick?: boolean
  title?: string
}>()

const emit = defineEmits(['close'])

const emitClose = () => emit('close')

const sizeClass = computed(() => {
  switch (props.size) {
    case 'xxl':
      return 'modal-xxl'
    case 'xl':
      return 'modal-xl'
    case 'l':
      return 'modal-l'
    case 'small':
      return 'modal-small'
    default:
      return 'modal-l'
  }
})

const titleClass = computed(() => {
  if (props.title) {
    return 'modal-container-title'
  }
})

function handleBackdropClick() {
  if (!props.ignoreBackdropClick) {
    emitClose()
  }
}
</script>

<style scoped lang="scss">
.container {
  display: flex;
  justify-content: center;
  padding: 20px 0px 0px 0px;
}

.modal-mask {
  position: fixed;
  z-index: 9999;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-container {
  position: relative;
  background: #fff;
  border-radius: 8px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);

  .container-component {
    width: 100%;
    display: block;
    flex-direction: column;
  }
}

/* 尺寸样式 */
.modal-xxl {
  width: 90vw;
  max-width: 1280px;
}

.modal-xl {
  width: 75vw;
  max-width: 960px;
}

.modal-l {
  width: 60vw;
  max-width: 720px;
}

.modal-small {
  width: 40vw;
  max-width: 480px;
}

.modal-container-title {
  padding-top: 56px;
}
/* modal 标题 */
.modal-title {
  position: absolute;
  top: 12px;
  left: 50%;
  padding-bottom: 10px;
  transform: translateX(-50%);
  font-size: 16px;
  color: #999;
  line-height: 1;
}
/* 关闭按钮样式 */
.modal-close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  background: transparent;
  border: none;
  font-size: 20px;
  line-height: 11px;
  cursor: pointer;
  color: #999;
  transition: color 0.2s ease;
}

.modal-close-btn:hover {
  color: #333;
}
</style>
