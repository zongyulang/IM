<template>
  <el-tag :type="tagInfo.type" size="small">{{ tagInfo.label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { withDefaults } from 'vue'

interface Option {
  label: string
  value: number | string
  type?: string
}

interface Props {
  options: Option[]
  value: number | string
  className?: string
}

const props = withDefaults(defineProps<Props>(), {
  className: 'dict-tag'
})

// 计算标签显示的文本和类型
const tagInfo = computed(() => {
  const option = props.options.find((opt) => opt.value === props.value)
  if (!option) {
    return {
      label: props.value?.toString() || '',
      type: 'info'
    }
  }

  // 如果没有指定类型，根据value设置默认类型
  let type = option.type
  if (!type) {
    switch (option.value) {
      case 0:
        type = 'success' // 成功状态使用绿色
        break
      case 1:
        type = 'danger' // 失败状态使用红色
        break
      default:
        type = 'info' // 其它状态使用灰色
    }
  }

  return {
    label: option.label,
    type
  }
})
</script>

<style scoped>
.dict-tag {
  display: inline-block;
  padding: 0 10px;
  font-size: 12px;
  border-radius: 4px;
  color: #fff;
  line-height: 24px;
}
</style>
