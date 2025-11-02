// 导入必要的依赖和类型
import type { MessageHandler } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@renderer/store/userStore'
import { useWsStore } from '@renderer/store/WsStore'
import MessageType from '@renderer/enum/MessageType'
import FetchRequest from '@renderer/api/FetchRequest'
import type { Message } from '@renderer/message/Message'
import type { User } from '@renderer/mode/User'
import { Loading } from '@element-plus/icons-vue'
import type { ExtendFile } from '@renderer/message/ExtendFile'
import type { Chat } from '@renderer/mode/Chat'
import { useChatStore } from '@renderer/store/chatStore'
import ChatUtils from '@renderer/utils/ChatUtils'

/**
 * 判断文件是否为图片类型
 * @param file 要检查的文件
 * @returns 如果是图片返回true，否则返回false
 */
const isImageFile = (file: File): boolean => file.type.indexOf('image') !== -1

/**
 * 判断文件是否为MP4视频类型
 * @param file 要检查的文件
 * @returns 如果是MP4视频返回true，否则返回false
 */
const isVideoFile = (file: File): boolean => file.type.indexOf('video/mp4') !== -1

/**
 * 检查文件类型是否在支持的文件类型列表中
 * @param file 要检查的文件
 * @param uploadFileType 支持的文件类型列表
 * @returns 如果文件类型被支持返回true，否则返回false
 */
const isSupportedFileType = (file: File, uploadFileType: string[]): boolean => {
  const suffix = file.name.substring(file.name.lastIndexOf('.') + 1).toLowerCase()
  return uploadFileType.includes(suffix)
}

/**
 * 根据文件类型获取对应的消息类型
 * @param file 文件对象
 * @param uploadFileType 支持的文件类型列表
 * @returns 返回对应的消息类型，如果不支持则返回null
 */
const getFileMessageType = (file: File, uploadFileType: string[]): string | null => {
  if (isImageFile(file)) return MessageType.IMAGE
  if (isVideoFile(file)) return MessageType.VIDEO
  if (isSupportedFileType(file, uploadFileType)) return MessageType.FILE
  return null
}

/**
 * 将经过安全处理的内容追加到消息文本区域
 * @param content 要追加的内容
 * @param messageTextArea 消息文本区域元素
 */
const appendSanitizedContent = (content: string, messageTextArea: HTMLElement): void => {
  const finalClean = ChatUtils.cleanHtml(content)
  if (finalClean) {
    messageTextArea.innerHTML = `${messageTextArea.innerHTML}${finalClean}`
    // 将光标移动到内容的最后
    const range = document.createRange()
    range.selectNodeContents(messageTextArea)
    range.collapse(false) // 将光标移动到内容的最后

    const selection = window.getSelection()
    selection?.removeAllRanges()
    selection?.addRange(range)

    // 确保 textarea 是聚焦状态
    messageTextArea.focus()
  }
}

/**
 * 文件处理类
 * 用于处理文件上传、验证和消息展示的相关操作
 */
export class FileHandler {
  private readonly uploadFileType: string[]
  private readonly messageTextArea: HTMLElement

  /**
   * 构造函数
   * @param options 选项对象，包含支持的文件类型列表和消息文本区域元素
   */
  constructor(options: { uploadFileType: string[]; messageTextArea: HTMLElement }) {
    this.uploadFileType = options.uploadFileType
    this.messageTextArea = options.messageTextArea
  }

  /**
   * 处理文件上传
   * @param chat
   * @param file 要上传的文件
   */
  async handleFile(chat: Chat, file: File): Promise<void> {
    const messageType = getFileMessageType(file, this.uploadFileType)

    if (!messageType) {
      ElMessage.error(`不支持的文件类型,仅支持：${this.uploadFileType.join(',')}`)
      return
    }
    await this.uploadFile(chat, file, messageType)
  }

  /**
   * 执行文件上传操作
   * @param chat
   * @param file 要上传的文件
   * @param messageType 消息类型
   */
  private async uploadFile(chat: Chat, file: File, messageType: string): Promise<void> {
    let loadingMessage: MessageHandler | undefined = undefined
    try {
      if (messageType !== MessageType.IMAGE) {
        await this.confirmUpload(file)
      }
      loadingMessage = ElMessage({
        dangerouslyUseHTMLString: true,
        message: `正在上传文件：${file.name}`,
        type: 'info',
        icon: Loading,
        customClass: 'loading-icon',
        duration: 0
      })
      const data = await FetchRequest.upload(file)
      const extend: ExtendFile = {
        url: data.url,
        fileName: file.name
      }

      if (extend.url) {
        uploadSuccess(chat, extend, messageType, this.messageTextArea)
      }
    } catch (error) {
      console.error('文件上传失败:', error)
      ElMessage.error(error instanceof Error ? error.message : '文件上传失败')
    } finally {
      if (loadingMessage) {
        loadingMessage.close()
      }
    }
  }

