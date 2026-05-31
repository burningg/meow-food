export interface DefaultAvatarOption {
  id: string
  label: string
  group: 'male' | 'female' | 'animal'
  src: string
}

export const defaultAvatarOptions: DefaultAvatarOption[] = [
  { id: 'male-olive', label: '短发男生', group: 'male', src: '/default-avatars/male-olive.svg' },
  { id: 'male-amber', label: '卷发男生', group: 'male', src: '/default-avatars/male-amber.svg' },
  { id: 'male-navy-glasses', label: '眼镜男生', group: 'male', src: '/default-avatars/male-navy-glasses.svg' },
  { id: 'male-terracotta', label: '寸头男生', group: 'male', src: '/default-avatars/male-terracotta.svg' },
  { id: 'female-coral-bob', label: '波波头女生', group: 'female', src: '/default-avatars/female-coral-bob.svg' },
  { id: 'female-teal-ponytail', label: '高马尾女生', group: 'female', src: '/default-avatars/female-teal-ponytail.svg' },
  { id: 'female-mustard-wave', label: '长卷发女生', group: 'female', src: '/default-avatars/female-mustard-wave.svg' },
  { id: 'female-rose-clip', label: '发夹女生', group: 'female', src: '/default-avatars/female-rose-clip.svg' },
  { id: 'animal-cat', label: '橘猫', group: 'animal', src: '/default-avatars/animal-cat.svg' },
  { id: 'animal-dog', label: '小狗', group: 'animal', src: '/default-avatars/animal-dog.svg' },
  { id: 'animal-rabbit', label: '兔子', group: 'animal', src: '/default-avatars/animal-rabbit.svg' },
  { id: 'animal-panda', label: '熊猫', group: 'animal', src: '/default-avatars/animal-panda.svg' },
]
