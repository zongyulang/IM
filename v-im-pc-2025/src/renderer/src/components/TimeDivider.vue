<template>
  <div class="time-divider">
    <span class="time-text">{{ formatTimeGroup }}</span>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { format } from 'date-fns'

interface IProps {
  timestamp: number
}
const props = defineProps<IProps>()

const formatTimeGroup = computed(() => {
  const now = new Date()
  const messageDate = new Date(props.timestamp)
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)

  if (messageDate >= today) {
    return format(messageDate, 'HH:mm')
  } else if (messageDate >= yesterday) {
    return '昨天 ' + format(messageDate, 'HH:mm')
  } else if (messageDate.getFullYear() === now.getFullYear()) {
    return format(messageDate, 'MM月dd日 HH:mm')
  } else {
    return format(messageDate, 'yyyy年MM月dd日 HH:mm')
  }
})
</script>

<style>
.time-divider {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10px 0;
}

.time-text {
  background-color: rgba(0, 0, 0, 0.1);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #999;
  line-height: 24px;
}
</style>
]]>
