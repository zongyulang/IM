import type { Message } from '@renderer/message/Message'
import type { ReadReceipt } from '@renderer/message/ReadReceipt'
import type { SendInfo } from '@renderer/message/SendInfo'
import type { OtherLogin } from '@renderer/message/OtherLogin'
import SendCode from '@renderer/enum/SendCode'
import ChatUtils from '@renderer/utils/ChatUtils'
import vimConfig from '@renderer/config/VimConfig'
import Auth from '@renderer/api/Auth'
import ChatType from '@renderer/enum/ChatType'
import { nextTick } from 'vue'
import { useUserStore } from '@renderer/store/userStore'
import { useChatStore } from '@renderer/store/chatStore'
import { useFriendStore } from '@renderer/store/friendStore'
import { useGroupStore } from '@renderer/store/groupStore'
import { ElMessage } from 'element-plus'
import type { ReadyAuth } from '@renderer/message/ReadyAuth'

// WebSocket 预定义消息
const ping = `ping` // 心跳检测消息
const pong = `pong` // 心跳检测消息

// WebSocket 配置常量，定义了心跳检测和重连的关键参数
const WS_CONFIG = {
  TIMEOUT: 3000, // 心跳检测间隔时间（毫秒）
  TIMEOUT_ERROR: 5000, // 超时重连时间（毫秒）
  MAX_RETRY_COUNT: 50, // 最大重试次数
  RETRY_INTERVAL: 3000 // 重试间隔时间（毫秒）
} as const

/**
 * 消息处理器接口
 * 定义了所有消息处理器必须实现的方法
 */
interface MessageHandler {
  handle(sendInfo: SendInfo<Message | ReadReceipt | OtherLogin>, wsRequest: WsRequest): void
}

/**
 * 消息处理器集合类
 * 使用策略模式实现不同类型消息的处理
 */
class MessageHandlers {
  // 静态只读的处理器映射表
  static readonly handlers: Record<string, MessageHandler> = {
    // 处理普通消息
    [SendCode.MESSAGE]: {
      handle(sendInfo: SendInfo<Message>, wsRequest: WsRequest) {
        wsRequest.onmessage(sendInfo.message)
      }
    },
    // 处理其他设备登录消息
    [SendCode.OTHER_LOGIN]: {
      handle(sendInfo: SendInfo<OtherLogin>, wsRequest: WsRequest) {
        const message = sendInfo.message
        if (message.uuid !== wsRequest.getUuid()) {
          ElMessage.info('账号已经在别处登录')
          Auth.logout()
        }
      }
    },
    // 处理新朋友请求消息
    [SendCode.FRIEND_REQUEST]: {
      async handle() {
        await useFriendStore().loadWaitCheckList()
        await useFriendStore().loadData()
      }
    },
    // 处理群验证消息
    [SendCode.GROUP_REQUEST]: {
      async handle() {
        await useGroupStore().loadWaitCheckList()
      }
    },
    // 处理消息已读回执
    [SendCode.READ]: {
      handle(sendInfo: SendInfo<ReadReceipt>) {
        useChatStore().setLastReadTime(sendInfo.message)
      }
    },
    // 默认消息处理器
    default: {
      handle(sendInfo: SendInfo<never>) {
        console.error('Unknown message type:', sendInfo.code)
      }
    }
  }

  /**
   * 获取对应消息类型的处理器
   * @param code 消息类型代码
   * @returns 对应的消息处理器，如果未找到则返回默认处理器
   */
  static getHandler(code: string): MessageHandler {
    return this.handlers[code] || this.handlers.default
  }
}

/**
 * WebSocket 请求管理类
 * 负责管理 WebSocket 连接、心跳检测和消息处理
 */
