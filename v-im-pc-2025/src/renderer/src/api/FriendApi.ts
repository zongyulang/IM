import FetchRequest from '@renderer/api/FetchRequest'
import type { User } from '@renderer/mode/User'
import type { Friend } from '@renderer/mode/Friend'
import type { AddFriendResult } from '@renderer/mode/AddFriendResult'

class FriendApi {
  static url = '/vim/server/friends'

  /**
   * 获取用户的所有好友
   */
  static friends(): Promise<User[]> {
    return FetchRequest.get(this.url, true)
  }

  /**
   * 获取用户的待验证好友
   */
  static waitCheckList(): Promise<Friend[]> {
    return FetchRequest.get(`${this.url}/validateList`, true)
  }

  /**
   * 添加好友
   * @param friend 好友
   */
  static add(friend: Friend): Promise<AddFriendResult> {
    return FetchRequest.post(`${this.url}/add`, JSON.stringify(friend), true)
  }

  /**
   * 同意加好友
   * @param friendId 好友ID
   */
  static agree(friendId: string): Promise<boolean> {
    return FetchRequest.post(`${this.url}/agree`, friendId, true)
  }

  /**
   * 删除好友
   * @param friendId 好友ID
   */
  static delete(friendId: string): Promise<boolean> {
    return FetchRequest.del(`${this.url}/delete`, friendId, true)
  }

  /**
   * 判断是否好友
   */
  static isFriend(friendId: string): Promise<boolean> {
    return FetchRequest.get(`${this.url}/isFriend?friendId=${friendId}`, true)
  }
}

export default FriendApi
