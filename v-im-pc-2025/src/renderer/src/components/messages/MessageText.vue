<template>
  <div class="im-chat-text" data-id="message.id">
    <pre v-html="ChatUtils.transformXss(message.content)"></pre>
  </div>
</template>

<script setup lang="ts">
import ChatUtils from '@renderer/utils/ChatUtils'
import type { Message } from '@renderer/message/Message'

interface Props<T> {
  message: T
  history?: boolean
}
defineProps<Props<Message>>()
</script>

<style scoped></style>
