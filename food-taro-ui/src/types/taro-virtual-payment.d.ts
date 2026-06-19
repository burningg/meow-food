import '@tarojs/taro'

interface RequestVirtualPaymentSuccessResult {
  errMsg: string
}

interface RequestVirtualPaymentFailResult {
  errMsg: string
  errCode?: number
}

interface RequestVirtualPaymentOption {
  signData: string
  paySig: string
  signature: string
  mode: 'short_series_goods' | 'short_series_coin'
  success?: (result: RequestVirtualPaymentSuccessResult) => void
  fail?: (result: RequestVirtualPaymentFailResult) => void
  complete?: (result: RequestVirtualPaymentSuccessResult | RequestVirtualPaymentFailResult) => void
}

declare module '@tarojs/taro' {
  interface TaroStatic {
    requestVirtualPayment(option: RequestVirtualPaymentOption): Promise<RequestVirtualPaymentSuccessResult>
  }
}
