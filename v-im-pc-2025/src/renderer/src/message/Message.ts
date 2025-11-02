export interface Message<T = any> {
  //消息id,雪花id,有序增长
  id?: string
  //消息文件类型
  messageType: string
  //聊天室id
  chatId: string
  //消息发送人
  fromId: string
  //是否是本人
  mine?: boolean
  //消息内容
  content: string
  //消息时间
  timestamp?: number
  //消息类型：私聊|群聊
  chatType: 'friend' | 'group'
  //扩展
  extend?: T
  //是否显示通知
  notification?: boolean
}
