<template>
  <view
    v-if="visible"
    class="cooking-overlay"
    :catch-move="true"
    @tap="closeSheet"
    @touchmove.stop.prevent="preventPageScroll"
  >
    <view class="cooking-sheet" @tap.stop @touchmove.stop.prevent="preventPageScroll">
      <view class="cooking-handle"></view>

      <view class="cooking-header">
        <text class="cooking-title">{{ dishName || '开始烹饪' }}</text>
        <text class="cooking-progress">{{ currentIndex + 1 }} / {{ normalizedSteps.length }}</text>
      </view>

      <view
        :class="['folded-step-card', { 'folded-step-card--disabled': !previousStep }]"
        @tap="goPrevious"
      >
        <view class="folded-step-inner">
          <view class="folded-step-copy">
            <text class="folded-step-number">{{ formatStepNo(currentIndex) }}</text>
            <view class="folded-step-line"></view>
            <text class="folded-step-text">{{ previousStep?.content || '已经是第一步' }}</text>
          </view>
          <text class="folded-step-caret">⌃</text>
        </view>
      </view>

      <view :class="['current-step-card', stepMotionClass]">
        <view class="current-step-inner">
          <view class="current-step-head">
            <text class="current-step-number">{{ formatStepNo(currentIndex + 1) }}</text>
            <text class="current-step-label">当前步骤</text>
          </view>
          <text class="current-step-text">{{ currentStep?.content || '' }}</text>
          <view class="current-step-divider"></view>
          <view class="timer-rail">
            <button class="voice-pill" @tap.stop="toggleTimer">
              <text :class="['voice-dot', { 'voice-dot--ringing': alarmRinging }]">{{ timerIconText }}</text>
              <text class="voice-text">{{ timerActionText }}</text>
            </button>

            <button
              :class="['time-editor', { 'time-editor--disabled': timerRunning }]"
              :disabled="timerRunning"
              @tap.stop="openTimeEditor"
            >
              <text class="time-value">{{ formattedRemaining }}</text>
            </button>
          </view>
        </view>
      </view>

      <view
        :class="['folded-step-card', 'folded-step-card--next', { 'folded-step-card--disabled': !nextStep }]"
        @tap="goNext"
      >
        <view class="folded-step-inner">
          <view class="folded-step-copy">
            <text class="folded-step-number">{{ formatStepNo(currentIndex + 2) }}</text>
            <view class="folded-step-line"></view>
            <text class="folded-step-text">{{ nextStep?.content || '已经是最后一步' }}</text>
          </view>
          <text class="folded-step-caret">⌄</text>
        </view>
      </view>

      <view class="step-dots">
        <text
          v-for="(_, index) in normalizedSteps"
          :key="`cook-dot-${index}`"
          :class="['step-dot', { active: index === currentIndex }]"
        ></text>
      </view>

      <view class="cooking-actions">
        <button class="cooking-action cooking-action--secondary" :disabled="!previousStep" @tap.stop="goPrevious">
          ‹ 上一步
        </button>
        <button class="cooking-action cooking-action--primary" :disabled="!nextStep" @tap.stop="goNext">
          下一步 ›
        </button>
      </view>
    </view>

    <view
      v-if="timeEditorVisible"
      class="time-modal-mask"
      :catch-move="true"
      @tap.stop="closeTimeEditor"
      @touchmove.stop.prevent="preventPageScroll"
    >
      <view class="time-modal" @tap.stop @touchmove.stop>
        <text class="time-modal-title">修改计时</text>
        <picker-view class="time-picker" :value="timePickerValue" @change="handleTimePickerChange">
          <picker-view-column>
            <view v-for="hour in hourOptions" :key="`timer-hour-${hour}`" class="time-picker-item">
              <text class="time-picker-number">{{ hour }}</text>
              <text class="time-picker-unit">时</text>
            </view>
          </picker-view-column>
          <picker-view-column>
            <view v-for="minute in minuteOptions" :key="`timer-minute-${minute}`" class="time-picker-item">
              <text class="time-picker-number">{{ minute }}</text>
              <text class="time-picker-unit">分</text>
            </view>
          </picker-view-column>
          <picker-view-column>
            <view v-for="second in secondOptions" :key="`timer-second-${second}`" class="time-picker-item">
              <text class="time-picker-number">{{ second }}</text>
              <text class="time-picker-unit">秒</text>
            </view>
          </picker-view-column>
        </picker-view>
        <view class="time-modal-actions">
          <button class="time-modal-button time-modal-button--ghost" @tap.stop="closeTimeEditor">取消</button>
          <button class="time-modal-button time-modal-button--primary" @tap.stop="confirmTimeEditor">确定</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import Taro from '@tarojs/taro'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { resolveSharePath } from '@/lib/navigation'
