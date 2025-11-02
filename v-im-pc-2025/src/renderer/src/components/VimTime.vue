<template>
  <text>
    {{ formatDate }}
  </text>
</template>

<script lang="ts" setup>
import { format, formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import { computed } from 'vue'

// 定义props
const props = defineProps({
  time: {
    type: [Number, Date, String],
    default: new Date()
  }
})

/**
 * 格式化时间
 */
const formatDate = computed(() => {
  const type = typeof props.time
  let time
  if (type === 'number') {
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    //@ts-ignore
    const timestamp = props.time.toString().length > 10 ? props.time : props.time * 1000
    time = new Date(timestamp).getTime()
  } else if (type === 'object') {
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    //@ts-ignore
    time = props.time.getTime()
  } else if (type === 'string') {
    time = new Date(props.time).getTime()
  }
  if (new Date().getTime() - time > 1000 * 60 * 60 * 24 * 3) {
    return format(time, 'yyyy-MM-dd HH:mm')
  } else {
    return formatDistanceToNow(time, {
      locale: zhCN,
      addSuffix: true
    })
  }
})
</script>

<style></style>
