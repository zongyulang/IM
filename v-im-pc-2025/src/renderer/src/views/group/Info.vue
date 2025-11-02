<template>
  <div>
    <div class="group">
      <div class="header">
        <vim-avatar :img="group.avatar" :name="group.name" size="large" style="border: 1px solid #CCC"/>
      </div>
      <div class="box">
        <el-descriptions :title="group.name" :column="2">
          <el-descriptions-item label="允许邀请">
            <el-switch v-model="openInvite" disabled />
          </el-descriptions-item>
          <el-descriptions-item label="邀请审核">
            <el-switch v-model="inviteCheck" disabled />
          </el-descriptions-item>
          <el-descriptions-item label="全体禁言">
            <el-switch v-model="prohibition" disabled />
          </el-descriptions-item>
          <el-descriptions-item label="禁加好友">
            <el-switch v-model="prohibitFriend" disabled />
          </el-descriptions-item>
          <el-descriptions-item label="群公告">
            {{ group.announcement }}
          </el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: 10px">
          <el-button v-if="isMaster" size="small" type="success" plain @click="updateGroupHandle"
            >编辑
          </el-button>
          <el-button v-if="isMaster" size="small" type="info" plain @click="handleEditAnnouncement(group)"
            >公告
          </el-button>
          <el-button v-if="isMaster" size="small" type="danger" plain @click="deleteGroup"
            >解散
          </el-button>
          <el-badge
            v-if="isMaster"
            :value="waitCheckValue"
            :hidden="!waitCheckValue"
            style="margin: 0 10px"
          >
            <el-button size="small" type="warning" plain @click="inviteCheckHandle">审核</el-button>
          </el-badge>

          <el-button v-if="!isMaster" size="small" plain type="danger" @click="exit">退群</el-button>
          <el-button size="small" type="primary" plain @click="openChat">聊天 </el-button>
        </div>
      </div>
    </div>
    <el-divider />
    <el-scrollbar style="flex: 1">
      <div class="members">
        <div class="user-list">
          <div v-if="isMaster || openInvite" class="user user-box" @click="addUser">
            <div class="add-user">
              <i class="iconfont icon-v-add"></i>
            </div>
          </div>
          <div v-if="isMaster" class="user user-box" @click="deleteUser">
            <div class="delete-user">
              <i class="iconfont icon-v-f00c"></i>
            </div>
          </div>
          <template v-for="item in sortedGroupUsers" :key="item.id">
            <div
              :title="item.name"
              class="user user-box"
              @click="prohibitFriend ? '' : showUser(item.id, true)"
              @contextmenu="groupUserRightEvent(item, groupId, isMaster, $event)"
            >
              <vim-avatar :img="item.avatar" :name="item.name" size="default" />
              <div class="username" :class="{ master: item.id === group.master }">
                {{ item.name }}
              </div>
            </div>
          </template>
        </div>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useChatStore } from '@renderer/store/chatStore'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import VimAvatar from '@renderer/components/VimAvatar.vue'
import GroupApi from '@renderer/api/GroupApi'
import showUser from '@renderer/components/user-modal/index'
import { useGroupStore } from '@renderer/store/groupStore'
import ChatType from '@renderer/enum/ChatType'
import groupUserRightEvent from '@renderer/hooks/useGroupUserRightEvent'
import showAddGroupUser from '@renderer/components/group/add-user/index'
import DictUtils from '@renderer/utils/DictUtils'
import { storeToRefs } from 'pinia'
import handleEditAnnouncement from '@renderer/components/group/announcement'
import showDeleteGroupUser from '@renderer/components/group/delete-user'

const groupStore = useGroupStore()
const { currentGroup: group, currentGroupUsers: groupUsers, isMaster } = storeToRefs(groupStore)
const openInvite = computed(() => group.value.openInvite === DictUtils.YES)
const inviteCheck = computed(() => group.value.inviteCheck === DictUtils.YES)
const prohibition = computed(() => group.value.prohibition === DictUtils.YES)
const prohibitFriend = computed(() => group.value.prohibitFriend === DictUtils.YES)
const router = useRouter()
const chatStore = useChatStore()
const route = useRoute()
const groupId = ref<string>(route.params.id as string)

onMounted(async () => {
  await groupStore.loadGroupData(groupId.value)
})

const waitCheckValue = computed(() => {
  return groupStore.waitCheckMap.get(group.value.id) ?? 0
})

const sortedGroupUsers = computed(() => {
  if (!group.value || !groupUsers.value) return []
  return [...groupUsers.value].sort((a, b) => {
    if (a.id === group.value.master) return -1
    if (b.id === group.value.master) return 1
    return 0
  })
})

const addUser = () => {
  showAddGroupUser(group.value)
}

const deleteUser = () => {
  showDeleteGroupUser(group.value)
}

const exit = async () => {
  try {
    await ElMessageBox.confirm('确定要退出该群吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await GroupApi.exit(group.value.id)
    ElMessage.success('退出成功')
    await groupStore.loadData()
    await router.push('/index/group')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('退出群组失败:', error)
      ElMessage.error('退出失败')
    }
  }
}

const updateGroupHandle = () => {
  router.push(`/index/group/edit?id=${group.value.id}`)
}

const deleteGroup = async () => {
  try {
    await ElMessageBox.confirm('确定要解散该群吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await GroupApi.delete(group.value.id)
    ElMessage.success('解散成功')
    await groupStore.loadData()
    await router.push('/index/group')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('解散群组失败:', error)
      ElMessage.error('解散失败')
    }
  }
}

const inviteCheckHandle = () => {
  router.push(`/index/group/invite?id=${group.value.id}`)
}

const openChat = () => {
  chatStore.openChat({
    id: group.value.id,
    name: group.value.name,
    avatar: group.value.avatar,
    type: ChatType.GROUP,
    unreadCount: 0
  })
  router.push('/index/chat')
}
</script>

<style scoped lang="less">
.group {
  padding: 15px;
  display: flex;
}

.header {
  width: 132px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
  border: 1px solid #ccc;

  .edit {
    color: #ff00ff;
    position: relative;
    bottom: 20px;
    right: 10px;
  }
}

.box {
  flex: 5;
  padding: 10px 20px;
  background-color: #ffffff;
}

.members {
  text-align: center;
  font-size: 12px;
  height: auto;
  margin: 15px;
}

.add-user,.delete-user {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 6rem;
  color: #ccc;

  i {
    font-size: 30px;
    line-height: 84px;
    width: 80px;
    height: 84px;
    text-align: center;
  }
}

.user {
  position: relative;

  .close {
    position: absolute;
    top: -9px;
    right: -9px;
    background-color: #cccccc;
    width: 18px;
    height: 18px;
    color: #ffffff;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;

    .icon-v-close {
      font-size: 10px;
    }
  }
}

.user-box {
  border: 1px solid #cccccc;
  padding: 15px 10px 10px 10px;

  .username {
    width: 100%;
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;
  }
}

.bg-none {
  background-color: transparent;
}
.username {
  margin-top: 5px;
}
.master {
  color: chocolate;
  font-weight: bold;
}

.user-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, 97px);
  grid-template-rows: repeat(auto-fit, 90px);
  grid-row-gap: 20px;
  grid-column-gap: 20px;
}
</style>
