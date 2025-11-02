<template>
  <div v-loading="loading">
    <div class="header">
      <vim-avatar :size="'large'" :img="groupForm.avatar" />
    </div>
    <el-divider />
    <div style="width: 500px; margin: 0 auto">
      <el-form
        ref="ruleFormRef"
        style="font-size: 12px"
        :inline="false"
        :model="groupForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="群名称:" prop="name">
          <el-input v-model="groupForm.name"></el-input>
        </el-form-item>
        <el-form-item label="允许邀请:" prop="openInvite">
          <el-switch v-model="groupForm.openInvite"></el-switch>
        </el-form-item>
        <el-form-item label="邀请审核:" prop="inviteCheck">
          <el-switch v-model="groupForm.inviteCheck"></el-switch>
        </el-form-item>
        <el-form-item label="全体禁言:" prop="prohibition">
          <el-switch v-model="groupForm.prohibition"></el-switch>
        </el-form-item>
        <el-form-item label="禁加好友:" prop="prohibitFriend">
          <el-switch v-model="groupForm.prohibitFriend"></el-switch>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" :disabled="saving" @click="saveGroup">
            {{ saving ? '保存中...' : '保存' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import FetchRequest from '@renderer/api/FetchRequest'
import Auth from '@renderer/api/Auth'
import { useGroupStore } from '@renderer/store/groupStore'
import { useChatStore } from '@renderer/store/chatStore'
import { useRoute, useRouter } from 'vue-router'
import type { Ref } from 'vue'
import { reactive, ref } from 'vue'
import GroupApi from '@renderer/api/GroupApi'
import { ElForm, ElMessage } from 'element-plus'
import { useUserStore } from '@renderer/store/userStore'
import DictUtils from '@renderer/utils/DictUtils'
import VimAvatar from '@renderer/components/VimAvatar.vue'
import { storeToRefs } from 'pinia'
import type { User } from '@renderer/mode/User'

const route = useRoute()
const loading = ref(false)
const router = useRouter()
const groupStore = useGroupStore()
const ruleFormRef = ref<InstanceType<typeof ElForm>>()
const userStore = useUserStore()
const saving = ref(false)
const { user } = storeToRefs(userStore) as { user: Ref<User> }
const groupForm = reactive({
  id: '',
  master: '',
  name: '',
  openInvite: true,
  inviteCheck: false,
  prohibition: false,
  prohibitFriend: false,
  announcement: '',
  avatar: ''
})

const rules = reactive({
  name: [
    {
      required: true,
      message: '必填',
      trigger: 'blur'
    },
    {
      min: 3,
      max: 10,
      message: '长度介于3-10',
      trigger: 'blur'
    }
  ],
  announcement: [
    {
      min: 0,
      max: 100,
      message: '长度介于0-100',
      trigger: 'blur'
    }
  ]
})

const groupId = route.query.id
if (typeof groupId === 'string') {
  loading.value = true
  const loadGroup = async () => {
    try {
      const res = await GroupApi.get(groupId)
      groupForm.avatar = res.avatar
      groupForm.id = res.id
      groupForm.name = res.name
      groupForm.master = res.master
      groupForm.openInvite = res.openInvite === DictUtils.YES
      groupForm.inviteCheck = res.inviteCheck === DictUtils.YES
      groupForm.prohibition = res.prohibition === DictUtils.YES
      groupForm.prohibitFriend = res.prohibitFriend === DictUtils.YES
      groupForm.announcement = res.announcement
      vimData.isMaster = res.master === user.value?.id
    } catch (err) {
      ElMessage.error(err instanceof Error ? err.message : '加载群组失败')
    } finally {
      loading.value = false
    }
  }
  loadGroup()
}

const vimData = reactive({
  host: FetchRequest.getHost(),
  headers: {
    'sa-token': Auth.getToken()
  },
  data: {
    type: 'file'
  },
  isMaster: false
})

const saveGroup = async () => {
  if (saving.value) return

  try {
    const valid = await ruleFormRef.value?.validate()
    if (!valid) return

    saving.value = true
    if (groupForm.id) {
      await updateExistingGroup()
    } else {
      await createNewGroup()
    }
    ElMessage.success('保存成功')
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存错误')
  } finally {
    saving.value = false
  }
}

const updateExistingGroup = async () => {
  await GroupApi.update(
    groupForm.id,
    groupForm.name,
    groupForm.avatar,
    groupForm.openInvite ? DictUtils.YES : DictUtils.NO,
    groupForm.inviteCheck ? DictUtils.YES : DictUtils.NO,
    groupForm.prohibition ? DictUtils.YES : DictUtils.NO,
    groupForm.prohibitFriend ? DictUtils.YES : DictUtils.NO,
    groupForm.announcement
  )
  await useChatStore().updateChat(groupForm.id, groupForm.name, groupForm.avatar)
  await groupStore.loadData()
  await router.push('/index/group/' + groupForm.id)
}

const createNewGroup = async () => {
  const res = await GroupApi.save(
    groupForm.name,
    groupForm.avatar,
    groupForm.openInvite ? DictUtils.YES : DictUtils.NO,
    groupForm.inviteCheck ? DictUtils.YES : DictUtils.NO,
    groupForm.prohibition ? DictUtils.YES : DictUtils.NO,
    groupForm.prohibitFriend ? DictUtils.YES : DictUtils.NO,
    groupForm.announcement
  )
  groupStore.checkIndex = groupStore.groupList.length
  await groupStore.loadData()
  await router.push('/index/group/' + res.id)
}
</script>

<style scoped lang="less">
.header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.add-user {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 6rem;
  border: 1px #ccc solid;
  color: #ccc;

  i {
    font-size: 30px;
    line-height: 84px;
    width: 80px;
    height: 84px;
    text-align: center;
    border: 1px solid #cccccc;
  }
}

.user {
  position: relative;

  .close {
    position: absolute;
    top: 0;
    right: -8px;
    background-color: #ff0000;
    width: 16px;
    height: 16px;
    color: #ffffff;
    border-radius: 8px;

    .icon-v-close {
      font-size: 12px;
    }
  }
}
</style>
