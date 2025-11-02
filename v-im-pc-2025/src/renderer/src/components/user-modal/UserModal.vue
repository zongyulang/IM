<template>
  <el-dialog
    v-model="open"
    width="40rem"
    center
    :show-close="false"
    :close-on-click-modal="false"
    @close="close"
  >
    <!-- 如果用户信息存在，则显示用户头像和详细信息 -->
    <div v-if="user" class="info">
      <vim-avatar :img="user.avatar" :name="user.name" size="large" />
      <el-descriptions class="description" :column="2">
        <el-descriptions-item label="姓名">{{ user.name }} </el-descriptions-item>
        <el-descriptions-item label="性别"
          >{{ user.sex === '0' ? '男' : '女' }}
        </el-descriptions-item>
        <el-descriptions-item label="电话">{{ user.mobile }} </el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ user.email }} </el-descriptions-item>
      </el-descriptions>
    </div>
    <!-- 对话框底部的操作按钮 -->
    <template #footer>
      <span class="dialog-footer">
        <!-- 关闭对话框的按钮 -->
        <el-button @click="close">关闭</el-button>
        <!-- 如果不是好友且允许添加好友，则显示加好友按钮 -->
        <el-button v-if="!isFriend && userSetting.canAddFriend" type="info" @click="add()"
          >加好友</el-button
        >
        <!-- 如果允许发送消息，则显示聊天按钮 -->
        <el-button
          v-if="(showSend && isFriend) || userSetting.canSendMessage"
          type="primary"
          @click="send()"
          >聊天</el-button
        >
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import VimAvatar from '@renderer/components/VimAvatar.vue'
import { useChatStore } from '@renderer/store/chatStore'
import { useUserStore } from '@renderer/store/userStore'
import { useRouter } from 'vue-router'
import UserApi from '@renderer/api/UserApi'
import FriendApi from '@renderer/api/FriendApi'
import type { User } from '@renderer/mode/User'
import ChatType from '@renderer/enum/ChatType'
import SettingApi from '@renderer/api/SettingApi'
import addFriend from '@renderer/components/add-friend'
import DictUtils from '@renderer/utils/DictUtils'

const router = useRouter()
const store = useChatStore()
const userStore = useUserStore()
const isFriend = ref(true)
const open = ref(false)

interface UserModalProps {
  userId: string
  showSend: boolean
  closeDialog: () => void
}

const props = defineProps<UserModalProps>()

const user = ref<User>()
const userSetting = reactive({
  canAddFriend: false,
  addFriendValidate: false,
  canSendMessage: false,
  canSoundRemind: false,
  canVoiceRemind: false
})

/**
 * 获取当前用户是否是好友
 */
const getIsFriend = async () => {
  try {
    isFriend.value = await FriendApi.isFriend(props.userId)
    if (props.userId === userStore.user?.id) {
      isFriend.value = true
    }
  } catch (error) {
    console.error('获取好友状态失败:', error)
  }
}

/**
 * 获取用户信息和设置
 */
const getUser = async () => {
  try {
    user.value = await UserApi.getUser(props.userId)
    const setting_ = await SettingApi.get(props.userId)
    userSetting.canAddFriend = setting_.canAddFriend === DictUtils.YES
    userSetting.addFriendValidate = setting_.addFriendValidate === DictUtils.YES
    userSetting.canSendMessage = setting_.canSendMessage === DictUtils.YES
    userSetting.canSoundRemind = setting_.canSoundRemind === DictUtils.YES
    userSetting.canVoiceRemind = setting_.canVoiceRemind === DictUtils.YES
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

/**
 * 组件挂载时，如果存在用户ID，则打开对话框并加载用户信息和好友状态
 */
onMounted(async () => {
  if (props.userId) {
    open.value = true
    await getUser()
    await getIsFriend()
  }
})

/**
 * 关闭对话框
 */
const close = () => {
  props.closeDialog()
}

/**
 * 添加好友
 */
const add = () => {
  if (!isFriend.value) {
    addFriend(props.userId)
    close()
  }
}

/**
 * 发送消息给用户
 */
const send = () => {
  props.closeDialog()
  if (user.value) {
    store.openChat({
      id: user.value.id,
      name: user.value.name,
      avatar: user.value.avatar,
      type: ChatType.FRIEND,
      unreadCount: 0
    })
  }
  router.push('/index/chat')
}
</script>

<style scoped lang="less">
.info {
  text-align: center;
  line-height: 200%;
}

.description {
  padding: 20px 20px 0 20px;
  background-color: #ffffff;
}
</style>
