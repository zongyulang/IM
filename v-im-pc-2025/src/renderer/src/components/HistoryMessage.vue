<template>
  <div>
    <!-- 搜索表单区域 -->
    <el-form :inline="true">
      <!-- 日期范围选择器 -->
      <el-form-item>
        <el-date-picker
          v-model="dateRange"
          style="width: 200px"
          type="daterange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          size="small"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <!-- 消息类型选择器 -->
      <el-form-item>
        <el-select v-model="messageType" placeholder="类型" size="small" style="width: 70px">
          <el-option
            v-for="item in options"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <!-- 关键词搜索输入框 -->
      <el-form-item>
        <el-input
          v-model="keyword"
          size="small"
          style="width: 100px"
          placeholder="请输入搜索内容"
        ></el-input>
      </el-form-item>
      <!-- 查询按钮 -->
      <el-form-item>
        <el-button size="small" type="primary" @click="change(1)">查询</el-button>
      </el-form-item>
    </el-form>
  </div>
  <div class="im-chat-main">
    <div id="his-chat-message" class="im-chat-main-box messages" style="height: 100%">
      <ul>
        <template v-for="(item, index) in hisMessageList" :key="item.id">
          <time-divider
            v-if="item.timestamp && shouldShowTimeDivider(index, item.timestamp)"
            :timestamp="item.timestamp"
          />
          <li :class="{ 'im-chat-mine': item.fromId === user?.id }">
            <chat-message-user
              v-if="item.messageType !== MessageType.EVENT"
              class="im-chat-user"
              :message="item"
            />
            <component
              :is="useMessageComponent(item.messageType)"
              :message="item"
              :history="true"
            ></component>
          </li>
        </template>
      </ul>
    </div>
  </div>
  <el-pagination
    v-model:current-page="current"
    background
    :page-size="size"
    layout="prev, pager, next"
    :total="total"
    @current-change="change"
  >
  </el-pagination>
</template>

<script setup lang="ts">
import type { Ref } from 'vue'
import { nextTick, reactive, ref, toRefs, watch } from 'vue'
import MessageApi from '@renderer/api/MessageApi'
import ChatMessageUser from '@renderer/components/ChatMessageUser.vue'
import ChatUtils from '@renderer/utils/ChatUtils'
import { useUserStore } from '@renderer/store/userStore'
import type { Message } from '@renderer/message/Message'
import useMessageComponent from '@renderer/hooks/useMessageComponent'
import MessageType from '@renderer/enum/MessageType'
import { storeToRefs } from 'pinia'
import type { User } from '@renderer/mode/User'
import TimeDivider from '@renderer/components/TimeDivider.vue'

// 消息类型选项配置
const options = [
  { value: '', label: '全部' },
  { value: MessageType.TEXT, label: '文本' },
  { value: MessageType.IMAGE, label: '图片' },
  { value: MessageType.FILE, label: '文件' },
  { value: MessageType.VOICE, label: '语音' },
  { value: MessageType.VIDEO, label: '视频' }
]

// 组件状态和引用
const userStore = useUserStore()
const dateRange = ref([])
const keyword = ref('')
const messageType = ref('')
const size = ref(100)
const { user } = storeToRefs(userStore) as { user: Ref<User> }

// Props 类型定义
interface Props {
  chatId: string // 聊天ID
  fromId: string // 发送者ID
  chatType: string // 聊天类型
  showHistory: boolean // 是否显示历史记录
}
const props = defineProps<Props>()

// 数据接口定义
interface IData {
  hisMessageList: Message[] // 历史消息列表
  current: number // 当前页码
  total: number // 总记录数
}

// 响应式数据
const data = reactive<IData>({
  hisMessageList: [],
  current: 1,
  total: 0
})

// 判断是否显示时间分割线
const shouldShowTimeDivider = (index: number, currentTimestamp: number) => {
  if (index === 0) return true
  const prevMessage = hisMessageList.value[index - 1]
  const timeDiff = currentTimestamp - (prevMessage.timestamp ?? 0)
  return timeDiff > 5 * 60 * 1000
}

// 分页查询方法
const change = (current: number) => {
  MessageApi.page(
    props.chatId,
    props.fromId,
    keyword.value,
    props.chatType,
    messageType.value,
    current,
    dateRange.value ? dateRange.value[0] : '',
    dateRange.value ? dateRange.value[1] : '',
    size.value
  ).then((res) => {
    data.hisMessageList = res.records.reverse()
    data.total = res.total
    data.current = current
    nextTick(() => {
      ChatUtils.imageLoad('his-chat-message') // 图片加载处理
    })
  })
}

// 监听历史记录显示状态
watch(
  () => {
    return props.showHistory
  },
  (n) => {
    if (n) {
      change(1)
    }
  },
  {
    immediate: true
  }
)

const { hisMessageList, current, total } = toRefs(data)
</script>

<style scoped>
.im-chat-main {
  height: calc(100% - 90px);
  overflow-y: hidden;
  overflow-x: hidden;
  margin-bottom: 10px;
}
.im-chat-main .messages {
  width: 100%;
  height: calc(100% - 3rem);
  overflow-y: scroll;
  overflow-x: hidden;
}

.im-chat-main-box {
  flex: 1;
  padding: 1rem 1rem 0 1rem;
  overflow-x: hidden;
  overflow-y: auto;
}

</style>
