<template>
  <div v-if="fromUser" @click="showUser(message.fromId, true)">
    <vim-avatar
      :img="fromUser.avatar"
      :name="fromUser.name"
      @contextmenu="groupChatUserRightEvent(fromUser.name, true, atCallBack, $event)"
    />
    <div v-if="message.fromId === user?.id" class="message-info right">
      <span>{{ fromUser.name }}</span>
    </div>
    <div v-if="message.fromId !== user?.id" class="message-info">
      <span>{{ fromUser.name }}</span>
    </div>
  </div>
</template>
<script lang="ts" setup>
import type { Ref } from 'vue'
import { computed } from 'vue'
import { useUserStore } from '@renderer/store/userStore'
import type { Message } from '@renderer/message/Message'
import VimAvatar from '@renderer/components/VimAvatar.vue'
import groupChatUserRightEvent from '@renderer/hooks/useGroupChatUserRightEvent'
import { storeToRefs } from 'pinia'
import type { User } from '@renderer/mode/User'
import showUser from '@renderer/components/user-modal'
const props = defineProps<Props<Message>>()
const userStore = useUserStore()
const { user } = storeToRefs(userStore) as { user: Ref<User> }
interface Props<T> {
  message: T
  atCallBack?: (item: string) => void
}
const fromUser = computed(() => {
  return userStore.getMapUser(props.message.fromId)
})
</script>
<style scoped lang="less">
.message-info {
  width: auto !important;
}
</style>
