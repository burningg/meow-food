<template>
  <!-- 首页宠物：橘猫厨厨。固定定位，平时趴在左下角，偶尔爬到菜谱卡片上卖萌 -->
  <view
    class="pet"
    :class="[`is-${state}`, { 'face-left': facing === 'left' }]"
    :style="petStyle"
    @tap="onTap"
  >
    <view class="pet-shadow"></view>

    <!-- pet-flip 只负责朝向翻转。逐帧图：当前状态的所有帧都渲染并堆叠，
         用 v-show 只显示当前帧——所有帧已在 DOM 解码，切帧零闪烁（小程序换 src 首帧会白屏）。 -->
    <view class="pet-flip">
      <image
        v-for="(src, i) in currentFrames"
        v-show="i === frameIndex"
        :key="src"
        class="pet-sprite"
        :src="src"
        mode="aspectFit"
      />
    </view>

    <!-- 趴着打盹时冒的 Z -->
    <text class="pet-zzz">Z</text>

    <!-- 点击开心反应：飘起的爱心 -->
    <view v-if="showHearts" class="pet-hearts">
      <text class="pet-heart pet-heart-a">❤</text>
      <text class="pet-heart pet-heart-b">❤</text>
      <text class="pet-heart pet-heart-c">❤</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import Taro from '@tarojs/taro'
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { PET_FRAMES, type PetAnim } from '@/assets/pet/frames'

const props = withDefaults(
  defineProps<{
    // 宠物会爬上去的目标卡片选择器
    cardSelector?: string
  }>(),
  {
    cardSelector: '.recent-card',
  },
)

// 预留交互钩子：父组件可监听 @tap 接喂养 / 跳转 / 宠物状态面板等逻辑
const emit = defineEmits<{ (e: 'tap'): void }>()

type PetState = 'idle' | 'walking' | 'lounging' | 'reacting'
type Rect = { left: number; top: number; width: number; height: number; bottom: number; right: number }

// 宠物图形的逻辑尺寸（与 CSS 中 .pet 宽高一致，用于居中换算）
const PET_W = 64
const PET_H = 72
// 位移过渡时长，必须和 .pet 的 transition 时长一致
const WALK_MS = 1050
// 底部为 tab-bar 让出的空间
const BOTTOM_RESERVE = 96

const pet = reactive({
  state: 'idle' as PetState,
  facing: 'right' as 'left' | 'right',
  left: 0,
  top: 0,
  showHearts: false,
})

// 模板里直接用解构后的只读引用书写更顺手
const state = computed(() => pet.state)
const facing = computed(() => pet.facing)
const showHearts = computed(() => pet.showHearts)
const petStyle = computed(() => ({ left: `${pet.left}px`, top: `${pet.top}px` }))

// 内部状态机 → 精灵动画名映射
const STATE_TO_ANIM: Record<PetState, PetAnim> = {
  idle: 'idle',
  walking: 'walk',
  lounging: 'lounge',
  reacting: 'react',
}
const anim = computed<PetAnim>(() => STATE_TO_ANIM[pet.state])
const currentFrames = computed(() => PET_FRAMES[anim.value].frames)
const frameIndex = ref(0)

// 是否禁用动效（无障碍）：停在第 0 帧，不启定时器。小程序无此媒体查询，回退为 false。
const reduceMotion = (() => {
  try {
    const mm = (globalThis as { matchMedia?: (q: string) => { matches: boolean } }).matchMedia
    return mm ? mm('(prefers-reduced-motion: reduce)').matches : false
  } catch {
    return false
  }
})()

let frameTimer: ReturnType<typeof setInterval> | null = null

// 按当前动画的 fps 重启帧定时器；状态切换时回到第 0 帧
function restartFrameTimer() {
  if (frameTimer) {
    clearInterval(frameTimer)
    frameTimer = null
  }
  frameIndex.value = 0
  if (reduceMotion) return
  const { frames, fps } = PET_FRAMES[anim.value]
  if (frames.length <= 1 || fps <= 0) return
  frameTimer = setInterval(() => {
    frameIndex.value = (frameIndex.value + 1) % frames.length
  }, Math.round(1000 / fps))
}

watch(anim, restartFrameTimer)

// 左下角主位坐标
const home = { left: 0, top: 0 }

let alive = true
// token 用于让旧的异步流程作废：任何新动作都会 +1，旧流程 await 后发现对不上就退出
let token = 0
let scheduleTimer: ReturnType<typeof setTimeout> | null = null

const wait = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms))
const randInt = (min: number, max: number) => Math.floor(min + Math.random() * (max - min))

function computeHome() {
  const info = Taro.getWindowInfo()
  const winW = info.windowWidth
  const winH = info.windowHeight
  const colW = Math.min(390, winW)
  const colLeft = (winW - colW) / 2
  home.left = colLeft + 14
  home.top = winH - BOTTOM_RESERVE - PET_H
}

function atHome() {
  return Math.abs(pet.left - home.left) < 1 && Math.abs(pet.top - home.top) < 1
}

function moveTo(left: number, top: number) {
  pet.left = left
  pet.top = top
}

// 取当前可见、且适合趴上去的菜谱卡片矩形
function getVisibleCardRects(): Promise<Rect[]> {
  return new Promise((resolve) => {
    Taro.createSelectorQuery()
      .selectAll(props.cardSelector)
      .boundingClientRect()
      .exec((res) => {
        const list = (res && (res[0] as unknown)) as Rect[] | undefined
        const rects = Array.isArray(list) ? list : []
        const winH = Taro.getWindowInfo().windowHeight
        // 只保留在视口内、不贴边、不被底部栏遮挡的卡片
        resolve(rects.filter((r) => r && r.width > 0 && r.top > 50 && r.bottom < winH - BOTTOM_RESERVE))
      })
  })
}

