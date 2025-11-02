<template>
  <div v-if="user" class="im-user">
    <!-- 用户头像和添加按钮区域 -->
    <div class="im-user-box">
      <vim-avatar :img="user.avatar" :name="user.name" />
      <div @click.stop="quickGroup(user.id)"><i class="iconfont icon-v-add"></i></div>
    </div>
    <chat-toggle />
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { User } from '@renderer/mode/User'
import VimAvatar from '@renderer/components/VimAvatar.vue'
import ChatToggle from '@renderer/components/ChatToggle.vue'
import quickGroup from '@renderer/components/group/quick-group'
import UserApi from '@renderer/api/UserApi'

const user = ref<User>()

interface Props {
  userId: string
}
const props = defineProps<Props>()

onMounted(async () => {
  user.value = await UserApi.getUser(props.userId)
})
</script>
<style scoped lang="less">
/* 用户信息侧边栏容器 */
.im-user {
  width: 180px;
  border-left: 1px solid #cccccc;

  /* 用户头像和添加按钮区域样式 */

  .im-user-box {
    display: flex;
    flex-direction: row;
    align-items: center;
    border-bottom: 1px solid #cccccc;
    margin-bottom: 10px;
    div {
      margin: 10px;
      display: flex;
      justify-content: center;
      align-items: center;
      width: 40px;
      height: 40px;

      i.icon-v-add {
        font-size: 28px;
        display: block;
        background-color: #efefef;
        padding: 5px;
        border: 1px #eee solid;
        cursor: pointer;
      }
    }
  }

  /* 聊天记录区域样式 */

  .history {
    background-color: #efefef;
    display: flex;
    align-items: center;
    flex-direction: row;
    cursor: pointer;
    margin: 0 0 20px 0;

    .history-title {
      padding-left: 15px;
      line-height: 32px;
      flex: 7;
      font-size: 1.25rem;
    }

    .history-icon {
      flex: 1;
      line-height: 32px;
      justify-content: center;
    }
  }
}
</style>
