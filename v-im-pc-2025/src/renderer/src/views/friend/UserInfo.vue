<template>
  <div v-if="user" class="main-view">
    <div class="text-center" style="margin-bottom: 30px">
      <vim-avatar :img="user.avatar" size="large" :name="user.name" />
    </div>
    <el-descriptions title="用户信息" class="description" :column="2">
      <el-descriptions-item label="姓名">{{ user.name }}</el-descriptions-item>
      <el-descriptions-item label="性别"
        >{{ user.sex === '0' ? '男' : '女' }}
      </el-descriptions-item>
      <el-descriptions-item label="电话">{{ user.mobile }} </el-descriptions-item>
      <el-descriptions-item label="邮箱">{{ user.email }}</el-descriptions-item>
      <el-descriptions-item label="部门" :role="2">
        <span v-for="item in deptList" :key="item.id">{{ item.name }},</span>
      </el-descriptions-item>
    </el-descriptions>
    <el-button class="send-btn" @click="send()">发送消息</el-button>
  </div>
</template>

<script setup lang="ts">
import { reactive, toRefs, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UserApi from '@renderer/api/UserApi'
import DeptApi from '@renderer/api/DeptApi'
import type { Dept } from '@renderer/mode/Dept'
import { useChatStore } from '@renderer/store/chatStore'
import type { User } from '@renderer/mode/User'
import VimAvatar from '@renderer/components/VimAvatar.vue'
import ChatType from '@renderer/enum/ChatType'

const store = useChatStore()
const router = useRouter()
const route = useRoute()

interface IData {
  user: User | null
  deptList: Array<Dept>
}

const data = reactive<IData>({
  user: null,
  deptList: new Array<Dept>()
})

const loadUserData = async (id: string | string[]) => {
  data.user = null
  data.deptList = []
  if (typeof id === 'string') {
    const userData = await UserApi.getUser(id)
    data.user = userData
    if (!userData || 'null' === userData.deptId || !userData.deptId) {
      return
    }
    const deptData = await DeptApi.parent(userData.deptId)
    deptData.forEach((item) => {
      data.deptList.push(item)
    })
  }
}

watch(
  () => route.params.id,
  async (newId, oldId) => {
    if (newId && newId !== oldId) {
      await loadUserData(newId)
    }
  },
  { immediate: true }
)

const send = () => {
  if (data.user) {
    store.openChat({
      id: data.user.id,
      name: data.user.name,
      avatar: data.user.avatar,
      type: ChatType.FRIEND,
      unreadCount: 0
    })
    router.push('/index/chat')
  }
}
const { user, deptList } = toRefs(data)
</script>

<style scoped lang="less">
.description {
  padding: 20px;
  background-color: #ffffff;
}

.send-btn {
  float: right;
  margin-top: 15px;
}
</style>
