import { defineStore } from 'pinia'
import SettingApi from '@renderer/api/SettingApi'
import type { Setting } from '@renderer/mode/Setting'
import { useUserStore } from '@renderer/store/userStore'

export interface IState {
  setting: Setting | undefined
}

export const useSettingStore = defineStore('setting_store', {
  state: (): IState => ({
    setting: undefined
  }),
  actions: {
    /**
     * 加载设置
     */
    loadData() {
      const userId = useUserStore().user?.id
      if (userId) {
        SettingApi.get(userId).then((res) => {
          this.setting = res
        })
      }
    }
  }
})
