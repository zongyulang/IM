<template>
  <el-dialog v-model="open" width="40rem" center :show-close="false" :close-on-click-modal="false">
    <el-form v-if="group" ref="ruleFormRef" :model="groupForm" :rules="rules">
      <el-form-item prop="announcement">
        <el-input
          v-model="groupForm.announcement"
          type="textarea"
          :rows="5"
          placeholder="群公告"
        ></el-input>
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
import SendCode from '@renderer/enum/SendCode'
import ChatType from '@renderer/enum/ChatType'
import MessageType from '@renderer/enum/MessageType'
import { useWsStore } from '@renderer/store/WsStore'
import { useGroupStore } from '@renderer/store/groupStore'

const ruleFormRef = ref<InstanceType<typeof ElForm>>()
const groupForm = reactive({
  id: '',
  announcement: ''
})

const rules = reactive({
  announcement: [
    {
      min: 0,
      max: 100,
      message: '字数0-100字',
      trigger: 'blur'
    }
  ]
})
onMounted(() => {
  groupForm.announcement = props.group.announcement
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

const saveGroup = async () => {
  if (!ruleFormRef.value) return

  try {
    await ruleFormRef.value.validate()
    if (!props.group) return

    saving.value = true
    await GroupApi.update(
      props.group.id,
      props.group.name,
      props.group.avatar,
      props.group.openInvite,
      props.group.inviteCheck,
      props.group.prohibition,
      props.group.prohibitFriend,
      groupForm.announcement
    )

    await useGroupStore().loadGroupData(props.group.id)

    const sendInfo = {
      code: SendCode.MESSAGE,
      message: {
        mine: true,
        fromId: props.group.master,
        chatId: props.group.id,
        chatType: ChatType.GROUP,
        messageType: MessageType.TEXT,
        content: `@[所有人] 群公告：${groupForm.announcement}`,
        timestamp: new Date().getTime(),
        extend: {
          atAll: true
        }
      }
    }
    useWsStore().send(JSON.stringify(sendInfo))

    ElMessage.success('保存成功')
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

.description {
  padding: 20px 20px 0px 20px;
  background-color: #ffffff;
}
.gongago {
  margin-right: 10px;
  font-weight: bold;
}
</style>