// 让出当前 token 给新动作，并停掉漫游计时器
function takeToken() {
  if (scheduleTimer) {
    clearTimeout(scheduleTimer)
    scheduleTimer = null
  }
  return ++token
}

// 安排下一次漫游（低频，避免打扰）
function scheduleNext() {
  if (scheduleTimer) clearTimeout(scheduleTimer)
  scheduleTimer = setTimeout(runWanderCycle, randInt(18000, 30000))
}

// 一次完整漫游：走到某张卡片 → 趴一会儿 → 走回角落
async function runWanderCycle() {
  const t = takeToken()
  const rects = await getVisibleCardRects()
  if (!alive || t !== token) return
  if (!rects.length) {
    scheduleNext()
    return
  }

  const target = rects[randInt(0, rects.length)]
  const targetLeft = target.left + (target.width - PET_W) / 2
  const targetTop = target.top - PET_H * 0.55

  // 朝向：目标在右侧则朝右，否则朝左
  pet.facing = targetLeft + PET_W / 2 > home.left + PET_W / 2 ? 'right' : 'left'
  pet.state = 'walking'
  moveTo(targetLeft, targetTop)
  await wait(WALK_MS)
  if (!alive || t !== token) return

  // 趴在卡片上卖萌
  pet.state = 'lounging'
  await wait(randInt(6000, 10000))
  if (!alive || t !== token) return

  // 走回左下角
  pet.facing = 'left'
  pet.state = 'walking'
  moveTo(home.left, home.top)
  await wait(WALK_MS)
  if (!alive || t !== token) return

  pet.state = 'idle'
  scheduleNext()
}

// 点击宠物：开心反应（蹦跳 + 爱心），随后回主位待机
async function onTap() {
  emit('tap')
  // TODO: 预留 —— 后续可在此接入喂养 / 跳转宠物页 / 弹出状态面板等逻辑

  if (pet.state === 'reacting') {
    // 连点时只刷新一次爱心，不重复进入流程
    pet.showHearts = false
    await wait(0)
    pet.showHearts = true
    return
  }

  const t = takeToken()
  pet.state = 'reacting'
  pet.showHearts = true
  await wait(1200)
  if (!alive || t !== token) return
  pet.showHearts = false

  if (!atHome()) {
    pet.facing = 'left'
    pet.state = 'walking'
    moveTo(home.left, home.top)
    await wait(WALK_MS)
    if (!alive || t !== token) return
  }

  pet.state = 'idle'
  scheduleNext()
}

// 同步初始化主位，避免首帧从 (0,0) 滑入
computeHome()
moveTo(home.left, home.top)

onMounted(() => {
  computeHome()
  moveTo(home.left, home.top)
  restartFrameTimer()
  scheduleNext()
})

onBeforeUnmount(() => {
  alive = false
  if (scheduleTimer) clearTimeout(scheduleTimer)
  if (frameTimer) clearInterval(frameTimer)
})
</script>

<style>
.pet {
  position: fixed;
  z-index: 16;
  width: 64px;
  height: 72px;
  /* 位移过渡（走路），时长需与 WALK_MS 保持一致 */
  transition: left 1.05s cubic-bezier(0.45, 0.05, 0.25, 1),
    top 1.05s cubic-bezier(0.45, 0.05, 0.25, 1);
  will-change: left, top;
}

.pet-shadow {
  position: absolute;
  left: 12px;
  bottom: -2px;
  width: 40px;
  height: 9px;
  border-radius: 50%;
  background: rgba(27, 58, 45, 0.22);
  filter: blur(2px);
}

.pet-flip {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  transition: transform 0.3s ease;
}

.pet.face-left .pet-flip {
  transform: scaleX(-1);
}

.pet-sprite {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
}

/* ---------- Z 与爱心 ---------- */
.pet-zzz {
  position: absolute;
  left: 50px;
  top: 2px;
  color: #9f8d7c;
  font-size: 13px;
  font-weight: 800;
  opacity: 0;
}

.pet-hearts {
  position: absolute;
  left: 22px;
  top: 6px;
  width: 24px;
  height: 30px;
  pointer-events: none;
}

.pet-heart {
  position: absolute;
  bottom: 0;
  color: #f06b7a;
  font-size: 12px;
  opacity: 0;
}

.pet-heart-a {
  left: 0;
  animation: pet-heart-float 1.2s ease-out forwards;
}

.pet-heart-b {
  left: 9px;
  font-size: 15px;
  animation: pet-heart-float 1.2s ease-out 0.15s forwards;
}

.pet-heart-c {
  left: 18px;
  animation: pet-heart-float 1.2s ease-out 0.3s forwards;
}

/* ---------- 各状态动作（逐帧由 JS 驱动 frameIndex，CSS 不再做身体动作） ---------- */

/* 趴着时头顶冒 Z（纯特效，保留） */
.pet.is-lounging .pet-zzz {
  animation: pet-zzz 2.6s ease-in-out infinite;
}

@keyframes pet-zzz {
  0% {
    opacity: 0;
    transform: translate(0, 4px) scale(0.8);
  }
  40% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translate(6px, -10px) scale(1.1);
  }
}

@keyframes pet-heart-float {
  0% {
    opacity: 0;
    transform: translateY(0) scale(0.6);
  }
  25% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translateY(-26px) scale(1.1);
  }
}

@media (prefers-reduced-motion: reduce) {
  /* 逐帧由 JS 在 reduceMotion 时停在第 0 帧；此处停掉残余特效动画 */
  .pet .pet-zzz,
  .pet .pet-heart {
    animation: none !important;
  }
}
</style>
