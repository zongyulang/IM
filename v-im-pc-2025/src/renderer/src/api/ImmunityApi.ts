import FetchRequest from '@renderer/api/FetchRequest'
import type { Immunity } from '@renderer/mode/Immunity'

class ImmunityApi {
  static url = '/vim/server/immunity'

  /**
   * 获取用户免打扰
   */
  static list(userId: string): Promise<Immunity[]> {
    return FetchRequest.get(`${this.url}/${userId}`, true)
  }

  /**
   *  保存用户免打扰
   */
  static save(userId: string, chatId: string): Promise<Immunity[]> {
    const immunity: Immunity = {
      userId: userId,
      chatId: chatId
    }
    return FetchRequest.post(this.url, JSON.stringify(immunity), true)
  }

  /**
   * 删除用户免打扰
   */
  static delete(userId: string, chatId: string): Promise<Immunity[]> {
    return FetchRequest.del(`${this.url}/${userId}-${chatId}`, '', true)
  }
}

export default ImmunityApi
