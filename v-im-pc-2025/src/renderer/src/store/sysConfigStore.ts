import { defineStore } from 'pinia'
import type { SysConfig } from '@renderer/config/SysConfig'
import { SysConfigApi } from '@renderer/api/SysConfigApi'

export interface IState {
  sysConfig: SysConfig
  permission: Array<string>
}
export const useSysConfigStore = defineStore('sys_config_store', {
  state: (): IState => ({
    //@过滤关键字
    sysConfig: {} as SysConfig,
    permission: Array<string>()
  }),
  // 开启数据缓存
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'config',
        storage: localStorage,
        paths: ['sysConfig', 'permission']
      }
    ]
  },
  getters: {
    config: (state) => state.sysConfig
  },
  actions: {
    async loadData() {
      this.sysConfig = await SysConfigApi.getConfig()
      this.permission = await SysConfigApi.getPermission()
    },
    hasPermission(permission: string): boolean {
      return this.permission.includes(permission)
    }
  }
})