import { CookingTimerService } from '@/services/cooking-timer-service'
import type { StepItem } from '@/services/food-service'

const DEFAULT_SECONDS = 120
const MIN_SECONDS = 1
const MAX_SECONDS = 4 * 60 * 60
const TIMER_AUDIO_SRC = '/audio/timer-done.wav'
const ALARM_AUTO_STOP_MS = 10 * 1000
const SUBSCRIBE_TEMPLATE_ID = '6uWQ2nw0Wr1R0Tlpm1kxYf2G6NEokTXxBFNAw7jk34E'
const cookingTimerService = new CookingTimerService()

type SubscribeMessageResult = {
  errMsg?: string
  [templateId: string]: string | undefined
}

type SubscribeMessageApi = (options: {
  tmplIds: string[]
  success?: (result: SubscribeMessageResult) => void
  fail?: (error: unknown) => void
}) => Promise<SubscribeMessageResult> | void

const props = withDefaults(
  defineProps<{
    visible: boolean
    dishId: string
    dishName: string
    steps: StepItem[]
  }>(),
  {
    visible: false,
    dishId: '',
    dishName: '',
    steps: () => [],
  },
)

const emit = defineEmits<{
  close: []
}>()

const currentIndex = ref(0)
const stepSeconds = ref<number[]>([])
const remainingSeconds = ref(DEFAULT_SECONDS)
const timerRunning = ref(false)
const timerScheduling = ref(false)
const alarmRinging = ref(false)
const timeEditorVisible = ref(false)
const timePickerValue = ref([0, 2, 0])
const stepMotion = ref<'from-prev' | 'from-next' | ''>('')
let timerId: ReturnType<typeof setInterval> | null = null
let backendTimerId: string | null = null
let stepMotionId: ReturnType<typeof setTimeout> | null = null
let alarmAutoStopId: ReturnType<typeof setTimeout> | null = null
let alarmAudio: ReturnType<typeof Taro.createInnerAudioContext> | null = null
const hourOptions = Array.from({ length: MAX_SECONDS / 3600 + 1 }, (_, index) => index)
const minuteOptions = Array.from({ length: 60 }, (_, index) => index)
const secondOptions = Array.from({ length: 60 }, (_, index) => index)

const normalizedSteps = computed(() =>
  props.steps
    .map((step, index) => ({
      stepNo: step.stepNo ?? index + 1,
      content: String(step.content || '').trim(),
    }))
    .filter((step) => step.content),
)

const previousStep = computed(() => normalizedSteps.value[currentIndex.value - 1])
const currentStep = computed(() => normalizedSteps.value[currentIndex.value])
const nextStep = computed(() => normalizedSteps.value[currentIndex.value + 1])

const formattedRemaining = computed(() => formatSeconds(remainingSeconds.value))

const stepMotionClass = computed(() => {
  if (stepMotion.value === 'from-prev') return 'current-step-card--from-prev'
  if (stepMotion.value === 'from-next') return 'current-step-card--from-next'
  return ''
})

const timerActionText = computed(() => {
  if (timerScheduling.value) return '启动中'
  if (alarmRinging.value || timerRunning.value) return '停止'
  return '开始'
})

