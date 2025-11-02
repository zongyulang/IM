<template>
  <div class="im-chat-text">
    <img
      v-preview:name="message.chatId"
      alt="图片"
      :src="message.extend?.url"
      style="width: 100%"
    />
  </div>
</template>

<script setup lang="ts">
import type { Message } from '@renderer/message/Message'

interface Props<T> {
  message: T
  history: boolean
}

defineProps<Props<Message>>()
</script>

<style scoped>
.im-chat-text {
  max-width: 300px;
  line-height: 100%;
  font-size: inherit;
}
</style>