class WsRequest {
  private lockReconnect: boolean // 重连锁，防止重复重连
  private url: string | undefined // WebSocket 连接地址
  private closeByUser: boolean // 是否用户主动关闭连接
  private heartTask: NodeJS.Timeout | null // 心跳定时器
  private reconnectTimeoutTask: NodeJS.Timeout | null // 重连定时器
  private socket: WebSocket | null // WebSocket 实例
  private readonly uuid: string // 客户端唯一标识
  private retryCount: number // 重连次数计数器
  private static instance: WsRequest // 单例实例
  /**
   * 私有构造函数，实现单例模式
   */
  private constructor() {
    this.lockReconnect = false
    this.url = ''
    this.closeByUser = false
    this.heartTask = null
    this.reconnectTimeoutTask = null
    this.socket = null
    this.uuid = ChatUtils.uuid()
    this.retryCount = 0
  }

  /**
   * 链接就绪状态
   */
  isOpen(): boolean {
    return this.socket?.readyState === WebSocket.OPEN
  }

  /**
   * 获取 WsRequest 单例实例
   */
  static getInstance(): WsRequest {
    if (!this.instance) {
      this.instance = new WsRequest()
    }
    return this.instance
  }

  /**
   * 初始化 WebSocket 连接
   */
  public init(): void {
    try {
      this.closeByUser = false
      this.url = this.buildWebSocketUrl()
      this.socket = new WebSocket(this.url)
      this.setupWebSocketHandlers()
    } catch (error) {
      console.error('WebSocket initialization failed:', error)
      this.handleConnectionError()
    }
  }

  /**
   * 构建 WebSocket 连接地址
   * @returns WebSocket 连接地址
   */
  private buildWebSocketUrl(): string {
    return `${vimConfig.wsProtocol}://${Auth.getIp()}:${vimConfig.wsPort}`
  }

  /**
   * 设置 WebSocket 事件处理函数
   */
  private setupWebSocketHandlers(): void {
    if (!this.socket) return

    this.socket.onopen = this.handleOpen.bind(this)
    this.socket.onclose = this.handleClose.bind(this)
    this.socket.onerror = this.handleError.bind(this)
    this.socket.onmessage = this.handleMessage.bind(this)
  }

  /**
   * 处理 WebSocket 连接打开事件
   */
  private handleOpen(): void {
    const ready: SendInfo<ReadyAuth> = {
      code: SendCode.READY,
      message: {
        token: Auth.getToken(),
        client: vimConfig.client,
        uuid: this.uuid
      }
    }
    this.send(JSON.stringify(ready))
    this.retryCount = 0
    if (this.reconnectTimeoutTask) {
      clearTimeout(this.reconnectTimeoutTask)
    }
    this.startHeartbeat()
  }

  /**
   * 处理 WebSocket 连接关闭事件
   */
  private handleClose(): void {
    if (!this.closeByUser) {
      this.handleConnectionError()
    }
  }

  /**
   * 处理 WebSocket 错误事件
   */
  private handleError(): void {
    this.handleConnectionError()
  }

  /**
   * 处理接收到的消息
   * @param event WebSocket 消息事件
   */
  private handleMessage(event: MessageEvent): void {
    try {
      const data = event.data
      if (data === pong) {
        this.startHeartbeat()
        return
      }

      const message: SendInfo<Message> = JSON.parse(data)
      this.processMessage(message)

      // 重置重试次数，因为收到了正常消息
      this.retryCount = 0
      this.startHeartbeat()
    } catch (error) {
      console.error('Message processing error:', error)
    }
  }

  /**
   * 使用策略模式处理不同类型的消息
   * @param message WebSocket 消息对象
   */
  private processMessage(message: SendInfo<Message | ReadReceipt | OtherLogin>): void {
    try {
      const handler = MessageHandlers.getHandler(message.code)
      handler.handle(message, this)
    } catch (error) {
      console.error('Error processing message:', error)
      console.error('Message:', message)
    }
  }

  /**
   * 处理连接错误
   * 包括重试逻辑和错误提示
   */
  private handleConnectionError(): void {
    if (this.retryCount < WS_CONFIG.MAX_RETRY_COUNT) {
      this.reconnect()
    } else {
      ElMessage.error('WebSocket连接失败，请检查网络连接')
      this.retryCount = 0 // 重置重试次数，允许下次重新尝试
      this.close()
      Auth.logout()
    }
  }

