<template :key="uniqueId">
  <div v-if="chat" class="im-chat">
    <chat-top
      :group-user-count="groupUserCount"
      :is-validated-user="isValidatedUser"
      @toggle-show-right-box="showRightBox = !showRightBox"
    ></chat-top>
    <div class="im-chat-main">
      <div class="im-chat-main-left">
        <div id="message-box" class="im-chat-main-box messages">
          <ul>
            <template v-for="(item, index) in messageList" :key="`message-${item.id}`">
              <time-divider
                v-if="item.timestamp && shouldShowTimeDivider(index, item.timestamp)"
                :key="item.id"
                :timestamp="item.timestamp"
              />
              <li
                :id="`m-${item.id}`"
                :class="{
                  'im-chat-mine': item.fromId === user?.id,
                  'im-chat-other': item.fromId === user?.id
                }"
                :style="`padding-left: 60px;padding-right: 60px`"
              >
                <chat-message-user
                  v-if="item.messageType !== MessageType.EVENT"
                  class="im-chat-user"
                  :style="`${item.fromId === user?.id ? 'right' : 'left'}:0`"
                  :message="item"
                  :at-call-back="atNameCallback"
                />
                <component
                  :is="useMessageComponent(item.messageType)"
                  :message="item"
                  :history="false"
                ></component>
                <div
                  v-if="
                    chat.type === ChatType.FRIEND &&
                    item.fromId === user?.id &&
                    item.messageType !== MessageType.EVENT
                  "
                  class="read-tips"
                  :class="
                    !chat.lastReadTime || (item.timestamp ?? 0) > chat.lastReadTime ? '' : 'read'
                  "
                >
                  {{
                    !chat.lastReadTime || (item.timestamp ?? 0) > chat.lastReadTime
                      ? '未读'
                      : '已读'
                  }}
                </div>
              </li>
            </template>
          </ul>
        </div>
        <div class="im-chat-footer">
          <div class="im-chat-tool">
            <i class="iconfont icon-v-smile" title="发送表情" @click="showFace = !showFace"></i>

            <file-upload
              :chat="chat"
              :m-type="MessageType.IMAGE"
              :file-types="['png', 'jpg', 'jpeg', 'gif']"
              @upload-success="uploadBack"
            >
              <i class="iconfont icon-v-tupian" title="发送图片"></i>
            </file-upload>
            <file-upload
              :chat="chat"
              :m-type="MessageType.FILE"
              :file-types="uploadFileType"
              @upload-success="uploadBack"
            >
              <i class="iconfont icon-v-wj-wjj" title="发送文件"></i>
            </file-upload>
            <file-upload
              :chat="chat"
              :m-type="MessageType.VIDEO"
              :file-types="['mp4']"
              @upload-success="uploadBack"
            >
              <i class="iconfont icon-v-dianyingshipin" title="发送mp4视频"></i>
            </file-upload>
            <vim-faces
              v-if="showFace"
              class="faces-box"
              @click="showFace = false"
              @insert-face="insertFace"
            />
            <el-button
              v-if="isValidatedUser"
              class="history-message-btn"
              size="small"
              @click="history()"
              >聊天记录
            </el-button>
          </div>
          <div
            id="messageTextArea"
            ref="messageTextArea"
            :contenteditable="contenteditable"
            class="textarea"
            @keyup="atEvent"
            @dragover="handleDrag"
            @drop="handleDrop"
            @paste="handlePaste"
            @keydown.enter.prevent="handleEnter"
            @contextmenu="chatRightEvent($event)"
            v-html="ChatUtils.transform(messageContent)"
          ></div>
          <div class="im-chat-send">
            <el-button-group>
              <el-button type="primary" @click="mineSend()"> 发送 </el-button>
              <el-dropdown trigger="click" @command="handleSendKeyChange">
                <el-button type="primary">
                  <el-icon><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="enter" :class="{ 'is-active': sendKey === 'enter' }">
                      按Enter键发送
                    </el-dropdown-item>
                    <el-dropdown-item
                      command="alt+enter"
                      :class="{ 'is-active': sendKey === 'alt+enter' }"
                    >
                      按Alt+Enter键发送
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </el-button-group>
          </div>
        </div>
      </div>
      <chat-group-info v-if="showRightBox && ChatType.GROUP === chat.type" :group-id="chat.id" />
      <chat-user-info v-if="showRightBox && ChatType.FRIEND === chat.type" :user-id="chat.id" />
    </div>
  </div>

  <el-drawer v-model="showHistory" title="聊天记录" :with-header="false" size="60%">
    <history-message
      :from-id="user?.id"
      :chat-id="chat.id"
      :chat-type="chat.type"
      :show-history="showHistory"
    ></history-message>
  </el-drawer>
