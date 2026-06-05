/// <reference types="@tarojs/taro" />

declare const __API_BASE_URL__: string

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module '*.svg' {
  const src: string
  export default src
}
