import FetchRequest from '@renderer/api/FetchRequest'
import type { Group } from '@renderer/mode/Group'
import type { User } from '@renderer/mode/User'
import type { QuickGroup } from '@renderer/mode/QuickGroup'

class GroupApi {
  //基础url
  static url = '/vim/server/groups'

  /**
   * 添加群组
   * @param name 群名称
   * @param avatar 群头像
   * @param openInvite  是否开放邀请
   * @param inviteCheck 加群是否需要审核
   * @param prohibition 禁言
   * @param prohibitFriend 是否禁加好友
   * @param announcement 群公告
   */
  static save(
    name: string,
    avatar: string,
    openInvite: string,
    inviteCheck: string,
    prohibition: string,
    prohibitFriend: string,
    announcement: string
  ): Promise<Group> {
    const data = {
      name: name,
      avatar: avatar,
      openInvite: openInvite,
      inviteCheck: inviteCheck,
      prohibition: prohibition,
      prohibitFriend: prohibitFriend,
      announcement: announcement
    }
    return FetchRequest.post(this.url, JSON.stringify(data), true)
  }

  /**
   * 更新群组
   * @param id 群id
   * @param name 群名称
   * @param avatar 群头像
   * @param inviteCheck 加群是否需要审核
   * @param prohibition 禁言
   * @param prohibitFriend 是否禁加好友
   * @param announcement 群公告
   * @param openInvite  是否开放邀请
   */
  static update(
    id: string,
    name: string,
    avatar: string,
    openInvite: string,
    inviteCheck: string,
    prohibition: string,
    prohibitFriend: string,
    announcement: string
  ): Promise<boolean> {
    const data = {
      name: name,
      avatar: avatar,
      openInvite: openInvite,
      inviteCheck: inviteCheck,
      prohibition: prohibition,
      prohibitFriend: prohibitFriend,
      announcement: announcement
    }
    return FetchRequest.patch(`${this.url}/${id}`, JSON.stringify(data), true)
  }

  /**
   * 更新群组
   * @param id 群id
   * @param name 群名称
   */
  static updateGroupName(id: string, name: string): Promise<boolean> {
    return FetchRequest.patch(`${this.url}/name`, JSON.stringify({ id: id, name: name }), true)
  }

  /**
   * 获取一个群的信息
   * @param id 群id
   */
  static get(id: string): Promise<Group> {
    return FetchRequest.get(`${this.url}/${id}`, true)
  }

  /**
   * 查询当前用户的群组
   */
  static list(): Promise<Group[]> {
    return FetchRequest.get(this.url, true)
  }

  /**
   * 获取一个群的所有用户
   * @param id 群id
   */
  static users(id: string): Promise<User[]> {
    return FetchRequest.get(`${this.url}/${id}/users`, true)
  }

  /**
   * 删除群
   * @param id 用户ID
   */
  static delete(id: string): Promise<boolean> {
    return FetchRequest.del(`${this.url}/${id}`, '', true)
  }

  /**
   * 退群
   * @param id 用户ID
   */
  static exit(id: string): Promise<boolean> {
    return FetchRequest.del(`${this.url}/${id}/exit`, '', true)
  }

  /**
   * 添加群成员
   * @param id 群id
   * @param userId userId
   */
  static addUsers(id: string, userId: string[]): Promise<string[]> {
    return FetchRequest.post(`${this.url}/${id}/users`, JSON.stringify(userId), true)
  }

  /**
   * 转让
   * @param id 群id
   * @param userId userId
   */
  static transference(id: string, userId: string): Promise<string[]> {
    return FetchRequest.post(`${this.url}/${id}/transference/${userId}`, '', true)
  }

  /**
   * 删除用户
   * @param id 用户ID
   * @param userId 用户ID
   */
  static deleteUser(id: string, userId: string): Promise<boolean> {
    return FetchRequest.del(`${this.url}/${id}/users/${userId}`, '', true)
  }

  /**
   * 快速建群
   * @param quickGroup 群id
   */
  static quickGroup(quickGroup: QuickGroup): Promise<Group> {
    return FetchRequest.post(`${this.url}/quickGroup`, JSON.stringify(quickGroup), true)
  }
}

export default GroupApi
