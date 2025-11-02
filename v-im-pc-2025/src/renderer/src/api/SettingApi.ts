import FetchRequest from '@renderer/api/FetchRequest'
import type { Setting } from '@renderer/mode/Setting'

class SettingApi {
  static url = '/vim/server/setting'

  /**
   * 获取用户设置
   * @param userId 用户id
   */
  static get(userId: string): Promise<Setting> {
    return FetchRequest.get(`${this.url}/${userId}`, true)
  }

  /**
   * 修改聊天设置
   *  @param setting 聊天设置
   */
  static update(setting: Setting): Promise<boolean> {
    return FetchRequest.put(this.url, JSON.stringify(setting), true)
  }
}

export default SettingApi
