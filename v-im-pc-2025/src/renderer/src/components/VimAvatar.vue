<template>
  <div class="avatar-box">
    <el-avatar v-if="url" shape="square" :size="size" :src="url" fit="cover"></el-avatar>
    <el-avatar
      v-if="!url"
      shape="square"
      :size="size"
      fit="cover"
    >
      <template #default>
        <span style="font-size: 1.5rem">{{ start }}</span>
      </template>
    </el-avatar>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import FetchRequest from '@renderer/api/FetchRequest'

const host = ref(FetchRequest.getHost())
interface IProps {
  size?: 'default' | 'large' | 'small' | number
  img?: string
  name?: string
  isGroup?: boolean
}
const props = withDefaults(defineProps<IProps>(), {
  size: 'default' as 'default' | 'large' | 'small' | number,
  img: '',
  name: '',
  isGroup: false
})
const url = computed(() => {
  if (props.img?.indexOf('http') > -1 || props.img?.startsWith('data:')) {
    return props.img
  } else if (props.img) {
    return host.value + props.img
  } else {
    return undefined
  }
})
const start = computed(() => {
  return props.name ? props.name.slice(0, 2) : ''
})
</script>
<style lang="less" scoped>
.avatar-box {
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
}
</style>
