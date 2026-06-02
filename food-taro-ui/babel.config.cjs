// babel-preset-taro options: https://docs.taro.zone/docs/babel-config
module.exports = {
  presets: [
    [
      'taro',
      {
        framework: 'vue3',
        ts: true,
        compiler: 'webpack5',
      },
    ],
  ],
}
