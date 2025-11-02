<template>
  <div class="im-chat-event" :class="(message.fromId === user.id) ? 'mine' : ''">
    <el-tag type="info">
      {{ message.content }}
    </el-tag>
  </div>
</template>

<script setup lang="ts">
import type { Message } from '@renderer/message/Message'
import { storeToRefs } from 'pinia'
import type { Ref } from 'vue'
import type { User } from '@renderer/mode/User'
import { useUserStore } from '@renderer/store/userStore'
const { user } = storeToRefs(useUserStore()) as { user: Ref<User> }
interface Props<T> {
  message: T
  history: boolean
}

defineProps<Props<Message>>()
</script>

<style>
.im-chat-event {
  margin-top: 10px;
  margin-left: -60px;
  font-size: 12px;
  text-align: center;
  color: #999;
}

.im-chat-event.mine {
  margin-left: 0;
}

.im-chat-event > div > div {
  background-color: #f8f8f8;
  font-size: 12px;
  color: #999999;
}
</style>
