<template>
  <!-- 首页宠物：像素小伙伴。固定定位，平时趴在左下角，偶尔爬到菜谱卡片上卖萌 -->
  <view
    class="pet"
    :class="[`is-${state}`, { 'face-left': facing === 'left' }]"
    :style="petStyle"
    @tap="onTap"
  >
    <view class="pet-shadow"></view>

    <!-- pet-flip 只负责朝向翻转。所有状态图都预渲染，走路切帧时不临时换图，减少小程序端闪白。 -->
    <view class="pet-flip">
      <image
        v-for="src in petFrameSources"
        v-show="src === activePetSvgSrc"
        :key="src"
        class="pet-figure"
        :src="src"
        :svg="true"
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
import { computed, onBeforeUnmount, onMounted, reactive } from 'vue'
import pixelCorgi from '@/assets/pet/pixel-corgi.svg'
import pixelCorgiLounge from '@/assets/pet/pixel-corgi-lounge.svg'
import pixelCorgiWalk1 from '@/assets/pet/pixel-corgi-walk-1.svg'
import pixelCorgiWalk2 from '@/assets/pet/pixel-corgi-walk-2.svg'
import pixelTabbyCat from '@/assets/pet/pixel-tabby-cat.svg'
import pixelTabbyCatLounge from '@/assets/pet/pixel-tabby-cat-lounge.svg'
import pixelTabbyCatWalk1 from '@/assets/pet/pixel-tabby-cat-walk-1.svg'
import pixelTabbyCatWalk2 from '@/assets/pet/pixel-tabby-cat-walk-2.svg'

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
type PetKind = 'cat' | 'dog'
type Rect = { left: number; top: number; width: number; height: number; bottom: number; right: number }

// 宠物图形的逻辑尺寸（与 CSS 中 .pet 宽高一致，用于居中换算）
const PET_W = 64
const PET_H = 72
// 行走由 JS 拆成小步推进，避免一次性 transition 看起来像滑过去。
const WALK_STEP_PX = 12
const WALK_STEP_MS = 125
const WALK_FRAME_STEP_INTERVAL = 2
const WALK_MIN_STEPS = 10
const WALK_MAX_STEPS = 30
// 底部为 tab-bar 让出的空间
const BOTTOM_RESERVE = 96
// 趴菜谱时放在卡片正上方，底部轻轻搭住卡片顶边。
const CARD_TOP_OVERLAP_Y = 18

const ACTIVE_PET_KIND: PetKind = 'cat'
const PET_SVG_SRC = {
  cat: {
    normal: pixelTabbyCat,
    lounge: pixelTabbyCatLounge,
    walk: [pixelTabbyCatWalk1, pixelTabbyCatWalk2],
  },
  dog: {
    normal: pixelCorgi,
    lounge: pixelCorgiLounge,
    walk: [pixelCorgiWalk1, pixelCorgiWalk2],
  },
} satisfies Record<PetKind, { normal: string; lounge: string; walk: [string, string] }>

// 小程序端可能没有 matchMedia，取不到时默认保留动效。
const reduceMotion = (() => {
  try {
    const matchMedia = (globalThis as { matchMedia?: (query: string) => { matches: boolean } }).matchMedia
    return matchMedia ? matchMedia('(prefers-reduced-motion: reduce)').matches : false
  } catch {
    return false
  }
})()

const pet = reactive({
  state: 'idle' as PetState,
  facing: 'right' as 'left' | 'right',
  left: 0,
  top: 0,
  walkFrameIndex: 0,
  showHearts: false,
})

// 模板里直接用解构后的只读引用书写更顺手
const state = computed(() => pet.state)
const facing = computed(() => pet.facing)
const showHearts = computed(() => pet.showHearts)
const petStyle = computed(() => ({ left: `${pet.left}px`, top: `${pet.top}px` }))
const petFrameSources = computed(() => {
  const source = PET_SVG_SRC[ACTIVE_PET_KIND]
  return [source.normal, source.lounge, ...source.walk]
})
const activePetSvgSrc = computed(() => {
  const source = PET_SVG_SRC[ACTIVE_PET_KIND]
  if (pet.state === 'walking') {
    if (reduceMotion) return source.normal
    return source.walk[pet.walkFrameIndex % source.walk.length]
  }
  return pet.state === 'lounging' ? source.lounge : source.normal
})

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

