<template>
  <div
    class="item chat-item"
    :data-id="id"
    :class="(active ? 'active ' : ' ') + (top ? 'top' : '')"
    @contextmenu="rightEvent?.(id, $event)"
  >
    <el-badge :value="unreadCount" :hidden="(unreadCount ?? 0) < 1">
      <vim-avatar :img="img" :name="username" class="avatar" :is-group="isGroup" />
    </el-badge>
    <div class="text">
      <div>
        <span class="username" :title="username">{{ username }}</span>
      </div>
      <div v-if="text">
        <div>{{ text }}</div>
      </div>
    </div>
    <div class="last-time">
      <div v-if="lastTime">
        {{
          formatDistanceToNow(lastTime, {
            locale: zhCN,
            addSuffix: false
          })
        }}
      </div>
      <div v-if="showNotice">
        <i class="iconfont icon-v-icon-test"></i>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import VimAvatar from '@renderer/components/VimAvatar.vue'
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import { storeToRefs } from 'pinia'
import { useImmunityStore } from '../store/immunityStore'
import { computed } from 'vue'
const immunityStore = useImmunityStore()
const { immunityList } = storeToRefs(immunityStore)
interface Props {
  id: string
  unreadCount?: number
  img?: string
  username?: string
  text?: string
  active?: boolean
  top?: boolean
  rightEvent?: (chatId: string, event: MouseEvent) => void
  lastTime?: number
  isGroup?: boolean
}

const props = defineProps<Props>()
// 免打扰标识
const showNotice = computed(() => {
  return immunityList.value.includes(props.id)
})
</script>

<style scoped lang="less">
.item {
  height: 6.5rem;
  display: flex;
  position: relative;
  padding-left: 8px;
  align-items: center;

  .username {
    text-overflow: ellipsis;
    overflow: hidden;
    white-space: nowrap;
  }

  .close {
    position: absolute;
    width: 1.5rem;
    height: 1.5rem;
    right: 15px;
    top: 1.825rem;
    display: none;
  }

  .top {
    position: absolute;
    width: 1.5rem;
    height: 1.5rem;
    right: 35px;
    top: 1.825rem;
    display: none;
  }

  &:hover {
    .close {
      display: block;
    }

    .top {
      display: block;
    }
  }

  .avatar {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 4.4rem;
    height: 4.4rem;
  }

  .text {
    margin-left: 8px;
    flex: 3;
    display: flex;
    flex-direction: column;
    height: 80%;
    flex-shrink: 0;
    overflow: hidden;

    & > div {
      display: flex;
      justify-content: flex-start;
      align-items: center;
      flex: 1;

      & > div {
        display: block;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        color: #999;
        font-size: 12px;
      }
    }
  }
}
.item:hover {
  background-color: #dfdfdf;
}
.online-img {
  width: 16px;
  height: 16px;
  margin-right: 10px;
}

.active {
  background-color: #ccc !important;
}
.top {
  background-color: #dfdfdf;
}

.grey {
  filter: grayscale(100%);
}

.last-time {
  width: 60px;
  text-align: center;
  font-size: 10px;
  color: #999;
}
</style>
