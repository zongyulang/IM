import { defineStore, storeToRefs } from 'pinia'
import type { Chat } from '@renderer/mode/Chat'
import type { Message } from '@renderer/message/Message'
import ChatType from '@renderer/enum/ChatType'
import GroupApi from '@renderer/api/GroupApi'
import router from '@renderer/router'
import UserApi from '@renderer/api/UserApi'
import { useUserStore } from '@renderer/store/userStore'
import { useImmunityStore } from '@renderer/store/immunityStore'
import MessageType from '@renderer/enum/MessageType'
import { useSettingStore } from '@renderer/store/settingStore'
import FetchRequest from '@renderer/api/FetchRequest'
import DictUtils from '@renderer/utils/DictUtils'
import type { ReadReceipt } from '@renderer/message/ReadReceipt'
import { useWsStore } from './WsStore'
import ChatApi from '@renderer/api/ChatApi'
import vimConfig from '../config/VimConfig'
import { nextTick } from 'vue'
import type { ExtendAt } from '@renderer/message/ExtendAt'

// 从 immunityStore 中解构出 immunityList，用于判断消息是否需要提醒
const { immunityList } = storeToRefs(useImmunityStore())

// 定义 ChatStore 的状态接口
export interface IState {
  chats: Array<Chat> // 聊天列表
  openId: string | undefined // 当前打开的聊天 ID
  last: number // 上次提醒时间，用于控制消息提醒频率
  chatMessage: Record<string, Message[]>
  chatLastTime: Record<string, number>
  chatLastMessage: Record<string, string>
  chatUnreadId: Record<string, Array<string>> // 聊天未读消息 ID 集合，键为 chatId
  isChatActive: boolean // 聊天窗口是否激活
}

