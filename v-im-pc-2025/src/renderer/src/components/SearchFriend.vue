<template>
  <el-dialog v-model="show" title="添加好友" width="400px" :before-close="handleClose">
    <div>
      <el-form-item label="">
        <el-input v-model="mobile" placeholder="请输入用户名或手机号" class="input-with-select">
          <template #append>
            <el-button @click="search">查找</el-button>
          </template>
        </el-input>
      </el-form-item>
      <div
        v-for="(user, index) in users"
        :key="index"
        class="solid"
        :class="checkedUserId === user.id ? 'active' : ''"
        @click="check(user)"
      >
        <chat-item
          :id="user.id"
          :img="user.avatar"
          :username="user.name"
          :show-del="false"
        ></chat-item>
      </div>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="close">取消</el-button>
        <el-button type="primary" @click="add">确定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import UserApi from '@renderer/api/UserApi'
import FriendApi from '@renderer/api/FriendApi'
import ChatItem from '@renderer/components/ChatItem.vue'
import type { User } from '@renderer/mode/User'
import { ElMessage } from 'element-plus/es'
import addFriend from '@renderer/components/add-friend/index'
import { useUserStore } from '@renderer/store/userStore'

/**
 * 组件事件
 */
const emit = defineEmits(['close'])

/**
 * 用户存储
 */
const userStore = useUserStore()

/**
 * 当前用户ID
 */
const currentUserId = computed(() => userStore.user?.id || '')

/**
 * 搜索输入框的值
 */
const mobile = ref('')

/**
 * 搜索结果用户列表
 */
const users = ref<Array<User>>([])

/**
 * 好友ID集合，用于快速判断是否为好友
 */
const friendIds = ref<Set<string>>(new Set())

/**
 * 组件属性定义
 * @interface Props
 * @property {boolean} dialogVisible - 控制对话框显示状态
 */
interface Props {
  dialogVisible: boolean
}
const props = defineProps<Props>()

/**
 * 当前选中的用户ID
 */
const checkedUserId = ref<string>('')

/**
 * 计算属性：对话框显示状态
 * @returns {boolean} 对话框是否显示
 */
const show = computed(() => {
  return props.dialogVisible
})

/**
 * 加载所有好友
 * 一次性获取所有好友并存储其ID
 */
const loadAllFriends = async () => {
  try {
    const friends = await FriendApi.friends()
    const idSet = new Set<string>()

    friends.forEach((friend) => {
      idSet.add(friend.id)
    })

    friendIds.value = idSet
  } catch (error) {
    console.error('加载好友列表失败:', error)
    ElMessage.error('加载好友列表失败，可能影响搜索结果')
  }
}

/**
 * 组件挂载时加载好友列表
 */
onMounted(() => {
  loadAllFriends()
})

/**
 * 处理对话框关闭事件
 * 清空搜索框并触发关闭事件
 */
const handleClose = () => {
  mobile.value = ''
  emit('close')
}

/**
 * 重置组件状态
 * 清空搜索框和搜索结果
 */
const reset = () => {
  mobile.value = ''
  users.value = []
}

/**
 * 选中用户
 * @param {User} user - 要选中的用户对象
 */
const check = (user: User) => {
  checkedUserId.value = user.id
}

/**
 * 关闭对话框
 * 重置状态并触发关闭事件
 */
const close = () => {
  reset()
  emit('close')
}

/**
 * 检查用户是否已经是好友
 * @param {string} userId - 用户ID
 * @returns {boolean} 是否为好友
 */
const checkIsFriend = (userId: string): boolean => {
  return friendIds.value.has(userId)
}

/**
 * 过滤用户列表
 * 排除自己和已经是好友的用户
 * @param {Array<User>} userList - 用户列表
 * @returns {Array<User>} 过滤后的用户列表
 */
const filterUsers = (userList: Array<User>): Array<User> => {
  if (!userList || userList.length === 0) return []

  return userList.filter((user) => {
    // 排除自己
    if (user.id === currentUserId.value) {
      return false
    }

    // 排除已经是好友的用户
    return !checkIsFriend(user.id)
  })
}

/**
 * 搜索用户
 * 根据输入的用户名或手机号查找用户，并排除自己和已经是好友的用户
 */
const search = async () => {
  const text = mobile.value.trim()
  if (text && text !== '') {
    try {
      const searchResults = await UserApi.search(text)

      if (searchResults.length === 0) {
        ElMessage.info('未找到用户信息')
        users.value = []
        return
      }

      // 过滤掉自己和已经是好友的用户
      const filteredResults = filterUsers(searchResults)

      users.value = filteredResults

      if (filteredResults.length === 0) {
        ElMessage.info('搜索到的用户都已经是您的好友或是您自己')
      }
    } catch (error) {
      console.error('搜索用户失败:', error)
      ElMessage.error('搜索用户失败，请稍后重试')
    }
  } else {
    ElMessage.info('请输入查询条件')
  }
}

/**
 * 添加好友
 * 添加当前选中的用户为好友
 */
const add = () => {
  if (checkedUserId.value !== '') {
    close()
    addFriend(checkedUserId.value)
  } else {
    ElMessage.error('请选择一个用户')
  }
}
</script>

<style scoped>
.solid {
  border: 1px solid #eeeeee;
}

.solid.active {
  border: 1px solid #5acc3e;
}
</style>
