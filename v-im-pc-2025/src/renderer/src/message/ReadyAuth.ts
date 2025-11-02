/**
 * 登录信息
 */
export interface ReadyAuth {
  // 登录token
  token: string
  // 客户端类型
  client: string
  // 随机ID，用于区分不同客户端
  uuid: string
}
