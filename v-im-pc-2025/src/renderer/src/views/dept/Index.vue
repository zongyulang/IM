<template>
  <div class="main">
    <div class="left">
      <div class="title">
        <el-row>
          <el-col :span="22">
            <div class="text">组织</div>
          </el-col>
        </el-row>
      </div>
      <el-scrollbar class="list">
        <el-tree
          :default-expand-all="true"
          :data="tree"
          :props="defaultProps"
          @node-click="handleNodeClick"
        />
      </el-scrollbar>
    </div>
    <div class="right">
      <vim-top />
      <router-view v-slot="{ Component }" :key="$route.fullPath" class="content">
        <component :is="Component" />
      </router-view>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onActivated, ref } from 'vue'
import { useRouter } from 'vue-router'
import VimTop from '@renderer/components/VimTop.vue'
import DeptApi from '@renderer/api/DeptApi'

const router = useRouter()
const id = ref()
interface Tree {
  id: string
  parentId: string
  label: string
  count: number
  children?: Tree[]
}

const defaultProps = {
  children: 'children',
  label: 'label'
}

const handleNodeClick = (data: Tree) => {
  if (data.children?.length === 0) {
    id.value = data.id
    router.push('/index/dept/' + data.id)
  }
}

const tree = ref<Array<Tree>>([])
const fetchDeptTree = async () => {
  try {
    const res = await DeptApi.list()
    tree.value = Array.isArray(res) ? res.map((item) => ({ ...item })) : []
  } catch (error) {
    console.error('Failed to fetch dept tree:', error)
  }
}
fetchDeptTree()

onActivated(() => {
  if (id.value) {
    router.push('/index/dept/' + id.value)
  }
})
</script>

<style lang="less" scoped>
.el-tree {
  background-color: transparent !important;
}
</style>
