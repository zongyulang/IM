<template>
  <div class="main">
    <div class="left">
      <div class="title">
        <el-row>
          <el-col :span="21">
            <el-input v-model="keyword" placeholder="搜索"></el-input>
          </el-col>
          <el-col :span="3" class="add">
            <i class="iconfont icon-v-add" title="新建群" @click.stop="newGroup"></i>
          </el-col>
        </el-row>
      </div>
      <el-scrollbar class="list">
        <chat-item
          v-for="(group, index) in list"
          :id="group.id"
          :key="group.id"
          :img="group.avatar"
          :username="group.name"
          :active="index === checkIndex"
          :unread-count="waitCheckMap.get(group.id) ?? 0"
          :right-event="chatRightEvent"
          @click="choose(index, group.id)"
        ></chat-item>
      </el-scrollbar>
    </div>
    <div class="right">
      <vim-top />
      <router-view v-slot="{ Component }" :key="$route.fullPath" class="content group-content">
        <component :is="Component" />
      </router-view>
    </div>
  </div>
</template>

<script setup lang="ts">
import ChatItem from '@renderer/components/ChatItem.vue'
import VimTop from '@renderer/components/VimTop.vue'
import { useRouter } from 'vue-router'
import { useGroupStore } from '@renderer/store/groupStore'
import keywordFilter from '@renderer/utils/PinYinUtils'
import { computed, onActivated, ref } from 'vue'
import { storeToRefs } from 'pinia'

const keyword = ref('')
const groupStore = useGroupStore()

const router = useRouter()
const { waitCheckMap, groupList, checkIndex } = storeToRefs(groupStore)
const list = computed(() => {
  return keywordFilter(groupList.value, keyword.value)
})
const chatRightEvent = () => {}
const choose = (index: number, id: string) => {
  groupStore.setCheckIndex(index)
  router.push('/index/group/' + id)
}

const newGroup = () => {
  groupStore.setCheckIndex(-1)
  router.push('/index/group/edit')
}

onActivated(() => {
  // 初始化加载数据
  groupStore.loadData()
})
</script>

<style scoped lang="less">
.group-content {
  height: calc(100% - 3rem);
  display: flex;
  flex-direction: column;
}
</style>