async function walkTo(left: number, top: number, activeToken: number) {
  const startLeft = pet.left
  const startTop = pet.top
  const dx = left - startLeft
  const dy = top - startTop
  const distance = Math.sqrt(dx * dx + dy * dy)

  if (distance < 1) {
    moveTo(left, top)
    return true
  }

  pet.walkFrameIndex = 0
  // 按距离换算步数，并对像素坐标取整，让像素宠物更像一格格走过去。
  const stepCount = Math.min(WALK_MAX_STEPS, Math.max(WALK_MIN_STEPS, Math.ceil(distance / WALK_STEP_PX)))
  for (let i = 1; i <= stepCount; i += 1) {
    if (!alive || activeToken !== token) return false
    if (!reduceMotion) {
      pet.walkFrameIndex = Math.floor(i / WALK_FRAME_STEP_INTERVAL) % PET_SVG_SRC[ACTIVE_PET_KIND].walk.length
    }
    const progress = i / stepCount
    moveTo(Math.round(startLeft + dx * progress), Math.round(startTop + dy * progress))
    await wait(WALK_STEP_MS)
  }

  pet.walkFrameIndex = 0
  moveTo(left, top)
  return alive && activeToken === token
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
  // 宠物趴在卡片正上方，视觉重心居中，避免挡住下方菜谱文案。
  const targetLeft = target.left + (target.width - PET_W) / 2
  const targetTop = target.top - PET_H + CARD_TOP_OVERLAP_Y

  // 朝向：目标在右侧则朝右，否则朝左
  pet.facing = targetLeft + PET_W / 2 > home.left + PET_W / 2 ? 'right' : 'left'
  pet.state = 'walking'
  if (!(await walkTo(targetLeft, targetTop, t))) return

  // 趴在卡片上卖萌
  pet.state = 'lounging'
  await wait(randInt(6000, 10000))
  if (!alive || t !== token) return

  // 走回左下角
  pet.facing = 'left'
  pet.state = 'walking'
  if (!(await walkTo(home.left, home.top, t))) return

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
    if (!(await walkTo(home.left, home.top, t))) return
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
  scheduleNext()
})

onBeforeUnmount(() => {
  alive = false
  if (scheduleTimer) clearTimeout(scheduleTimer)
})
</script>

<style>
.pet {
  position: fixed;
  z-index: 16;
  width: 64px;
  height: 72px;
  /* 位置由 JS 小步更新，避免整段平滑位移产生“滑行感”。 */
  transition: none;
  will-change: left, top;
}

.pet-shadow {
  position: absolute;
  left: 13px;
  bottom: -2px;
  width: 38px;
  height: 8px;
  border-radius: 50%;
  background: rgba(159, 92, 56, 0.14);
  filter: blur(3px);
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

.pet-figure {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  transform-origin: 50% 84%;
  image-rendering: pixelated;
  animation: pet-idle 2.2s steps(2, end) infinite;
  will-change: transform;
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

/* ---------- 各状态动作：CSS 驱动像素宠物本体，位移仍由 JS 状态机控制 ---------- */

.pet.is-walking .pet-figure {
  animation: pet-walk 0.72s steps(2, end) infinite;
}

.pet.is-lounging .pet-figure {
  animation: pet-lounge 2.4s steps(2, end) infinite;
}

.pet.is-reacting .pet-figure {
  animation: pet-react 0.6s steps(2, end) 2;
}

.pet.is-walking .pet-shadow {
  animation: pet-shadow-walk 0.72s steps(2, end) infinite;
}

.pet.is-lounging .pet-shadow {
  transform: scaleX(1.12);
  opacity: 0.9;
}

.pet.is-reacting .pet-shadow {
  animation: pet-shadow-react 0.6s steps(2, end) 2;
}

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

@keyframes pet-idle {
  0%,
  100% {
    transform: translateY(0) scaleY(1);
  }
  50% {
    transform: translateY(1px) scaleY(0.98);
  }
}

@keyframes pet-walk {
  0%,
  100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-3px) rotate(2deg);
  }
}

@keyframes pet-lounge {
  0%,
  100% {
    transform: translateY(1px);
  }
  50% {
    transform: translateY(2px);
  }
}

@keyframes pet-react {
  0%,
  100% {
    transform: translateY(0) scale(1);
  }
  45% {
    transform: translateY(-10px) scale(1.04);
  }
  70% {
    transform: translateY(1px) scaleY(0.94);
  }
}

@keyframes pet-shadow-walk {
  0%,
  100% {
    transform: scaleX(1);
    opacity: 1;
  }
  50% {
    transform: scaleX(0.82);
    opacity: 0.75;
  }
}

@keyframes pet-shadow-react {
  0%,
  100% {
    transform: scaleX(1);
    opacity: 1;
  }
  45% {
    transform: scaleX(0.72);
    opacity: 0.62;
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
  /* 动效降级：宠物保持静态，但不影响位置和点击流程。 */
  .pet .pet-figure,
  .pet .pet-shadow,
  .pet .pet-zzz,
  .pet .pet-heart {
    animation: none !important;
  }
}
</style>