</template>

<script setup lang="ts">
// 引入Store相关
import { useChatStore } from '@renderer/store/chatStore'
import { useUserStore } from '@renderer/store/userStore'
import { useWsStore } from '@renderer/store/WsStore'
import { useMessageStore } from '@renderer/store/messageStore'
import { useTextAreaStore } from '@renderer/store/TextAreaStore'
import { useSysConfigStore } from '@renderer/store/sysConfigStore'

// 引入组件
import ChatMessageUser from '@renderer/components/ChatMessageUser.vue'
import VimFaces from '@renderer/components/VimFaces.vue'
import HistoryMessage from '@renderer/components/HistoryMessage.vue'
import ChatGroupInfo from '@renderer/components/ChatGroupInfo.vue'
import ChatTop from '@renderer/components/chat-top/index.vue'
import FileUpload from '@renderer/components/FileUpload.vue'

// 引入工具和类型
import type { Message } from '@renderer/message/Message'
import ChatUtils from '@renderer/utils/ChatUtils'
import ChatType from '@renderer/enum/ChatType'
import MessageType from '@renderer/enum/MessageType'
import type { User } from '@renderer/mode/User'
import { storeToRefs } from 'pinia'

// 引入hooks
import useMessageComponent from '@renderer/hooks/useMessageComponent'
import { groupNodes } from '@renderer/hooks/useUploadOperation'
import MessageApi from '@renderer/api/MessageApi'
import useChatInit from '@renderer/hooks/useChatInit'
import { useChatTextArea } from '@renderer/hooks/useChatTextArea'

import type { Ref } from 'vue'
import {
  computed,
  getCurrentInstance,
  nextTick,
  onActivated,
  onMounted,
  onUnmounted,
  ref,
  watch
} from 'vue'
import TimeDivider from '@renderer/components/TimeDivider.vue'
import type { Chat } from '@renderer/mode/Chat'
import ChatUserInfo from '@renderer/components/ChatUserInfo.vue'
import { ArrowDown } from '@element-plus/icons-vue'
import type { ComponentPublicInstance } from 'vue'

// 初始化聊天相关功能
const { isMaster, isValidatedUser, loadUserOrGroup, readMessage, users, groupUserCount } =
  useChatInit()

const instance = getCurrentInstance()
const proxy = instance?.proxy as ComponentPublicInstance & { $winControl: { isWeb: boolean } }
const chatStore = useChatStore()
const userStore = useUserStore()
const wsStore = useWsStore()
const textAreaStore = useTextAreaStore()
const { sysConfig } = storeToRefs(useSysConfigStore())
// 界面显示控制
const showRightBox = ref(false) //是否展示群用户
const showHistory = ref(false) //是否展示聊天记录
const isWeb = ref(proxy?.$winControl?.isWeb ?? false) //编辑模式，为了兼容web端
const contenteditable = computed(() => (proxy?.$winControl?.isWeb ? 'true' : 'plaintext-only'))

// 消息输入相关
const { messageTextArea } = storeToRefs(textAreaStore) //消息输入框
const uploadFileType = computed(() => {
  return sysConfig.value.uploadType.split(',')
}) //上传文件类型

// 用户和聊天信息
// 当前用户
const { user } = storeToRefs(userStore) as { user: Ref<User> }
const { chat } = storeToRefs(chatStore) //当前聊天
// 格式化时间
const shouldShowTimeDivider = (index: number, currentTimestamp: number) => {
  if (index === 0) return true
  const prevMessage = messageList.value[index - 1]
  const timeDiff = currentTimestamp - (prevMessage.timestamp ?? 0)
  return timeDiff > 5 * 60 * 1000
}

/**
 * 重置聊天室切换的变量，重置多选状态，清空输入框消息等
 * @param oldChat 切换前聊天室
 * @param newChat 当前聊天室
 */
