<template>
  <el-drawer v-model="drawer" title="删除群成员" size="50%" direction="rtl" @close="close" custom-class="add-group-user-drawer">
    <el-row class="d-row" :gutter="20">
      <el-col :span="12">
        <div style="margin-bottom: 10px">
          <el-input v-model="keyword" placeholder="搜索"></el-input>
        </div>
        <el-scrollbar class="list">
          <div
            v-for="item in keywordFilter(groupUsers, keyword)"
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
          <div v-for="item in checkedUsers" :key="item.user.id" class="user">
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
        type="danger"
        :loading="saveLoading"
        :disabled="canSave"
        @click.stop="handleDeleteGroupUsers"
      >
        删除
      </el-button>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import VimAvatar from '../../VimAvatar.vue'
import type { User } from '@renderer/mode/User'
import { computed, onMounted, ref } from 'vue'
import GroupApi from '@renderer/api/GroupApi'
import type { Group } from '@renderer/mode/Group'
import { ElMessage, ElMessageBox } from 'element-plus/es'
import { useGroupStore } from '@renderer/store/groupStore'
import { match } from 'pinyin-pro'

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
const groupUsers = ref(new Array<UserCheck>())
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

onMounted(async () => {
  loading.value = true
  try {
    const users = await GroupApi.users(props.group.id)
    // 过滤掉群主，不允许删除群主
    const filteredUsers = users.filter(user => user.id !== props.group.master)
    groupUsers.value = filteredUsers.map(user => ({ user, isCheck: false }))
  } catch (error) {
    console.error('获取群成员失败:', error)
    ElMessage.error('获取群成员失败')
  } finally {
    loading.value = false
  }
})

const checkedUsers = computed((): Array<UserCheck> => {
  return groupUsers.value.filter((item) => item.isCheck)
})

const userIds = computed((): string[] => {
  return checkedUsers.value.map((item) => item.user.id)
})

const canSave = computed((): boolean => {
  return userIds.value.length === 0
})

// 执行删除群成员
const handleDeleteGroupUsers = async () => {
  try {
    await ElMessageBox.confirm('确定要删除选中的群成员吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    saveLoading.value = true
    const promises = userIds.value.map(userId => GroupApi.deleteUser(props.group.id, userId))
    await Promise.all(promises)
    
    ElMessage.success('删除群成员成功')
    await useGroupStore().loadGroupData(props.group.id)
    props.closeDialog()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除群成员失败:', error)
      ElMessage.error('删除群成员失败')
    }
  } finally {
    saveLoading.value = false
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

