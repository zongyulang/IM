<template>
  <div style="padding: 0 15px">
    <div>
      <el-button size="small" type="primary" :icon="Back" plain @click="back" />
    </div>
    <el-table :data="list" style="width: 100%">
      <el-table-column prop="fromId" label="邀请人" width="180">
        <template #default="scope">
          <user-name-tag :id="scope.row.fromId" />
        </template>
      </el-table-column>
      <el-table-column prop="userId" label="被邀请人" width="180">
        <template #default="scope">
          <user-name-tag :id="scope.row.userId" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="邀请时间" />
      <el-table-column prop="id" label="审核" width="150px">
        <template #default="scope">
          <el-button type="primary" size="small" @click="agree(scope.row.id, scope.row.userId)"
            >同意
          </el-button>
          <el-button type="danger" size="small" @click="refuse(scope.row.id)">拒绝 </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import GroupInviteApi from '@renderer/api/GroupInviteApi'
import type { Ref } from 'vue'
import { ref, onMounted } from 'vue'
import UserNameTag from '@renderer/components/UserNameTag.vue'
import { ElMessage } from 'element-plus/es'
import type { GroupInvite } from '@renderer/mode/GroupInvite'
import { useRoute } from 'vue-router'
import type { Message } from '@renderer/message/Message'
import MessageType from '@renderer/enum/MessageType'
import ChatType from '@renderer/enum/ChatType'
import { useUserStore } from '@renderer/store/userStore'
import { useGroupStore } from '@renderer/store/groupStore'
import { useWsStore } from '@renderer/store/WsStore'
import router from '../../router'
import { Back } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import type { User } from '@renderer/mode/User'

const userStore = useUserStore()
const groupStore = useGroupStore()
const route = useRoute()
const { user } = storeToRefs(userStore) as { user: Ref<User> }
const list = ref<GroupInvite[]>([])
const groupId = route.query.id

const loadInviteList = async () => {
  if (typeof groupId === 'string') {
    try {
      list.value = await GroupInviteApi.list(groupId)
    } catch (error) {
      console.error('加载邀请列表失败:', error)
      ElMessage.error('加载邀请列表失败')
    }
  }
}

onMounted(() => {
  loadInviteList()
})

/**
 * 同意邀请
 * @param id 邀请id
 * @param userId userId
 */
const agree = async (id: string, userId: string) => {
  try {
    await GroupInviteApi.agree(id)
    list.value = list.value.filter((item) => item.id !== id)
    ElMessage.success('已同意')
    if (typeof groupId === 'string') {
      const content = '新用户 ' + userStore.getMapUser(userId)?.name + ' 入群'
      const msg: Message = {
        messageType: MessageType.EVENT,
        chatId: groupId,
        fromId: user.value.id,
        mine: true,
        content: content,
        timestamp: new Date().getTime(),
        chatType: ChatType.GROUP
      }
      useWsStore().wsRequest.sendMessage(msg)
      await groupStore.loadData()
      await loadInviteList();
    }
  } catch (error) {
    console.error('同意邀请失败:', error)
    ElMessage.error('操作失败')
  }
}

/**
 * 拒绝邀请
 * @param id 邀请id
 */
const refuse = async (id: string) => {
  try {
    await GroupInviteApi.refuse(id)
    list.value = list.value.filter((item) => item.id !== id)
    ElMessage.success('已拒绝')
    await groupStore.loadData()
    await loadInviteList()
  } catch (error) {
    console.error('拒绝邀请失败:', error)
    ElMessage.error('操作失败')
  }
}

const back = () => {
  router.back()
}
</script>

<style scoped></style>
