<template>
  <div
    class="im-chat-text"
    style="max-width: 300px; line-height: 100%; font-size: inherit"

  >
    <video style="width: 100%" controls playsinline>
      <source :src="message.extend?.url" type="video/mp4" />
    </video>
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

<style scoped></style>
