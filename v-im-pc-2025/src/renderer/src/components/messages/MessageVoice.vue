<template>
  <div
    class="im-chat-text"
    :style="{
      width: message.extend?.time
        ? message.extend.time > 40
          ? '50%'
          : message.extend.time + 10 + '%'
        : '0%'
    }"
  >
    <div @click="handleAudio(message)">
      <i class="iconfont icon-v-voice" :class="{ 'icon-v-voice-right': message.mine }"></i>
    </div>
  </div>
  <!--播放语音-->
  <audio ref="Audio"></audio>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { Message } from '@renderer/message/Message'
import type { ExtendVoice } from '@renderer/message/ExtendVoice'

interface Props<T> {
  message: T
  history: boolean
}
defineProps<Props<Message<ExtendVoice>>>()
//语音
const Audio = ref()
//控制播放还是暂停音频文件
const handleAudio = (item: Message) => {
  if (Audio.value.paused) {
    Audio.value.src = item.extend?.url
    Audio.value.play()
  } else {
    Audio.value.src = ''
    Audio.value.stop()
  }
}
</script>

<style scoped></style>
