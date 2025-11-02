<template>
  <div class="main">
    <div class="left">
      <div class="title">
        <el-row>
          <el-col :span="21">
            <el-input v-model="keyword" placeholder="搜索"></el-input>
          </el-col>
          <el-col :span="3" class="add">
            <i
              class="iconfont icon-v-add"
              title="加好友"
              @click.stop="showAddFriend = !showAddFriend"
            ></i>
          </el-col>
        </el-row>
        <search-friend :dialog-visible="showAddFriend" @close="close" />
      </div>
      <div class="new-friend" @click="friendValidateHandle">
        <el-badge :value="validateFriendCount" :hidden="validateFriendCount === 0"
        >新的朋友</el-badge
        >
      </div>
      <el-scrollbar class="list">
        <div v-for="(user, index) in keywordFilter(friendList, keyword)" :key="user.id">
          <list-item
            :id="user.id"
            :img="user.avatar"
            :username="user.name"
            :active="index === checkIndex"
            :show-del="true"
            :right-event="friendRightEvent"
            @click="choose(index, user.id)"
          ></list-item>
        </div>
      </el-scrollbar>
    </div>
    <div class="right">
      <vim-top />
      <div>
        <router-view v-slot="{ Component }" class="content">
          <component :is="Component" />
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, ref } from 'vue'
import VimTop from '@renderer/components/VimTop.vue'
import ListItem from '@renderer/components/ChatItem.vue'
import SearchFriend from '@renderer/components/SearchFriend.vue'
import { useRouter } from 'vue-router'
import { useFriendStore } from '@renderer/store/friendStore'
import { storeToRefs } from 'pinia'
import keywordFilter from '@renderer/utils/PinYinUtils'
import friendRightEvent from '@renderer/hooks/useFriendRightEvent'

const router = useRouter()

const friendStore = useFriendStore()
const checkIndex = ref(-1)
const showAddFriend = ref(false)
const keyword = ref('')

const { friendList } = storeToRefs(friendStore)

const validateFriendCount = computed(() => {
  return friendStore.waitCheckList.length
})

const choose = (index: number, id: string) => {
  checkIndex.value = index
  router.push('/index/friend/' + id)
}

onActivated(() => {
  if (checkIndex.value !== -1) {
    router.push('/index/friend/' + friendList.value[checkIndex.value].id)
  }
})

const close = () => {
  showAddFriend.value = false
  friendStore.loadData()
}

friendStore.loadData()

const friendValidateHandle = () => {
  router.push('/index/friend/validate')
}
</script>

<style scoped lang="less">
.new-friend {
  padding: 0 20px;
  line-height: 40px;
  background-color: #ffffff;
  cursor: pointer;
}
</style>
