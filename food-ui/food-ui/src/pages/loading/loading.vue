<template>
  <div ref="pageRef" class="loading-page">
    <div class="loading-page__glow loading-page__glow--top"></div>
    <div class="loading-page__glow loading-page__glow--bottom"></div>
    <span class="loading-page__spark loading-page__spark--warm"></span>
    <span class="loading-page__spark loading-page__spark--sage"></span>
    <span class="loading-page__spark loading-page__spark--cream"></span>

    <main class="loading-page__content">
      <div ref="emblemRef" class="loading-page__emblem">
        <span>🍲</span>
      </div>

      <div class="loading-page__copy">
        <h1 ref="titleRef" class="loading-page__title">
          <span
            v-for="(char, index) in titleLeadChars"
            :key="`${char}-${index}`"
            class="loading-page__title-char loading-page__title-char--lead"
          >
            {{ char === ' ' ? '\u00A0' : char }}
          </span>
          <span class="loading-page__title-meow">
            <span
              v-for="(letter, index) in meowLetters"
              :key="`${letter.char}-${index}`"
              class="loading-page__title-char loading-page__title-char--meow"
            >
              {{ letter.char }}
            </span>
          </span>
          <span
            v-for="(char, index) in titleTailChars"
            :key="`${char}-${index}`"
            class="loading-page__title-char loading-page__title-char--tail"
          >
            {{ char === ' ' ? '\u00A0' : char }}
          </span>
        </h1>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { gsap } from 'gsap'
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const pageRef = ref<HTMLElement | null>(null)
const emblemRef = ref<HTMLElement | null>(null)
const titleRef = ref<HTMLElement | null>(null)
const titleLeadChars = Array.from('欢迎来到')
const titleTailChars = Array.from('餐厅')
const meowLetters = [
  { char: 'm', settleY: 6 },
  { char: 'e', settleY: -2 },
  { char: 'o', settleY: 8 },
  { char: 'w', settleY: 1 },
]

let animationContext: gsap.Context | undefined
let redirectTimer: number | undefined

onMounted(() => {
  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

  if (!prefersReducedMotion && pageRef.value) {
    animationContext = gsap.context(() => {
      const titleChars = gsap.utils.toArray<HTMLElement>(
        '.loading-page__title-char--lead, .loading-page__title-char--tail',
      )
      const meowChars = gsap.utils.toArray<HTMLElement>('.loading-page__title-char--meow')

      gsap.set(titleRef.value, {
        autoAlpha: 1,
      })
      gsap.set(titleChars, {
        y: 20,
        scale: 0.82,
        autoAlpha: 0,
        transformOrigin: '50% 100%',
      })
      gsap.set(meowChars, {
        y: -76,
        scale: 0.72,
        autoAlpha: 0,
        rotate: (index) => [-9, 7, -6, 5][index] ?? 0,
        transformOrigin: '50% 100%',
      })
      gsap.set(emblemRef.value, {
        y: 14,
        scale: 0.94,
        autoAlpha: 0,
      })

      gsap.timeline().to(
        emblemRef.value,
        {
          y: 0,
          scale: 1,
          autoAlpha: 1,
          duration: 0.56,
          ease: 'back.out(1.6)',
        },
        0,
      )
      gsap.to(emblemRef.value, {
        y: -10,
        scale: 1.05,
        duration: 0.65,
        ease: 'sine.inOut',
        repeat: -1,
        yoyo: true,
        delay: 0.56,
      })
      gsap.to(titleChars, {
        keyframes: [
          { y: -14, scale: 1.08, autoAlpha: 1, duration: 0.18, ease: 'power2.out' },
          { y: 0, scale: 0.96, duration: 0.14, ease: 'power2.in' },
          { y: -4, scale: 1.02, duration: 0.1, ease: 'sine.out' },
          { y: 0, scale: 1, duration: 0.16, ease: 'bounce.out' },
        ],
        stagger: 0.08,
        delay: 0.22,
      })
      gsap.to(meowChars, {
        keyframes: [
          {
            y: (index) => meowLetters[index]?.settleY ?? 0,
            scale: 1.08,
            rotate: 0,
            autoAlpha: 1,
            duration: 0.28,
            ease: 'power2.in',
          },
          {
            y: (index) => (meowLetters[index]?.settleY ?? 0) - 16,
            scale: 0.96,
            duration: 0.16,
            ease: 'power2.out',
          },
          {
            y: (index) => (meowLetters[index]?.settleY ?? 0) + 7,
            scaleY: 0.95,
            scaleX: 1.03,
            duration: 0.12,
            ease: 'power2.in',
          },
          {
            y: (index) => (meowLetters[index]?.settleY ?? 0) - 7,
            scaleX: 0.98,
            scaleY: 1.02,
            duration: 0.11,
            ease: 'sine.out',
          },
          {
            y: (index) => meowLetters[index]?.settleY ?? 0,
            scale: 1,
            duration: 0.16,
            ease: 'bounce.out',
          },
        ],
        stagger: 0.08,
        delay: 0.52,
      })
    }, pageRef.value)
  }

  redirectTimer = window.setTimeout(() => {
    router.replace({ name: 'home' })
  }, 3000)
})

