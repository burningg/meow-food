import { defineConfig } from '@tarojs/cli'
import path from 'node:path'
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
  alias: {
    '@': path.resolve(process.cwd(), 'src'),
  },
  plugins: ['@tarojs/plugin-html'],
  framework: 'vue3',
  compiler: {
    type: 'webpack5',
    prebundle: {
      enable: false,
    },
  },
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
    enableSourceMap: false,
    optimizeMainPackage: {
      enable: true,
    },
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

export default defineConfig(async (merge, { command, mode }) => {
  if (command === 'build' && mode === 'production') {
    return merge({}, config, prodConfig)
  }
  return merge({}, config, devConfig)
})
