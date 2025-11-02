import { defineStore } from 'pinia'

export const useAtStore = defineStore('at_store', {
  state: () => ({
    //@过滤关键字
    keyword: ''
  }),
  actions: {
    setKeyword(keyword: string) {
      this.keyword = keyword
    }
  }
})
