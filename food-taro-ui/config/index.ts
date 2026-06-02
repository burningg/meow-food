import { defineConfig } from '@tarojs/cli'
import devConfig from './dev'
import prodConfig from './prod'

const config = {
  projectName: 'food-taro-ui',
  date: '2026-06-02',
  designWidth: 390,
  deviceRatio: {
    390: 2,
  },
  sourceRoot: 'src',
  outputRoot: 'dist',
  plugins: ['@tarojs/plugin-html'],
  framework: 'vue3',
  compiler: 'webpack5',
  cache: {
    enable: false,
  },
  copy: {
    patterns: [
      {
        from: 'public',
        to: 'dist',
      },
    ],
    options: {},
  },
  mini: {
    postcss: {
      pxtransform: {
        enable: true,
        config: {},
      },
      cssModules: {
        enable: false,
      },
    },
  },
  h5: {
    publicPath: '/',
    staticDirectory: 'static',
    router: {
      mode: 'browser',
    },
    postcss: {
      autoprefixer: {
        enable: true,
      },
      cssModules: {
        enable: false,
      },
    },
  },
}

export default defineConfig(async (merge, { command }) => {
  if (command === 'build') {
    return merge({}, config, prodConfig)
  }
  return merge({}, config, devConfig)
})
