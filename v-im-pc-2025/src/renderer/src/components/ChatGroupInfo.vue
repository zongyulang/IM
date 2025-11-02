<template>
  <div v-if="group" class="im-chat-users">
    <div class="title">
      <div>
        <span>公告</span>
      </div>
      <i v-if="isMaster" class="iconfont icon-v-edit1" @click="handleEditAnnouncement(group)"></i>
    </div>
    <div class="announcement">
      {{ group?.announcement }}
    </div>
    <edit-group-name v-if="isMaster" :group="group" />
    <chat-toggle />
    <div class="title border-top">
      <div>
        <span>成员</span>
      </div>
      <i
        v-if="isMaster || group.openInvite === DictUtils.YES"
        class="iconfont icon-v-add"
        @click="showAddGroupUser(group)"
      ></i>
    </div>
    <el-scrollbar class="chat-user-list">
      <div
        v-for="item in sortedGroupUsers"
        :key="item.id"
        class="user"
        @click="group.prohibitFriend === DictUtils.YES ? '' : showUser(item.id, true)"
      >
        <vim-avatar :img="item.avatar" :name="item.name" :size="'small'" />
        <div style="padding-left: 10px" :class="{ master: item.id === group.master }">
          {{ item.name }}
        </div>
      </div>
    </el-scrollbar>
  </div>
</template>
<script setup lang="ts">
import showUser from './user-modal/index'
import VimAvatar from '@renderer/components/VimAvatar.vue'
import DictUtils from '../utils/DictUtils'
import showAddGroupUser from './group/add-user'
import handleEditAnnouncement from './group/announcement'
import { computed, onMounted } from 'vue'
import { useGroupStore } from '../store/groupStore'
import { storeToRefs } from 'pinia'
import ChatToggle from '@renderer/components/ChatToggle.vue'
import EditGroupName from '@renderer/components/EditGroupName.vue'

interface IProps {
  groupId: string
}

const props = defineProps<IProps>()
const groupStore = useGroupStore()
const { currentGroupUsers, currentGroup: group, isMaster } = storeToRefs(groupStore)

onMounted(() => {
  groupStore.loadGroupData(props.groupId)
})

const sortedGroupUsers = computed(() => {
  if (!group.value || !currentGroupUsers.value) return []
  return [...currentGroupUsers.value].sort((a, b) => {
    if (a.id === group.value.master) return -1
    if (b.id === group.value.master) return 1
    return 0
  })
})
</script>
<style scoped lang="less">
.border-top {
  border-top: 1px dotted #cccccc;
}
.title {
  padding: 10px;
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: space-between;
  .icon-v-edit1 {
    cursor: pointer;
    display: block;
  }
  .icon-v-add {
    cursor: pointer;
    display: block;
  }
}
.announcement {
  padding: 10px;
  font-size: 12px;
  color: #666;
  background-color: #ffffff;
  line-height: 150%;
  text-indent: 2em;
  min-height: 15rem;
  word-break: break-all;
}
.im-chat-users {
  width: 180px;
  border-left: 1px solid #cccccc;

  .chat-user-list {
    height: calc(100% - 205px);
    list-style: none;
    margin: 0;
    background-color: #ffffff;
    .user {
      cursor: pointer;
      padding: 5px;
      position: relative;
      display: flex;
      align-items: center;
      font-size: 1.25rem;
      &:hover {
        background-color: #eeeeee;

        &:after {
          content: '...';
          position: absolute;
          right: 10px;
          font-weight: bold;
          bottom: 13px;
        }
      }

      & > .im-chat-avatar {
        width: 3.2rem;
        height: 3.2rem;
        display: inline-block;
        vertical-align: middle;

        & > img {
          width: 100%;
          height: 100%;
        }
      }
    }
  }
}
.master {
  color: chocolate;
  font-weight: bold;
}
.master-logo {
  padding: 0 10px;
  flex: 1;
  text-align: right;
}
</style>
