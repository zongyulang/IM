import type { Ref } from 'vue'
import { computed, ref } from 'vue'
import { useAtStore } from '@renderer/store/atStore'
import type { User } from '@renderer/mode/User'
import type { Chat } from '@renderer/mode/Chat'
import { dragEvent, pasteEvent, uploadSuccess } from '@renderer/hooks/useUploadOperation'
import atMenu from '@renderer/components/at-menu'
import ChatType from '@renderer/enum/ChatType'
import { storeToRefs } from 'pinia'
import { useSysConfigStore } from '@renderer/store/sysConfigStore'
import { useTextAreaStore } from '@renderer/store/TextAreaStore'
import type { menusType } from 'vue3-menus'
import { menusEvent } from 'vue3-menus'
import type { ExtendFile } from '@renderer/message/ExtendFile'

/**
 * 聊天文本区域组件的Props接口
 */
interface ChatMessageTextAreaProps {
  chat: Ref<Chat> // 当前聊天对象
  users: Ref<User[]> // 用户列表
  isMaster: Ref<boolean> // 是否是群主
  isWeb: Ref<boolean> // 是否是Web模式
}

/**
 * 聊天文本区域的Hook
 * 处理消息输入、表情、@功能、文件上传等功能
 */
export function useChatTextArea({ chat, users, isMaster, isWeb }: ChatMessageTextAreaProps) {
  const { keyword } = storeToRefs(useAtStore())
  const showFace = ref(false) // 是否显示表情面板
  const messageContent = ref('') // 消息内容
  const { sysConfig } = storeToRefs(useSysConfigStore())
  const { messageTextArea } = storeToRefs(useTextAreaStore())

  // 上传文件类型
  const uploadFileType = computed(() => {
    return sysConfig.value.uploadType.split(',')
  })

  /**
   * @事件处理
   * 处理@功能的键盘事件
   * @param e 键盘事件
   * @returns boolean 是否触发了@事件
   */
  const atEvent = (e: KeyboardEvent) => {
    if (chat.value.type !== ChatType.GROUP) {
      return false
    }
    const html = messageTextArea.value?.innerHTML
    if (html) {
      const lastIsAt = html.endsWith('@')
      if (e.key === '@' || (lastIsAt && html.length > 0)) {
        //光标位置信息
        const item = window.getSelection()?.getRangeAt(0).getClientRects().item(0)
        if (users.value) {
          atMenu(isMaster.value, users.value, item?.x ?? 0, item?.y ?? 0, atCallback)
        }
      } else {
        const regex = /@[\s\S]*$/
        const match = html.match(regex)
        if (match) {
          useAtStore().setKeyword(match[0].substring(1))
        }
      }
    }
    return false
  }

  /**
   * 处理拖拽事件
   * 支持文件拖拽上传
   * @param e 拖拽事件
   */
  const handleDrop = (e: DragEvent) => {
    e.stopPropagation()
    e.preventDefault()
    if (messageTextArea.value) {
      dragEvent(e, uploadFileType.value, messageTextArea.value)
    }
  }

  /**
   * 处理拖动事件
   * 设置拖动效果为复制
   * @param e 拖动事件
   */
  const handleDrag = (e: DragEvent) => {
    e.stopPropagation()
    e.preventDefault()
    if (e.dataTransfer) {
      e.dataTransfer.dropEffect = 'copy'
    }
  }

  /**
   * 处理粘贴事件
   * 支持粘贴文件和图片
   * @param e 粘贴事件
   */
  const handlePaste = async (e: Event) => {
    if (messageTextArea.value) {
      console.log('粘贴事件',e)
      await pasteEvent(e as ClipboardEvent, uploadFileType.value, messageTextArea.value)
    }
  }

  /**
   * 从输入框中获取@用户的id
   * 解析输入框中的@标记，返回对应的用户ID数组
   * @param str 输入的信息
   * @returns 用户ID数组
   */
  const getAtUserIds = (str: string): Array<string> => {
    if (users.value) {
      const regex = /@\[([^\]]+)\]/g
      let match
      const usernames = new Array<string>()
      while ((match = regex.exec(str)) !== null) {
        usernames.push(match[1])
      }
      const ids = users.value
        .filter((user) => {
          return usernames.indexOf(user.name) > -1
        })
        .map((user) => {
          return user.id
        })
      return Array.from(new Set(ids))
    } else {
      return []
    }
  }

  /**
   * 插入表情
   * @param item 表情代码
   */
  const insertFace = (item: string) => {
    messageContent.value = messageTextArea.value?.innerHTML + 'face' + item
    showFace.value = false
  }

  /**
   * @功能回调
   * 在输入框中插入@用户标记
   * @param item 用户名
   */
  const atCallback = (item: string) => {
    const str = messageTextArea.value?.innerHTML.replace(new RegExp(keyword.value + '$'), '')
    messageContent.value = `${str}[${item}]`
  }

  /**
   * @名称回调
   * 在输入框中插入@用户名标记
   * @param item 用户名
   */
  const atNameCallback = (item: string) => {
    const str = messageTextArea.value?.innerHTML.replace(new RegExp(keyword.value + '$'), '')
    messageContent.value = `${str}@[${item}]`
  }

  /**
   * 上传成功回调
   * 处理文件上传成功后的操作
   * @param chat chat
   * @param extend 附件信息
   * @param messageType 消息类型
   */
  const uploadBack = (chat: Chat, extend: ExtendFile, messageType: string) => {
    if (messageTextArea.value) {
      uploadSuccess(chat, extend, messageType, messageTextArea.value)
    }
  }

  /**
   * 输入框右键菜单处理
   * 非Web模式下支持右键粘贴功能
   * @param event 鼠标事件
   */
  const chatRightEvent = (event: MouseEvent) => {
    //web模式下没有办法执行粘贴操作
    if (!isWeb.value) {
      const menus = [
        {
          label: '粘贴',
          click: () => {
            document.execCommand('paste')
          }
        }
      ]
      //修复历史消息右键菜单无法显示的问题
      const menusType: menusType = {
        menus: menus,
        zIndex: 99999999999999
      }
      menusEvent(event, menusType, null)
      event.preventDefault()
    }
  }

  return {
    showFace,
    messageContent,
    atEvent,
    uploadBack,
    handleDrop,
    handleDrag,
    handlePaste,
    insertFace,
    atCallback,
    atNameCallback,
    chatRightEvent,
    getAtUserIds
  }
}
