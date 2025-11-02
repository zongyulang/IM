import ChatType from '@renderer/enum/ChatType'
import GroupApi from '@renderer/api/GroupApi'
import type { User } from '@renderer/mode/User'
import UserApi from '@renderer/api/UserApi'
import { computed, nextTick, reactive, toRefs } from 'vue'
import { useChatStore } from '@renderer/store/chatStore'
import { useUserStore } from '@renderer/store/userStore'
import MessageApi from '@renderer/api/MessageApi'
import type { Message } from '@renderer/message/Message'
import type { Chat } from '@renderer/mode/Chat'
import ChatUtils from '../utils/ChatUtils'
import type { ReadReceipt } from '@renderer/message/ReadReceipt'
import { useWsStore } from '@renderer/store/WsStore'
import DictUtils from '../utils/DictUtils'
import { ElMessage } from 'element-plus'
import type { Group } from '@renderer/mode/Group'

export interface ChatState {
  // 加载状态
  loading: boolean
  // 错误信息
  error: string | null
  // 群组信息
  group: Group | null
  // 用户列表
  users: User[]
  // 是否是有效用户（在群组中或好友关系）
  isValidatedUser: boolean
  // 是否是群主
  isMaster: boolean
  // 是否禁止加好友
  prohibitFriend: boolean
  // 群组用户数量
  groupUserCount: number
}

export default function useChatInit() {
  // 状态管理
  const state = reactive<ChatState>({
    loading: false,
    error: null,
    group: null,
    users: [],
    isValidatedUser: true,
    isMaster: false,
    prohibitFriend: false,
    groupUserCount: 0
  })
  // Store
  const chatStore = useChatStore()
  const userStore = useUserStore()
  const wsStore = useWsStore()

  // 计算属性
  const isGroupChat = computed(() => state.group !== null)

  // 错误处理
  const handleError = (error: Error, message: string) => {
    console.error(message, error)
    state.error = message
    ElMessage.error(message)
  }

  // 消息处理
  const readMessage = (chat: Chat, user: User) => {
    if (!user) return

    const receipt: ReadReceipt = {
      chatId: chat.id,
      fromId: user.id,
      timestamp: Date.now(),
      type: chat.type
    }
    wsStore.sendReceipt(receipt)
  }

  // 加载消息
  const loadMessages = async (chatId: string, chatType: string) => {
    try {
      state.loading = true
      const list = await MessageApi.list(chatId, chatType, 100)
      list.forEach((item: Message) => {
        chatStore.addToMessageList(item, chatId)
        nextTick(() => ChatUtils.imageLoad('message-box'))
      })
    } catch (error) {
      handleError(error as Error, '加载消息失败')
    } finally {
      state.loading = false
    }
  }

  /**
   * 加载群组用户列表
   */
  const loadGroupUsers = async (chatId: string, currentUser: User) => {
    const users = await GroupApi.users(chatId)

    // 更新用户列表状态
    state.users = users
    state.groupUserCount = users.length
    state.isValidatedUser = users.some((item) => item.id === currentUser?.id)

    // 更新用户信息到store
    users.forEach((item: User) => {
      userStore.storeUser(item.id, {
        name: item.name,
        avatar: item.avatar
      })
      chatStore.updateChat(item.id, item.name, item.avatar, false)
    })

    return state.isValidatedUser
  }

  /**
   * 加载群组信息
   */
  const loadGroupInfo = async (chatId: string, currentUser: User) => {
    const group = await GroupApi.get(chatId)
    state.group = group
    state.isMaster = group.master === currentUser?.id
    state.prohibitFriend = group.prohibitFriend === DictUtils.YES
    await chatStore.updateChat(chatId, group.name, group.avatar)
  }

  /**
   * 加载单个用户信息
   */
  const loadUserInfo = async (chatId: string) => {
    const chatUser = await UserApi.getUser(chatId)
    await chatStore.updateChat(chatUser.id, chatUser.name, chatUser.avatar)
    userStore.storeUser(chatUser.id, {
      name: chatUser.name,
      avatar: chatUser.avatar
    })
    return chatUser
  }

  /**
   * 初始化聊天页面信息
   */
  const loadUserOrGroup = async (chatId: string, chatType: string, user: User) => {
    try {
      if (chatType === ChatType.GROUP) {
        // 加载群组信息
        const isValidUser = await loadGroupUsers(chatId, user)
        await loadGroupInfo(chatId, user)

        // 如果是有效用户，加载消息
        if (isValidUser) {
          await loadMessages(chatId, chatType)
        }
      } else {
        // 重置群组相关状态
        state.isValidatedUser = true
        state.users = []
        state.groupUserCount = 0

        // 加载用户信息和消息
        await loadUserInfo(chatId)
        await loadMessages(chatId, chatType)
      }
    } catch (error) {
      console.error('Failed to load chat information:', error)
      // 这里可以添加错误处理逻辑
    }
  }

  return {
    ...toRefs(state),
    isGroupChat,
    readMessage,
    loadMessages,
    loadUserOrGroup
  }
}
