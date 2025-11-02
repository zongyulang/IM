<template>
  <div v-if="user" v-loading.fullscreen.lock="fullscreenLoading" class="v-im">
    <nav class="left-bar" style="-webkit-app-region: drag">
      <ul>
        <!-- 用户头像 -->
        <li class="user-photo">
          <div @click="showUser(user.id, false)">
            <vim-avatar :img="user.avatar" size="default" :name="user.name" />
          </div>
        </li>

        <!-- 主要导航项 -->
        <li v-for="(item, index) in mainNavItems" :key="index" :title="item.title">
          <router-link :to="item.route" @click="item.onClick">
            <el-badge :value="item.badgeValue" :hidden="!item.badgeValue">
              <i :class="['iconfont', item.icon]" @dblclick="item.onDblClick"></i>
            </el-badge>
          </router-link>
        </li>

        <!-- 组织导航项(可选) -->
        <li v-if="sysConfig.showDept" title="组织">
          <router-link to="/index/dept">
            <i class="iconfont icon-v-icon_xinyong_xianxing_jijin-284"></i>
          </router-link>
        </li>

        <!-- 设置导航项 -->
        <li title="设置">
          <router-link to="/index/system/user">
            <i class="iconfont icon-v-shezhi"></i>
          </router-link>
        </li>

        <!-- 连接状态 -->
        <li>
          <i class="iconfont icon-v-aixin" :class="{ 'icon-heart': linked }"></i>
        </li>

        <!-- 退出按钮 -->
        <li title="退出" class="logout" @click="vimLogout">
          <i class="iconfont icon-v-weibiaoti2"></i>
        </li>
      </ul>
    </nav>
    <router-view v-slot="{ Component }" :key="$route.matched[1]?.path" class="content">
      <keep-alive>
        <component :is="Component" />
      </keep-alive>
    </router-view>
  </div>
</template>

<script setup lang="ts">
import type { Ref } from 'vue'
import { computed, getCurrentInstance, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { storeToRefs } from 'pinia'
import showUser from '@renderer/components/user-modal/index'
import VimAvatar from '@renderer/components/VimAvatar.vue'
import { useUserStore } from '@renderer/store/userStore'
import { useChatStore } from '@renderer/store/chatStore'
import { useFriendStore } from '@renderer/store/friendStore'
import { useGroupStore } from '@renderer/store/groupStore'
import { useSettingStore } from '@renderer/store/settingStore'
import { useImmunityStore } from '@renderer/store/immunityStore'
import { useWsStore } from '@renderer/store/WsStore'
import { useSysConfigStore } from '@renderer/store/sysConfigStore'
import Auth from '@renderer/api/Auth'
import router from '@renderer/router'
import type { User } from '@renderer/mode/User'
import type { SysConfig } from '@renderer/config/SysConfig'

// 组件实例
const { proxy } = getCurrentInstance()!

// Store
const userStore = useUserStore()
const chatStore = useChatStore()
const friendStore = useFriendStore()
const groupStore = useGroupStore()
const wsStore = useWsStore()

// 响应式数据
const linked = ref(false)
const fullscreenLoading = ref(false)
const { user } = storeToRefs(userStore) as { user: Ref<User> }
const { sysConfig } = storeToRefs(useSysConfigStore()) as { sysConfig: Ref<SysConfig> }

// 主导航项配置
const mainNavItems = computed(() => [
  {
    title: '会话',
    route: '/index/chat',
    icon: 'icon-v-liaotian',
    badgeValue: unreadCount.value,
    onDblClick: scrollToFirstUnread
  },
  {
    title: '好友',
    route: '/index/friend',
    icon: 'icon-v-wode',
    badgeValue: friendStore.waitCheckCount,
    onClick: friendStore.loadData
  },
  {
    title: '群',
    route: '/index/group',
    icon: 'icon-v-qunzhong',
    badgeValue: groupStore.waitCheckCount
  }
])

// 计算未读消息总数
const unreadCount = computed(() =>
  chatStore.chats.reduce((acc, item) => acc + (item.unreadCount || 0), 0)
)

// 退出登录
const vimLogout = async () => {
  const res = await ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  if ('confirm' === res) {
    fullscreenLoading.value = true
    Auth.logout()
  }
}

// 滚动到第一条未读消息
const scrollToFirstUnread = (): void => {
  router.push('/index/chat')
  nextTick(() => chatStore.scrollToFirstUnread())
}

// 生命周期钩子
onMounted(() => {
  if (!user.value) return

  // 初始化数据
  groupStore.loadData()
  friendStore.loadData()
  useSysConfigStore().loadData()
  wsStore.init()

  // 设置消息提醒
  chatStore.tips = () => {
    proxy.$winControl.flashFrame()
    proxy.$winControl.flashIcon()
  }

  if (!proxy.$winControl.isWeb) {
    chatStore.notification = (content: string) => {
      proxy.$winControl.notification(content)
    }
  }

  // 加载配置
  useSettingStore().loadData()
  useImmunityStore().loadData()
  friendStore.loadWaitCheckList()

  // 注册窗口事件
  setupWindowEvents()
})

// 设置窗口事件
const setupWindowEvents = () => {
  proxy.$winControl.onResume(() => {
    useWsStore().setSleep(false)
    setTimeout(() => wsStore.checkStatus(), 5000)
  })

  proxy.$winControl.onSleep(() => {
    useWsStore().setSleep(true)
    wsStore.close()
  })

  proxy.$winControl.onBlur(() => {
    wsStore.setBlur(true)
  })

  proxy.$winControl.onFocus(() => {
    wsStore.setBlur(false)
    useChatStore().handleReceipt()
  })
}

// 监听websocket状态
watch(
  wsStore.wsRequest,
  (n) => {
    linked.value = n.isOpen()
  },
  { immediate: true, deep: true }
)
</script>

<style lang="less" scoped>
.v-im {
  display: flex;
  flex-direction: row;

  .left-bar {
    background-color: #1c2438;
    width: 6rem;
    height: 100%;

    ul {
      padding: 3rem 1.2rem 1.2rem;
      list-style: none;
      height: 100%;
      position: relative;

      li {
        -webkit-app-region: no-drag;
        display: block;
        width: 3.6rem;
        height: 3.6rem;
        text-align: center;
        margin-bottom: 1rem;
        cursor: pointer;

        a {
          display: block;
          text-decoration: none;
        }

        .iconfont {
          font-size: 2.4rem;
          color: #999;
          margin: 0.3rem;
          cursor: pointer;
          transition: color 0.3s ease;

          &:hover {
            color: #fff;
          }
        }

        .router-link-active .iconfont {
          color: #fff;
        }
      }

      .logout {
        position: absolute;
        bottom: 0;
      }

      .user-photo {
        margin-bottom: 3rem;

        .my-avatar {
          width: 100%;
        }

        img {
          width: 3.6rem;
          height: 3.6rem;
        }
      }
    }
  }

  .content {
    flex: 1;
  }
}

@keyframes heartbeat {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.3);
  }
}

.icon-heart {
  display: block;
  color: red !important;
  animation: heartbeat 1s infinite;
}
</style>
