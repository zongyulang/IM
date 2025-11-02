import { defineStore } from 'pinia'
import GroupApi from '@renderer/api/GroupApi'
import GroupInviteApi from '@renderer/api/GroupInviteApi'
import { useUserStore } from './userStore'
import type { Group } from '@renderer/mode/Group'
import type { User } from '@renderer/mode/User'

export interface IState {
  checkIndex: number
  groupList: Group[]
  waitCheckMap: Map<string, number>
  currentGroup: Group
  currentGroupUsers: User[]
  isMaster: boolean
}

export const useGroupStore = defineStore('group_store', {
  state: (): IState => ({
    checkIndex: 0,
    groupList: [],
    waitCheckMap: new Map<string, number>(),
    currentGroup: {} as Group,
    currentGroupUsers: [],
    isMaster: false
  }),

  getters: {
    waitCheckCount(): number {
      let total = 0
      this.waitCheckMap.forEach((value) => {
        total += value
      })
      return total
    }
  },

  actions: {
    setCheckIndex(index: number) {
      this.checkIndex = index
    },

    async loadData() {
      this.groupList = await GroupApi.list()
      await this.loadWaitCheckList()
    },

    async loadWaitCheckList() {
      this.waitCheckMap.clear()
      const list = await GroupInviteApi.waitCheckList()
      list.forEach((item) => {
        this.waitCheckMap.set(item.groupId, item.count)
      })
    },

    async loadGroupData(groupId: string) {
      const res = await GroupApi.get(groupId)
      this.currentGroup = res
      this.isMaster = res.master === useUserStore().user?.id
      this.currentGroupUsers = await GroupApi.users(groupId)
    }
  }
})
