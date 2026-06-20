import pixelCorgi from '@/assets/pet/pixel-corgi.svg'
import pixelCorgiLounge from '@/assets/pet/pixel-corgi-lounge.svg'
import pixelCorgiWalk1 from '@/assets/pet/pixel-corgi-walk-1.svg'
import pixelCorgiWalk2 from '@/assets/pet/pixel-corgi-walk-2.svg'
import pixelTabbyCat from '@/assets/pet/pixel-tabby-cat.svg'
import pixelTabbyCatLounge from '@/assets/pet/pixel-tabby-cat-lounge.svg'
import pixelTabbyCatWalk1 from '@/assets/pet/pixel-tabby-cat-walk-1.svg'
import pixelTabbyCatWalk2 from '@/assets/pet/pixel-tabby-cat-walk-2.svg'

export type PetType = 'tabby_cat' | 'corgi'

export interface PetAssetSet {
  normal: string
  lounge: string
  walk: [string, string]
}

export interface PetDefinition {
  type: PetType
  label: string
  shortLabel: string
  description: string
  defaultName: string
  assets: PetAssetSet
}

const PET_DEFINITIONS: Record<PetType, PetDefinition> = {
  tabby_cat: {
    type: 'tabby_cat',
    label: '狸花猫',
    shortLabel: '猫猫',
    description: '爱蹭饭桌，也会认真守着你的菜谱。',
    defaultName: '饭团',
    assets: {
      normal: pixelTabbyCat,
      lounge: pixelTabbyCatLounge,
      walk: [pixelTabbyCatWalk1, pixelTabbyCatWalk2],
    },
  },
  corgi: {
    type: 'corgi',
    label: '柯基',
    shortLabel: '狗狗',
    description: '热情巡逻厨房，最会提醒你按时开饭。',
    defaultName: '年糕',
    assets: {
      normal: pixelCorgi,
      lounge: pixelCorgiLounge,
      walk: [pixelCorgiWalk1, pixelCorgiWalk2],
    },
  },
}

export const PET_TYPE_OPTIONS: PetDefinition[] = [
  PET_DEFINITIONS.tabby_cat,
  PET_DEFINITIONS.corgi,
]

export function normalizePetType(value?: string | null): PetType {
  return value === 'corgi' ? 'corgi' : 'tabby_cat'
}

export function getPetDefinition(value?: string | null) {
  return PET_DEFINITIONS[normalizePetType(value)]
}
