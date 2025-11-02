<template>
  <div class="main">
    <div class="left">
      <div class="title">
        <el-row>
          <el-col :span="24">
            <el-input v-model="keyword" placeholder="搜索"></el-input>
          </el-col>
        </el-row>
      </div>
      <el-scrollbar v-if="show" ref="chatScrollbarRef" class="list">
        <chat-item
          v-for="item in filteredChats"
          :id="item.id"
          :key="item.id"
          :img="item.avatar"
          :username="item.name"
          :top="!!item.top"
          :unread-count="item.unreadCount"
          :last-time="chatLastTime[item.id]"
          :text="chatLastMessage[item.id]"
          :active="item.id === openId"
          :right-event="chatRightEvent"
          :is-group="item.type === ChatType.GROUP"
          @click="showChat(item)"
        ></chat-item>
      </el-scrollbar>
    </div>
    <div class="right">
      <vim-top />
      <chat-box v-if="chat"></chat-box>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useChatStore } from '@renderer/store/chatStore'
import type { Chat } from '@renderer/mode/Chat'
import ChatBox from '@renderer/views/chat/ChatBox.vue'
import VimTop from '@renderer/components/VimTop.vue'
import ChatItem from '@renderer/components/ChatItem.vue'
import { nextTick, onActivated, onMounted, ref, computed, watch } from 'vue'
import ChatUtils from '@renderer/utils/ChatUtils'
import chatRightEvent from '@renderer/hooks/useChatRightEvent'
import { match } from 'pinyin-pro'
import { storeToRefs } from 'pinia'
import ChatType from '@renderer/enum/ChatType'

// 基础状态
const keyword = ref('')
const show = ref<boolean>(false)
const chatScrollbarRef = ref<any>(null)

// Store 相关
const chatStore = useChatStore()
const { chats, openId, chat, chatLastMessage, chatLastTime } = storeToRefs(chatStore)

// 计算属性：过滤后的聊天列表
const filteredChats = computed(() => {
  return keyword.value.trim() === ''
    ? chats.value
    : chats.value.filter((item) => match(item.name, keyword.value))
})

/**
 * 显示选中的聊天
 * @param selectedChat 选中的聊天对象
 */
const showChat = async (selectedChat: Chat) => {
  chatStore.openChat(selectedChat)

  // 滚动到选中的聊天项
  await nextTick()
  scrollToActiveChat()
}

/**
 * 滚动到当前激活的聊天项
 * 如果聊天项已在可视区域内，则不滚动
 */
const scrollToActiveChat = () => {
  if (!chatScrollbarRef.value || !openId.value) return

  nextTick(() => {
    const activeElements = document.querySelectorAll('.chat-item')
    let activeElement: Element | null = null

    // 查找当前激活的聊天项
    for (const element of activeElements) {
      if (element.getAttribute('data-id') === openId.value) {
        activeElement = element
        break
      }
    }

    if (activeElement && chatScrollbarRef.value) {
      // 检查元素是否在可视区域内
      const scrollContainer = chatScrollbarRef.value.wrapRef
      if (scrollContainer) {
        const containerRect = scrollContainer.getBoundingClientRect()
        const elementRect = (activeElement as HTMLElement).getBoundingClientRect()

        // 判断元素是否完全在可视区域内
        const isInView =
          elementRect.top >= containerRect.top && elementRect.bottom <= containerRect.bottom

        // 只有当元素不在可视区域内时才滚动
        if (!isInView) {
          const offsetTop = (activeElement as HTMLElement).offsetTop
          chatScrollbarRef.value.setScrollTop(offsetTop)
        }
      }
    }
  })
}

// 生命周期钩子
onMounted(async () => {
  show.value = true
  await chatStore.reloadChats()
  await nextTick()
  scrollToActiveChat()
})

// 监听 openId 的变化
watch(
  () => openId.value,
  async () => {
    await nextTick()
    scrollToActiveChat()
  }
)

// 组件激活时
onActivated(() => {
  nextTick(() => {
    ChatUtils.imageLoad('message-box')
    scrollToActiveChat()
  })
})
</script>

<style lang="less" scoped>
.main {
  display: flex;
  height: 100%;

  .left {
    display: flex;
    flex-direction: column;
    border-right: 1px solid var(--el-border-color-lighter);

    .title {
      padding: 10px;
    }

    .list {
      flex: 1;
      overflow-y: auto;
    }
  }

  .right {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
}
</style>
