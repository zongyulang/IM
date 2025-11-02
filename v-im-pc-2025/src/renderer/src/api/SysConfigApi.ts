import type { SysConfig } from '@renderer/config/SysConfig'
import FetchRequest from './FetchRequest'

/**
 * 系统配置API类
 */
export class SysConfigApi {
  //基础url
  static url = '/vim/server/config'

  /**
   * 获取系统配置
   * @returns Promise<SysConfig> 返回系统配置信息
   */
  static async getConfig(): Promise<SysConfig> {
    return FetchRequest.get(this.url, true)
  }

  static async getPermission(): Promise<Array<string>> {
    return FetchRequest.get(`${this.url}/permission`, true)
  }
}
