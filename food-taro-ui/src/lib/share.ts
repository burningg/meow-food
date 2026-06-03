import Taro from '@tarojs/taro'

type ShareToGroupOptions = {
  title: string
  path: string
  imageUrl?: string
}

type WechatShareApi = {
  shareAppMessageToGroup?: (options: ShareToGroupOptions) => Promise<unknown> | void
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
