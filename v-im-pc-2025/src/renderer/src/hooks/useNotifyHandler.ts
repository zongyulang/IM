import SendCode from '@renderer/enum/SendCode'
import { useUserStore } from '@renderer/store/userStore'
import ChatType from '@renderer/enum/ChatType'
import MessageType from '@renderer/enum/MessageType'
import { useWsStore } from '@renderer/store/WsStore'

/**
 * 新的好友通知
 * @param friendId 好友id
 */
export const newFriendNotify = async (friendId: string) => {
  const sendInfo = {
    code: SendCode.MESSAGE,
    message: {
      mine: true,
      fromId: useUserStore().user?.id,
      chatId: friendId,
      chatType: ChatType.FRIEND,
      messageType: MessageType.TEXT,
      content: `我们已经是好友了,快来一起聊天吧！`,
      timestamp: new Date().getTime()
    }
  }
  useWsStore().send(JSON.stringify(sendInfo))
}
