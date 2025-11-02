<template>
  <div style="margin-bottom: 15px">
    <el-input v-model="keyword" placeholder="搜索"></el-input>
  </div>
  <el-row :gutter="10">
    <el-col :span="12">
      <el-checkbox-group v-model="itemChecked" @change="change">
        <div
          v-for="chat in keywordFilter([...itemList.values()], keyword)"
          :key="chat.id"
          class="item-box"
        >
          <el-checkbox :value="chat">
            <template #default>
              <div class="check-item">
                <div class="check-item-avatar">
                  <vim-avatar :img="chat.avatar" :name="chat.name" />
                </div>
                <div class="check-item-name">
                  {{ chat.name }}
                </div>
              </div>
            </template>
          </el-checkbox>
        </div>
      </el-checkbox-group>
    </el-col>
    <el-col :span="12">
      <div v-for="chat in itemChecked" :key="chat.id" class="item-box">
        <div class="check-item">
          <div class="check-item-avatar">
            <vim-avatar :img="chat.avatar" :name="chat.name" />
          </div>
          <div class="check-item-name">
            {{ chat.name }}
          </div>
        </div>
      </div>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { ref } from 'vue'
import VimAvatar from '@renderer/components/VimAvatar.vue'
import type { User } from '@renderer/mode/User'
import type { Chat } from '@renderer/mode/Chat'
import keywordFilter from '@renderer/utils/PinYinUtils'
import type { Group } from '@renderer/mode/Group'
import type { ChatSimple } from '@renderer/mode/ChatSimple'
import ChatType from '@renderer/enum/ChatType'
import { useFriendStore } from '@renderer/store/friendStore'
import { useGroupStore } from '@renderer/store/groupStore'
import { useChatStore } from '@renderer/store/chatStore'

const emit = defineEmits(['set-data'])
const chatStore = useChatStore()
const keyword = ref('')
//选中的chats
const itemChecked = ref<Array<Chat>>([])
//好友
const itemList = ref(new Map<string, ChatSimple>())
//好友
const { friendList } = storeToRefs(useFriendStore())
//群
const { groupList } = storeToRefs(useGroupStore())
// checkbox 变化
const change = () => {
  emit('set-data', itemChecked.value)
}

chatStore.chats.forEach((item: Chat) => {
  itemList.value.set(item.id, {
    id: item.id,
    name: item.name,
    avatar: item.avatar,
    type: item.type
  })
})
friendList.value.forEach((item: User) => {
  itemList.value.set(item.id, {
    id: item.id,
    name: item.name,
    avatar: item.avatar,
    type: ChatType.FRIEND
  })
})
groupList.value.forEach((item: Group) => {
  itemList.value.set(item.id, {
    id: item.id,
    name: item.name,
    avatar: item.avatar,
    type: ChatType.GROUP
  })
})
</script>

<style lang="less" scoped>
.item-box {
  height: 60px;
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  border: 1px #ccc dotted;
  padding-left: 5px;
}
.check-item {
  display: flex;

  .check-item-avatar {
    flex: 2;
    display: flex;
    align-items: center;
    justify-content: center;

    .avatar {
      height: 40px;
      width: 40px;
    }
  }

  .check-item-name {
    flex: 6;
    padding-left: 15px;
    width: 150px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    line-height: 1.3;
    max-height: 2.6em;
    overflow: hidden;
    text-overflow: ellipsis;
    word-break: break-all;
    word-wrap: break-word;
    white-space: normal;
  }
}
</style>
