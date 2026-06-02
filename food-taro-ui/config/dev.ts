const API_BASE_URL = process.env.TARO_APP_API_BASE || 'http://localhost:8080'

export default {
  env: {
    NODE_ENV: '"development"',
  },
  defineConstants: {
    __API_BASE_URL__: JSON.stringify(API_BASE_URL),
  },
}