onUnmounted(() => {
  if (redirectTimer) {
    window.clearTimeout(redirectTimer)
  }
  animationContext?.revert()
})
</script>

<style scoped>
.loading-page {
  position: relative;
  width: min(390px, 100vw);
  min-height: 100vh;
  margin: 0 auto;
  overflow: hidden;
  background: linear-gradient(180deg, #fff5ea 0%, #f7f6f3 38%, #f2f7ee 100%);
  padding: 14px 24px 64px;
}

.loading-page__content,
.loading-page__copy {
  display: flex;
}

.loading-page__content {
  position: relative;
  z-index: 2;
  min-height: 100vh;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 20px;
  padding: 0 4px 64px;
}

.loading-page__emblem {
  width: 120px;
  height: 120px;
  border-radius: 36px;
  display: grid;
  place-items: center;
  background: linear-gradient(180deg, #fff1dd 0%, #f4d5aa 100%);
  box-shadow: 0 12px 32px rgba(200, 135, 59, 0.1);
}

.loading-page__emblem span {
  color: #7a401d;
  font-size: 44px;
  line-height: 1;
}

.loading-page__copy {
  width: 100%;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.loading-page__title {
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: flex-end;
  gap: 1px;
}

.loading-page__title {
  color: #3e2a1f;
  font-family: 'Noto Serif SC', var(--font-serif), serif;
  font-size: 30px;
  font-weight: 600;
  line-height: 1.2;
  max-width: 100%;
}

.loading-page__title-meow {
  display: inline-flex;
  align-items: flex-end;
  gap: 1px;
  margin: 0 3px;
}

.loading-page__title-char {
  display: inline-block;
  will-change: transform, opacity;
}

.loading-page__glow,
.loading-page__spark {
  position: absolute;
  pointer-events: none;
}

.loading-page__glow {
  border-radius: 999px;
}

.loading-page__glow--top {
  top: 88px;
  right: -16px;
  width: 154px;
  height: 154px;
  background: radial-gradient(circle, rgba(245, 179, 106, 0.33) 0%, rgba(245, 179, 106, 0) 72%);
}

.loading-page__glow--bottom {
  left: -84px;
  bottom: 36px;
  width: 168px;
  height: 168px;
  background: radial-gradient(circle, rgba(141, 176, 125, 0.2) 0%, rgba(141, 176, 125, 0) 72%);
}

.loading-page__spark {
  border-radius: 999px;
}

.loading-page__spark--warm {
  top: 154px;
  left: 58px;
  width: 12px;
  height: 12px;
  background: #d98a41;
}

.loading-page__spark--sage {
  top: 238px;
  right: 64px;
  width: 8px;
  height: 8px;
  background: #6c9667;
}

.loading-page__spark--cream {
  right: 74px;
  bottom: 168px;
  width: 16px;
  height: 16px;
  background: #f1d8b8;
}
</style>
