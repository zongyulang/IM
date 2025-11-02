/**
 * 群邀请
 */
export interface GroupInvite {
  id: string
  groupId: string
  fromId: string
  userId: string
  checkUserId: string
  checkMessage: string
  waitCheck: string
  checkResult: string
  createTime: string
}
