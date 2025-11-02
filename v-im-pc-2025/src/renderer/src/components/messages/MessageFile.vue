<template>
  <div class="im-chat-text">
    <a
      v-if="message.extend && message.extend.fileName && message.extend.url"
      class="file-box"
      :title="getFileName(message.extend.fileName)"
      :href="message.extend?.url"
      @click="openFile($event, message.extend?.url, message.extend?.fileName)"
    >
      <div class="file-icon">
        <template v-if="isImageIcon">
          <img :src="iconSrc" alt="文件图标" class="file-image-icon" />
        </template>
        <template v-else>
          <i :class="iconClass"></i>
        </template>
      </div>
      <div class="file-text">
        <div class="file-name">{{ getFileName(message.extend?.fileName) }}</div>
      </div>
    </a>
  </div>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance } from 'vue'
import type { Message } from '@renderer/message/Message'
import ChatUtils from '@renderer/utils/ChatUtils'
import getFileName from '@renderer/utils/FileUtils'
import zipIcon from '@renderer/assets/icon/ZIP.png'
import wordIcon from '@renderer/assets/icon/WORD.png'
import pptIcon from '@renderer/assets/icon/PPT.png'
import pdfIcon from '@renderer/assets/icon/PDF.png'
import excelIcon from '@renderer/assets/icon/EXCEL.png'
import txtIcon from '@renderer/assets/icon/TXT.png'
//eslint-disable-next-line @typescript-eslint/ban-ts-comment
//@ts-ignore
const { proxy } = getCurrentInstance()!
interface Props<T> {
  message: T
  history: boolean
}
const props = defineProps<Props<Message>>()

const openFile = (event: Event, url: string, fileName: string) => {
  event.preventDefault()
  ChatUtils.openFile(url, fileName, proxy)
}

const fileExtension = computed(() => {
  return props.message.extend?.fileName?.split('.').pop()?.toLowerCase()
})

const isImageIcon = computed(() => {
  const imageExtensions = ['zip', 'rar', 'doc', 'docx', 'xls', 'xlsx', 'pdf', 'ppt', 'pptx', 'txt']
  if (fileExtension.value) {
    return imageExtensions.includes(fileExtension.value)
  } else return ''
})

const iconSrc = computed(() => {
  switch (fileExtension.value) {
    case 'zip':
      return zipIcon
    case 'rar':
      return zipIcon
    case 'doc':
      return wordIcon
    case 'docx':
      return wordIcon
    case 'xls':
      return excelIcon
    case 'xlsx':
      return excelIcon
    case 'pdf':
      return pdfIcon
    case 'ppt':
      return pptIcon
    case 'pptx':
      return pptIcon
    case 'txt':
      return txtIcon
    default:
      return ''
  }
})

const iconClass = computed(() => {
  if (isImageIcon.value) {
    return ''
  }
  return 'iconfont icon-v-xiazai'
})
</script>

<style lang="less" scoped>
.im-chat-text {
  width: 50%;
}
.file-box {
  width: 100%;
  display: flex;
  background-color: #efefef;
  color: #666666;

  .file-icon {
    background-color: #cccccc;
    padding: 10px;
    width: 60px;
    flex-shrink: 0;

    .file-image-icon {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }

    .iconfont {
      line-height: normal;
      font-size: 4rem;
    }

    .icon-zip {
      color: #ffa000;
    }

    .icon-word {
      color: #4285f4;
    }

    .icon-excel {
      color: #0f9d58;
    }

    .icon-pdf {
      color: #db4437;
    }
  }

  .file-text {
    width: 0;
    padding: 10px;
    flex: 5;
    display: flex;
    align-items: center;
    flex-shrink: 0;
    overflow: hidden;

    .file-name {
      -webkit-line-clamp: 2;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      overflow-wrap: break-word;
      word-break: break-all;
    }
  }
}
</style>
