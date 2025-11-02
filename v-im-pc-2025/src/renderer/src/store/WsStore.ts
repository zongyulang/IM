import { defineStore } from 'pinia'
import WsRequest from '@renderer/api/WsRequest'
import type { Message } from '@renderer/message/Message'
import type { ReadReceipt } from '@renderer/message/ReadReceipt'

export const useWsStore = defineStore('online_store', {
  state: () => ({
    wsRequest: WsRequest.getInstance(),
    //是否失去焦点
    blur: false,
    //是否睡眠状态
    sleep: false
  }),

  actions: {
    /**
     * 初始化websocket
     */
    init(): void {
      this.wsRequest.init()
    },
    /**
     * 关闭websocket
     */
    close(): void {
      this.wsRequest.close()
    },
    /**
     * 发送消息
     * @param str 消息内容
     */
    send(str: string): void {
      this.wsRequest.send(str)
    },
    /**
     * 发送消息
     * @param message 消息对象
     */
    sendMessage(message: Message): void {
      this.wsRequest.sendMessage(message)
    },
    /**
     * 发送已读回执
     * @param receipt 回执对象
     */
    sendReceipt(receipt: ReadReceipt): void {
      this.wsRequest.sendReceipt(receipt)
    },
    /**
     * 检查状态
     */
    checkStatus(): void {
      this.wsRequest.checkStatus()
    },
    /**
     * 设置是否失去焦点
     * @param blur 失去焦点
     */
    setBlur(blur: boolean): void {
      this.blur = blur
    },
    /**
     * 设置是否睡眠
     * @param sleep 睡眠
     */
    setSleep(sleep: boolean): void {
      this.sleep = sleep
    }
  }
})
