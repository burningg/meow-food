import Taro from '@tarojs/taro'
import { resolveSharePath } from '@/lib/navigation'

type ShareToGroupOptions = {
  title: string
  path: string
  imageUrl?: string
}

type HomeShareOptions = {
  title: string
}

type WechatShareApi = {
  shareAppMessageToGroup?: (options: ShareToGroupOptions) => Promise<unknown> | void
}

export function createHomeShareMessage(options: HomeShareOptions) {
  // 四个主页面统一分享回首页，避免访客进入需要特定上下文的页面。
  return {
    title: options.title,
    path: resolveSharePath('home'),
  }
}

export function canShareAppMessageToGroup() {
  return process.env.TARO_ENV === 'weapp' && Taro.canIUse('shareAppMessageToGroup')
}

export async function shareAppMessageToGroup(options: ShareToGroupOptions) {
  if (!canShareAppMessageToGroup()) {
    throw new Error('当前微信版本暂不支持分享到聊天，请升级微信后重试')
  }

  const wechat = (globalThis as unknown as { wx?: WechatShareApi }).wx
  if (!wechat?.shareAppMessageToGroup) {
    throw new Error('当前环境暂不支持分享到聊天')
  }

  await wechat.shareAppMessageToGroup(options)
}
