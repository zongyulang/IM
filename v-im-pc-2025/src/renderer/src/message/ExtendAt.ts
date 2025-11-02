/**
 * 扩展消息属性接口
 * 用于定义消息的额外特性和元数据
 */
export interface ExtendAt {
  /** 消息中@的用户ID列表 */
  atUserIds?: string[]
  /** 是否@所有人的标志 */
  atAll?: boolean
}
