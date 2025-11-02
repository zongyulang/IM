<template>
  <el-scrollbar v-loading="loading" class="dept-box">
    <div class="search-box">
      <el-input
        v-model="keyword"
        placeholder="请输入搜索内容(支持拼音搜索)"
        :suffix-icon="Search"
      />
    </div>
    <el-space v-if="users.length > 0" wrap>
      <div
        v-for="user in keywordFilter(users, keyword)"
        :key="user.id"
        class="user"
        :title="user.name"
        @click="showUser(user.id, true)"
      >
        <vim-avatar :img="user.avatar" :name="user.name" />
        <div class="username">{{ user.name }}</div>
      </div>
    </el-space>
    <div v-if="pages > 1" class="user more" @click="moreUser()">
      <i class="iconfont icon-v-gengduo"></i>
    </div>
    <el-result v-if="users.length === 0 && !loading" icon="info">
      <template #sub-title>
        <p>此部门暂时没有用户哦！</p>
      </template>
    </el-result>
  </el-scrollbar>
</template>

<script setup lang="ts">
import { reactive, ref, toRefs, watch } from 'vue'
import type { User } from '@renderer/mode/User'
import VimAvatar from '@renderer/components/VimAvatar.vue'
import showUser from '@renderer/components/user-modal'
import { useRoute } from 'vue-router'
import DeptApi from '@renderer/api/DeptApi'
import { Search } from '@element-plus/icons-vue'
import { match } from 'pinyin-pro'

const route = useRoute()
const keyword = ref('')
const loading = ref(true)
const data = reactive({
  users: new Array<User>(),
  pageNo: 1,
  pageSize: 50,
  pages: 1
})
const deptId = route.params.id

const loadData = async () => {
  if (typeof deptId === 'string') {
    loading.value = true
    try {
      data.users = await DeptApi.users(deptId, data.pageNo, data.pageSize)
    } finally {
      loading.value = false
    }
  }
}

const moreUser = () => {
  data.pageSize = 99999999999
  loadData()
}

/**
 * 过滤器
 * @param items 聊天列表
 * @param keyword 关键词
 */
const keywordFilter = (items: User[], keyword: string): User[] => {
  return items.filter((item) => {
    return !!(keyword.trim() === '' || match(item.name, keyword))
  })
}
watch(
  () => deptId,
  async (newId) => {
    if (newId) {
      await loadData()
    }
  },
  { immediate: true }
)



const { users, pages } = toRefs(data)
</script>

<style scoped lang="less">
.dept-box {
  background-color: #ffffff;
  height: calc(100% - 40px);
  padding: 20px !important;
  box-sizing: border-box;
}
.user {
  text-align: center;
  cursor: pointer;
  padding: 15px 15px 10px 15px;
  border: 1px solid #cccccc;
  width: 9rem;

  .username {
    width: 100%;
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;
    line-height: 200%;
    font-size: 1.2rem;
  }
}

.search-box {
  padding: 0 0 15px 0;
}
</style>
