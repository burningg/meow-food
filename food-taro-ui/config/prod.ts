const API_BASE_URL = process.env.TARO_APP_API_BASE || 'https://meow-service.youl.work'

export default {
  env: {
    NODE_ENV: '"production"',
  },
  defineConstants: {
    __API_BASE_URL__: JSON.stringify(API_BASE_URL),
  },
}