const resetHandler = (oldChat: Chat | undefined, newChat: Chat) => {
  useMessageStore().setCheckWidth('0')
  //当切换聊天室时，清空输入框的消息
  if (messageTextArea.value && oldChat && newChat.id !== oldChat.id) {
    messageContent.value = ''
    messageTextArea.value.innerHTML = ''
  }
  //每次切换聊天室时候，不再显示群用户
  showRightBox.value = false
}

/**
 * 读取消息回执
 * @param newChat 当前聊天室
 */
const readReceiptMessage = (newChat: Chat) => {
  if (ChatType.FRIEND === newChat.type) {
    //获取读取消息的时间,这里要颠倒下，是查询对方的读取时间，不是自己的
    //所以聊天室chatId是自己,fromId是对方
    MessageApi.getReadTime(user.value.id, newChat.id).then((res) => {
      if (res) {
        chatStore.setCurrentChatLastReadTime(parseInt(res))
      }
    })
  }
  if (wsStore.wsRequest.isOpen()) {
    readMessage(newChat, user.value)
  } else {
    setTimeout(() => {
      readMessage(newChat, user.value)
    }, 1000)
  }
}

// 监听聊天室变化
watch(
  chat,
  (newChat, oldChat) => {
    //切换聊天室
    if (newChat && newChat.id != oldChat?.id) {
      resetHandler(oldChat, newChat)
      loadUserOrGroup(newChat.id, newChat.type, user.value)
      readReceiptMessage(newChat)
    }
  },
  {
    immediate: true,
    deep: true
  }
)

// 消息列表
const messageList = computed((): Array<Message> => {
  return chatStore.chatMessage[chat.value.id] ?? new Array<Message>()
})

// 定时刷新图片加载
const uniqueId = ref(0)
setInterval(() => {
  uniqueId.value = new Date().getTime()
  ChatUtils.imageLoad('message-box')
}, 600000)

/**
 * 发送消息
 * 处理不同类型消息的发送逻辑，包括文本、图片、文件和视频
 */
const mineSend = (): void => {
  if (!isValidatedUser.value || !messageTextArea.value) return

  const list = groupNodes(messageTextArea.value)
  for (let i = 0; i < list.length; i++) {
    const node = list[i]
    const msg = createMessageFromNode(node as HTMLElement)
    if (!msg) continue

    // 发送消息并清空输入框
    wsStore.sendMessage(msg)
  }

  // 清空输入框和引用消息
  messageContent.value = ''
  if (messageTextArea.value) messageTextArea.value.innerHTML = ''
}

/**
 * 根据节点类型创建对应的消息对象
 */
const createMessageFromNode = (node: HTMLElement | string): Message | null => {
  const msg: Message = {
    messageType: MessageType.TEXT,
    chatId: chat.value.id,
    fromId: user.value.id,
    content: '',
    chatType: chat.value.type
  }

  // 处理文本消息
  if (typeof node === 'string' && node.trim() !== '') {
    return createTextMessage(node, msg)
  }
  // 处理图片消息
  else if (typeof node !== 'string' && node.nodeName === 'IMG') {
    msg.messageType = MessageType.IMAGE
    msg.extend = { url: node.getAttribute('src') }
  }
  // 处理文件消息
  else if (typeof node !== 'string' && node.nodeName === 'A') {
    msg.messageType = MessageType.FILE
    msg.extend = { url: node.getAttribute('href') }
  }
  // 处理视频消息
  else if (typeof node !== 'string' && node.nodeName === 'VIDEO') {
    msg.messageType = MessageType.VIDEO
    msg.extend = { url: node.getAttribute('src') }
  } else {
    return null
  }

  return msg
}

/**
 * 创建文本类型的消息
 */
const createTextMessage = (node: string, msg: Message): Message | null => {
  msg.messageType = MessageType.TEXT
  msg.content = node.replace(/\n\n$/, '').replace(/\n$/, '')
  if (msg.content.trim() === '') {
    return null
  }

  msg.extend = {}

  // 处理@所有人
  if (isMaster.value && msg.content.includes('@[所有人]')) {
    msg.extend.atAll = true
  }

  // 处理@用户
  const atUserIds = getAtUserIds(msg.content)
  if (atUserIds.length > 0) {
    msg.extend.atUserIds = atUserIds
  }

  return msg
}

