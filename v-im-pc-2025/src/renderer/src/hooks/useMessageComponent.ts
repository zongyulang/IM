import type { Component } from 'vue'
import MessageType from '@renderer/enum/MessageType'
import MessageText from '@renderer/components/messages/MessageText.vue'
import MessageImage from '@renderer/components/messages/MessageImage.vue'
import MessageFile from '@renderer/components/messages/MessageFile.vue'
import MessageVoice from '@renderer/components/messages/MessageVoice.vue'
import MessageVideo from '@renderer/components/messages/MessageVideo.vue'
import MessageEvent from '@renderer/components/messages/MessageEvent.vue'

const useMessageComponent = (type: string): Component | null => {
  switch (type) {
    case MessageType.TEXT:
      return MessageText
    case MessageType.IMAGE:
      return MessageImage
    case MessageType.FILE:
      return MessageFile
    case MessageType.VOICE:
      return MessageVoice
    case MessageType.VIDEO:
      return MessageVideo
    case MessageType.EVENT:
      return MessageEvent
    default:
      return null
  }
}

export default useMessageComponent