  /**
   * 启动心跳检测
   */
  private startHeartbeat(): void {
    this.clearTimers()
    this.heartTask = setTimeout(() => {
      this.send(ping)
      this.setupReconnectTimeout()
    }, WS_CONFIG.TIMEOUT)
  }

  /**
   * 设置重连超时定时器
   */
  private setupReconnectTimeout(): void {
    if (this.reconnectTimeoutTask) {
      clearTimeout(this.reconnectTimeoutTask)
    }
    this.reconnectTimeoutTask = setTimeout(() => {
      this.reconnect()
    }, WS_CONFIG.TIMEOUT_ERROR)
  }

  /**
   * 清除定时器
   */
  private clearTimers(): void {
    if (this.heartTask) {
      clearTimeout(this.heartTask)
    }
    if (this.reconnectTimeoutTask) {
      clearTimeout(this.reconnectTimeoutTask)
    }
  }

  /**
   * 发送消息
   * @param value 消息内容
   */
  public send(value: string): void {
    if (this.isOpen()) {
      this.socket?.send(value)
    } else {
      console.warn('WebSocket is not connected. Message not sent:', value)
    }
  }

  /**
   * 处理接收到的消息
   * @param message 消息对象
   */
  onmessage = (message: Message): void => {
    const user = useUserStore().user
    //群聊里面，自己发的消息不再显示
    if (user?.id === message.fromId) {
      message.mine = true
    }
    //友聊换chatId,chatId 不一样
    if (ChatType.FRIEND === message.chatType && user?.id !== message.fromId) {
      message.chatId = message.fromId
    }
    useChatStore().pushMessage(message).then()
    //确保消息已经渲染到页面上了，再去滚动到底部
    nextTick(() => {
      ChatUtils.imageLoad('message-box')
    }).then(() => {})
  }

  /**
   * 发送真正的聊天消息
   * @param message 消息对象
   */
  sendMessage(message: Message): void {
    const sendInfo = {
      code: SendCode.MESSAGE,
      message: message
    }
    this.send(JSON.stringify(sendInfo))
  }

  /**
   * 发送已读取消息
   * @param receipt 消息读取回执
   */
  sendReceipt(receipt: ReadReceipt): void {
    const sendInfo = {
      code: SendCode.READ,
      message: receipt
    }
    this.send(JSON.stringify(sendInfo))
  }

  /**
   *  立即验证连接有效性
   *  重置心跳检测和重连检测
   *  立刻发送一个心跳信息
   *  如果没有收到消息，就会执行重连
   */
  checkStatus(): void {
    // 清除定时器重新发送一个心跳信息
    if (this.heartTask) {
      clearTimeout(this.heartTask)
    }
    if (this.reconnectTimeoutTask) {
      clearTimeout(this.reconnectTimeoutTask)
    }
    this.lockReconnect = false
    this.send(ping)
    //onmessage拿到消息就会清理 reconnectTimeoutTask，如果没有清理，就会执行重连
    this.reconnectTimeoutTask = setTimeout(() => {
      this.reconnect()
    }, WS_CONFIG.TIMEOUT_ERROR - WS_CONFIG.TIMEOUT)
  }

  /**
   * 重连
   */
  reconnect(): void {
    if (this.lockReconnect) return

    this.clearTimers()
    this.lockReconnect = true

    setTimeout(() => {
      try {
        this.init()
      } catch (error) {
        console.error('Reconnection failed:', error)
      } finally {
        this.lockReconnect = false
        this.retryCount++
      }
    }, WS_CONFIG.RETRY_INTERVAL)
  }

  /**
   * 手动关闭连接
   */
  close(): void {
    this.clearTimers()
    this.lockReconnect = false
    //主动关闭
    this.closeByUser = true
    if (this.socket) {
      this.socket.close()
    }
  }

  /**
   * 获取客户端唯一标识
   * @returns 客户端唯一标识
   */
  public getUuid(): string {
    return this.uuid
  }
}

export default WsRequest
