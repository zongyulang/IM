<template>
  <!-- 对话框组件，用于显示群组信息 -->
  <el-dialog v-model="open" width="40rem" center :show-close="false" :close-on-click-modal="false">
    <!-- 如果群组信息存在，则显示群组的头像和名称 -->
    <div v-if="group" class="info">
      <vim-avatar :img="group.avatar" :name="group.name" />
      <div>
        <div>{{ group.name }}</div>
      </div>
    </div>
    <!-- 对话框底部的操作按钮 -->
    <template #footer>
      <span class="dialog-footer">
        <!-- 关闭对话框的按钮 -->
        <el-button @click="close">关闭</el-button>
        <!-- 显示群组详情的按钮 -->
        <el-button @click="showGroup">详情</el-button>
        <!-- 如果showSend为真，则显示发送消息的按钮 -->
        <el-button v-if="showSend" type="primary" @click="send()">聊天</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useChatStore } from '@renderer/store/chatStore'
import { useRouter } from 'vue-router'
import VimAvatar from '../../VimAvatar.vue'
import GroupApi from '@renderer/api/GroupApi'
import ChatType from '@renderer/enum/ChatType'
import type { Group } from '@renderer/mode/Group'
import { useGroupStore } from '@renderer/store/groupStore'
import { storeToRefs } from 'pinia'

// 初始化群组存储和聊天存储
const groupStore = useGroupStore()
const { groupList } = storeToRefs(useGroupStore())

const router = useRouter()
const chatStore = useChatStore()
// 定义 emits 对象，用于触发关闭对话框的事件
defineEmits(['close'])
// 控制对话框的打开状态
const open = ref(false)
// 接收父组件传递的属性，包括群组ID、是否显示发送按钮和关闭对话框的方法
const props = defineProps<{
  groupId: string
  showSend: boolean
  closeDialog: () => void
}>()

// 存储从API获取的群组信息
const group = ref<Group>()

// 根据群组ID加载群组信息
const loadGroup = async (groupId: string) => {
  group.value = await GroupApi.get(groupId)
}

// 在组件挂载时，如果存在群组ID，则打开对话框并加载群组信息
onMounted(async () => {
  if (props.groupId) {
    open.value = true
    await loadGroup(props.groupId)
  }
})

// 发送消息给群组
const send = () => {
  if (group.value) {
    props.closeDialog()
    chatStore.openChat({
      id: group.value.id,
      name: group.value.name,
      avatar: group.value.avatar,
      type: ChatType.GROUP,
      unreadCount: 0
    })
    router.push('/index/chat')
  }
}

// 关闭对话框
const close = () => {
  props.closeDialog()
}

// 显示群组详情
const showGroup = () => {
  props.closeDialog()
  groupList.value.forEach((item, index) => {
    if (item.id === props.groupId) {
      groupStore.setCheckIndex(index)
    }
  })
  router.push(`/index/group/${props.groupId}`)
}
</script>

<style scoped lang="less">
// 群组信息的样式
.info {
  text-align: center;
  line-height: 200%;
}

// 描述的样式
.description {
  padding: 20px 20px 0px 20px;
  background-color: #ffffff;
}
</style>
