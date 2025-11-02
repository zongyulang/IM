<template>
  <el-drawer v-model="drawer" title="添加群成员" size="50%" direction="rtl" @close="close" custom-class="add-group-user-drawer">
    <el-row class="d-row" :gutter="20">
      <el-col :span="12">
        <div style="margin-bottom: 10px">
          <el-input v-model="keyword" placeholder="搜索"></el-input>
        </div>
        <el-scrollbar class="list">
          <div
            v-for="item in keywordFilter(friends, keyword)"
            :key="item.user.id"
            v-loading="loading"
            class="user"
          >
            <div class="avatar">
              <vim-avatar :img="item.user.avatar" :name="item.user.name" />
            </div>
            <div class="name">{{ item.user.name }}</div>
            <div class="state">
              <el-checkbox v-model="item.isCheck" size="large"></el-checkbox>
            </div>
          </div>
        </el-scrollbar>
      </el-col>
      <el-col :span="12">
        <el-scrollbar class="list">
          <div v-for="item in checkedFriends" :key="item.user.id" class="user">
            <div class="avatar">
              <vim-avatar :img="item.user.avatar" :name="item.user.name" />
            </div>
            <div class="name">{{ item.user.name }}</div>
            <div class="state"></div>
          </div>
        </el-scrollbar>
      </el-col>
    </el-row>
    <div class="footer text-right">
      <el-button
        type="primary"
        :loading="saveLoading"
        :disabled="canSave"
        @click.stop="handleAddGroupUser"
      >
        确定
      </el-button>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import VimAvatar from '../../VimAvatar.vue'
import FriendApi from '@renderer/api/FriendApi'
import type { User } from '@renderer/mode/User'
import { useUserStore } from '@renderer/store/userStore'
import type { Ref } from 'vue'
import { computed, onMounted, ref } from 'vue'
import GroupApi from '@renderer/api/GroupApi'
import ChatType from '@renderer/enum/ChatType'
import type { Group } from '@renderer/mode/Group'
import MessageType from '@renderer/enum/MessageType'
import { ElMessage } from 'element-plus/es'
import { useWsStore } from '@renderer/store/WsStore'
import SendCode from '@renderer/enum/SendCode'
import { match } from 'pinyin-pro'
import { storeToRefs } from 'pinia'
import { useGroupStore } from '@renderer/store/groupStore'

interface UserCheck {
  user: User
  isCheck: boolean
}

interface Props {
  group: Group
  closeDialog: () => void
}

const props = defineProps<Props>()
const keyword = ref('')
const userStore = useUserStore()
const friends = ref(new Array<UserCheck>())
const { user } = storeToRefs(userStore) as { user: Ref<User> }
const drawer = ref(true)
const loading = ref(false)
const saveLoading = ref(false)
/**
 * 聊天过滤器
 * @param items 聊天列表
 * @param keyword 关键词
 */
const keywordFilter = (items: UserCheck[], keyword: string): UserCheck[] => {
  return items.filter((item) => {
    return !!(keyword.trim() === '' || match(item.user.name, keyword))
  })
}
const tempList = new Array<UserCheck>()
onMounted(async () => {
  loading.value = true
  const friendList = await FriendApi.friends()
  friendList.forEach((item: User) => {
    tempList.push({ user: item, isCheck: false })
  })
  const users = await GroupApi.users(props.group.id)
  const ids = users.map((item: User) => item.id)
  friends.value = tempList.filter((item) => ids.indexOf(item.user.id) === -1)
  loading.value = false
})

const checkedFriends = computed((): Array<UserCheck> => {
  return friends.value.filter((item) => item.isCheck)
})
const userIds = computed((): string[] => {
  return checkedFriends.value.map((item) => item.user.id)
})
const canSave = computed((): boolean => {
  return userIds.value.length === 0
})

//执行添加到数据库
const handleAddGroupUser = async () => {
  try {
    saveLoading.value = true
    const res = await GroupApi.addUsers(props.group.id, userIds.value)
    if (res.length === 0) {
      ElMessage.warning('请等待审核')
      if (user.value) {
        notifyMaster(props.group.master, user.value.id)
      }
    }
    await useGroupStore().loadGroupData(props.group.id)
    props.closeDialog()
  } catch (error) {
    console.error('添加群成员失败:', error)
  } finally {
    saveLoading.value = false
  }
}
/**
 * 通知群主
 * @param masterId 群主id
 * @param fromId 发送人
 */
const notifyMaster = (masterId: string, fromId: string) => {
  if (masterId) {
    const sendInfo = {
      code: SendCode.GROUP_REQUEST,
      message: {
        mine: true,
        fromId: fromId,
        chatId: masterId,
        chatType: ChatType.FRIEND,
        messageType: MessageType.TEXT,
        content: '',
        timestamp: new Date().getTime()
      }
    }
    useWsStore().send(JSON.stringify(sendInfo))
  }
}

const close = () => {
  props.closeDialog()
}
</script>

<style scoped lang="less">
.d-row {
  height: 90%;
}

.user {
  display: flex;
  background-color: #eeeeee;
  padding: 5px 10px;
  margin-bottom: 5px;

  .avatar {
    width: 6rem;
    display: flex;
    align-items: center;
    justify-content: flex-start;
  }

  .name {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: flex-start;
  }

  .state {
    width: 6rem;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.footer {
  position: fixed;
  right: 15px;
  bottom: 15px;
}

</style>