  /**
   * 确认文件上传
   * @param file 要上传的文件
   */
  private async confirmUpload(file: File): Promise<void> {
    try {
      await ElMessageBox.confirm(`确定发送【${file.name}】吗?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      })
    } catch {
      throw new Error('用户取消上传')
    }
  }
}

/**
 * 处理拖拽事件
 * @param e 拖拽事件对象
 * @param uploadFileType 支持的文件类型列表
 * @param messageTextArea 消息文本区域元素
 */
export const dragEvent = (
  e: DragEvent,
  uploadFileType: string[],
  messageTextArea: HTMLElement
): void => {
  e.preventDefault()
  const files = e.dataTransfer?.files
  if (!files?.length) return
  const chat = useChatStore().chat
  const fileHandler = new FileHandler({ uploadFileType, messageTextArea })
  Array.from(files).forEach((file) => fileHandler.handleFile(chat, file))
}

/**
 * 处理粘贴事件
 * @param e 粘贴事件对象
 * @param uploadFileType 支持的文件类型列表
 * @param messageTextArea 消息文本区域元素
 */
export const pasteEvent = async (
  e: ClipboardEvent,
  uploadFileType: string[],
  messageTextArea: HTMLElement
): Promise<void> => {
  e.preventDefault()
  const items = e.clipboardData?.items
  if (!items?.length) return

  const fileHandler = new FileHandler({ uploadFileType, messageTextArea })

  // 处理剪贴板项
  const item = items[items.length > 1 ? 1 : 0]

  const file = item.getAsFile()
  const chat = useChatStore().chat
  if (file) {
    await fileHandler.handleFile(chat, file)
  } else if (item.type === 'text/html') {
    item.getAsString((html) => {
      const parser = new DOMParser()
      const doc = parser.parseFromString(html, 'text/html')
      const htmlContent = doc.body.innerHTML.replaceAll('\\n', '')
      //复制的a标签的url
      const regex =
        '<!--StartFragment--><a\\s+href="((http|https)?:\\/\\/[^"]+)"[^>]*>([^<]*)<\\/a><!--EndFragment-->'
      const arr = htmlContent.match(regex)
      if (arr) {
        appendSanitizedContent(arr[1], messageTextArea)
      } else {
        appendSanitizedContent(doc.body.innerHTML, messageTextArea)
      }
    })
  } else {
    item.getAsString((text) => appendSanitizedContent(text, messageTextArea))
  }
}

/**
 * 检查节点是否为文本节点
 * @param node 要检查的节点
 * @returns 如果是文本节点返回true，否则返回false
 */
export const isText = (node: Element): boolean => node.nodeType === Node.TEXT_NODE

/**
 * 检查节点是否为表情图片节点
 * @param node 要检查的节点
 * @returns 如果是表情图片节点返回true，否则返回false
 */
export const isFace = (node: Element): boolean =>
  node.nodeName.toUpperCase() === 'IMG' && node.getAttribute('data-face') !== null

/**
 * 将消息文本区域的节点分组
 * @param messageTextArea 消息文本区域元素
 * @returns 返回分组后的节点数组
 */
export const groupNodes = (messageTextArea: Element): Array<string | Element> => {
  const result: Array<string | Element> = []
  let text = ''

  messageTextArea.childNodes.forEach((node) => {
    const element = node as Element

    if (isText(element)) {
      text += element.textContent ?? ''
    } else if (isFace(element)) {
      text += `face${element.getAttribute('alt')}`
    } else {
      if (text) {
        result.push(text)
        text = ''
      }
      result.push(element)
    }
  })

  if (text) {
    result.push(text)
  }

  return result
}

/**
 * 上传成功的通用回调处理
 * @param chat 上传时候的chat
 * @param extend 附件信息
 * @param messageType 消息类型
 * @param messageTextArea 消息文本区域元素
 */
export function uploadSuccess(
  chat: Chat,
  extend: ExtendFile,
  messageType: string,
  messageTextArea: HTMLElement
): void {
  if (messageType === MessageType.IMAGE) {
    messageTextArea.innerHTML += `<img class="message-img" alt="图片" src="${extend.url}"/>`
    return
  }

  const user = useUserStore().user as User
  if (!user) {
    throw new Error('用户未登录')
  }

  const message: Message = {
    messageType,
    chatId: chat.id,
    fromId: user.id,
    mine: true,
    content: '',
    chatType: chat.type,
    timestamp: Date.now(),
    extend
  }

  useWsStore().sendMessage(message)
}
