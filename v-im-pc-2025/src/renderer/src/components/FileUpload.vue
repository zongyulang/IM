<template>
  <!-- 使用 Element Plus 的文件上传组件 -->
  <el-upload
    :action="`${host}/vim/upload`"
    :headers="headers"
    :show-file-list="false"
    :on-success="handleSuccess"
    :on-error="handleError"
    :before-upload="beforeUpload"
    :accept="acceptedFileTypes"
    class="file-upload"
  >
    <!-- 自定义内容的插槽 -->
    <slot></slot>
  </el-upload>
</template>

<script setup lang="ts">
// 导入必要的 Vue 和 Element Plus 组件
import { reactive, toRefs, computed } from 'vue'
import FetchRequest from '@renderer/api/FetchRequest'
import Auth from '@renderer/api/Auth'
import type { MessageHandler } from 'element-plus'
import { ElMessage } from 'element-plus'
import MessageType from '@renderer/enum/MessageType'
import { storeToRefs } from 'pinia'
import { useSysConfigStore } from '@renderer/store/sysConfigStore'
import { Loading } from '@element-plus/icons-vue'
import type { ExtendFile } from '@renderer/message/ExtendFile'
import type { Chat } from '@renderer/mode/Chat'

// 从 store 中获取系统配置
const { sysConfig } = storeToRefs(useSysConfigStore())
// 定义 Vim 数据和组件属性的接口
interface VimData {
  host: string
  headers: {
    'sa-token': string
  }
}

interface Props {
  fileTypes: string[]
  mType: string
  chat: Chat
}

// 设置默认属性
const props = withDefaults(defineProps<Props>(), {
  fileTypes: () => [],
  mType: ''
})

// 变量用于保存加载消息处理器
let loadingMessage: MessageHandler | undefined = undefined

// 添加一个变量来存储上传开始时的 chat
let currentChat: Chat | null = null

// 响应式对象用于保存 Vim 数据
const vimData = reactive<VimData>({
  host: FetchRequest.getHost(),
  headers: {
    'sa-token': Auth.getToken()
  }
})

// 定义上传成功事件的 emits
const emits = defineEmits(['uploadSuccess'])

// 上传前的回调，用于验证文件类型和大小
// @param file - 要上传的文件
const beforeUpload = (file: File) => {
  // 在上传开始时保存当前的 chat
  currentChat = props.chat

  // 获取文件后缀并检查是否符合允许的类型
  const suffix = file.name.substring(file.name.lastIndexOf('.') + 1)
  const suffixes = props.fileTypes
  const len = suffixes.filter((item) => {
    return item === suffix.toLowerCase()
  }).length
  if (len === 0) {
    ElMessage.error('不支持的文件类型,仅支持：' + suffixes.join(','))
    return false
  }
  // 检查文件大小是否符合系统配置
  const size = file.size
  if (size > sysConfig.value.uploadSize) {
    ElMessage.error(`文件大小不能超过${sysConfig.value.uploadSize / 1024 / 1024}MB`)
    return false
  }
  // 显示加载消息
  loadingMessage = ElMessage({
    dangerouslyUseHTMLString: true,
    message: `正在上传文件：${file.name}`,
    type: 'info',
    icon: Loading,
    customClass: 'loading-icon',
    duration: 0
  })
  return true
}

// 上传成功的回调
// @param res - 上传的结果
const handleSuccess = (res) => {
  // 关闭加载消息
  if (loadingMessage) {
    loadingMessage.close()
  }
  // 使用保存的 currentChat 而不是 props.chat
  emits('uploadSuccess', currentChat, getByMessageType(res.data), props.mType)
}

// 上传失败的回调
const handleError = () => {
  // 显示错误消息
  ElMessage.error('上传失败')
  // 关闭加载消息
  if (loadingMessage) {
    loadingMessage.close()
  }
}

// 根据消息类型格式化上传结果数据的函数
interface UploadResult {
  url: string
  viewUrl: string
  fileName?: string
  originalFilename?: string
}

// 根据消息类型返回格式化后的上传结果数据
const getByMessageType = (res: UploadResult): ExtendFile => {
  switch (props.mType) {
    case MessageType.FILE:
      return { url: res.url, viewUrl: res.viewUrl, fileName: res.originalFilename }
    case MessageType.VIDEO:
      return { url: res.url, fileName: res.originalFilename }
    default:
      return { url: res.url }
  }
}

// 解构 Vim 数据引用
const { host, headers } = toRefs(vimData)

// 计算属性，用于格式化上传组件的接受文件类型
const acceptedFileTypes = computed(() => {
  return props.fileTypes.map((type) => `.${type}`).join(',')
})
</script>

<style scoped>
.file-upload {
  display: inline-block;
}
</style>
