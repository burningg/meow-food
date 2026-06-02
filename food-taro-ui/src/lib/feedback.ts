import Taro from '@tarojs/taro'

type ToastType = 'success' | 'error' | 'warning' | 'info'

function toast(title: string, type: ToastType = 'info') {
  return Taro.showToast({
    title,
    icon: type === 'success' ? 'success' : 'none',
    duration: 1800,
  })
}

export const Message = {
  success: (title: string) => toast(title, 'success'),
  error: (title: string) => toast(title, 'error'),
  warning: (title: string) => toast(title, 'warning'),
  info: (title: string) => toast(title, 'info'),
}

export async function confirmDialog(options: { title: string; message: string }) {
  const result = await Taro.showModal({
    title: options.title,
    content: options.message,
    confirmText: '确认',
    cancelText: '取消',
  })
  if (!result.confirm) {
    throw new Error('cancelled')
  }
}
