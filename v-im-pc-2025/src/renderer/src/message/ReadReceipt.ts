/**
 * 接受到的消息回执
 */
export interface ReadReceipt {
  chatId: string
  fromId: string
  timestamp: number
  type: 'friend' | 'group'
}
