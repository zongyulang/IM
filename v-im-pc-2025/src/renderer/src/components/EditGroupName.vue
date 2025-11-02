<template>
  <div class="list" title="修改群名称" @click="editGroupName">
    <div class="title">{{ group.name }}</div>
    <div class="action">
      <i class="iconfont icon-v-edit1"></i>
    </div>
  </div>
</template>
<script setup lang="ts">
import type { Group } from '@renderer/mode/Group'
import handleEditGroupName from '@renderer/components/group/edit-name'

interface Props {
  group: Group
}
const props = defineProps<Props>()

/**
 * 点击编辑群名称
 */
const editGroupName = () => {
  handleEditGroupName(props.group)
}
</script>

<style scoped lang="less">
/* 列表项通用样式（用于免打扰和置顶选项） */
.list {
  display: flex;
  flex-direction: row;
  align-items: center;
  cursor: pointer;
  border-top: 1px dotted #ccc;
  border-bottom: 1px dotted #ccc;
  .title {
    font-size: 1.25rem;
    padding-left: 15px;
    line-height: 32px;
    flex: 7;
    white-space: nowrap;
    overflow: hidden;
    width: 100%;
    text-overflow: ellipsis;
  }

  .action {
    flex: 2;
    display: flex;
    justify-content: center;
  }
}
</style>
