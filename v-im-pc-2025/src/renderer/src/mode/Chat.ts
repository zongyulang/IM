export interface Chat {
  id: string
  name: string
  avatar: string
  type: 'friend' | 'group'
  unreadCount: number
  //是否置顶
  top?: boolean
  //最近一次消息的时间戳
  lastReadTime?: number
}
