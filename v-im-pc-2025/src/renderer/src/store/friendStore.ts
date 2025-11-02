import { defineStore } from 'pinia'
import FriendApi from '@renderer/api/FriendApi'
import type { User } from '@renderer/mode/User'
import type { Friend } from '@renderer/mode/Friend'
import { useWsStore } from './WsStore'
import { useUserStore } from './userStore'
import ChatType from '@renderer/enum/ChatType'
import MessageType from '@renderer/enum/MessageType'
import SendCode from '@renderer/enum/SendCode'

export interface IState {
  friendList: User[]
  waitCheckList: Friend[]
}

export const useFriendStore = defineStore('friend_store', {
  state: (): IState => ({
    friendList: [],
    waitCheckList: []
  }),
  getters: {
    waitCheckCount(): number {
      return this.waitCheckList.length
    }
  },
  actions: {
    /**
     * 加载好友列表
     */
    async loadData() {
      const userStore = useUserStore()
      this.friendList = await FriendApi.friends()
      //缓存好友，用于聊天时头像名称显示
      this.friendList.forEach((item: User) => {
        userStore.storeUser(item.id, {
          name: item.name,
          avatar: item.avatar
        })
      })
    },
    /**
     * 加载等待审核列表
     */
    async loadWaitCheckList() {
      this.waitCheckList = await FriendApi.waitCheckList()
    },
    /**
     * 添加好友
     * @param friendId 好友id
     */
    sendTips(friendId: string) {
      const fromId = useUserStore().user?.id
      if (fromId) {
        useWsStore().sendMessage({
          mine: true,
          fromId: fromId,
          chatId: friendId,
          chatType: ChatType.FRIEND,
          messageType: MessageType.TEXT,
          content: '我们已经是好友了，快来聊天吧',
          timestamp: new Date().getTime()
        })
      }
    },
    /**
     * 告诉对方需要刷新store
     * @param userId 对方id
     */
    notifyFlushFriendStore(userId: string) {
      const fromId = useUserStore().user?.id
      if (fromId) {
        const sendInfo = {
          code: SendCode.FRIEND_REQUEST,
          message: {
            mine: true,
            fromId: fromId,
            chatId: userId,
            chatType: ChatType.FRIEND,
            messageType: MessageType.TEXT,
            content: '',
            timestamp: new Date().getTime()
          }
        }
        useWsStore().send(JSON.stringify(sendInfo))
      }
    }
  }
})
