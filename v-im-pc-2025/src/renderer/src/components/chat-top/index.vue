<template>
  <div class="im-chat-top">
    <div style="flex: 1">
      <span>{{ chat.name }}</span>
      <span v-if="chat.type === ChatType.GROUP">（{{ groupUserCount }}人）</span>
    </div>
    <div
      v-if="chat.type === ChatType.FRIEND"
      class="pull-right menu"
      @click="showUser(chat.id, false)"
    >
      <i class="iconfont icon-v-xinxi" />
    </div>
    <div
      v-if="chat.type === ChatType.GROUP && isValidatedUser"
      class="pull-right menu"
      @click="showGroup(chat.id, false)"
    >
      <i class="iconfont icon-v-xinxi" />
    </div>
    <div
      v-if="isValidatedUser"
      class="pull-right menu"
      @click="$emit('toggleShowRightBox')"
    >
      <i class="iconfont icon-v-gengduo" />
    </div>
    <div
      v-if="chat.type === ChatType.GROUP && !isValidatedUser"
      class="pull-right"
      style="font-size: 12px; margin-right: 10px"
    >
      非群用户
    </div>
  </div>
</template>
<script setup lang="ts">
import { storeToRefs } from 'pinia'
import ChatType from '@renderer/enum/ChatType'
import showGroup from '@renderer/components/group/info'
import showUser from '@renderer/components/user-modal'
import { useChatStore } from '@renderer/store/chatStore'
const { chat } = storeToRefs(useChatStore())
interface IProps {
  groupUserCount: number
  isValidatedUser: boolean
}

defineProps<IProps>()
defineEmits(['toggleShowRightBox'])
</script>
<style scoped lang="less"></style>
