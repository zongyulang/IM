<template>
  <!-- 使用 Element Plus 的 el-upload 组件进行文件上传 -->
  <el-upload
    :action="`${host}/vim/upload`"
    :headers="headers"
    :data="data"
    :show-file-list="false"
    :on-success="handleSuccess"
    :before-upload="beforeUpload"
  >
    <vim-avatar v-if="avatar" size="large" :img="avatar" />
    <i v-if="!avatar" class="iconfont icon-v-add"></i>
  </el-upload>
</template>

<script setup lang="ts">
import { computed, reactive, toRefs } from 'vue'
import FetchRequest from '@renderer/api/FetchRequest'
import Auth from '@renderer/api/Auth'
import VimAvatar from '@renderer/components/VimAvatar.vue'
import { compressAccurately, EImageType } from 'image-conversion'

// 定义上传数据接口
interface VimData {
  host: string
  headers: unknown
  data: unknown
}

// 定义组件属性接口
interface IProps {
  avatar: string
}

// 定义组件属性
const props = defineProps<IProps>()

// 计算属性，返回头像 URL
const avatar = computed(() => {
  return props.avatar
})

// 获取用户认证 token
const token = Auth.getToken()

// 定义上传数据
const vimData = reactive<VimData>({
  host: FetchRequest.getHost(), // 获取上传主机地址
  headers: {
    'sa-token': token // 设置请求头中的认证 token
  },
  data: {
    type: 'file' // 设置上传数据类型为文件
  }
})

// 定义事件发射器
const emits = defineEmits(['uploadSuccess'])

// 上传成功回调函数
const handleSuccess = (res) => {
  emits('uploadSuccess', res.data.url) // 发射上传成功事件，并传递上传后的图片 URL
}

// 上传前处理函数
const beforeUpload = async (file: File): Promise<File | Blob> => {
  try {
    // 压缩图片
    return await compressAccurately(file, {
      width: 100, // 设置压缩后的图片宽度
      height: 100, // 设置压缩后的图片高度
      orientation: 1, // 保持图片方向
      type: EImageType.JPEG, // 设置输出图片类型为 JPEG
      size: 10 // 设置输出图片大小为 10KB
    })
  } catch (error) {
    console.error('图片压缩失败:', error) // 打印错误信息
    throw new Error('图片处理失败，请重试') // 抛出错误
  }
}

// 解构上传数据
const { host, headers, data } = toRefs(vimData)
</script>

<style scoped>
.icon-v-add {
  font-size: 4rem; /* 设置图标的字体大小 */
  background-color: #cccccc; /* 设置图标的背景颜色 */
  padding: 1rem; /* 设置图标的内边距 */
}
</style>