/**
 * 切换历史消息显示状态
 */
const history = () => {
  showHistory.value = !showHistory.value
}

// 从聊天文本区域hook获取功能
const {
  showFace,
  messageContent,
  atEvent,
  handleDrop,
  handleDrag,
  uploadBack,
  handlePaste,
  getAtUserIds,
  insertFace,
  chatRightEvent,
  atNameCallback
} = useChatTextArea({
  chat,
  users,
  isMaster,
  isWeb
})

// 发送快捷键设置
const sendKey = ref(localStorage.getItem('sendKey') || 'enter') // 默认使用Enter发送

/**
 * 处理发送快捷键变更
 */
const handleSendKeyChange = (key: string) => {
  sendKey.value = key
  localStorage.setItem('sendKey', key)
}

/**
 * 处理回车事件
 * 根据设置决定是发送还是换行
 */
const handleEnter = (e: KeyboardEvent): void => {
  if (sendKey.value === 'enter') {
    // Enter发送，Shift+Enter换行
    if (e.shiftKey) {
      document.execCommand('insertLineBreak')
    } else {
      mineSend()
    }
  } else {
    // Alt+Enter发送，Enter换行
    if (e.altKey) {
      mineSend()
    } else {
      document.execCommand('insertLineBreak')
    }
  }
}

// 组件生命周期钩子
onMounted(() => {
  nextTick(() => {
    ChatUtils.imageLoad('message-box')
  })
})

onActivated(() => {
  chatStore.setChatActive(true)
  chatStore.handleReceipt()
})

onUnmounted(() => {
  chatStore.setChatActive(false)
})
</script>

<style lang="less">
@import '../../assets/styles/theme.less';

.im-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.im-chat-top {
  border-bottom: 1px solid #cccccc;
  color: @color-default;
  padding: 0 0 0.2rem 1rem;
  font-size: 1.6rem;
  font-weight: bold;
  height: 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .menu {
    color: @color-default;
    display: flex;
    padding: 0 10px;
    width: 30px;
    justify-content: center;
    cursor: pointer;
  }
}

.user-model {
  .user-model-img {
    padding: 15px;
    text-align: center;

    img {
      border-radius: 50%;
    }
  }

  .user-model-item {
    display: flex;
    padding: 5px 0;

    label {
      flex: 2;
      font-weight: bold;
      text-align: right;
    }

    span {
      flex: 3;
    }
  }
}

.im-chat-main {
  flex: 1;
  display: flex;
  flex-direction: row;
  height: calc(100% - 40px);
  background-color: #f8f8f8;

  .im-chat-main-left {
    flex: 4;
    display: flex;
    flex-direction: column;

    .im-chat-main-box {
      flex: 1;
      padding: 1rem 1rem 0 1rem;
      overflow-x: hidden;
      overflow-y: auto;
    }
  }

  .message-img {
    max-width: 20rem;
  }

  .messages {
    width: 100%;
    height: calc(100% - 3rem);
    overflow-y: scroll;

    ul {
      width: 100%;

      li {
        position: relative;
        font-size: 0;
        margin-bottom: 10px;
        padding-left: 60px;
        min-height: 30px;

        .im-chat-text {
          margin-right: 60px;
          position: relative;
          line-height: 150%;
          margin-top: 25px;
          padding: 5px 10px;
          background-color: #e2e2e2;
          border-radius: 3px;
          color: #666;
          word-break: break-all;
          display: inline-block;
          vertical-align: top;
          font-size: 14px;

          img {
            vertical-align: middle;
          }

          &:after {
            content: '';
            position: absolute;
            left: -10px;
            top: 13px;
            width: 0;
            height: 0;
            border-style: solid dashed dashed;
            border-color: #e2e2e2 transparent transparent;
            overflow: hidden;
            border-width: 10px;
          }

          pre {
            width: 100%;
            white-space: pre-wrap;
            word-break: break-all;

            img {
              display: inline-block;
            }
          }
        }
      }
    }

    .im-chat-user {
      width: 4rem;
      height: 4rem;
      position: absolute;
      display: inline-block;
      vertical-align: top;
      font-size: 14px;
      left: 3px;
      right: auto;

      .message-info {
        position: absolute;
        left: 60px;
        top: -2px;
        width: 500px;
        line-height: 24px;
        font-size: 12px;
        white-space: nowrap;
        color: #999;
        text-align: left;
        font-style: normal;

        i {
          font-style: normal;
          padding-left: 15px;
        }
      }

      .right {
        right: 0;
        text-align: right;
        left: auto;

        i {
          padding-right: 15px;
        }
      }

      img {
        width: 4rem;
        height: 4rem;
      }
    }

    .im-chat-mine {
      text-align: right;
      padding-left: 0;
      padding-right: 60px;

      .im-chat-text {
        margin-right: 0;
        margin-left: 60px;
        text-align: left;
        background-color: @color-message-bg;
        color: #fff;
        display: inline-block;
        vertical-align: top;
        font-size: 14px;

        img {
          vertical-align: middle;
        }

        &:after {
          left: auto;
          right: -10px;
          border-top-color: @color-message-bg;
        }
      }

      .im-chat-user {
        left: auto;
        right: 3px;

        cite {
          left: auto;
          right: 60px;
          text-align: right;

          i {
            padding-left: 0;
            padding-right: 15px;
          }
        }

        .message-info {
          right: 60px !important;
          display: inline-block;
        }
      }
    }
  }
}

