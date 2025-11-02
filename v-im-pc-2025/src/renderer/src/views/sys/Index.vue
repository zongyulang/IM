<template>
  <div class="main">
    <div class="left">
      <div class="title">
        <el-row>
          <el-col :span="20">
            <div class="text">设置</div>
          </el-col>
          <el-col :span="4" class="add"></el-col>
        </el-row>
      </div>
      <el-scrollbar class="list">
        <ul class="sys-menu">
          <li v-for="(item, index) in list" :key="index" :class="item.active ? 'active' : ''">
            <router-link :to="item.url" class="block" @click="handleClick(index)">
              {{ item.text }}
            </router-link>
          </li>
        </ul>
      </el-scrollbar>
    </div>
    <div class="right">
      <vim-top />
      <router-view v-slot="{ Component }">
        <keep-alive>
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </div>
  </div>
</template>

<script setup lang="ts">
import VimTop from '@renderer/components/VimTop.vue'
import { onActivated, ref } from 'vue'
import { useRouter } from 'vue-router'

const checkIndex = ref(-1)
const routerName = useRouter().currentRoute.value.name
const list = ref([
  {
    text: '个人信息',
    url: '/index/system/user',
    active: routerName === 'sys-user'
  },
  { text: '修改密码', url: '/index/system/pwd', active: routerName === 'pwd' },
  {
    text: '聊天设置',
    url: '/index/system/setting',
    active: routerName === 'setting'
  },
  {
    text: '系统信息',
    url: '/index/system/info',
    active: routerName === 'info'
  }
])

const router = useRouter()
const handleClick = (index: number) => {
  list.value.forEach((item) => {
    item.active = false
  })
  list.value[index].active = true
  checkIndex.value = index
}

onActivated(() => {
  if (checkIndex.value !== -1) {
    router.push(list.value[checkIndex.value].url)
  }
})
</script>
<style scoped lang="less">
.sys-menu {
  list-style: none;
  padding: 0;
  margin: 0;
  li {
    line-height: 30px;
    padding: 10px;
  }
  .active {
    background: #f8f8f8;
  }
}
.block {
  display: block;
}
</style>
