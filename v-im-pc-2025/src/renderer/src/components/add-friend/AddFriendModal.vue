<template>
  <el-dialog v-model="open" width="40rem" center :show-close="false" :close-on-click-modal="false">
    <div v-if="friend" class="info">
      <vim-avatar :img="friend.avatar" :name="friend.name" />
      <div>{{ friend.name }}</div>
      <el-form-item v-if="userSetting.addFriendValidate">
        <el-input v-model="message" type="textarea" placeholder="验证消息" />
      </el-form-item>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="loading" :disabled="loading" @click="add()"
          >确定</el-button
        >
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import UserApi from '@renderer/api/UserApi'
import SettingApi from '@renderer/api/SettingApi'
import type { User } from '@renderer/mode/User'
import VimAvatar from '@renderer/components/VimAvatar.vue'
import FriendApi from '@renderer/api/FriendApi'
import { ElMessage } from 'element-plus'
import DictUtils from '@renderer/utils/DictUtils'
import { useUserStore } from '@renderer/store/userStore'
import { useFriendStore } from '@renderer/store/friendStore'
import { newFriendNotify } from '@renderer/hooks/useNotifyHandler'
import AddFriendResultEnum from '@renderer/enum/AddFriendResultEnum'

const open = ref(true)
const friend = ref<User>()
const message = ref<string>('')
const userSetting = reactive({
  canAddFriend: false,
  addFriendValidate: false,
  canSendMessage: false,
  canSoundRemind: false,
  canVoiceRemind: false
})
defineEmits(['close'])

const props = defineProps<{
  friendId: string
  closeDialog: () => void
}>()

const loading = ref(false)

onMounted(async () => {
  try {
    friend.value = await UserApi.getUser(props.friendId)
    const setting_ = await SettingApi.get(props.friendId)
    userSetting.canAddFriend = setting_.canAddFriend === DictUtils.YES
    userSetting.addFriendValidate = setting_.addFriendValidate === DictUtils.YES
    userSetting.canSendMessage = setting_.canSendMessage === DictUtils.YES
    userSetting.canSoundRemind = setting_.canSoundRemind === DictUtils.YES
    userSetting.canVoiceRemind = setting_.canVoiceRemind === DictUtils.YES
  } catch (error) {
    console.error('获取用户或设置失败', error)
  }
})

/**
 * 添加好友
 */
const add = async () => {
  loading.value = true
  try {
    const result = await FriendApi.add({
      friendId: props.friendId,
      userId: useUserStore().user?.id,
      message: message.value
    })

    const handleSuccess = async () => {
      ElMessage.success(result.msg)
      await useFriendStore().loadData()
      await newFriendNotify(props.friendId)
    }

    switch (result.code) {
      case AddFriendResultEnum.SUCCESS:
        await handleSuccess()
        break
      case AddFriendResultEnum.WAIT_CHECK:
        ElMessage.warning(result.msg)
        useFriendStore().notifyFlushFriendStore(props.friendId)
        break
      case AddFriendResultEnum.NOT_ALLOW_FRIEND:
      case AddFriendResultEnum.ALREADY_FRIEND:
      case AddFriendResultEnum.ALREADY_REQUEST:
        ElMessage.warning(result.msg)
        break
      default:
        ElMessage.error('未知错误')
        break
    }
  } finally {
    closeDialog()
    loading.value = false
  }
}

/**
 * 关闭
 */
const closeDialog = () => {
  props.closeDialog()
}
</script>

<style scoped lang="less">
.info {
  text-align: center;
  line-height: 200%;
}

.description {
  padding: 20px 20px 0px 20px;
  background-color: #ffffff;
}
</style>