const timerIconText = computed(() => {
  if (alarmRinging.value || timerRunning.value) return '■'
  return '▶'
})

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      setupTimers()
      return
    }
    cleanupTimer()
    detachBackendTimer()
    cleanupStepMotion()
    stopAlarm()
    closeTimeEditor()
  },
)

watch(
  () => props.steps,
  () => {
    if (!props.visible) return
    setupTimers()
  },
  { deep: true },
)

onBeforeUnmount(() => {
  cleanupTimer()
  detachBackendTimer()
  cleanupStepMotion()
  stopAlarm()
})

function setupTimers() {
  const timers = normalizedSteps.value.map((step) => parseStepSeconds(step.content))
  stepSeconds.value = timers.length ? timers : [DEFAULT_SECONDS]
  currentIndex.value = clampIndex(currentIndex.value)
  resetTimerForStep(currentIndex.value)
}

function closeSheet() {
  cleanupTimer()
  detachBackendTimer()
  cleanupStepMotion()
  stopAlarm()
  closeTimeEditor()
  emit('close')
}

function preventPageScroll(event: any) {
  event?.stopPropagation?.()
  event?.preventDefault?.()
}

function goPrevious() {
  if (!previousStep.value) return
  switchStep(currentIndex.value - 1)
}

function goNext() {
  if (!nextStep.value) return
  switchStep(currentIndex.value + 1)
}

function switchStep(index: number) {
  const nextIndex = clampIndex(index)
  if (nextIndex === currentIndex.value) return
  triggerStepMotion(nextIndex > currentIndex.value ? 'from-next' : 'from-prev')
  currentIndex.value = nextIndex
  resetTimerForStep(nextIndex)
}

function resetTimerForStep(index: number) {
  cleanupTimer()
  detachBackendTimer()
  stopAlarm()
  closeTimeEditor()
  remainingSeconds.value = stepSeconds.value[index] ?? DEFAULT_SECONDS
}

async function toggleTimer() {
  if (timerScheduling.value) return

  if (alarmRinging.value) {
    stopAlarm()
    remainingSeconds.value = stepSeconds.value[currentIndex.value] ?? DEFAULT_SECONDS
    return
  }

  if (timerRunning.value) {
    void cancelBackendTimer()
    cleanupTimer()
    remainingSeconds.value = stepSeconds.value[currentIndex.value] ?? DEFAULT_SECONDS
    return
  }

  if (remainingSeconds.value <= 0) {
    remainingSeconds.value = stepSeconds.value[currentIndex.value] ?? DEFAULT_SECONDS
  }
  await createBackendTimer()
  startTimer()
}

async function createBackendTimer() {
  const subscribed = await requestTimerSubscribe()
  if (!subscribed) {
    void Taro.showToast({
      title: '烹饪计时提醒',
      icon: 'none',
      duration: 1600,
    })
    return
  }

  timerScheduling.value = true
  try {
    const { data } = await cookingTimerService.createTimer({
      dishName: props.dishName || '烹饪计时',
      stepText: currentStep.value?.content || '',
      seconds: remainingSeconds.value,
      page: props.dishId
        ? resolveSharePath({ name: 'dish-detail', params: { id: props.dishId } })
        : undefined,
    })
    backendTimerId = data.timerId
  } catch (error) {
    backendTimerId = null
    void Taro.showToast({
      title: '烹饪提醒启动失败',
      icon: 'none',
      duration: 1800,
    })
  } finally {
    timerScheduling.value = false
  }
}

async function requestTimerSubscribe() {
  const requestSubscribeMessage = resolveSubscribeMessageApi()
  if (typeof requestSubscribeMessage !== 'function') {
    return false
  }
  try {
    const result = await new Promise<SubscribeMessageResult>((resolve, reject) => {
      const maybePromise = requestSubscribeMessage({
        tmplIds: [SUBSCRIBE_TEMPLATE_ID],
        success: resolve,
        fail: reject,
      })
      if (maybePromise && typeof maybePromise.then === 'function') {
        maybePromise.then(resolve).catch(reject)
      }
    })
    return result?.[SUBSCRIBE_TEMPLATE_ID] === 'accept'
  } catch (error) {
    console.log(error)
    return false
  }
}

