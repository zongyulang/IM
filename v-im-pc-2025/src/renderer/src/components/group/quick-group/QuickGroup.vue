<template>
  <el-drawer v-model="drawer" title="快速建群" size="50%" direction="rtl" @close="close">
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
        @click.stop="quickGroupHandle"
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
import { computed, onMounted, type Ref, ref } from 'vue'
import GroupApi from '@renderer/api/GroupApi'
import { ElMessage } from 'element-plus/es'
import { match } from 'pinyin-pro'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@renderer/store/userStore'

interface UserCheck {
  user: User
  isCheck: boolean
}

interface Props {
  startUserId: string
  closeDialog: () => void
}
const { user } = storeToRefs(useUserStore()) as { user: Ref<User> }
const props = defineProps<Props>()
const keyword = ref('')
const friends = ref<UserCheck[]>([])
const drawer = ref(true)
const loading = ref(false)
const saveLoading = ref(false)

/**
 * 搜索过滤函数 - 根据关键词过滤好友列表
 * @param items 好友列表
 * @param keyword 搜索关键词
 * @returns 过滤后的好友列表
 */
const keywordFilter = (items: UserCheck[], keyword: string): UserCheck[] => {
  return items.filter((item) => {
    return !!(keyword.trim() === '' || match(item.user.name, keyword))
  })
}

/**
 * 初始化组件
 * 获取好友列表并设置初始状态
 */
onMounted(async () => {
  loading.value = true
  try {
    const friendList = await FriendApi.friends()
    friends.value = friendList.map((item: User) => {
      // 如果是startUserId指定的用户，默认选中
      const isStartUser = item.id === props.startUserId
      return {
        user: item,
        isCheck: isStartUser
      }
    })
    friends.value.push({ user: user.value, isCheck: true })

  } catch (error) {
    console.error('获取好友列表失败:', error)
    ElMessage.error('获取好友列表失败')
  } finally {
    loading.value = false
  }
})

/**
 * 计算属性：已选择的好友列表
 */
const checkedFriends = computed((): UserCheck[] => {
  return friends.value.filter((item) => item.isCheck)
})

/**
 * 计算属性：已选择的用户ID列表
 */
const userIds = computed((): string[] => {
  return checkedFriends.value.map((item) => item.user.id)
})

/**
 * 计算属性：是否禁用保存按钮
 */
const canSave = computed((): boolean => {
  return userIds.value.length === 0
})

/**
 * 创建群聊
 * 使用已选好友创建新的群聊
 */
const quickGroupHandle = async () => {
  try {
    saveLoading.value = true
    // 生成群名称（最多显示前9个成员的名字）
    const groupName = `${checkedFriends.value
      .slice(0, 9)
      .map((item) => item.user.name)
      .join(',')}的群聊`

    await GroupApi.quickGroup({
      name: groupName,
      userIds: userIds.value
    })

    ElMessage.success('创建群聊成功')
    close()
  } catch (error) {
    console.error('创建群聊失败:', error)
    ElMessage.error('创建群聊失败')
  } finally {
    saveLoading.value = false
  }
}

/**
 * 关闭对话框
 */
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
