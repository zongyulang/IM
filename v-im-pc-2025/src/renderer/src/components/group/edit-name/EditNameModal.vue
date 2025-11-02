<template>
  <el-dialog v-model="open" title="修改群名称" width="40rem" center :show-close="false" :close-on-click-modal="false">
    <el-form v-if="group" ref="ruleFormRef" :model="groupForm" :rules="rules">
      <el-form-item prop="name">
        <el-input v-model="groupForm.name" placeholder="群名称"></el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button type="primary" :loading="saving" :disabled="saving" @click="saveGroup">
          {{ saving ? '保存中...' : '保存' }}
        </el-button>
        <el-button @click="close">关闭</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import GroupApi from '@renderer/api/GroupApi'
import type { Group } from '@renderer/mode/Group'
import { ElForm, ElMessage } from 'element-plus'
import { useGroupStore } from '@renderer/store/groupStore'
import { useChatStore } from '@renderer/store/chatStore'

const ruleFormRef = ref<InstanceType<typeof ElForm>>()
const groupForm = reactive({
  name: ''
})

const rules = reactive({
  name: [
    {
      required: true,
      message: '群名称不能为空',
      trigger: 'blur'
    },
    {
      min: 2,
      max: 10,
      message: '长度介于3-10个字符',
      trigger: 'blur'
    }
  ]
})

onMounted(() => {
  if (props.group) {
    groupForm.name = props.group.name
  }
})

defineEmits(['close'])
const open = ref(false)
interface IProps {
  group: Group
  closeDialog: () => void
}
const props = defineProps<IProps>()

if (props.group) {
  open.value = true
}

const close = () => {
  props.closeDialog()
}

const saving = ref(false)
const groupStore = useGroupStore()
const chatStore = useChatStore()

const saveGroup = async () => {
  if (!ruleFormRef.value) return

  try {
    await ruleFormRef.value.validate()
    if (!props.group) return
    saving.value = true
    await GroupApi.updateGroupName(props.group.id, groupForm.name)
    // 更新群组列表
    await groupStore.loadData()
    await groupStore.loadGroupData(props.group.id)
    // 更新聊天信息
    await chatStore.updateChat(props.group.id, groupForm.name, props.group.avatar)

    ElMessage.success('群名称修改成功')
    close()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="less">
.info {
  text-align: center;
  line-height: 200%;
}
</style>
