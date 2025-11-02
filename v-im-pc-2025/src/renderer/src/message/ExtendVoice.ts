/**
 * 扩展消息属性接口
 * 用于定义消息的额外特性和元数据
 */
export interface ExtendVoice {
  /** 消息关联的URL链接 */
  url?: string | null
  /** 消息的时间 */
  time?: number
}