// 定义和导出 chat 相关的 Pinia Store
export const useChatStore = defineStore('chat_store', {
  // Store 的状态
  state: (): IState => ({
    chats: new Array<Chat>(),
    openId: undefined,
    last: 0,
    chatMessage: {},
    chatLastTime: {},
    chatLastMessage: {},
    chatUnreadId: {},
    isChatActive: true
  }),
  // 开启数据缓存到 localStorage
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'chat',
        storage: localStorage,
        paths: ['chatLastTime', 'chatLastMessage', 'chatUnreadId'] // 需要持久化的状态
      }
    ]
  },
  // 计算属性
  getters: {
    // 获取当前打开的聊天对话
    chat(state): Chat {
      return state.chats.find((item) => item.id === state.openId) as Chat
    }
  },
  // Action，用于修改状态
  actions: {
    // 设置聊天窗口激活状态
    setChatActive(isChatActive: boolean) {
      useWsStore().setBlur(false) // 当聊天窗口激活时，取消 WsStore 的 blur 状态
      useWsStore().setSleep(false) // 当聊天窗口激活时，取消 WsStore 的 sleep 状态
      this.isChatActive = isChatActive
    },
    // 移除重复的聊天项
    removeDuplicateChats(chatArr: Chat[]) {
      return chatArr.reduce(
        (prev: Chat[], cur: Chat) =>
          prev.some((item) => item.id === cur.id) ? prev : [...prev, cur],
        []
      )
    },
    // 根据消息 ID 获取消息
    getMessage(id: string) {
      return this.chatMessage[id]?.find((item) => item.id === id) || null
    },
    // 异步重新加载聊天列表，包括置顶和普通聊天
    async reloadChats() {
      try {
        const [topChats, recentChats] = await Promise.all([ChatApi.topList(), ChatApi.list()]) // 并行请求置顶和普通聊天列表
        topChats.forEach((item) => (item.top = true)) // 标记置顶聊天
        const allChats = topChats.concat(recentChats)
        allChats.forEach((item) => (item.unreadCount = (this.chatUnreadId[item.id] ?? []).length)) // 更新未读消息数
        this.chats = this.removeDuplicateChats(allChats)
      } catch (error) {
        console.error('Failed to reload chats:', error)
      }
    },
    // 异步推送新消息到聊天列表
    async pushMessage(message: Message) {
      const hideIndex = 10
      const chatIndex = this.getChatIndex(message.chatId)

      // 如果新消息的聊天在隐藏索引之后，将其移动到前面
      if (chatIndex > hideIndex) {
        const topList = this.chats.filter((item) => !!item.top)
        const topIndex = topList.length
        if (topIndex < hideIndex) {
          const [temp] = this.chats.splice(chatIndex, 1)
          this.chats.splice(topIndex, 0, temp)
          try {
            await ChatApi.move(message.chatId) // 通知服务器移动了聊天
          } catch (error) {
            console.error('Failed to move chat:', error)
          }
        }
      }
      let chat = this.getChat(message.chatId)
      if (chat) {
        this.addMessage(chat, message) // 如果聊天已存在，添加消息
      } else {
        await this.createChatRoom(message) // 如果聊天不存在，创建聊天室
        chat = this.getChat(message.chatId)
        if (chat) {
          this.addMessage(chat, message)
        }
      }
      // 发送已读回执，确保聊天窗口处于打开状态才会发送，负责是不会发送的
      this.handleReceipt()
    },
    // 发送已读回执处理逻辑
    handleReceipt() {
      if (!this.openId || !this.isChatActive || useWsStore().sleep || useWsStore().blur) {
        return
      }
      const chat = this.getChat(this.openId)
      const user = useUserStore().user
      //必须是当天聊天窗口打开状态才发送已读回执
      if (user && chat && this.openId !== user.id) {
        const receipt: ReadReceipt = {
          chatId: chat.id,
          fromId: user?.id,
          timestamp: new Date().getTime(),
          type: chat.type
        }
        useWsStore().sendReceipt(receipt) // 通过 WebSocket 发送已读回执
        chat.unreadCount = 0
        this.chatUnreadId[this.openId] = []
      }
    },
    // 清空聊天消息
    clearMessage(): void {
      this.chatMessage = {}
    },
    // 处理消息撤回
    backMessage(message: Message) {
      this.chatMessage[message.chatId]?.forEach((item) => {
        if (item.id === message.id) {
          if (message.content === this.chatLastMessage[message.chatId]) {
            this.chatLastMessage[message.chatId] = ''
          }
          item.content = '该消息已被撤回'
          item.messageType = MessageType.EVENT
        }
      })
    },
    // 将消息插入到消息列表，保持时间顺序
    insertMessage(list: Message[], message: Message): void {
      const repeat = list.findIndex((item) => item.id === message.id)
      if (repeat > -1) {
        return
      }
      const len = list.findIndex((n) => (n.timestamp ?? -1) > (message.timestamp ?? 0))
      if (len > -1) {
        list.splice(len, 0, message)
      } else {
        list.push(message)
      }
    },
    // 异步创建聊天室
    async createChatRoom(message: Message) {
      try {
        const chat: Chat = {
          id: message.chatId,
          name: '加载中...',
          avatar: '',
          type: message.chatType,
          unreadCount: 0
        }
        this.chatLastMessage[chat.id] = message.content
        this.chatLastTime[chat.id] = message.timestamp ?? 0
        this.newChat(chat) // 添加到聊天列表
        if (message.chatType === ChatType.GROUP) {
          const res = await GroupApi.get(message.chatId)
          await this.updateChat(chat.id, res.name, res.avatar, false)
        } else {
          const res = await UserApi.getUser(message.mine ? message.chatId : message.fromId)
          await this.updateChat(chat.id, res.name, res.avatar, false)
        }
      } catch (error) {
        console.error('Failed to create chat room:', error)
      }
    },
    // 设置最后阅读时间
    setLastReadTime(receipt: ReadReceipt) {
      this.chats.forEach((item: Chat) => {
        if (receipt.fromId === item.id) {
          item.lastReadTime = receipt.timestamp
        }
      })
    },
    // 设置当前聊天对象的最后阅读时间
    setCurrentChatLastReadTime(time: number) {
      if (this.chat) {
        this.chat.lastReadTime = time
      }
    },
    // 添加消息到聊天会话中
    addMessage(chat: Chat, message: Message) {
      const canTips =
        (message.fromId != useUserStore().user?.id &&
          immunityList.value.indexOf(message.chatId) === -1) ||
        (message.chatType === ChatType.GROUP && this.isAtMine(message.extend)) ||
        (message.fromId === useUserStore().user?.id && message.messageType === MessageType.EVENT)

      // 如果当前聊天窗口是打开的，则直接添加消息并标记为已读
      if (chat.id === this.openId && '/index/chat' === router.currentRoute.value.fullPath) {
        chat.unreadCount = 0
        this.chatUnreadId[chat.id] = []
        this.chatLastMessage[chat.id] = message.content
        this.chatLastTime[chat.id] = message.timestamp ?? 0
        this.addToMessageList(message, chat.id)
      } else {
        // 否则，添加到消息列表并更新未读消息数
        const isDouble = this.addToMessageList(message, chat.id)
        if (canTips && !isDouble) {
          this.chatLastMessage[chat.id] = message.content
          this.chatLastTime[chat.id] = message.timestamp ?? 0
          if (!this.chatUnreadId[chat.id]) {
            this.chatUnreadId[chat.id] = []
          }
          const array = this.chatUnreadId[chat.id] ?? []
          if (message.id && !array.includes(message.id)) {
            this.chatUnreadId[chat.id].push(message.id)
          }
          chat.unreadCount = this.chatUnreadId[chat.id].length
        }
      }

      if (message.notification) {
        this.notification(message.content) // 发送通知
      }

      if (canTips) {
        this.soundTips() // 播放提示音
        this.tips() // 触发其他提示（如闪烁）
      }
    },
    // 判断当前用户是否被 @
    isAtMine(extend: ExtendAt | undefined): boolean {
      const user = useUserStore().user
      if (extend && extend.atAll && user) {
        return true // @所有人
      }
      if (extend && extend.atUserIds && user) {
        return extend.atUserIds.includes(user.id) // @指定用户
      }
      return false
    },
    // 打开聊天会话
    openChat(chat: Chat) {
      const existingChat = this.chats.find((item) => item.id === chat.id)
      if (existingChat) {
        this.openId = chat.id
        existingChat.unreadCount = 0
        this.chatUnreadId[existingChat.id] = []
      } else {
        chat.unreadCount = 0
        this.chatUnreadId[chat.id] = []
        this.newChat(chat)
        ChatApi.add(chat).then()
        this.openId = chat.id
      }

      // 如果是好友聊天，获取用户信息并存储
      if (chat && chat.type === ChatType.FRIEND) {
        UserApi.getUser(chat.id).then((res) => {
          const chatUser = res
          useUserStore().storeUser(chatUser.id, {
            name: chatUser.name,
            avatar: chatUser.avatar
          })
        })
      }
    },
    // 获取聊天在列表中的索引
    getChatIndex(id: string) {
      return this.chats.findIndex((item) => item.id === id)
    },
    // 根据 ID 获取聊天对象
    getChat(id: string): Chat | null {
      return this.chats.find((item) => item.id === id) || null
    },
    // 添加消息到指定聊天的消息列表中
    addToMessageList(message: Message, chatId: string): boolean {
      const messageList = this.chatMessage[chatId] ?? []
      const isDouble = messageList.some((item) => item.id === message.id)
      this.insertMessage(messageList, message)
      // 限制消息列表长度，避免内存溢出
      if (messageList.length > 150) {
        messageList.splice(0, 50)
      }
      this.chatMessage[chatId] = messageList
      return isDouble
    },
    // 异步删除聊天
    async delChat(id: string) {
      this.chats.splice(this.getChatIndex(id), 1)
      try {
        await ChatApi.delete(id)
      } catch (error) {
        console.error('Failed to delete chat:', error)
      }
    },
    // 异步置顶聊天
    async topChat(id: string) {
      const itemIndex = this.getChatIndex(id)
      if (itemIndex !== -1) {
        const chat = { ...this.chats[itemIndex] }
        this.chats.splice(itemIndex, 1)
        const topIndex = this.chats.findIndex((item) => !!item.top)
        this.chats.splice(topIndex > -1 ? topIndex : 0, 0, chat)
        chat.top = true
        try {
          await ChatApi.top(id)
        } catch (error) {
          console.error('Failed to top chat:', error)
        }
      }
    },
    // 异步取消置顶
    async cancelTop(id: string) {
      const itemIndex = this.getChatIndex(id)
      if (itemIndex !== -1 && this.chats[itemIndex].top) {
        const chat = { ...this.chats[itemIndex] }
        chat.top = false
        this.chats.push(chat)
        this.chats.splice(itemIndex, 1)
        try {
          await ChatApi.cancelTop(id)
        } catch (error) {
          console.error('Failed to cancel top chat:', error)
        }
      }
    },
    // 添加新的聊天会话
    newChat(chat: Chat) {
      const items = this.chats
      const topList = items.filter((item) => item.top) // 查找已置顶元素的位置
      const topIndex = topList.length
      if (this.chats.findIndex((item) => item.id === chat.id) === -1) {
        items.splice(topIndex, 0, chat) // 将该元素插入已置顶元素的后面
        ChatApi.add(chat).then()
      }
    },
    // 更新聊天会话的信息
    /**
     * 更新聊天室信息
     * @param id 聊天室id
     * @param name 名称
     * @param avatar 头像
     * @param updateUnreadCount 是否更新未读消息数
     */
    async updateChat(id: string, name: string, avatar: string, updateUnreadCount = true) {
      this.chats.forEach((item: Chat) => {
        if (item.id === id) {
          item.name = name
          item.avatar = avatar
          if (updateUnreadCount) {
            item.unreadCount = 0
            this.chatUnreadId[item.id] = []
          }
          ChatApi.update(item)
        }
      })
    },
    // 播放提示音，限制播放频率
    soundTips() {
      const now = Date.now()
      if (now - this.last >= 1000) {
        this.last = now
        // 检查是否允许声音提醒以及当前窗口是否失焦
        if (useSettingStore().setting?.canSoundRemind === DictUtils.YES && useWsStore().blur) {
          new Audio(FetchRequest.getHost() + vimConfig.soundPath).play().then()
        }
      }
    },
    // 触发提示（例如 Dock 栏图标跳动）- 此方法在 Vue 组件中实现
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    tips(): void {},
    // 发送系统通知 - 此方法在 Vue 组件中实现
    notification(content: string): void {
      console.log(content)
    },
    //滚动到第一个未读消息
    scrollToFirstUnread() {
      const firstUnread = this.chats.find((chat) => chat.unreadCount > 0)
      if (firstUnread) {
        nextTick(() => {
          this.openChat(firstUnread)
        }).then()
      }
    }
  }
})
