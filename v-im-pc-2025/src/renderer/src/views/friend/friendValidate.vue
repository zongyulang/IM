<template>
  <div style="padding: 0 15px">
    <el-table :data="waitCheckList" style="width: 100%">
      <el-table-column prop="userId" label="新的好友">
        <template #default="scope">
          <user-name-tag :id="scope.row.userId" />
        </template>
      </el-table-column>
      <el-table-column prop="message" label="消息" />
      <el-table-column prop="createTime" label="时间" />
      <el-table-column prop="id" label="审核" width="140px">
        <template #default="scope">
          <el-button type="primary" size="small" @click="agree(scope.row.userId)">同意</el-button>
          <el-button type="danger" size="small" @click="refuse(scope.row.userId)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import FriendApi from '@renderer/api/FriendApi'
import { useFriendStore } from '@renderer/store/friendStore'
import UserNameTag from '@renderer/components/UserNameTag.vue'
import { storeToRefs } from 'pinia'
import { onMounted } from 'vue'

const { waitCheckList } = storeToRefs(useFriendStore())
const friendStore = useFriendStore()

/**
 * 同意加好友
 * @param userId userId
 */
const agree = async (userId: string) => {
  await FriendApi.agree(userId)
  await friendStore.loadData()
  await friendStore.loadWaitCheckList()
  friendStore.sendTips(userId)
}

/**
 * 拒绝邀请
 * @param userId 邀请id
 */
const refuse = async (userId: string) => {
  await FriendApi.delete(userId)
  await friendStore.loadData()
  await friendStore.loadWaitCheckList()
  friendStore.notifyFlushFriendStore(userId)
}

onMounted(async () => {
  await friendStore.loadWaitCheckList()
})
</script>

<style scoped></style>
