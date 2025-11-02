import { defineStore } from 'pinia'
import type { Message } from '../message/Message'

export interface IState {
  checkWidth: string
  checkList: Array<Message>
}
export const useMessageStore = defineStore('message_store', {
  state: (): IState => ({
    checkWidth: '0',
    checkList: []
  }),
  actions: {
    /**
     * 设置宽度,聊天界面多选checkbox的宽度
     * @param width 宽度
     */
    setCheckWidth(width: string) {
      this.checkWidth = width
    },
    /**
     * 多选列表
     * @param list 消息列表
     */
    setCheckList(list: Array<Message>) {
      this.checkList = list
    }
  }
})
