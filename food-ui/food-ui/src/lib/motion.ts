import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

export type PageTransition = 'tab' | 'forward'

type MotionOptions = {
  reducedMotion?: boolean
}

type StaggerOptions = MotionOptions & {
  y?: number
  x?: number
  duration?: number
  stagger?: number
  delay?: number
  ease?: string
  clearProps?: string
}

type RevealOptions = MotionOptions & {
  y?: number
  duration?: number
  stagger?: number
  start?: string
}

type PressOptions = {
  activeScale?: number
  idleY?: number
}

export function runScopedMotion(
  root: Element,
  setup: (context: MotionOptions) => void | (() => void),
) {
  let teardown: VoidFunction | undefined
  const motionContext = gsap.context(() => {
    const mm = gsap.matchMedia()
    mm.add(
      {
        reduce: '(prefers-reduced-motion: reduce)',
      },
      (context) => {
        teardown = setup({
          reducedMotion: Boolean(context.conditions?.reduce),
        }) || undefined
        return () => {
          teardown?.()
        }
      },
    )
    return () => {
      teardown?.()
      mm.revert()
    }
  }, root)

  return () => {
    motionContext.revert()
  }
}

export function animatePageIn(
  element: Element,
  transition: PageTransition = 'tab',
  { reducedMotion = false }: MotionOptions = {},
) {
  if (reducedMotion) {
    gsap.set(element, { autoAlpha: 1, clearProps: 'all' })
    return
  }

  const fromVars =
    transition === 'forward'
      ? { autoAlpha: 0, x: 22, y: 6, filter: 'blur(8px)' }
      : { autoAlpha: 0, y: 18, scale: 0.985, filter: 'blur(10px)' }

  gsap.fromTo(
    element,
    fromVars,
    {
      autoAlpha: 1,
      x: 0,
      y: 0,
      scale: 1,
      filter: 'blur(0px)',
      duration: transition === 'forward' ? 0.34 : 0.3,
      ease: transition === 'forward' ? 'power2.out' : 'power3.out',
      clearProps: 'transform,filter,opacity,visibility',
    },
  )
}

export function animateStagger(
  targets: ArrayLike<Element> | Element[],
  {
    reducedMotion = false,
    y = 18,
    x = 0,
    duration = 0.42,
    stagger = 0.08,
    delay = 0,
    ease = 'power2.out',
    clearProps = 'transform,opacity,visibility',
  }: StaggerOptions = {},
) {
  const elements = Array.from(targets)
  if (!elements.length) return

  if (reducedMotion) {
    gsap.set(elements, { autoAlpha: 1, clearProps })
    return
  }

  gsap.fromTo(
    elements,
    {
      autoAlpha: 0,
      x,
      y,
    },
    {
      autoAlpha: 1,
      x: 0,
      y: 0,
      duration,
      delay,
      ease,
      stagger,
      clearProps,
      overwrite: 'auto',
    },
  )
}

export function attachRevealOnScroll(
  targets: ArrayLike<Element> | Element[],
  {
    reducedMotion = false,
    y = 26,
    duration = 0.42,
    stagger = 0.1,
    start = 'top 86%',
  }: RevealOptions = {},
) {
  const elements = Array.from(targets)
  if (!elements.length) return () => undefined

  if (reducedMotion) {
    gsap.set(elements, { autoAlpha: 1, clearProps: 'transform,opacity,visibility' })
    return () => undefined
  }

  gsap.set(elements, { autoAlpha: 0, y })

  const triggers = ScrollTrigger.batch(elements, {
    start,
    once: true,
    onEnter: (batch) => {
      gsap.to(batch, {
        autoAlpha: 1,
        y: 0,
        duration,
        stagger,
        ease: 'power2.out',
        clearProps: 'transform,opacity,visibility',
        overwrite: 'auto',
      })
    },
  })

  return () => {
    triggers.forEach((trigger) => trigger.kill())
  }
}

export function attachPressAnimation(element: HTMLElement, options: PressOptions = {}) {
  const { activeScale = 0.96, idleY = 0 } = options
  let isPressed = false

  const release = () => {
    if (!isPressed) return
    isPressed = false
    gsap.to(element, {
      scale: 1,
      y: idleY,
      duration: 0.28,
      ease: 'back.out(1.7)',
      overwrite: 'auto',
    })
  }

  const press = () => {
    isPressed = true
    gsap.to(element, {
      scale: activeScale,
      y: idleY + 1,
      duration: 0.12,
      ease: 'power2.out',
      overwrite: 'auto',
    })
  }

  element.addEventListener('pointerdown', press)
  element.addEventListener('pointerup', release)
  element.addEventListener('pointerleave', release)
  element.addEventListener('pointercancel', release)

  return () => {
    release()
    element.removeEventListener('pointerdown', press)
    element.removeEventListener('pointerup', release)
    element.removeEventListener('pointerleave', release)
    element.removeEventListener('pointercancel', release)
  }
}

export function attachPressAnimations(root: Element, selector: string, options?: PressOptions) {
  const cleanups = Array.from(root.querySelectorAll<HTMLElement>(selector)).map((element) =>
    attachPressAnimation(element, options),
  )

  return () => {
    cleanups.forEach((cleanup) => cleanup())
  }
}

export function animatePulse(element: Element, { reducedMotion = false }: MotionOptions = {}) {
  if (reducedMotion) return () => undefined

  const tween = gsap.to(element, {
    y: -4,
    scale: 1.03,
    duration: 1.8,
    ease: 'sine.inOut',
    repeat: -1,
    yoyo: true,
  })

  return () => {
    tween.kill()
    gsap.set(element, { clearProps: 'transform' })
  }
}
