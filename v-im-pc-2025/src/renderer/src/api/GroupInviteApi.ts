import FetchRequest from '@renderer/api/FetchRequest'
import type { GroupInvite } from '@renderer/mode/GroupInvite'
import type { GroupInviteCount } from '@renderer/mode/GroupInviteCount'

class GroupInviteApi {
  //基础url
  static url = '/vim/server/groupInvites'

  /**
   * 查询当前待审核的群邀请
   */
  static list(groupId: string): Promise<GroupInvite[]> {
    return FetchRequest.get(`${this.url}?groupId=${groupId}`, true)
  }

  /**
   * 查询当前待审核的群邀请
   */
  static waitCheckList(): Promise<GroupInviteCount[]> {
    return FetchRequest.get(`${this.url}/waitCheckList`, true)
  }

  /**
   * 同意加群
   * @param id 邀请id
   */
  static agree(id: string): Promise<boolean> {
    return FetchRequest.post(`${this.url}/agree/${id}`, '', true)
  }

  /**
   * 拒绝加群
   * @param id 邀请id
   */
  static refuse(id: string): Promise<boolean> {
    return FetchRequest.post(`${this.url}/refuse/${id}`, '', true)
  }
}

export default GroupInviteApi
