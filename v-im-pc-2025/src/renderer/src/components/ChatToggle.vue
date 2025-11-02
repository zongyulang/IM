<template>
  <!-- 消息免打扰设置区域 -->
  <div class="list">
    <div class="title">聊天置顶</div>
    <div class="action">
      <el-switch v-model="isTop" size="small" @change="changeTop" />
    </div>
  </div>
  <!-- 聊天置顶设置区域 -->
  <div class="list">
    <div class="title">消息免打扰</div>
    <div class="action">
      <el-switch v-model="isImmunity" size="small" @change="changeImmunity" />
    </div>
  </div>
</template>
<script lang="ts" setup>
// 初始化各个状态管理store
import { useChatStore } from '@renderer/store/chatStore'
import { useImmunityStore } from '@renderer/store/immunityStore'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@renderer/store/userStore'
import { computed } from 'vue'
import type { Ref } from 'vue'
import type { User } from '@renderer/mode/User'
import ImmunityApi from '@renderer/api/ImmunityApi'

const chatStore = useChatStore()
const immunityStore = useImmunityStore()
const { immunityList } = storeToRefs(immunityStore)
const { user } = storeToRefs(useUserStore()) as { user: Ref<User> }
const { chat } = storeToRefs(chatStore)

// 计算属性：是否置顶当前聊天
const isTop = computed(() => {
  return chat.value.top ?? false
})

// 计算属性：是否开启当前聊天的免打扰
const isImmunity = computed(() => {
  return immunityList.value.includes(chat.value.id)
})

/**
 * 切换聊天置顶状态
 * @param val 是否置顶
 */
const changeTop = (val: boolean) => {
  if (val) {
    chatStore.topChat(chat.value.id)
  } else {
    chatStore.cancelTop(chat.value.id)
  }
}

/**
 * 切换消息免打扰状态
 * @param val 是否免打扰
 */
const changeImmunity = async (val: boolean) => {
  if (val) {
    await ImmunityApi.save(user.value.id, chat.value.id)
  } else {
    await ImmunityApi.delete(user.value.id, chat.value.id)
  }
  immunityStore.loadData()
}
</script>
<style scoped lang="less">
/* 列表项通用样式（用于免打扰和置顶选项） */
.list {
  display: flex;
  flex-direction: row;
  align-items: center;

  .title {
    font-size: 1.25rem;
    padding-left: 15px;
    line-height: 32px;
    flex: 5;
  }

  .action {
    flex: 2;
  }
}
</style>
