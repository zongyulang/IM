<template>
  <div class="main-view">
    <el-form ref="pwdRef" :model="user" :rules="rules" label-width="80px">
      <el-form-item label="旧密码" prop="oldPassword">
        <el-input
          v-model="user.oldPassword"
          placeholder="请输入旧密码"
          type="password"
          show-password
        />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="user.newPassword"
          placeholder="请输入新密码"
          type="password"
          show-password
        />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="user.confirmPassword"
          placeholder="请确认新密码"
          type="password"
          show-password
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">保存</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script lang="ts" setup>
import UserApi from '@renderer/api/UserApi'
import { getCurrentInstance, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useSysConfigStore } from '@renderer/store/sysConfigStore'

const { proxy } = getCurrentInstance()!
const sysConfigStore = useSysConfigStore()
onMounted(async () => {
  // Ensure config is loaded
  if (Object.keys(sysConfigStore.config).length === 0) {
    await sysConfigStore.loadData()
  }
})
const sysConfig = sysConfigStore.config
const user = reactive({
  oldPassword: undefined,
  newPassword: undefined,
  confirmPassword: undefined
})

const equalToPassword = (rule, value, callback) => {
  if (user.newPassword !== value) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}
const rules = reactive({
  oldPassword: [{ required: true, message: '旧密码不能为空', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '新密码不能为空', trigger: 'blur' },
    {
      pattern: new RegExp(sysConfig.passwordRegex),
      message: sysConfig.passwordRegexDesc,
      trigger: 'blur'
    },
    {
      validator: (rule, value, callback) => {
        if (value === user.oldPassword) {
          callback(new Error('新密码不能与旧密码相同'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '确认密码不能为空', trigger: 'blur' },
    { required: true, validator: equalToPassword, trigger: 'blur' }
  ]
})

/** 提交按钮 */
function submit() {
  proxy.$refs.pwdRef.validate((valid) => {
    if (valid) {
      UserApi.updateUserPwd(user.oldPassword, user.newPassword).then(() => {
        user.oldPassword = undefined
        user.newPassword = undefined
        user.confirmPassword = undefined
        ElMessage.success('修改成功')
      })
    }
  })
}
</script>

<style scoped>
.el-form-item {
  margin-bottom: 36px;
}

:deep(.el-form-item__error) {
  word-break: break-word;
  white-space: normal;
  padding-top: 4px;
}
</style>
