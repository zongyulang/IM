import FetchRequest from '@renderer/api/FetchRequest'
import type { Page } from '@renderer/mode/Page'
import type { Message } from '@renderer/message/Message'

class MessageApi {
  static url = '/vim/sdk/message'

  /**
   * 消息列表
   * @param chatId 群id或者用户id
   * @param chatType 消息类型
   * @param pageSize 页面大小
   */
  static list(chatId: string, chatType: string, pageSize: number): Promise<Message[]> {
    const param = `?chatId=${chatId}&chatType=${chatType}&pageSize=${pageSize}`
    return FetchRequest.get(this.url + param, true)
  }

  static get(id: string, chatKey: string): Promise<Message> {
    return FetchRequest.get(`${this.url}/${id}?chatKey=${chatKey}`, true)
  }

  static page(
    chatId: string,
    fromId: string,
    searchText: string,
    type: string,
    messageType: string,
    current: number,
    dateRange1: string,
    dateRange2: string,
    size: number
  ): Promise<Page<Message>> {
    let param = `?chatId=${chatId}&fromId=${fromId}&searchText=${searchText}&chatType=${type}&messageType=${messageType}&current=${current}&size=${size}`
    if (dateRange1 != null && dateRange1 != '') {
      param += `&dateRange=${dateRange1}`
    }
    if (dateRange2 != null && dateRange2 != '') {
      param += `&dateRange=${dateRange2}`
    }
    return FetchRequest.get(`${this.url}/page${param}`, true)
  }

  static getReadTime(chatId: string, fromId: string): Promise<string> {
    return FetchRequest.get(`${this.url}/getReadTime?chatId=${chatId}&fromId=${fromId}`, true)
  }
}

export default MessageApi
