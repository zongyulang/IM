import FetchRequest from '@renderer/api/FetchRequest'
import type { User } from '@renderer/mode/User'

/**
 * 用户接口
 */
class UserApi {
  static url = '/vim/server/users'

  /**
   * 根据id获取用户信息
   * @param id id
   */
  static getUser(id: string): Promise<User> {
    return FetchRequest.get(`${this.url}/${id}`, true)
  }

  /**
   * 获取当前用户信息
   * @returns Promise
   */
  static currentUser(): Promise<User> {
    return FetchRequest.get(`${this.url}/my`, true)
  }

  /**
   * 更新用户信息
   * @param id id
   * @param user  user
   */
  static update(id: string, user: User): Promise<boolean> {
    user.id = id
    return FetchRequest.put(`${this.url}/update`, JSON.stringify(user), true)
  }

  /**
   * 用户在线状态
   */
  static wsOnline(): Promise<any> {
    return FetchRequest.get('/wsOnline', true)
  }

  /**
   * search好友
   * @param mobile mobile
   */
  static search(mobile: string): Promise<User[]> {
    return FetchRequest.get(`${this.url}/search?mobile=${mobile}`, true)
  }

  /**
   * 刷新用户密钥
   * @param oldPassword 旧密码
   * @param newPassword 新密码
   */
  static updateUserPwd(oldPassword: string, newPassword: string): Promise<boolean> {
    const data = {
      oldPassword: oldPassword,
      newPassword: newPassword
    }
    return FetchRequest.put(`${this.url}/updatePwd`, JSON.stringify(data), true)
  }
}

export default UserApi
