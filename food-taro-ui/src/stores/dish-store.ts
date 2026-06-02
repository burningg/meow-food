import { defineStore } from 'pinia'

export const useDishStore = defineStore('dish', {
  state: () => ({
    dish: undefined as any,
    mode: 'create',
  }),
  actions: {
    setData(dish: any, mode: any) {
      this.dish = dish
      this.mode = mode
    },
  },
})