function resolveSubscribeMessageApi() {
  const taroApi = (Taro as unknown as { requestSubscribeMessage?: SubscribeMessageApi }).requestSubscribeMessage
  if (typeof taroApi === 'function') {
    return taroApi
  }
  const wechatApi = (globalThis as unknown as { wx?: { requestSubscribeMessage?: SubscribeMessageApi } }).wx
    ?.requestSubscribeMessage
  if (typeof wechatApi !== 'function') {
    return undefined
  }
  return (options: Parameters<SubscribeMessageApi>[0]) =>
    wechatApi({
      ...options,
      tmplIds: [SUBSCRIBE_TEMPLATE_ID],
    })
}

async function cancelBackendTimer() {
  const activeTimerId = backendTimerId
  backendTimerId = null
  if (!activeTimerId) return
  try {
    await cookingTimerService.cancelTimer(activeTimerId)
  } catch (error) {
    // 停止本地计时优先，后端取消失败只影响离开页面提醒兜底。
  }
}

function detachBackendTimer() {
  backendTimerId = null
}

function startTimer() {
  cleanupTimer()
  timerRunning.value = true
  timerId = setInterval(() => {
    remainingSeconds.value -= 1
    if (remainingSeconds.value <= 0) {
      remainingSeconds.value = 0
      finishTimer()
    }
  }, 1000)
}

function finishTimer() {
  cleanupTimer()
  backendTimerId = null
  playAlarm()
  void Taro.vibrateLong().catch(() => undefined)
  void Taro.showToast({
    title: '计时结束',
    icon: 'none',
    duration: 1800,
  })
}

function cleanupTimer() {
  if (timerId) {
    clearInterval(timerId)
    timerId = null
  }
  timerRunning.value = false
}

function triggerStepMotion(direction: 'from-prev' | 'from-next') {
  cleanupStepMotion()
  // 切换步骤时让新步骤像折叠卡片一样滚入中间，再展开成当前步骤卡。
  stepMotion.value = direction
  stepMotionId = setTimeout(() => {
    stepMotion.value = ''
    stepMotionId = null
  }, 320)
}

function cleanupStepMotion() {
  if (stepMotionId) {
    clearTimeout(stepMotionId)
    stepMotionId = null
  }
  stepMotion.value = ''
}

function openTimeEditor() {
  if (timerRunning.value) return
  stopAlarm()
  timePickerValue.value = secondsToPickerValue(remainingSeconds.value)
  timeEditorVisible.value = true
}

function closeTimeEditor() {
  timeEditorVisible.value = false
}

function handleTimePickerChange(event: any) {
  const value = event?.detail?.value
  if (!Array.isArray(value)) return
  timePickerValue.value = [
    clampPickerIndex(Number(value[0]), hourOptions.length - 1),
    clampPickerIndex(Number(value[1]), minuteOptions.length - 1),
    clampPickerIndex(Number(value[2]), secondOptions.length - 1),
  ]
}

function confirmTimeEditor() {
  const nextSeconds =
    timePickerValue.value[0] * 3600 + timePickerValue.value[1] * 60 + timePickerValue.value[2]
  if (nextSeconds <= 0) {
    void Taro.showToast({
      title: '至少要1秒哦～',
      icon: 'none',
      duration: 1600,
    })
    return
  }
  // 手动修改只影响当前步骤本次弹层状态，不写回菜谱步骤。
  const safeSeconds = clampSeconds(nextSeconds)
  remainingSeconds.value = safeSeconds
  stepSeconds.value[currentIndex.value] = safeSeconds
  closeTimeEditor()
}

