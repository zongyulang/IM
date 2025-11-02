import FetchRequest from '@renderer/api/FetchRequest'
import type { Chat } from '@renderer/mode/Chat'

class ChatApi {
  static url = '/vim/server/chat'

  /**
   * 获取当前用户的非置顶聊天列表
   */
  static list(): Promise<Chat[]> {
    return FetchRequest.get(`${this.url}/list`, true)
  }

  /**
   * 获取当前用户的置顶聊天列表
   */
  static topList(): Promise<Chat[]> {
    return FetchRequest.get(`${this.url}/topList`, true)
  }

  /**
   * 新增聊天
   * @param chat chat
   */
  static add(chat: Chat): Promise<boolean> {
    return FetchRequest.post(this.url, JSON.stringify(chat), true)
  }

  /**
   * 跟新聊天
   * @param chat chat
   */
  static update(chat: Chat): Promise<boolean> {
    const chatTemp = JSON.parse(JSON.stringify(chat))
    chatTemp.unreadCount = 0
    return FetchRequest.put(this.url, JSON.stringify(chatTemp), true)
  }

  /**
   * 批量更新聊天
   * @param chatList chatList
   */
  static batch(chatList: Array<Chat>): Promise<boolean> {
    const chatListTemp = JSON.parse(JSON.stringify(chatList))
    chatListTemp.forEach((chat) => {
      chat.unreadCount = 0
    })
    return FetchRequest.put(`${this.url}/batch`, JSON.stringify(chatListTemp), true)
  }

  /**
   * 移动聊天室位置
   * @param chatId chatId
   */
  static move(chatId: string) {
    return FetchRequest.get(`${this.url}/move?chatId=${chatId}`, true)
  }

  /**
   *  置顶聊天
   *  @param chatId chatId
   */
  static top(chatId: string): Promise<boolean> {
    return FetchRequest.get(`${this.url}/top?chatId=${chatId}`, true)
  }

  /**
   *  取消置顶聊天
   *  @param chatId 收藏id
   */
  static cancelTop(chatId: string): Promise<boolean> {
    return FetchRequest.get(`${this.url}/cancelTop?chatId=${chatId}`, true)
  }

  /**
   * 删除聊天
   * @param chatId 聊天id
   */
  static delete(chatId: string): Promise<boolean> {
    return FetchRequest.del(`${this.url}/${chatId}`, '', true)
  }
}

export default ChatApi
