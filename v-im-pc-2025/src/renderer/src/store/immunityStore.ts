import { defineStore } from 'pinia'
import ImmunityApi from '@renderer/api/ImmunityApi'
import { useUserStore } from '@renderer/store/userStore'

/**
 * @description 免打扰库
 */
export const useImmunityStore = defineStore('immunity_store', {
  state: () => ({
    immunityList: new Array<string>()
  }),
  actions: {
    /**
     * 加载免打扰列表
     */
    loadData() {
      const userId = useUserStore().user?.id
      if (userId) {
        ImmunityApi.list(userId).then((res) => {
          this.immunityList = res.map((item) => item.chatId)
        })
      }
    }
  }
})