function playAlarm() {
  stopAlarm()
  alarmRinging.value = true
  try {
    alarmAudio = Taro.createInnerAudioContext()
    alarmAudio.src = TIMER_AUDIO_SRC
    alarmAudio.loop = true
    alarmAudio.play()
    // 闹铃只做结束提醒，避免超过 10 秒后继续打扰厨房操作。
    alarmAutoStopId = setTimeout(() => {
      stopAlarm()
    }, ALARM_AUTO_STOP_MS)
  } catch (error) {
    alarmAudio = null
    alarmRinging.value = false
    cleanupAlarmAutoStop()
  }
}

function stopAlarm() {
  cleanupAlarmAutoStop()
  alarmRinging.value = false
  if (!alarmAudio) return
  try {
    alarmAudio.stop()
    alarmAudio.destroy()
  } catch (error) {
    // 音频兜底失败不影响计时主流程，振动和提示仍然可用。
  } finally {
    alarmAudio = null
  }
}

function cleanupAlarmAutoStop() {
  if (alarmAutoStopId) {
    clearTimeout(alarmAutoStopId)
    alarmAutoStopId = null
  }
}

function clampIndex(index: number) {
  const maxIndex = Math.max(normalizedSteps.value.length - 1, 0)
  return Math.min(Math.max(index, 0), maxIndex)
}

function clampSeconds(seconds: number) {
  return Math.min(MAX_SECONDS, Math.max(MIN_SECONDS, Math.round(seconds)))
}

function formatStepNo(stepNo: number) {
  return String(Math.max(stepNo, 1)).padStart(2, '0')
}