.im-chat-multiple {
  border-top: 1px solid @color-gray;
  height: 15rem;
  flex-direction: column;
  padding: 10px;

  .close {
    text-align: right;

    .icon-v-close {
      cursor: pointer;
      display: inline-block;
    }
  }
}

.im-chat-footer {
  border-top: 1px solid @color-gray;
  height: 15rem;
  display: flex;
  flex-direction: column;

  .im-chat-tool {
    padding: 0.5rem 1rem;
    height: 3.4rem;
    position: relative;

    i {
      font-size: 2rem;
      cursor: pointer;
      margin: 1rem;
    }

    .faces-box {
      position: absolute;
      bottom: 3.8rem;
    }

    .ivu-upload {
      display: inline-block;
    }

    .history-message-btn {
      float: right;
    }
  }

  .textarea {
    border: 0;
    padding: 0.5rem;
    width: 100%;
    flex: 1;
    resize: none;
    background-color: @color-box-bg !important;
    height: 100%;
    text-rendering: optimizeLegibility;
    word-break: break-word;
    overflow-y: scroll;

    img {
      max-width: 50px;
      max-height: 50px;
    }

    &:focus {
      border: 0;
    }
  }

  .im-chat-send {
    height: 4rem;
    text-align: right;
    padding: 0 1rem 1rem 0;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 8px;
  }
}

.ivu-scroll-wrapper {
  margin: 0 !important;
}

.ivu-scroll-container {
  padding: 15px 15px 5px;
  overflow-y: visible !important;
}

/* 重新覆盖iview 里面的 model 小于768px 时候 宽度变100% 的问题 */
@media (max-width: 768px) {
  .user-model {
    .ivu-modal {
      width: 30rem !important;
      margin: 0 auto;
    }
  }
}

.history-message {
  width: 80%;
  height: calc(100% - 30px);
}

.page {
  position: fixed;
  bottom: 0;
  width: 100%;
  margin: 0.5rem;
}

.ivu-drawer-body {
  padding: 0 !important;

  .messages {
    height: calc(100% - 3rem);
  }
}

.model-footer {
  text-align: right;
  margin: 10px;
}

.icon-v-voice-right:after {
  transform: rotate(180deg) !important;
}

.message-img {
  max-width: 50px;
  max-height: 50px;
}

.read-tips {
  font-size: 10px;
  line-height: 24px;
}

.read {
  color: #5fb878;
}

.check-message {
  position: absolute;
  display: inline-block;
}

.im-chat-multiple-item {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 15px;
  cursor: pointer;
}

.is-active {
  color: @color-message-bg;
  font-weight: bold;
}

.el-dropdown {
  display: inline-flex;
}

/* Add custom styling for the dropdown button to make it narrower */
.im-chat-send .el-button-group .el-dropdown .el-button {
  width: 30px;
  min-width: 30px;
  padding-left: 5px;
  padding-right: 5px;
}
</style>