function formatSeconds(seconds: number) {
  const safeSeconds = Math.max(0, seconds)
  const hours = Math.floor(safeSeconds / 3600)
  const minutes = Math.floor((safeSeconds % 3600) / 60)
  const restSeconds = safeSeconds % 60
  if (hours) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${String(restSeconds).padStart(2, '0')}`
  }
  return `${String(minutes).padStart(2, '0')}:${String(restSeconds).padStart(2, '0')}`
}

function secondsToPickerValue(seconds: number) {
  const safeSeconds = clampSeconds(seconds)
  const hours = Math.floor(safeSeconds / 3600)
  const minutes = Math.floor((safeSeconds % 3600) / 60)
  const restSeconds = safeSeconds % 60
  return [hours, minutes, restSeconds]
}

function clampPickerIndex(index: number, maxIndex: number) {
  if (!Number.isFinite(index)) return 0
  return Math.min(Math.max(Math.round(index), 0), maxIndex)
}

function parseStepSeconds(content: string) {
  const text = normalizeTimeText(content)
  const candidates = collectTimeCandidates(text)
  if (!candidates.length) return DEFAULT_SECONDS
  candidates.sort((left, right) => left.index - right.index || right.priority - left.priority)
  return clampSeconds(candidates[0].seconds)
}

type TimeCandidate = {
  index: number
  seconds: number
  priority: number
}

const quantityPattern = String.raw`(?:\d+(?:\.\d+)?|[零〇一二两三四五六七八九十百]+|半)`

function collectTimeCandidates(text: string) {
  const candidates: TimeCandidate[] = []
  collectRangeCandidates(text, candidates)
  collectHourMinuteCandidates(text, candidates)
  collectMinuteSecondCandidates(text, candidates)
  collectSingleUnitCandidates(text, candidates)
  return candidates.filter((candidate) => candidate.seconds > 0)
}

function collectRangeCandidates(text: string, candidates: TimeCandidate[]) {
  const rangeReg = new RegExp(
    `(${quantityPattern})\\s*(?:-|－|–|—|~|～|到|至)\\s*(${quantityPattern})\\s*(小时|小時|分钟|分鐘|分|min|m|秒|s|h)`,
    'gi',
  )
  for (const match of text.matchAll(rangeReg)) {
    const seconds = unitSeconds(match[3], parseQuantity(match[2]))
    candidates.push({ index: match.index ?? 0, seconds, priority: 4 })
  }
}

function collectHourMinuteCandidates(text: string, candidates: TimeCandidate[]) {
  const hourMinuteReg = new RegExp(
    `(${quantityPattern})\\s*(?:个)?\\s*(小时|小時|h)\\s*(?:(${quantityPattern})\\s*(分钟|分鐘|分|min|m))?`,
    'gi',
  )
  for (const match of text.matchAll(hourMinuteReg)) {
    const hourSeconds = unitSeconds(match[2], parseQuantity(match[1]))
    const minuteSeconds = match[3] ? unitSeconds(match[4], parseQuantity(match[3])) : 0
    candidates.push({ index: match.index ?? 0, seconds: hourSeconds + minuteSeconds, priority: 3 })
  }
}

function collectMinuteSecondCandidates(text: string, candidates: TimeCandidate[]) {
  const minuteSecondReg = new RegExp(
    `(${quantityPattern})\\s*(分钟|分鐘|分|min|m)\\s*(${quantityPattern})\\s*(秒|s)`,
    'gi',
  )
  for (const match of text.matchAll(minuteSecondReg)) {
    const minuteSeconds = unitSeconds(match[2], parseQuantity(match[1]))
    const secondSeconds = unitSeconds(match[4], parseQuantity(match[3]))
    candidates.push({ index: match.index ?? 0, seconds: minuteSeconds + secondSeconds, priority: 3 })
  }
}

function collectSingleUnitCandidates(text: string, candidates: TimeCandidate[]) {
  const singleReg = new RegExp(`(${quantityPattern})\\s*(小时|小時|分钟|分鐘|分|min|m|秒|s|h)`, 'gi')
  for (const match of text.matchAll(singleReg)) {
    candidates.push({
      index: match.index ?? 0,
      seconds: unitSeconds(match[2], parseQuantity(match[1])),
      priority: 1,
    })
  }
}

function normalizeTimeText(content: string) {
  return content
    .replace(/[０-９]/g, (char) => String.fromCharCode(char.charCodeAt(0) - 0xfee0))
    .replace(/\s+/g, '')
}

function parseQuantity(value: string) {
  if (!value) return 0
  if (value === '半') return 0.5
  const numericValue = Number(value)
  if (!Number.isNaN(numericValue)) return numericValue
  return parseChineseNumber(value)
}

function unitSeconds(unit: string, value: number) {
  const normalizedUnit = unit.toLowerCase()
  if (['小时', '小時', 'h'].includes(normalizedUnit)) return value * 3600
  if (['分钟', '分鐘', '分', 'min', 'm'].includes(normalizedUnit)) return value * 60
  return value
}

function parseChineseNumber(text: string) {
  const digitMap: Record<string, number> = {
    零: 0,
    '〇': 0,
    一: 1,
    二: 2,
    两: 2,
    三: 3,
    四: 4,
    五: 5,
    六: 6,
    七: 7,
    八: 8,
    九: 9,
  }
  let result = 0
  let current = 0
  for (const char of text) {
    if (char === '百') {
      result += (current || 1) * 100
      current = 0
      continue
    }
    if (char === '十') {
      result += (current || 1) * 10
      current = 0
      continue
    }
    current = digitMap[char] ?? current
  }
  return result + current
}
</script>

<style>
.cooking-overlay {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(21, 21, 21, 0.45);
}

.cooking-sheet {
  box-sizing: border-box;
  width: min(390px, 100vw);
  height: 92vh;
  max-height: none;
  overflow: hidden;
  border: 1px solid #efe3d1;
  border-radius: 24px 24px 0 0;
  background: #fff;
  padding: 12px 18px calc(30px + constant(safe-area-inset-bottom));
  padding-bottom: calc(30px + env(safe-area-inset-bottom));
  box-shadow: 0 -10px 36px rgba(138, 78, 41, 0.1);
}

.cooking-handle {
  width: 42px;
  height: 4px;
  margin: 0 auto 18px;
  border-radius: 999px;
  background: #e6dbcf;
}

.cooking-header,
.folded-step-inner,
.folded-step-copy,
.timer-rail,
.cooking-actions,
.step-dots {
  display: flex;
  align-items: center;
}

.cooking-header {
  justify-content: space-between;
  margin-bottom: 14px;
}

.cooking-title {
  flex: 1;
  overflow: hidden;
  color: var(--text-main);
  font-size: 21px;
  font-weight: 800;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cooking-progress {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  height: 28px;
  border-radius: 999px;
  background: var(--accent-dark);
  color: #fff;
  font-size: var(--text-xs);
  font-weight: 800;
}

.folded-step-card {
  height: 88px;
  border: 1px solid #ece7df;
  border-radius: 22px;
  background: #f1f0ee;
  padding: 5px;
  box-shadow: none;
}

.folded-step-card--next {
  margin-top: 14px;
}

.folded-step-card--disabled {
  opacity: 0.58;
}

.folded-step-inner {
  height: 100%;
  justify-content: space-between;
  border-radius: 18px;
  background: #f8f7f5;
  padding: 0 13px;
}

.folded-step-copy {
  min-width: 0;
  flex: 1;
  gap: 10px;
}

.folded-step-number {
  flex: 0 0 auto;
  color: #a38a77;
  font-size: var(--text-xs);
  font-weight: 800;
}

.folded-step-line {
  width: 1px;
  height: 34px;
  flex: 0 0 auto;
  background: #e2ddd5;
}

.folded-step-text {
  display: -webkit-box;
  overflow: hidden;
  flex: 1;
  color: #7f7b75;
  font-size: var(--text-sm);
  line-height: 1.25;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.folded-step-caret {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  border-radius: 999px;
  background: #efede9;
  color: #9a8778;
  font-size: 18px;
  font-weight: 900;
}

.current-step-card {
  height: 292px;
  margin-top: 16px;
  border: 2px solid #b36a42;
  border-radius: 32px;
  background: #fff3e4;
  padding: 6px;
  box-shadow: 0 20px 42px rgba(138, 78, 41, 0.16);
  transform-origin: center;
}

.current-step-card--from-prev {
  animation: cook-step-expand-from-top 320ms cubic-bezier(0.2, 0.86, 0.28, 1) both;
}

.current-step-card--from-next {
  animation: cook-step-expand-from-bottom 320ms cubic-bezier(0.2, 0.86, 0.28, 1) both;
}

.current-step-card--from-prev .current-step-inner,
.current-step-card--from-next .current-step-inner {
  animation: cook-step-inner-reveal 260ms ease-out 60ms both;
}

.current-step-inner {
  display: flex;
  height: 100%;
  flex-direction: column;
  justify-content: space-between;
  border-radius: 27px;
  background: #fff;
  padding: 18px;
}

.current-step-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.current-step-number {
  display: flex;
  width: 48px;
  height: 34px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--accent-dark);
  color: #fff;
  font-size: 18px;
  font-weight: 900;
  line-height: 1;
}

.current-step-label {
  color: var(--accent-dark);
  font-size: var(--text-sm);
  font-weight: 900;
  line-height: 1;
}

.current-step-text {
  display: -webkit-box;
  overflow: hidden;
  color: var(--text-main);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 7;
}

.current-step-divider {
  height: 1px;
  background: #efe3d1;
}

.timer-rail {
  justify-content: space-between;
  gap: 8px;
}

.voice-pill,
.time-editor {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  border-radius: 999px;
}

.voice-pill {
  width: 128px;
  gap: 7px;
  background: var(--accent-soft);
}

.voice-dot {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: #346538;
  color: #fff;
  font-size: 9px;
  line-height: 1;
  box-shadow: 0 3px 8px rgba(52, 101, 56, 0.14);
}

.voice-dot--ringing {
  background: var(--accent-dark);
}

.voice-text {
  color: #346538;
  font-size: var(--text-sm);
  font-weight: 800;
}

.time-editor {
  flex: 1;
  min-width: 0;
  gap: 6px;
  border: 1px solid #efe3d1;
  background: #fff8ef;
  padding: 0 8px;
}

.time-editor--disabled {
  opacity: 0.45;
}

.time-label,
.time-hint {
  flex: 0 0 auto;
  color: #7b6a5d;
  font-size: 10px;
  font-weight: 800;
}

.time-value {
  flex: 1;
  color: var(--accent-dark);
  font-size: var(--text-sm);
  font-weight: 900;
  line-height: 1;
  text-align: center;
}

.step-dots {
  justify-content: center;
  gap: 6px;
  margin: 14px 0 10px;
}

.step-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #e6dbcf;
}

.step-dot.active {
  width: 24px;
  background: var(--accent-dark);
}

.cooking-actions {
  gap: 12px;
  padding-bottom: 4px;
}

.cooking-action {
  display: flex;
  height: 50px;
  flex: 1;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 800;
  line-height: 1;
  text-align: center;
}

.cooking-action--secondary {
  border: 1px solid #efe3d1;
  background: #f4eee7;
  color: #7b6a5d;
}

.cooking-action--primary {
  background: var(--accent-dark);
  color: #fff;
  box-shadow: 0 10px 22px rgba(159, 92, 56, 0.16);
}

.cooking-action[disabled] {
  opacity: 1;
  box-shadow: none;
}

.cooking-action--secondary[disabled] {
  border-color: #eadfd4;
  background: #faf6f1;
  color: #9f6b4f;
}

.cooking-action--primary[disabled] {
  background: #ead8cc;
  color: #8c5638;
}

.time-modal-mask {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(21, 21, 21, 0.28);
}

.time-modal {
  width: 310px;
  border: 1px solid #efe3d1;
  border-radius: 20px;
  background: #fff;
  padding: 18px;
  box-shadow: 0 18px 42px rgba(27, 58, 45, 0.14);
}

.time-modal-title {
  display: block;
  color: var(--text-main);
  font-size: var(--title-md);
  font-weight: 900;
  text-align: center;
}

.time-modal-helper {
  display: block;
  margin-top: 8px;
  color: var(--text-muted);
  font-size: var(--text-xs);
  line-height: 1.45;
  text-align: center;
}

.time-picker {
  width: 100%;
  height: 178px;
  box-sizing: border-box;
  margin-top: 14px;
  border: 1px solid #efe3d1;
  border-radius: 14px;
  background: #fbf8f4;
  overflow: hidden;
}

.time-picker-item {
  display: flex;
  height: 44px;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.time-picker-number {
  min-width: 24px;
  color: var(--text-main);
  font-size: 20px;
  font-weight: 900;
  line-height: 1;
  text-align: right;
}

.time-picker-unit {
  color: #7b6a5d;
  font-size: var(--text-xs);
  font-weight: 800;
  line-height: 1;
}

.time-modal-actions {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.time-modal-button {
  display: flex;
  height: 44px;
  flex: 1;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  font-size: var(--text-sm);
  font-weight: 800;
}

.time-modal-button--ghost {
  background: #f4eee7;
  color: #7b6a5d;
}

.time-modal-button--primary {
  background: var(--accent-dark);
  color: #fff;
}

@keyframes cook-step-expand-from-top {
  0% {
    opacity: 0.7;
    transform: translateY(-42px) scale(0.94);
  }
  58% {
    opacity: 1;
    transform: translateY(3px) scale(1.012);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes cook-step-expand-from-bottom {
  0% {
    opacity: 0.7;
    transform: translateY(42px) scale(0.94);
  }
  58% {
    opacity: 1;
    transform: translateY(-3px) scale(1.012);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes cook-step-inner-reveal {
  0% {
    opacity: 0.82;
    transform: scaleY(0.86);
  }
  100% {
    opacity: 1;
    transform: scaleY(1);
  }
}
</style>
